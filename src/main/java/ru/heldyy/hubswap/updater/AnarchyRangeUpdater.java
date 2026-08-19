package ru.heldyy.hubswap.updater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.AnarchyRanges;
import ru.heldyy.hubswap.config.ModConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AnarchyRangeUpdater {
    public static final long REFRESH_INTERVAL_MS = 15L * 60L * 1000L;
    public static final String REMOTE_URL =
            "https://raw.githubusercontent.com/Heldyy90/HubSwap/main/anarchy_ranges.json";

    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "HubSwap-AnarchyRanges");
        thread.setDaemon(true);
        return thread;
    });

    private static final AtomicBoolean CHECKING = new AtomicBoolean(false);
    private static volatile long nextCheckAt = 0L;
    private static volatile long lastSuccessfulCheckAt = 0L;

    private AnarchyRangeUpdater() {
    }

    public static void onClientTick(MinecraftClient client) {
        ModConfig config = HubSwap.getConfig();
        if (!config.isRemoteRangesEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextCheckAt || !CHECKING.compareAndSet(false, true)) {
            return;
        }

        nextCheckAt = now + REFRESH_INTERVAL_MS;
        EXECUTOR.execute(() -> fetchAndApply(client));
    }

    public static long getLastSuccessfulCheckAt() {
        return lastSuccessfulCheckAt;
    }

    private static void fetchAndApply(MinecraftClient client) {
        try {
            AnarchyRanges remote = downloadRanges();
            if (remote == null || !remote.isValid()) {
                return;
            }

            AnarchyRanges safeCopy = remote.copy();
            client.execute(() -> {
                ModConfig config = HubSwap.getConfig();
                if (!config.isRemoteRangesEnabled()) {
                    return;
                }

                if (!safeCopy.equals(config.getAnarchyRanges())) {
                    config.setAnarchyRanges(safeCopy);
                    HubSwap.saveConfig();
                }
                lastSuccessfulCheckAt = System.currentTimeMillis();
            });
        } catch (Exception ignored) {
            
        } finally {
            CHECKING.set(false);
        }
    }

    private static AnarchyRanges downloadRanges() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(REMOTE_URL).toURL().openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("User-Agent", "HubSwap/1.0.8");

        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                return GSON.fromJson(reader, AnarchyRanges.class);
            }
        } finally {
            connection.disconnect();
        }
    }
}
