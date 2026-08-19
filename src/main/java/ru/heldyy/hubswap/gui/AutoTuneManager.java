package ru.heldyy.hubswap.gui;

import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;

public final class AutoTuneManager {
    private static final long WARMUP_AFTER_JOIN_MS = 30_000L;
    private static final long WARMUP_ATTEMPTS_MAX_AGE_MS = 120_000L;
    private static final int WARMUP_ATTEMPTS = 3;
    private static final int COLD_START_MIN_CLICK_DELAY = 230;

    private static long lastServerJoinAt = 0L;
    private static int warmupAttemptsLeft = 0;
    private static boolean connected = false;
    private static boolean sessionWarmupStarted = false;

    private AutoTuneManager() {
    }

    public static void onServerJoin() {
        connected = true;

        if (!sessionWarmupStarted) {
            sessionWarmupStarted = true;
            lastServerJoinAt = System.currentTimeMillis();
            warmupAttemptsLeft = WARMUP_ATTEMPTS;
        }
    }

    public static void onServerDisconnect() {
        connected = false;

    }

    public static boolean isWarmupActive() {
        if (!connected || lastServerJoinAt <= 0L) {
            return false;
        }

        long age = System.currentTimeMillis() - lastServerJoinAt;
        return age < WARMUP_AFTER_JOIN_MS
                || (warmupAttemptsLeft > 0 && age < WARMUP_ATTEMPTS_MAX_AGE_MS);
    }

    private static boolean consumeWarmupLearningPause() {
        if (!isWarmupActive()) {
            return false;
        }

        if (warmupAttemptsLeft > 0) {
            warmupAttemptsLeft--;
        }

        return true;
    }

    public static Delays getLiveDelays(ModConfig config) {
        return getLiveDelays(config, TransitionMode.CLASSIC);
    }

    public static Delays getLiveDelays(ModConfig config, TransitionMode mode) {
        boolean liteMode = mode == TransitionMode.LIGHT || mode == TransitionMode.LIGHT120 || mode == TransitionMode.PRIME;

        if (!config.isSmartAutoTuneEnabled()) {
            return new Delays(
                    liteMode ? 0 : config.getClassicDelay(),
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

        int hubDelay = liteMode ? 0 : 400 + safePing * 3 + jitter;

        int clickDelay = config.getAutoTunedClickDelay();

        if (samples < 5) {
            if (!liteMode) {
                hubDelay += 150;
            }
        }

        if (fps > 0 && fps < 60) {
            if (!liteMode) {
                hubDelay += 80;
            }
        }
        if (fps > 0 && fps < 30) {
            if (!liteMode) {
                hubDelay += 150;
            }
            
            clickDelay += 25;
        }

        if (isWarmupActive()) {
            clickDelay = Math.max(clickDelay, COLD_START_MIN_CLICK_DELAY);
        }

        if (!liteMode) {

            hubDelay += Math.min(config.getLearnedHubOffset(), 1200);
        }

        int confirmDelay = 2800 + jitter * 4 + config.getFailStreak() * 90;
        if (fps > 0 && fps < 30) {
            confirmDelay += 150;
        }

        hubDelay = liteMode ? 0 : clamp(hubDelay, 400, 3500);
        clickDelay = liteMode ? clamp(clickDelay, 20, 700) : clamp(clickDelay, 20, 1000);
        confirmDelay = clamp(confirmDelay, 2250, 4500);

        return new Delays(hubDelay, clickDelay, confirmDelay, ping, avgPing, jitter, fps, samples);
    }

    public static void recordSuccess() {
        recordSuccess(TransitionMode.CLASSIC);
    }

    public static void recordSuccess(TransitionMode mode) {
        ModConfig config = HubSwap.getConfig();
        if (!config.isSmartAutoTuneEnabled()) {
            return;
        }

        if (consumeWarmupLearningPause()) {

            HubSwap.saveConfig();
            return;
        }

        config.recordSuccess(shouldLearnHubOffset(mode));
        HubSwap.saveConfig();
    }

    public static void recordFailure() {
        recordFailure(TransitionMode.CLASSIC);
    }

    public static void recordFailure(TransitionMode mode) {
        ModConfig config = HubSwap.getConfig();
        if (!config.isSmartAutoTuneEnabled()) {
            return;
        }

        if (consumeWarmupLearningPause()) {

            HubSwap.saveConfig();
            return;
        }

        config.recordFailure(shouldLearnHubOffset(mode));
        HubSwap.saveConfig();
    }

    private static boolean shouldLearnHubOffset(TransitionMode mode) {
        return mode == TransitionMode.CLASSIC;
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
