package ru.heldyy.hubswap.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class UpdateChecker {
    private static final String MOD_ID = "hubswap";

    private static final String GITHUB_API =
            "https://api.github.com/repos/HolyWorldWEB/HubSwap/releases/latest";

    private static final String REPO_URL =
            "https://github.com/HolyWorldWEB/HubSwap/releases/latest";

    private static boolean checked = false;

    public static synchronized void checkAfterJoin() {
        if (checked) return;
        checked = true;

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3500);

                String currentVersion = normalizeVersion(getCurrentVersion());
                ReleaseInfo latest = getLatestRelease();
                if (latest == null || latest.tagName == null) return;

                String latestVersion = normalizeVersion(latest.tagName);
                if (isNewerVersion(latestVersion, currentVersion)) {
                    sendUpdateMessage(latest.tagName, safeReleaseUri(latest.htmlUrl));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[HubSwap] Update check failed: " + e.getMessage());
            }
        }, "HubSwap Update Checker");
        thread.setDaemon(true);
        thread.start();
    }

    private static String getCurrentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static ReleaseInfo getLatestRelease() throws Exception {
        URL url = new URL(GITHUB_API);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "HubSwap-UpdateChecker");
            connection.setRequestProperty("Accept", "application/vnd.github+json");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                consumeQuietly(connection.getErrorStream());
                return null;
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            String tagName = json.has("tag_name") && !json.get("tag_name").isJsonNull()
                    ? json.get("tag_name").getAsString() : null;
            String htmlUrl = json.has("html_url") && !json.get("html_url").isJsonNull()
                    ? json.get("html_url").getAsString() : REPO_URL;

            return new ReleaseInfo(tagName, safeReleaseUri(htmlUrl).toString());
        } finally {
            connection.disconnect();
        }
    }

    private static void consumeQuietly(InputStream stream) {
        if (stream == null) return;
        try (InputStream ignored = stream) {
            byte[] buffer = new byte[1024];
            while (ignored.read(buffer) != -1) { }
        } catch (Exception ignored) { }
    }

    private static void sendUpdateMessage(String latestVersion, URI uri) {
        MinecraftClient client = MinecraftClient.getInstance();

        client.execute(() -> {
            if (client.player == null) return;

            Text prefix = Text.literal("[HubSwap] ")
                    .formatted(Formatting.AQUA, Formatting.BOLD);

            Text message = Text.literal("Вышла новая версия мода: ")
                    .formatted(Formatting.WHITE)
                    .append(Text.literal(latestVersion).formatted(Formatting.GREEN))
                    .append(Text.literal("  "));

            Text link = Text.literal("[СКАЧАТЬ]")
                    .styled(style -> style
                            .withColor(Formatting.AQUA)
                            .withUnderline(true)
                            .withClickEvent(new ClickEvent.OpenUrl(uri))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    Text.literal("Открыть страницу релиза HubSwap")
                            ))
                    );

            client.player.sendMessage(prefix.copy().append(message).append(link), false);
        });
    }

    private static URI safeReleaseUri(String value) {
        URI fallback = URI.create(REPO_URL);
        if (value == null || value.isBlank()) return fallback;
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) return fallback;
            String host = uri.getHost();
            if (host == null || !host.equalsIgnoreCase("github.com")) return fallback;
            String path = uri.getPath();
            if (path == null || !path.startsWith("/HolyWorldWEB/HubSwap/")) return fallback;
            return uri;
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String normalizeVersion(String version) {
        if (version == null) return "0.0.0";

        String clean = version.trim().toLowerCase(Locale.ROOT).replace("version", "");
        int metadata = firstMetadataSeparator(clean);
        if (metadata >= 0) clean = clean.substring(0, metadata);

        clean = clean
                .replaceAll("[^0-9.]", "")
                .replaceAll("^\\.+", "")
                .replaceAll("\\.+$", "")
                .trim();

        return clean.isEmpty() ? "0.0.0" : clean;
    }

    private static int firstMetadataSeparator(String value) {
        int plus = value.indexOf('+');
        int dash = value.indexOf('-');
        if (plus < 0) return dash;
        if (dash < 0) return plus;
        return Math.min(plus, dash);
    }

    private static boolean isNewerVersion(String latest, String current) {
        int[] latestParts = parseVersion(latest);
        int[] currentParts = parseVersion(current);

        for (int i = 0; i < 3; i++) {
            if (latestParts[i] > currentParts[i]) return true;
            if (latestParts[i] < currentParts[i]) return false;
        }
        return false;
    }

    private static int[] parseVersion(String normalizedVersion) {
        int[] result = new int[]{0, 0, 0};
        if (normalizedVersion == null) return result;

        String[] parts = normalizedVersion.split("\\.", -1);
        for (int i = 0; i < result.length && i < parts.length; i++) {
            String part = parts[i];
            if (part == null || part.isBlank()) continue;
            try {
                result[i] = Integer.parseInt(part);
            } catch (NumberFormatException ignored) { }
        }
        return result;
    }

    private record ReleaseInfo(String tagName, String htmlUrl) { }
}
