package ru.heldyy.hubswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.heldyy.hubswap.config.ModConfig;
import ru.heldyy.hubswap.config.StatsData;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    static {
        
        
        
        saveConfig();
    }

    public static ModConfig getConfig() {
        return CONFIG;
    }

    public static StatsData getStats() {
        return STATS;
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(CONFIG, w);
            }
        } catch (Exception ignored) {
        }
    }

    public static void saveStats() {
        try {
            STATS.flushCurrentServer();
            Files.createDirectories(STATS_PATH.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(STATS_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(STATS, w);
            }
        } catch (Exception ignored) {
        }
    }

    public static void reloadConfig() {
        CONFIG = loadConfigInternal();
        saveConfig();
    }

    private static ModConfig loadConfigInternal() {
        if (Files.exists(CONFIG_PATH)) {
            try (BufferedReader r = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                ModConfig cfg = GSON.fromJson(r, ModConfig.class);
                return cfg != null ? cfg : new ModConfig();
            } catch (Exception ignored) {
            }
        }
        return new ModConfig();
    }

    private static StatsData loadStatsInternal() {
        if (Files.exists(STATS_PATH)) {
            try (BufferedReader r = Files.newBufferedReader(STATS_PATH, StandardCharsets.UTF_8)) {
                StatsData s = GSON.fromJson(r, StatsData.class);
                return s != null ? s : new StatsData();
            } catch (Exception ignored) {
            }
        }
        return new StatsData();
    }
}
