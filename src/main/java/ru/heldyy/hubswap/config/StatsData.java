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

    public void recordSwitch(String mode, int number) {
        totalSwitches++;
        sessionSwitches++;
        String key = mode + "_" + number;
        serverCounts.merge(key, 1L, Long::sum);
    }

    public long getTotalSwitches() { return totalSwitches; }
    public long getSessionSwitches() { return sessionSwitches; }

    
    public void onServerChange(String newType) {
        flushCurrentServer();
        currentServerType = newType;
        serverJoinTime = newType != null ? System.currentTimeMillis() : 0;
    }

    
    public void flushCurrentServer() {
        if (currentServerType != null && serverJoinTime > 0) {
            long spent = System.currentTimeMillis() - serverJoinTime;
            timeSpentMs.merge(currentServerType, spent, Long::sum);
            serverJoinTime = System.currentTimeMillis();
        }
    }

    
    public long getTimeSpentMs(String type) {
        return timeSpentMs.getOrDefault(type, 0L);
    }

    
    public String getFavoriteKey() {
        return serverCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    
    public long getCountForKey(String key) {
        return serverCounts.getOrDefault(key, 0L);
    }

    
    public Map<String, Long> getServerCounts() {
        return serverCounts;
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
            case "light120" -> "Lite 1.20";
            case "prime" -> "Prime";
            default -> "Lite";
        };
        return type + " #" + parts[1];
    }
}
