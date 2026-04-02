package ru.heldyy.hubswap.gui;

import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;

public final class AutoTuneManager {
    private AutoTuneManager() {
    }

    public static Delays getLiveDelays(ModConfig config) {
        if (!config.isSmartAutoTuneEnabled()) {
            return new Delays(
                    config.getClassicDelay(),
                    config.getClickDelay(),
                    3000,
                    -1,
                    -1,
                    0,
                    -1,
                    0
            );
        }

        int ping = MinecraftStatsHelper.getApproxPing();
        int avgPing = MinecraftStatsHelper.getAveragePing();
        int jitter = MinecraftStatsHelper.getJitter();
        int fps = MinecraftStatsHelper.getApproxFps();
        int samples = MinecraftStatsHelper.getSampleCount();

        int safePing = avgPing > 0 ? avgPing : (ping > 0 ? ping : 0);

        int hubDelay = 400 + safePing * 3 + jitter;
        int clickDelay = 40 + safePing / 6 + jitter / 6;

        if (samples < 5) {
            hubDelay += 150;
            clickDelay += 20;
        }

        if (fps > 0 && fps < 60) {
            hubDelay += 80;
            clickDelay += 10;
        }
        if (fps > 0 && fps < 30) {
            hubDelay += 150;
            clickDelay += 20;
        }

        hubDelay += config.getLearnedHubOffset();
        clickDelay += config.getLearnedClickOffset();

        int confirmDelay = 2800 + jitter * 4 + config.getFailStreak() * 90;
        if (fps > 0 && fps < 30) {
            confirmDelay += 150;
        }

        hubDelay = clamp(hubDelay, 400, 5000);
        clickDelay = clamp(clickDelay, 40, 1000);
        confirmDelay = clamp(confirmDelay, 2250, 4500);

        return new Delays(hubDelay, clickDelay, confirmDelay, ping, avgPing, jitter, fps, samples);
    }

    public static void recordSuccess() {
        ModConfig config = HubSwap.getConfig();
        if (!config.isSmartAutoTuneEnabled()) {
            return;
        }

        config.recordSuccess();
        HubSwap.saveConfig();
    }

    public static void recordFailure() {
        ModConfig config = HubSwap.getConfig();
        if (!config.isSmartAutoTuneEnabled()) {
            return;
        }

        config.recordFailure();
        HubSwap.saveConfig();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Delays(
            int hubDelay,
            int clickDelay,
            int confirmDelay,
            int ping,
            int avgPing,
            int jitter,
            int fps,
            int samples
    ) {
    }
}
