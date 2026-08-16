package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModConfig {
    @Expose
    private boolean notificationsEnabled = true;

    @Expose
    private ColorTheme colorTheme = ColorTheme.AQUA;

    @Expose
    private Formatting linkColor = Formatting.GOLD;

    @Expose
    private int timeoutTicks = 200;

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
                    new RangeEntry("all", "Все", 1, 5)
            ), 5)
    );

    @Expose
    private ModeConfig prime = new ModeConfig(
            "prime",
            List.of("pm"),
            new RangeConfig(List.of(
                    new RangeEntry("all", "Все", 1, 9)
            ), 9)
    );

    @Expose
    private List<HotkeySlot> hotkeySlots = createDefaultHotkeySlots();

    public ModConfig() {}

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean enabled) { this.notificationsEnabled = enabled; }

    public ColorTheme getColorTheme() { return colorTheme; }
    public void setColorTheme(ColorTheme theme) { this.colorTheme = theme != null ? theme : ColorTheme.AQUA; }

    public Formatting getLinkColor() { return linkColor; }
    public void setLinkColor(Formatting color) { this.linkColor = color != null ? color : Formatting.GOLD; }

    public int getTimeoutTicks() { return timeoutTicks; }
    public void setTimeoutTicks(int ticks) { this.timeoutTicks = Math.max(20, Math.min(600, ticks)); }

    public ModeConfig getLite() { return lite; }
    public ModeConfig getLite120() { return lite120; }
    public ModeConfig getClassic() { return classic; }
    public ModeConfig getPrime() { return prime; }

    public ModeConfig getMode(String modeName) {
        if (modeName == null) {
            throw new IllegalArgumentException("Неизвестный режим: null");
        }
        return switch (modeName.toLowerCase(Locale.ROOT)) {
            case "lite", "light" -> lite;
            case "lite120", "light120" -> lite120;
            case "classic" -> classic;
            case "prime" -> prime;
            default -> throw new IllegalArgumentException("Неизвестный режим: " + modeName);
        };
    }

    public List<HotkeySlot> getHotkeySlots() {
        if (hotkeySlots == null) hotkeySlots = createDefaultHotkeySlots();
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

    public static class ModeConfig {
        @Expose
        private final String id;

        @Expose
        private List<String> aliases;

        @Expose
        private RangeConfig ranges;

        public ModeConfig() {
            this.id = "unknown";
            this.aliases = new ArrayList<>();
            this.ranges = new RangeConfig();
        }

        public ModeConfig(String id, List<String> aliases, RangeConfig ranges) {
            this.id = id;
            this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
            this.ranges = ranges == null ? new RangeConfig() : ranges;
        }

        public String getId() { return id; }
        public List<String> getAliases() {
            if (aliases == null) aliases = new ArrayList<>();
            return aliases;
        }
        public void setAliases(List<String> aliases) {
            this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        }
        public RangeConfig getRanges() {
            if (ranges == null) ranges = new RangeConfig();
            return ranges;
        }
        public void setRanges(RangeConfig ranges) { this.ranges = ranges == null ? new RangeConfig() : ranges; }
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
            this.entries = normalizeEntries(entries);
            this.total = Math.max(1, total);
            syncSingleAllRange();
        }

        public List<RangeEntry> getEntries() {
            if (entries == null) entries = new ArrayList<>();
            return entries;
        }

        public void setEntries(List<RangeEntry> entries) {
            this.entries = normalizeEntries(entries);
            syncSingleAllRange();
        }

        public int getTotal() { return Math.max(1, total); }

        public void setTotal(int total) {
            this.total = Math.max(1, total);
            syncSingleAllRange();
        }

        public RangeEntry find(int number) {
            for (RangeEntry e : getEntries()) {
                if (number >= e.min && number <= e.max) return e;
            }
            return null;
        }

        public int getMin() { return 1; }
        public int getMax() { return getTotal(); }

        public boolean isValid(int number) {
            if (number < 1 || number > getTotal()) return false;
            return getEntries().isEmpty() || find(number) != null;
        }

        private void syncSingleAllRange() {
            if (entries != null && entries.size() == 1 && "all".equals(entries.get(0).key)) {
                entries.get(0).min = 1;
                entries.get(0).max = getTotal();
            }
        }

        private static List<RangeEntry> normalizeEntries(List<RangeEntry> entries) {
            List<RangeEntry> result = new ArrayList<>();
            if (entries == null) return result;
            for (RangeEntry entry : entries) {
                if (entry == null) continue;
                result.add(new RangeEntry(entry.key, entry.name, entry.min, entry.max));
            }
            return result;
        }
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
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
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
