package ru.heldyy.hubswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.heldyy.hubswap.config.ModConfig;

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

    private static ModConfig CONFIG = loadConfigInternal();

    public static ModConfig getConfig() {
        return CONFIG;
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

    public static void reloadConfig() {
        CONFIG = loadConfigInternal();
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
}
