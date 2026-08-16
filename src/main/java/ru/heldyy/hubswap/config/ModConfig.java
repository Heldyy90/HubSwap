package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;


public class ModConfig {
    // ===== ОБЩИЕ НАСТРОЙКИ =====
    @Expose
    private boolean notificationsEnabled = true;

    @Expose
    private ColorTheme colorTheme = ColorTheme.AQUA;

    @Expose
    private Formatting linkColor = Formatting.GOLD;

    @Expose
    private int timeoutTicks = 200; // таймаут в тиках (~10 сек)

    // ===== РЕЖИМЫ =====
    // Каждый режим имеет свои алиасы и диапазоны

    @Expose
    private ModeConfig lite = new ModeConfig(
            "lite",
            List.of("an", "ан", "ft"),
            new RangeConfig(List.of(
                    new RangeEntry("solo", "Соло", 1, 17),
                    new RangeEntry("duo", "Дуо", 18, 38),
                    new RangeEntry("trio", "Трио", 39, 57),
                    new RangeEntry("clans", "Кланы", 58, 74)
            ), 74)
    );

    @Expose
    private ModeConfig lite120 = new ModeConfig(
            "lite120",
            List.of(),
            new RangeConfig(List.of(
                    new RangeEntry("all", "Все", 1, 3)
            ), 3)
    );

    @Expose
    private ModeConfig classic = new ModeConfig(
            "classic",
            List.of("cn"),
            new RangeConfig(List.of(
                    new RangeEntry("all", "Все", 1, 3)
            ), 3)
    );

    @Expose
    private ModeConfig prime = new ModeConfig(
            "prime",
            List.of("pm"),
            new RangeConfig(List.of(
                    new RangeEntry("all", "Все", 1, 9)
            ), 9)
    );

    // ===== ХОТКЕИ =====
    @Expose
    private List<HotkeySlot> hotkeySlots = createDefaultHotkeySlots();

    // ===== КОНСТРУКТОРЫ И ГЕТТЕРЫ =====

    public ModConfig() {}

    // --- Общие ---
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean enabled) { this.notificationsEnabled = enabled; }

    public ColorTheme getColorTheme() { return colorTheme; }
    public void setColorTheme(ColorTheme theme) { this.colorTheme = theme != null ? theme : ColorTheme.AQUA; }

    public Formatting getLinkColor() { return linkColor; }
    public void setLinkColor(Formatting color) { this.linkColor = color != null ? color : Formatting.GOLD; }

    public int getTimeoutTicks() { return timeoutTicks; }
    public void setTimeoutTicks(int ticks) { this.timeoutTicks = Math.max(20, Math.min(600, ticks)); }

    // --- Режимы ---
    public ModeConfig getLite() { return lite; }
    public ModeConfig getLite120() { return lite120; }
    public ModeConfig getClassic() { return classic; }
    public ModeConfig getPrime() { return prime; }

    public ModeConfig getMode(String modeName) {
        return switch (modeName.toLowerCase()) {
            case "lite" -> lite;
            case "lite120" -> lite120;
            case "classic" -> classic;
            case "prime" -> prime;
            default -> throw new IllegalArgumentException("Неизвестный режим: " + modeName);
        };
    }

    // --- Хоткеи ---
    public List<HotkeySlot> getHotkeySlots() {
        while (hotkeySlots.size() < 8) hotkeySlots.add(new HotkeySlot());
        return hotkeySlots;
    }

    private static List<HotkeySlot> createDefaultHotkeySlots() {
        List<HotkeySlot> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            list.add(new HotkeySlot());
        }
        return list;
    }

    // ===== ВЛОЖЕННЫЕ КЛАССЫ =====

    public static class ModeConfig {
        @Expose
        private final String id; // идентификатор (lite, lite120, classic, prime)

        @Expose
        private List<String> aliases;

        @Expose
        private RangeConfig ranges;

        // Для десериализации Gson нужен конструктор без аргументов
        public ModeConfig() {
            this.id = "unknown";
            this.aliases = new ArrayList<>();
            this.ranges = new RangeConfig();
        }

        public ModeConfig(String id, List<String> aliases, RangeConfig ranges) {
            this.id = id;
            this.aliases = new ArrayList<>(aliases);
            this.ranges = ranges;
        }

        public String getId() { return id; }
        public List<String> getAliases() { return aliases; }
        public void setAliases(List<String> aliases) {
            this.aliases = new ArrayList<>(aliases);
        }
        public RangeConfig getRanges() { return ranges; }
        public void setRanges(RangeConfig ranges) { this.ranges = ranges; }
    }

    public static class RangeConfig {
        @Expose
        private List<RangeEntry> entries;

        @Expose
        private int total;

        public RangeConfig() {
            this.entries = new ArrayList<>();
            this.total = 1;
        }

        public RangeConfig(List<RangeEntry> entries, int total) {
            this.entries = new ArrayList<>(entries);
            this.total = Math.max(1, total);
        }

        public List<RangeEntry> getEntries() { return entries; }
        public void setEntries(List<RangeEntry> entries) { this.entries = new ArrayList<>(entries); }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = Math.max(1, total); }

        public RangeEntry find(int number) {
            for (RangeEntry e : entries) {
                if (number >= e.min && number <= e.max) return e;
            }
            return null;
        }

        public int getMin() { return 1; }
        public int getMax() { return total; }
        public boolean isValid(int number) { return number >= 1 && number <= total; }
    }

    public static class RangeEntry {
        @Expose
        public String key;

        @Expose
        public String name;

        @Expose
        public int min;

        @Expose
        public int max;

        public RangeEntry() {}

        public RangeEntry(String key, String name, int min, int max) {
            this.key = key;
            this.name = name;
            this.min = min;
            this.max = max;
        }
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

        public String getDisplayName() { return displayName; }
        public Formatting getFormatting() { return formatting; }
        public int getRgbColor() { return rgbColor; }

        public ColorTheme next() {
            ColorTheme[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }
}