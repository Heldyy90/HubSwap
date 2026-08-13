package ru.heldyy.hubswap.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.gui.TransitionDetector;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker {
    private static final String MOD_ID = "hubswap";
    private static final String REMOTE_NOTICE_URL =
            "https://raw.githubusercontent.com/HolyWorldWEB/HubSwap/main/announcement.json";
    private static final String REMOTE_NOTICE_SIGNATURE_URL = REMOTE_NOTICE_URL + ".sig";

    // X.509 SubjectPublicKeyInfo, Ed25519. Private key is intentionally NOT shipped with the mod.
    private static final String NOTICE_PUBLIC_KEY_X509_B64 =
            "MCowBQYDK2VwAyEAA/cE2RYv+6fJsoYaHYVVCRqsNXaN0i8bF5MGtI4Q2F8=";

    private static final long CHECK_INTERVAL_MS = 300_000L;
    private static final long FIRST_CHECK_DELAY_MS = 30_000L;
    private static final int MAX_NOTICE_BYTES = 64 * 1024;
    private static final int MAX_SIGNATURE_BYTES = 1024;
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private static final Set<String> ALLOWED_DOWNLOAD_HOSTS = Set.of(
            "github.com",
            "raw.githubusercontent.com",
            "objects.githubusercontent.com",
            "github-releases.githubusercontent.com",
            "release-assets.githubusercontent.com"
    );

    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HubSwap Remote Notice");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean CHECK_IN_FLIGHT = new AtomicBoolean(false);
    private static final Set<String> SESSION_SUPPRESSED = Collections.synchronizedSet(new HashSet<>());

    private static volatile boolean wasInHub = false;
    private static volatile long hubEnteredAt = 0L;
    private static volatile long lastCheckAt = 0L;
    private static volatile String displayedFingerprintThisHub = "";
    private static volatile RemoteNotice currentNotice = null;

    private UpdateChecker() {
    }

    public static void onServerJoin() {
        resetSessionState();
    }

    public static void onDisconnect() {
        resetSessionState();
    }

    public static void checkAfterJoin() {
        onServerJoin();
    }

    private static void resetSessionState() {
        wasInHub = false;
        hubEnteredAt = 0L;
        lastCheckAt = 0L;
        displayedFingerprintThisHub = "";
        currentNotice = null;
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null) {
            wasInHub = false;
            hubEnteredAt = 0L;
            return;
        }

        if (!HubSwap.getConfig().isRemoteNoticesEnabled()) {
            wasInHub = false;
            hubEnteredAt = 0L;
            return;
        }

        boolean inHub = TransitionDetector.isInHub(client);
        long now = System.currentTimeMillis();

        if (!inHub) {
            wasInHub = false;
            hubEnteredAt = 0L;
            displayedFingerprintThisHub = "";
            return;
        }

        boolean justEnteredHub = !wasInHub;
        wasInHub = true;

        if (justEnteredHub) {
            hubEnteredAt = now;
            lastCheckAt = 0L;
            displayedFingerprintThisHub = "";
            return;
        }

        if (lastCheckAt == 0L) {
            if (now - hubEnteredAt >= FIRST_CHECK_DELAY_MS) {
                requestCheck(true);
            }
            return;
        }

        if (now - lastCheckAt >= CHECK_INTERVAL_MS) {
            requestCheck(false);
        }
    }

    public static void forceCheck() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!HubSwap.getConfig().isRemoteNoticesEnabled()) {
            sendInfo("Удалённые объявления отключены в настройках.");
            return;
        }

        if (!TransitionDetector.isInHub(client)) {
            sendInfo("Проверка объявления выполняется в Hub.");
            return;
        }

        lastCheckAt = 0L;
        requestCheck(true);
    }

    public static void hideCurrentNotice() {
        RemoteNotice notice = currentNotice;
        if (notice == null || notice.fingerprint().isBlank()) {
            sendInfo("Сейчас нет активного объявления.");
            return;
        }

        HubSwap.getConfig().setHiddenRemoteNoticeFingerprint(notice.fingerprint());
        HubSwap.saveConfig();
        displayedFingerprintThisHub = notice.fingerprint();
        sendInfo("Это уведомление больше не будет показываться. Новое сообщение появится автоматически.");
    }

    public static void downloadCurrentNotice() {
        RemoteNotice notice = currentNotice;
        if (notice == null || notice.downloadUrl().isBlank()) {
            sendInfo("Ссылка на загрузку недоступна.");
            return;
        }

        if (!isSafeDownloadUrl(notice.downloadUrl())) {
            sendInfo("Ссылка загрузки заблокирована: недопустимый URL.");
            return;
        }

        SESSION_SUPPRESSED.add(notice.fingerprint());
        displayedFingerprintThisHub = notice.fingerprint();

        try {
            openInBrowser(URI.create(notice.downloadUrl()));
            sendInfo("Открыта страница загрузки. Уведомление скрыто до перезапуска Minecraft.");
        } catch (Exception e) {
            SESSION_SUPPRESSED.remove(notice.fingerprint());
            sendInfo("Не удалось открыть ссылку загрузки.");
        }
    }

    private static void requestCheck(boolean immediate) {
        long now = System.currentTimeMillis();
        if (!immediate && now - lastCheckAt < CHECK_INTERVAL_MS) {
            return;
        }
        if (!CHECK_IN_FLIGHT.compareAndSet(false, true)) {
            return;
        }

        lastCheckAt = now;
        NETWORK.execute(() -> {
            try {
                RemoteNotice notice = fetchRemoteNotice();
                if (notice == null) return;

                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> handleRemoteNotice(client, notice));
            } catch (Exception ignored) {
            } finally {
                CHECK_IN_FLIGHT.set(false);
            }
        });
    }

    private static void handleRemoteNotice(MinecraftClient client, RemoteNotice notice) {
        if (client.player == null
                || !HubSwap.getConfig().isRemoteNoticesEnabled()
                || !TransitionDetector.isInHub(client)) {
            return;
        }

        if (!notice.enabled()) {
            return;
        }

        if (notice.isUpdate() && !notice.version().isBlank()) {
            String currentVersion = getCurrentVersion();
            if (!isNewerVersion(notice.version(), currentVersion)) {
                return;
            }
        }

        String hidden = HubSwap.getConfig().getHiddenRemoteNoticeFingerprint();
        if (notice.fingerprint().equals(hidden)) {
            return;
        }
        if (SESSION_SUPPRESSED.contains(notice.fingerprint())) {
            return;
        }
        if (notice.fingerprint().equals(displayedFingerprintThisHub)) {
            return;
        }

        currentNotice = notice;
        displayedFingerprintThisHub = notice.fingerprint();
        sendRemoteNotice(notice);
    }

    private static RemoteNotice fetchRemoteNotice() throws Exception {
        byte[] body = requestBytes(REMOTE_NOTICE_URL, MAX_NOTICE_BYTES, "application/json,text/plain,*/*");
        if (body == null || body.length == 0) return null;

        byte[] signatureBody = requestBytes(
                REMOTE_NOTICE_SIGNATURE_URL,
                MAX_SIGNATURE_BYTES,
                "text/plain,*/*"
        );
        if (signatureBody == null || signatureBody.length == 0) return null;

        String signatureB64 = new String(signatureBody, StandardCharsets.US_ASCII).trim();
        if (!verifySignature(body, signatureB64)) {
            return null; // fail closed: unsigned or modified notices are never trusted
        }

        JsonObject json = JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
        boolean enabled = getBoolean(json, "enabled", true);
        String type = getString(json, "type", "announcement").toLowerCase(Locale.ROOT);
        if (!type.equals("announcement") && !type.equals("update")) {
            type = "announcement";
        }

        String id = limit(getString(json, "id", "notice"), 64);
        int revision = Math.max(0, Math.min(1_000_000, getInt(json, "revision", 1)));
        String title = limit(
                getString(json, "title", type.equals("update") ? "Доступно обновление HubSwap" : "Объявление HubSwap"),
                64
        );
        String version = limit(getString(json, "version", ""), 16);
        String downloadUrl = getString(json, "downloadUrl", "").trim();
        if (!isSafeDownloadUrl(downloadUrl)) {
            downloadUrl = "";
        }

        boolean downloadButton = json.has("downloadButton")
                ? getBoolean(json, "downloadButton", false)
                : type.equals("update") && !downloadUrl.isBlank();
        downloadButton = downloadButton && !downloadUrl.isBlank();

        List<String> lines = readLines(json);
        if (lines.isEmpty() && type.equals("update") && !version.isBlank()) {
            lines.add("Доступна новая версия: " + version);
        }

        String canonical = enabled + "|" + type + "|" + id + "|" + revision + "|" + title + "|"
                + String.join("\\n", lines) + "|" + version + "|" + downloadUrl + "|" + downloadButton;
        String fingerprint = sha256(canonical);

        return new RemoteNotice(
                enabled,
                type,
                id,
                revision,
                title,
                lines,
                version,
                downloadUrl,
                downloadButton,
                fingerprint
        );
    }

    private static boolean verifySignature(byte[] body, String signatureB64) {
        if (body == null || signatureB64 == null || signatureB64.isBlank()) return false;
        try {
            byte[] publicKeyBytes = Base64.getDecoder().decode(NOTICE_PUBLIC_KEY_X509_B64);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureB64.replaceAll("\\s+", ""));

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(body);
            return verifier.verify(signatureBytes);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static List<String> readLines(JsonObject json) {
        List<String> result = new ArrayList<>();

        if (json.has("lines") && json.get("lines").isJsonArray()) {
            JsonArray array = json.getAsJsonArray("lines");
            for (JsonElement element : array) {
                if (result.size() >= 8) break;
                if (element == null || element.isJsonNull()) continue;
                addLine(result, element.getAsString());
            }
        } else {
            String text = getString(json, "text", "");
            if (!text.isBlank()) {
                for (String line : text.split("\\R")) {
                    if (result.size() >= 8) break;
                    addLine(result, line);
                }
            }
        }

        return result;
    }

    private static void addLine(List<String> result, String raw) {
        if (raw == null) return;
        String line = raw.trim();
        if (line.isEmpty()) return;
        result.add(limit(line, 220));
    }

    private static String limit(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static void sendRemoteNotice(RemoteNotice notice) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Formatting theme = HubSwap.getConfig().getColorTheme().getFormatting();

        Text header = Text.literal("[HubSwap] ").formatted(theme, Formatting.BOLD)
                .append(Text.literal(notice.title()).formatted(Formatting.WHITE, Formatting.BOLD));
        if (notice.isUpdate() && !notice.version().isBlank()) {
            header = header.copy()
                    .append(Text.literal(" "))
                    .append(Text.literal(notice.version()).formatted(Formatting.GREEN, Formatting.BOLD));
        }
        client.player.sendMessage(header, false);

        for (String line : notice.lines()) {
            client.player.sendMessage(
                    Text.literal("  • ").formatted(theme)
                            .append(Text.literal(line).formatted(Formatting.WHITE)),
                    false
            );
        }

        Text buttons = Text.empty();
        if (notice.downloadButton() && isSafeDownloadUrl(notice.downloadUrl())) {
            buttons = buttons.copy().append(Text.literal("[СКАЧАТЬ]")
                    .styled(style -> style
                            .withColor(Formatting.AQUA)
                            .withBold(true)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent.RunCommand("/hsdownload"))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Text.literal("Открыть загрузку HubSwap")
                            ))));
            buttons = buttons.copy().append(Text.literal("  "));
        }

        buttons = buttons.copy().append(Text.literal("[СКРЫТЬ УВЕДОМЛЕНИЕ]")
                .styled(style -> style
                        .withColor(Formatting.GRAY)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.RunCommand("/hshide"))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Не показывать именно это сообщение")
                        ))));

        client.player.sendMessage(Text.literal("  ").append(buttons), false);
    }

    private static void sendInfo(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (client.player == null) return;
            Formatting theme = HubSwap.getConfig().getColorTheme().getFormatting();
            client.player.sendMessage(
                    Text.literal("[HubSwap] ").formatted(theme, Formatting.BOLD)
                            .append(Text.literal(message).formatted(Formatting.WHITE)),
                    false
            );
        });
    }

    private static boolean isSafeDownloadUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI uri = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) return false;
            if (uri.getUserInfo() != null) return false;
            if (uri.getPort() != -1 && uri.getPort() != 443) return false;

            String host = uri.getHost();
            if (host == null) return false;
            return ALLOWED_DOWNLOAD_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void openInBrowser(URI uri) throws Exception {
        if (uri == null || !isSafeDownloadUrl(uri.toString())) {
            throw new IllegalArgumentException("Unsafe download URI");
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new UnsupportedOperationException("Desktop browser integration is unavailable");
        }

        Desktop.getDesktop().browse(uri);
    }

    private static byte[] requestBytes(String apiUrl, int maxBodyBytes, String accept) throws Exception {
        URI uri = URI.create(apiUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"raw.githubusercontent.com".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Unexpected remote notice endpoint");
        }

        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(4000);
        connection.setRequestProperty("User-Agent", "HubSwap (+https://github.com/HolyWorldWEB/HubSwap)");
        connection.setRequestProperty("Accept", accept);
        connection.setRequestProperty("Cache-Control", "no-cache");

        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) return null;

            long declaredLength = connection.getContentLengthLong();
            if (declaredLength > maxBodyBytes) return null;

            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maxBodyBytes, 8192))) {
                byte[] buffer = new byte[4096];
                int read;
                int total = 0;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > maxBodyBytes) {
                        return null;
                    }
                    output.write(buffer, 0, read);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String getCurrentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static boolean isNewerVersion(String latest, String current) {
        int[] a = parseVersion(latest);
        int[] b = parseVersion(current);
        for (int i = 0; i < 3; i++) {
            if (a[i] > b[i]) return true;
            if (a[i] < b[i]) return false;
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        int[] parts = {0, 0, 0};
        if (version == null) return parts;

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) return parts;

        for (int i = 1; i <= 3; i++) {
            String group = matcher.group(i);
            if (group == null || group.isBlank()) continue;
            try {
                parts[i - 1] = Integer.parseInt(group);
            } catch (NumberFormatException ignored) {
            }
        }
        return parts;
    }

    private static String getString(JsonObject json, String key, String fallback) {
        try {
            if (!json.has(key) || json.get(key).isJsonNull()) return fallback;
            return json.get(key).getAsString();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        try {
            if (!json.has(key) || json.get(key).isJsonNull()) return fallback;
            return json.get(key).getAsBoolean();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        try {
            if (!json.has(key) || json.get(key).isJsonNull()) return fallback;
            return json.get(key).getAsInt();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private record RemoteNotice(
            boolean enabled,
            String type,
            String id,
            int revision,
            String title,
            List<String> lines,
            String version,
            String downloadUrl,
            boolean downloadButton,
            String fingerprint
    ) {
        boolean isUpdate() {
            return "update".equalsIgnoreCase(type);
        }
    }
}
