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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
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
            "https://raw.githubusercontent.com/Heldyy90/HubSwap/main/announcement.json";
    private static final long CHECK_INTERVAL_MS = 10_000L;
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HubSwap Remote Notice");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean CHECK_IN_FLIGHT = new AtomicBoolean(false);
    private static final Set<String> SESSION_SUPPRESSED = Collections.synchronizedSet(new HashSet<>());

    private static volatile boolean wasInHub = false;
    private static volatile long lastCheckAt = 0L;
    private static volatile String displayedFingerprintThisHub = "";
    private static volatile RemoteNotice currentNotice = null;

    private UpdateChecker() {
    }

    public static void onServerJoin() {
        wasInHub = false;
        lastCheckAt = 0L;
        displayedFingerprintThisHub = "";
        currentNotice = null;
    }

    public static void onDisconnect() {
        wasInHub = false;
        lastCheckAt = 0L;
        displayedFingerprintThisHub = "";
        currentNotice = null;
    }

    public static void checkAfterJoin() {
        onServerJoin();
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null) {
            wasInHub = false;
            return;
        }

        boolean inHub = TransitionDetector.isInHub(client);
        long now = System.currentTimeMillis();

        if (!inHub) {
            wasInHub = false;
            displayedFingerprintThisHub = "";
            return;
        }

        boolean justEnteredHub = !wasInHub;
        wasInHub = true;

        if (justEnteredHub) {
            displayedFingerprintThisHub = "";
            requestCheck(true);
            return;
        }

        if (now - lastCheckAt >= CHECK_INTERVAL_MS) {
            requestCheck(false);
        }
    }

    public static void forceCheck() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

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
        if (client.player == null || !TransitionDetector.isInHub(client)) {
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
        String raw = request(REMOTE_NOTICE_URL + "?hs=" + System.currentTimeMillis());
        if (raw == null || raw.isBlank()) return null;

        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        boolean enabled = getBoolean(json, "enabled", true);
        String type = getString(json, "type", "announcement").toLowerCase(Locale.ROOT);
        String id = getString(json, "id", "notice");
        int revision = getInt(json, "revision", 1);
        String title = getString(json, "title", type.equals("update") ? "Доступно обновление HubSwap" : "Объявление HubSwap");
        String version = getString(json, "version", "");
        String downloadUrl = getString(json, "downloadUrl", "");
        boolean downloadButton = json.has("downloadButton")
                ? getBoolean(json, "downloadButton", false)
                : type.equals("update") && !downloadUrl.isBlank();

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
        if (line.length() > 220) line = line.substring(0, 220);
        result.add(line);
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
        if (notice.downloadButton() && !notice.downloadUrl().isBlank()) {
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

    private static void openInBrowser(URI uri) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
            return;
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri.toString()).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", uri.toString()).start();
        } else {
            new ProcessBuilder("xdg-open", uri.toString()).start();
        }
    }

    private static String request(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setUseCaches(false);
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(4000);
        connection.setRequestProperty("User-Agent", "HubSwap/1.0.8 (+https://github.com/Heldyy90/HubSwap)");
        connection.setRequestProperty("Accept", "application/json,text/plain,*/*");
        connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0");
        connection.setRequestProperty("Pragma", "no-cache");

        int code = connection.getResponseCode();
        if (code != 200) return null;

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
        } finally {
            connection.disconnect();
        }

        return response.toString();
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
