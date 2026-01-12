package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;
import net.minecraft.text.Text;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Formatting;

public class ModConfig {
    @Expose
    private int classicDelay = 1500;
    @Expose
    private int clickDelay = 200;

    @Expose
    private String classicCommand = "cn";
    @Expose
    private String lightCommand = "ln";
    @Expose
    private String light120Command = "ln120";

    @Expose
    private boolean notificationsEnabled = true;

    @Expose
    private ColorTheme colorTheme = ColorTheme.AQUA; 
    
    @Expose
    private Formatting linkColor = Formatting.GOLD;

    public int getClassicDelay() {
        return classicDelay;
    }

    public int getClickDelay() {
        return clickDelay;
    }

    public String getClassicCommand() {
        return classicCommand;
    }

    public String getLightCommand() {
        return lightCommand;
    }

    public String getLight120Command() {
        return light120Command;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public ColorTheme getColorTheme() {
        return colorTheme;
    }

    public Formatting getLinkColor() {
        return linkColor;
    }

    public void setDelays(int classicDelay, int clickDelay) {
        this.classicDelay = Math.max(100, Math.min(5000, classicDelay));
        this.clickDelay = Math.max(50, Math.min(1000, clickDelay));
    }

    public void setCommands(String classicCommand, String lightCommand, String light120Command) {
        this.classicCommand = validateCommand(classicCommand, "cn");
        this.lightCommand = validateCommand(lightCommand, "ln");
        this.light120Command = validateCommand(light120Command, "ln120");
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    public void setColorTheme(ColorTheme theme) {
        this.colorTheme = theme;
    }

    public void setLinkColor(Formatting color) {
        this.linkColor = color;
    }

    private String validateCommand(String command, String defaultCommand) {
        if (command == null || command.trim().isEmpty()) {
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("[HubSwap] Используется команда по умолчанию: " + defaultCommand), false);
                }
            });
            return defaultCommand;
        }
        String cleaned = command.trim()
                .replace("/", "")
                .replaceAll("[^a-zA-Z0-9_а-яА-ЯёЁ]", "");
        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(0, 10);
            MinecraftClient.getInstance().execute(() -> {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.sendMessage(Text.literal("[HubSwap] Команда сокращена до 10 символов"), false);
                }
            });
        }
        return cleaned.isEmpty() ? defaultCommand : cleaned;
    }
    
    public enum ColorTheme {
        AQUA("Синяя", Formatting.AQUA, 0x00d9ff),
        RED("Красная", Formatting.RED, 0xff4444),
        BLUE("Голубая", Formatting.BLUE, 0x5555ff),
        GREEN("Зелёная", Formatting.GREEN, 0x55ff55),
        LIGHT_PURPLE("Фиолетовая", Formatting.LIGHT_PURPLE, 0xff55ff),
        GOLD("Золотая", Formatting.GOLD, 0xffaa00);

        private final String displayName;
        private final Formatting formatting;
        private final int rgbColor;

        ColorTheme(String displayName, Formatting formatting, int rgbColor) {
            this.displayName = displayName;
            this.formatting = formatting;
            this.rgbColor = rgbColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Formatting getFormatting() {
            return formatting;
        }

        public int getRgbColor() {
            return rgbColor;
        }

        public ColorTheme next() {
            ColorTheme[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}
