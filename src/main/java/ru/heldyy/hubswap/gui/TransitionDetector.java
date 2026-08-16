package ru.heldyy.hubswap.gui;

import ru.heldyy.hubswap.HubSwap;

public final class TransitionDetector {
    private TransitionDetector() {}

    public static void onDisconnect() {
        HubSwap.getStats().onServerChange(null);
        HubSwap.saveStats();
    }
}
