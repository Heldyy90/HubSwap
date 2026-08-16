package ru.heldyy.hubswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import ru.heldyy.hubswap.config.HotkeySlot;
import ru.heldyy.hubswap.config.ModConfig;
import ru.heldyy.hubswap.config.StatsData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class HubSwap {
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("hubswap.json");

    private static final Path STATS_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("hubswap_stats.json");

    private static ModConfig CONFIG = loadConfigInternal();
    private static StatsData STATS = loadStatsInternal();

    private static Timer autoSaveTimer;

    public static ModConfig getConfig() {
        return CONFIG;
    }

    public static StatsData getStats() {
        return STATS;
    }

    public static synchronized void saveConfig() {
        try {
            writeJsonAtomically(CONFIG_PATH, CONFIG);
            System.out.println("[HubSwap] Config saved to " + CONFIG_PATH);
        } catch (Exception e) {
            System.err.println("[HubSwap] Failed to save config: " + e.getMessage());
        }
    }

    public static void saveStats() {
        StatsData stats = STATS;
        synchronized (stats) {
            try {
                stats.flushCurrentServer();
                writeJsonAtomically(STATS_PATH, stats);
                System.out.println("[HubSwap] Stats saved to " + STATS_PATH);
            } catch (Exception e) {
                System.err.println("[HubSwap] Failed to save stats: " + e.getMessage());
            }
        }
    }

    public static synchronized void reloadConfig() {
        CONFIG = loadConfigInternal();
        System.out.println("[HubSwap] Config reloaded.");
    }

    private static ModConfig loadConfigInternal() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader r = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
                ModConfig cfg = GSON.fromJson(json, ModConfig.class);
                if (cfg != null) {
                    migrateLegacyConfig(json, cfg);
                    System.out.println("[HubSwap] Config loaded from " + CONFIG_PATH);
                    return cfg;
                }
            } catch (Exception e) {
                System.err.println("[HubSwap] Failed to load config: " + e.getMessage());
            }
        }
        System.out.println("[HubSwap] No config found, creating default.");
        return new ModConfig();
    }

    private static void migrateLegacyConfig(JsonObject json, ModConfig cfg) {
        addLegacyAlias(json, "lightCommand", cfg.getLite().getAliases(), "ln");
        addLegacyAlias(json, "light120Command", cfg.getLite120().getAliases(), "ln120");
        addLegacyAlias(json, "classicCommand", cfg.getClassic().getAliases(), "cn");
        addLegacyAlias(json, "primeCommand", cfg.getPrime().getAliases(), "pn");

        for (HotkeySlot slot : cfg.getHotkeySlots()) {
            if (slot == null || slot.getMode() == null) continue;
            if ("light".equalsIgnoreCase(slot.getMode())) slot.setMode("lite");
            if ("light120".equalsIgnoreCase(slot.getMode())) slot.setMode("lite120");
        }
    }

    private static void addLegacyAlias(JsonObject json, String key, List<String> aliases, String fixedCommand) {
        if (!json.has(key) || json.get(key).isJsonNull()) return;
        try {
            String value = json.get(key).getAsString().trim();
            if (value.isEmpty() || value.equalsIgnoreCase(fixedCommand)) return;
            if (aliases.stream().noneMatch(value::equalsIgnoreCase)) {
                aliases.add(value);
            }
        } catch (Exception ignored) { }
    }

    private static StatsData loadStatsInternal() {
        if (Files.exists(STATS_PATH)) {
            try (BufferedReader r = Files.newBufferedReader(STATS_PATH, StandardCharsets.UTF_8)) {
                StatsData s = GSON.fromJson(r, StatsData.class);
                if (s != null) {
                    System.out.println("[HubSwap] Stats loaded from " + STATS_PATH);
                    return s;
                }
            } catch (Exception e) {
                System.err.println("[HubSwap] Failed to load stats: " + e.getMessage());
                backupCorruptStats();
            }
        }
        System.out.println("[HubSwap] No stats found, creating new.");
        return new StatsData();
    }

    private static void backupCorruptStats() {
        try {
            if (!Files.exists(STATS_PATH)) return;
            String fileName = "hubswap_stats.corrupt-" + System.currentTimeMillis() + ".json";
            Path backup = STATS_PATH.resolveSibling(fileName);
            Files.move(STATS_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[HubSwap] Corrupt stats moved to " + backup);
        } catch (Exception e) {
            System.err.println("[HubSwap] Failed to back up corrupt stats: " + e.getMessage());
        }
    }

    private static void writeJsonAtomically(Path path, Object value) throws Exception {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(value, w);
        }
        try {
            Files.move(temp, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void init() {
        startAutoSave();
    }

    public static synchronized void startAutoSave() {
        if (autoSaveTimer != null) return;
        autoSaveTimer = new Timer("HubSwap Stats Autosave", true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                saveStats();
            }
        }, 30000, 30000);
        System.out.println("[HubSwap] Auto-save started (interval: 30s)");
    }

    public static synchronized void stopAutoSave() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer.purge();
            autoSaveTimer = null;
            System.out.println("[HubSwap] Auto-save stopped");
        }
    }
}
