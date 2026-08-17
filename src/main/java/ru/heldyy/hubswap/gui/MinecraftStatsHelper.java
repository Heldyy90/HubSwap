package ru.heldyy.hubswap.gui;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

public final class MinecraftStatsHelper {
    private static final int MAX_SAMPLES = 15;
    private static final Deque<Integer> PING_SAMPLES = new ArrayDeque<>();

    private static int lastKnownPing = -1;
    private static long lastPollAt = 0L;

    private MinecraftStatsHelper() {
    }

    public static void onClientTick() {
        long now = System.currentTimeMillis();
        if (now - lastPollAt < 500L) {
            return;
        }
        lastPollAt = now;

        int ping = readPing();
        if (ping >= 0) {
            lastKnownPing = ping;
            PING_SAMPLES.addLast(ping);
            while (PING_SAMPLES.size() > MAX_SAMPLES) {
                PING_SAMPLES.removeFirst();
            }
        }
    }

    public static int getApproxPing() {
        int ping = readPing();
        if (ping >= 0) {
            lastKnownPing = ping;
            PING_SAMPLES.addLast(ping);
            while (PING_SAMPLES.size() > MAX_SAMPLES) {
                PING_SAMPLES.removeFirst();
            }
            return ping;
        }
        return lastKnownPing;
    }

    public static int getAveragePing() {
        if (PING_SAMPLES.isEmpty()) {
            return getApproxPing();
        }

        int sum = 0;
        int count = 0;
        for (int value : PING_SAMPLES) {
            if (value >= 0) {
                sum += value;
                count++;
            }
        }
        return count > 0 ? sum / count : -1;
    }

    public static int getJitter() {
        if (PING_SAMPLES.isEmpty()) {
            return 0;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        boolean found = false;

        for (int value : PING_SAMPLES) {
            if (value >= 0) {
                min = Math.min(min, value);
                max = Math.max(max, value);
                found = true;
            }
        }

        return found ? Math.max(0, max - min) : 0;
    }

    public static int getSampleCount() {
        return PING_SAMPLES.size();
    }

    public static int getApproxFps() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return -1;
        }

        try {
            int fps = client.getCurrentFps();
            return fps > 0 ? fps : -1;
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static int readPing() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) {
            return -1;
        }

        try {
            var entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) {
                int latency = entry.getLatency();
                if (latency > 0) {
                    return latency;
                }
            }
        } catch (Exception ignored) {
        }

        return -1;
    }
}