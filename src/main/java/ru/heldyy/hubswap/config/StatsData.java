package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;

import java.util.HashMap;
import java.util.Map;

public class StatsData {

    @Expose
    private long totalSwitches = 0;

    @Expose
    private Map<String, Long> serverCounts = new HashMap<>();

    @Expose
    private Map<String, Long> timeSpentMs = new HashMap<>();

    private transient long sessionSwitches = 0;
    private transient String currentServerType = null;
    private transient long serverJoinTime = 0;

    public synchronized void recordSwitch(String mode, int number) {
        totalSwitches++;
        sessionSwitches++;
        String key = mode + "_" + number;
        serverCounts.merge(key, 1L, Long::sum);
    }

    public synchronized long getTotalSwitches() { return totalSwitches; }
    public synchronized long getSessionSwitches() { return sessionSwitches; }

    public synchronized void onServerChange(String newType) {
        flushCurrentServer();
        currentServerType = normalizeMode(newType);
        serverJoinTime = currentServerType != null ? System.currentTimeMillis() : 0;
    }

    public synchronized void flushCurrentServer() {
        if (currentServerType != null && serverJoinTime > 0) {
            long spent = Math.max(0L, System.currentTimeMillis() - serverJoinTime);
            timeSpentMs.merge(currentServerType, spent, Long::sum);
            serverJoinTime = System.currentTimeMillis();
        }
    }

    public synchronized long getTimeSpentMs(String type) {
        String normalized = normalizeMode(type);
        return normalized == null ? 0L : timeSpentMs.getOrDefault(normalized, 0L);
    }

    public synchronized String getFavoriteKey() {
        return serverCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public synchronized long getCountForKey(String key) {
        return serverCounts.getOrDefault(key, 0L);
    }

    public synchronized Map<String, Long> getServerCounts() {
        return new HashMap<>(serverCounts);
    }

    public static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) return hours + "ч " + minutes + "м";
        if (minutes > 0) return minutes + "м";
        return "< 1м";
    }

    public static String formatKey(String key) {
        if (key == null) return "—";
        String[] parts = key.split("_", 2);
        if (parts.length < 2) return key;
        String type = switch (parts[0]) {
            case "classic" -> "Classic";
            case "lite120", "light120" -> "Lite 1.20";
            case "prime" -> "Prime";
            case "lite", "light" -> "Lite";
            default -> parts[0];
        };
        return type + " #" + parts[1];
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return null;
        return switch (mode.toLowerCase(java.util.Locale.ROOT)) {
            case "light" -> "lite";
            case "light120" -> "lite120";
            default -> mode.toLowerCase(java.util.Locale.ROOT);
        };
    }
}
