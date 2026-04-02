package ru.heldyy.hubswap.gui;

import net.minecraft.client.MinecraftClient;
import ru.heldyy.hubswap.HubSwap;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class TransitionDetector {
    private static final Deque<String> RECENT_CHAT = new ArrayDeque<>();
    private static final int MAX_CHAT = 40;

    private static TransitionAttempt currentAttempt;
    private static int chatCursor = 0;

    private TransitionDetector() {
    }

    public static void startAttempt(TransitionMode mode, int targetNumber, int hubDelay, int clickDelay, int confirmDelay) {
        currentAttempt = new TransitionAttempt(mode, targetNumber, hubDelay, clickDelay, confirmDelay);
        chatCursor = RECENT_CHAT.size();
    }

    public static void onChatMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return;
        }

        RECENT_CHAT.addLast(rawMessage);
        while (RECENT_CHAT.size() > MAX_CHAT) {
            RECENT_CHAT.removeFirst();
        }
    }

    public static void onClientTick(MinecraftClient client) {
        if (currentAttempt == null || currentAttempt.isFinished()) {
            return;
        }

        long elapsed = System.currentTimeMillis() - currentAttempt.getStartedAt();

        if (isSuccess(client, currentAttempt, elapsed)) {
            finishSuccess();
            return;
        }

        if (elapsed >= currentAttempt.getConfirmDelay() && isInHub(client)) {
            finishFailure();
        }
    }

    private static boolean isSuccess(MinecraftClient client, TransitionAttempt attempt, long elapsed) {
        if (matchedChat(attempt)) {
            return true;
        }

        return elapsed > 2200L && !isInHub(client);
    }

    private static boolean matchedChat(TransitionAttempt attempt) {
        int index = 0;
        for (String raw : RECENT_CHAT) {
            if (index++ < chatCursor) {
                continue;
            }

            String msg = normalize(raw);

            if (attempt.getMode() == TransitionMode.CLASSIC) {
                if (msg.contains("выполняется подключение")
                        || (msg.contains("рады вновь тебя видеть") && msg.contains("приятной игры"))) {
                    return true;
                }
            } else {
                if (msg.contains("прямо сейчас идет набор")
                        || msg.contains("в команду проекта на должность стажера")
                        || msg.contains("в команду проекта на должность стажёра")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isInHub(MinecraftClient client) {
        if (client == null || client.player == null) {
            return false;
        }

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        return Math.abs(x - 317.62) <= 8.0
                && Math.abs(y - 29.00) <= 5.0
                && Math.abs(z - 302.47) <= 8.0;
    }

    private static void finishSuccess() {
        if (currentAttempt != null) {
            String serverType = modeToStatsKey(currentAttempt.getMode());
            HubSwap.getStats().onServerChange(serverType);
            currentAttempt.finish();
        }
        AutoTuneManager.recordSuccess();
        NotificationRenderer.showNotification("Успешный переход на анархию!");
        currentAttempt = null;
    }

    
    public static String modeToStatsKey(TransitionMode mode) {
        return switch (mode) {
            case CLASSIC  -> "classic";
            case LIGHT120 -> "light120";
            default       -> "light";
        };
    }

    
    public static void onDisconnect() {
        HubSwap.getStats().onServerChange(null);
        HubSwap.saveStats();
    }

    private static void finishFailure() {
        if (currentAttempt != null) {
            currentAttempt.finish();
        }
        AutoTuneManager.recordFailure();
        NotificationRenderer.showNotification("Переход не удался");
        currentAttempt = null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }
}