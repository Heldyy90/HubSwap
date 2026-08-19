package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;

import java.util.Objects;

public class AnarchyRanges {
    public static final int MAX_SERVERS_PER_PAGE = 36;

    @Expose
    private Range solo = new Range(1, 17);

    @Expose
    private Range duo = new Range(18, 38);

    @Expose
    private Range trio = new Range(39, 57);

    @Expose
    private Range clan = new Range(58, 74);

    public AnarchyRanges() {
    }

    public AnarchyRanges(Range solo, Range duo, Range trio, Range clan) {
        this.solo = copyOrDefault(solo, 1, 17);
        this.duo = copyOrDefault(duo, 18, 38);
        this.trio = copyOrDefault(trio, 39, 57);
        this.clan = copyOrDefault(clan, 58, 74);
    }

    public static AnarchyRanges defaults() {
        return new AnarchyRanges();
    }

    public AnarchyRanges copy() {
        ensureInitialized();
        return new AnarchyRanges(solo, duo, trio, clan);
    }

    public Range getSolo() {
        ensureInitialized();
        return solo;
    }

    public Range getDuo() {
        ensureInitialized();
        return duo;
    }

    public Range getTrio() {
        ensureInitialized();
        return trio;
    }

    public Range getClan() {
        ensureInitialized();
        return clan;
    }

    public int getMaxServerNumber() {
        ensureInitialized();
        return Math.max(Math.max(solo.end, duo.end), Math.max(trio.end, clan.end));
    }

    public boolean contains(int number) {
        return getPageIndex(number) >= 0;
    }

    public int getPageIndex(int number) {
        ensureInitialized();
        if (solo.contains(number)) return 0;
        if (duo.contains(number)) return 1;
        if (trio.contains(number)) return 2;
        if (clan.contains(number)) return 3;
        return -1;
    }

    public int getOffset(int number) {
        ensureInitialized();
        if (solo.contains(number)) return number - solo.start;
        if (duo.contains(number)) return number - duo.start;
        if (trio.contains(number)) return number - trio.start;
        if (clan.contains(number)) return number - clan.start;
        return -1;
    }

    public String validationError() {
        ensureInitialized();

        String error = validateRange("Solo", solo);
        if (error != null) return error;
        error = validateRange("Duo", duo);
        if (error != null) return error;
        error = validateRange("Trio", trio);
        if (error != null) return error;
        error = validateRange("Clan", clan);
        if (error != null) return error;

        if (solo.end >= duo.start) return "Диапазоны Solo и Duo пересекаются";
        if (duo.end >= trio.start) return "Диапазоны Duo и Trio пересекаются";
        if (trio.end >= clan.start) return "Диапазоны Trio и Clan пересекаются";

        return null;
    }

    public boolean isValid() {
        return validationError() == null;
    }

    private String validateRange(String name, Range range) {
        if (range.start < 1 || range.end < 1) {
            return name + ": номера должны быть больше 0";
        }
        if (range.start > range.end) {
            return name + ": начало диапазона больше конца";
        }
        if (range.size() > MAX_SERVERS_PER_PAGE) {
            return name + ": максимум " + MAX_SERVERS_PER_PAGE + " серверов на одной странице";
        }
        return null;
    }

    private void ensureInitialized() {
        if (solo == null) solo = new Range(1, 17);
        if (duo == null) duo = new Range(18, 38);
        if (trio == null) trio = new Range(39, 57);
        if (clan == null) clan = new Range(58, 74);
    }

    private static Range copyOrDefault(Range value, int defaultStart, int defaultEnd) {
        return value == null ? new Range(defaultStart, defaultEnd) : new Range(value.start, value.end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnarchyRanges other)) return false;
        ensureInitialized();
        other.ensureInitialized();
        return Objects.equals(solo, other.solo)
                && Objects.equals(duo, other.duo)
                && Objects.equals(trio, other.trio)
                && Objects.equals(clan, other.clan);
    }

    @Override
    public int hashCode() {
        ensureInitialized();
        return Objects.hash(solo, duo, trio, clan);
    }

    public static class Range {
        @Expose
        private int start;

        @Expose
        private int end;

        public Range() {
        }

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public boolean contains(int number) {
            return number >= start && number <= end;
        }

        public int size() {
            return end - start + 1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Range range)) return false;
            return start == range.start && end == range.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
}
