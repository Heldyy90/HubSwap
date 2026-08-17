package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

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
    private String primeCommand = "pn";

    @Expose
    private String hiddenRemoteNoticeFingerprint = "";

    @Expose
    private boolean notificationsEnabled = true;

    @Expose
    private boolean remoteNoticesEnabled = true;

    @Expose
    private boolean smartAutoTuneEnabled = true;

    @Expose
    private ColorTheme colorTheme = ColorTheme.AQUA;

    @Expose
    private Formatting linkColor = Formatting.GOLD;

    @Expose
    private int learnedHubOffset = 0;

    @Expose
    private int learnedClickOffset = 200;

    @Expose
    private int bestClickDelay = 0;

    @Expose
    private int minClickDelayFloor = 20;

    @Expose
    private int successStreak = 0;

    @Expose
    private int failStreak = 0;

    @Expose
    private List<HotkeySlot> hotkeySlots = createDefaultHotkeySlots();

    private static List<HotkeySlot> createDefaultHotkeySlots() {
        List<HotkeySlot> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            list.add(new HotkeySlot());
        }
        return list;
    }

    public List<HotkeySlot> getHotkeySlots() {
        while (hotkeySlots.size() < 8) hotkeySlots.add(new HotkeySlot());
        return hotkeySlots;
    }

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

    public String getPrimeCommand() {
        return primeCommand;
    }

    public String getHiddenRemoteNoticeFingerprint() {
        return hiddenRemoteNoticeFingerprint == null ? "" : hiddenRemoteNoticeFingerprint;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isRemoteNoticesEnabled() {
        return remoteNoticesEnabled;
    }

    public boolean isSmartAutoTuneEnabled() {
        return smartAutoTuneEnabled;
    }

    public ColorTheme getColorTheme() {
        return colorTheme;
    }

    public Formatting getLinkColor() {
        return linkColor;
    }

    public int getLearnedHubOffset() {
        return learnedHubOffset;
    }

    public int getLearnedClickOffset() {
        return learnedClickOffset;
    }

    public int getAutoTunedClickDelay() {
        if (learnedClickOffset <= 0) {
            learnedClickOffset = 200;
        }
        learnedClickOffset = roundToStep(clamp(learnedClickOffset, 20, 1000), 5);
        return learnedClickOffset;
    }

    public int getBestClickDelay() {
        return bestClickDelay;
    }

    public int getMinClickDelayFloor() {
        return minClickDelayFloor;
    }

    public int getSuccessStreak() {
        return successStreak;
    }

    public int getFailStreak() {
        return failStreak;
    }

    public void setDelays(int classicDelay, int clickDelay) {
        this.classicDelay = clamp(classicDelay, 100, 5000);
        this.clickDelay = clamp(clickDelay, 50, 1000);
    }

    public void setCommands(String classicCommand, String lightCommand, String light120Command, String primeCommand) {
        this.classicCommand = validateCommand(classicCommand, "cn");
        this.lightCommand = validateCommand(lightCommand, "ln");
        this.light120Command = validateCommand(light120Command, "ln120");
        this.primeCommand = validateCommand(primeCommand, "pn");
    }

    public void setCommands(String classicCommand, String lightCommand, String light120Command) {
        setCommands(classicCommand, lightCommand, light120Command, this.primeCommand);
    }

    public void setHiddenRemoteNoticeFingerprint(String fingerprint) {
        this.hiddenRemoteNoticeFingerprint = fingerprint == null ? "" : fingerprint.trim();
    }

    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }

    public void setRemoteNoticesEnabled(boolean enabled) {
        this.remoteNoticesEnabled = enabled;
    }

    public void setSmartAutoTuneEnabled(boolean enabled) {
        this.smartAutoTuneEnabled = enabled;
    }

    public void setColorTheme(ColorTheme theme) {
        this.colorTheme = theme == null ? ColorTheme.AQUA : theme;
    }

    public void setLinkColor(Formatting color) {
        this.linkColor = color == null ? Formatting.GOLD : color;
    }

    public void recordSuccess() {
        recordSuccess(true);
    }

    public void recordSuccess(boolean learnHubOffset) {
        normalizeAutoTuneState();

        failStreak = 0;
        successStreak = Math.min(100, successStreak + 1);

        
        
        
        if (successStreak >= 2) {
            if (learnHubOffset) {
                learnedHubOffset = Math.max(0, learnedHubOffset - 50);
            }

            int currentDelay = getAutoTunedClickDelay();
            if (bestClickDelay <= 0 || currentDelay < bestClickDelay) {
                bestClickDelay = currentDelay;
            }

            boolean hasSafetyFloor = minClickDelayFloor > 20;
            if (hasSafetyFloor && currentDelay <= minClickDelayFloor + 5) {
                
                minClickDelayFloor = roundToStep(clamp(minClickDelayFloor - 15, 20, 1000), 5);
            }

            int decreaseStep = hasSafetyFloor ? 10 : 30;
            int nextDelay = currentDelay - decreaseStep;
            nextDelay = Math.max(nextDelay, minClickDelayFloor);
            nextDelay = Math.max(nextDelay, 20);

            learnedClickOffset = roundToStep(clamp(nextDelay, 20, 1000), 5);
            successStreak = 0;
        }
    }

    public void recordFailure() {
        recordFailure(true);
    }

    public void recordFailure(boolean learnHubOffset) {
        normalizeAutoTuneState();

        successStreak = 0;
        failStreak = Math.min(100, failStreak + 1);

        if (learnHubOffset) {
            learnedHubOffset = clamp(learnedHubOffset + 100 + (failStreak >= 2 ? 50 : 0), 0, 1500);
        }

        int currentDelay = getAutoTunedClickDelay();
        int clickIncrease = failStreak == 1 ? 20 : 25;
        int safetyMargin = failStreak == 1 ? 10 : 15;

        
        
        minClickDelayFloor = roundToStep(clamp(Math.max(minClickDelayFloor, currentDelay + safetyMargin), 20, 1000), 5);

        int targetDelay = currentDelay + clickIncrease;
        if (bestClickDelay > 0) {
            targetDelay = Math.max(targetDelay, bestClickDelay + 20);
        }
        targetDelay = Math.max(targetDelay, minClickDelayFloor);

        learnedClickOffset = roundToStep(clamp(targetDelay, 20, 1000), 5);
    }

    private void normalizeAutoTuneState() {
        learnedClickOffset = roundToStep(clamp(learnedClickOffset <= 0 ? 200 : learnedClickOffset, 20, 1000), 5);
        minClickDelayFloor = roundToStep(clamp(minClickDelayFloor, 20, 1000), 5);

        
        
        
        
        if (bestClickDelay > 0 && minClickDelayFloor > bestClickDelay + 60) {
            bestClickDelay = 0;
        }

        if (minClickDelayFloor > learnedClickOffset) {
            minClickDelayFloor = learnedClickOffset;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int roundToStep(int value, int step) {
        if (step <= 1) {
            return value;
        }
        return Math.round(value / (float) step) * step;
    }

    private String validateCommand(String command, String defaultCommand) {
        if (command == null || command.trim().isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                                Text.literal("[HubSwap] Используется команда по умолчанию: " + defaultCommand),
                                false
                        );
                    }
                });
            }
            return defaultCommand;
        }

        String cleaned = command.trim()
                .replace("/", "")
                .replaceAll("[^a-zA-Z0-9_а-яА-ЯёЁ]", "");

        if (cleaned.length() > 10) {
            cleaned = cleaned.substring(0, 10);
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