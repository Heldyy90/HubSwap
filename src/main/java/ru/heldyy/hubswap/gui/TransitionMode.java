package ru.heldyy.hubswap.gui;

public enum TransitionMode {
    CLASSIC("Classic"),
    LIGHT("Lite"),
    LIGHT120("Lite 1.20"),
    PRIME("Prime");

    private final String displayName;

    TransitionMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}