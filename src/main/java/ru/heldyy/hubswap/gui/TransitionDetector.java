package ru.heldyy.hubswap.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import ru.heldyy.hubswap.HubSwap;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class TransitionDetector {
    private static final Deque<String> RECENT_CHAT = new ArrayDeque<>();
    private static final int MAX_CHAT = 40;

    private static World prevWorld = null;

    private TransitionDetector() {}

    public static void setPrevWorld(World world) {
        prevWorld = world;
    }

    public static World getPrevWorld() {
        return prevWorld;
    }

    public static void onChatMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) return;
        RECENT_CHAT.addLast(rawMessage);
        while (RECENT_CHAT.size() > MAX_CHAT) {
            RECENT_CHAT.removeFirst();
        }
    }

    public static void onDisconnect() {
        HubSwap.getStats().onServerChange(null);
        HubSwap.saveStats();
        prevWorld = null;
    }
}