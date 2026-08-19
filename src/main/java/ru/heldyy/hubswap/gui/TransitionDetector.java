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
    private static String lastTabText = "";
    private static long lastTabTextAt = 0L;

    private TransitionDetector() {
    }

    public static void startAttempt(TransitionMode mode, int targetNumber, int hubDelay, int clickDelay, int confirmDelay) {
        currentAttempt = new TransitionAttempt(mode, targetNumber, hubDelay, clickDelay, confirmDelay);
        chatCursor = RECENT_CHAT.size();
    }

    public static void markOpeningMenu() {
        if (currentAttempt != null && !currentAttempt.isFinished()) {
            currentAttempt.setStage(TransitionAttempt.Stage.OPENING_MENU);
        }
    }

    public static void markClicking() {
        if (currentAttempt != null && !currentAttempt.isFinished()) {
            currentAttempt.setStage(TransitionAttempt.Stage.CLICKING);
        }
    }

    public static void markTargetClicked() {
        if (currentAttempt != null && !currentAttempt.isFinished()) {
            currentAttempt.markTargetClicked();
        }
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

    public static void onTabText(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }

        lastTabText = normalize(rawText);
        lastTabTextAt = System.currentTimeMillis();
    }

    public static void onClientTick(MinecraftClient client) {
        if (currentAttempt == null || currentAttempt.isFinished()) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean inHub = isInHub(client);

        if (inHub) {
            currentAttempt.markHubSeen();
        }

        if (isSuccess(client, currentAttempt, inHub)) {
            finishSuccess();
            return;
        }

        if (isFailureTimeout(currentAttempt, now, inHub)) {
            finishFailure();
        }
    }

    private static boolean isFailureTimeout(TransitionAttempt attempt, long now, boolean inHub) {
        long stageElapsed = now - attempt.getStageStartedAt();

        return switch (attempt.getStage()) {
            case WAITING_HUB -> stageElapsed >= getWaitingHubTimeout(attempt) && !attempt.hasHubBeenSeen();
            case OPENING_MENU -> stageElapsed >= getOpeningMenuTimeout(attempt);
            case CLICKING -> stageElapsed >= getClickingTimeout(attempt);
            case CONFIRM_TRANSFER -> stageElapsed >= getConfirmTransferTimeout(attempt) && isStillInHubForConfirm();
        };
    }

    private static long getWaitingHubTimeout(TransitionAttempt attempt) {
        if (attempt.getMode() == TransitionMode.CLASSIC) {
            return Math.max(3500L, Math.min(6500L, attempt.getHubDelay() + 1500L));
        }

        return 3500L;
    }

    private static long getOpeningMenuTimeout(TransitionAttempt attempt) {
        long click = Math.max(120L, attempt.getClickDelay());

        if (attempt.getMode() == TransitionMode.CLASSIC) {
            return clampLong(1200L + click * 2L, 1600L, 3000L);
        }

        return clampLong(900L + click * 2L, 1200L, 2500L);
    }

    private static long getClickingTimeout(TransitionAttempt attempt) {
        long click = Math.max(120L, attempt.getClickDelay());

        if (attempt.getMode() == TransitionMode.CLASSIC) {
            return clampLong(1200L + click * 4L, 1800L, 3500L);
        }

        if (attempt.getMode() == TransitionMode.LIGHT120) {
            return clampLong(700L + click * 2L, 1000L, 2200L);
        }

        return clampLong(900L + click * 3L, 1200L, 3000L);
    }

    private static long getConfirmTransferTimeout(TransitionAttempt attempt) {
        long click = Math.max(120L, attempt.getClickDelay());

        if (attempt.getMode() == TransitionMode.CLASSIC) {
            return clampLong(1200L + click * 2L, 1600L, 3500L);
        }

        if (attempt.getMode() == TransitionMode.LIGHT120) {
            return clampLong(900L + click * 2L, 1200L, 3000L);
        }

        return clampLong(1000L + click * 2L, 1300L, 3200L);
    }

    private static boolean isSuccess(MinecraftClient client, TransitionAttempt attempt, boolean inHub) {

        if (!attempt.hasTargetClicked()) {
            return false;
        }

        if (matchedChat(attempt)) {
            return true;
        }

        long afterLastClick = System.currentTimeMillis() - attempt.getLastClickAt();
        return attempt.hasHubBeenSeen() && afterLastClick >= 250L && !isStillInHubForConfirm();
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
                        || msg.contains("в команду проекта на должность стажера")) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isInHub(MinecraftClient client) {
        if (client == null || client.player == null) {
            return false;
        }

        if (isInHubByCoords(client)) {
            return true;
        }

        return isInHubByTab(5000L);
    }

    private static boolean isStillInHubForConfirm() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || client.player == null) {
            return false;
        }

        if (isInHubByCoords(client)) {
            return true;
        }

        return isInHubByTab(1500L);
    }

    private static boolean isInHubByCoords(MinecraftClient client) {
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();

        return Math.abs(x - 317.62) <= 35.0
                && Math.abs(y - 29.00) <= 12.0
                && Math.abs(z - 302.47) <= 35.0;
    }

    private static boolean isInHubByTab(long maxAgeMs) {
        if (lastTabText.isEmpty()) {
            return false;
        }

        long age = System.currentTimeMillis() - lastTabTextAt;

        if (age > maxAgeMs) {
            return false;
        }

        return lastTabText.contains("лобби")
                || lastTabText.contains("lobby")
                || lastTabText.contains("хаб")
                || lastTabText.contains("hub");
    }

    private static void finishSuccess() {
        if (currentAttempt != null) {
            String serverType = modeToStatsKey(currentAttempt.getMode());
            HubSwap.getStats().onServerChange(serverType);
            currentAttempt.finish();
        }
        TransitionMode mode = currentAttempt != null ? currentAttempt.getMode() : TransitionMode.CLASSIC;
        AutoTuneManager.recordSuccess(mode);
        NotificationRenderer.showNotification("Успешный переход на анархию!");
        currentAttempt = null;
    }

    public static String modeToStatsKey(TransitionMode mode) {
        return switch (mode) {
            case CLASSIC  -> "classic";
            case LIGHT120 -> "light120";
            case PRIME    -> "prime";
            default       -> "light";
        };
    }

    public static void onDisconnect() {
        HubSwap.getStats().onServerChange(null);
        HubSwap.saveStats();
    }

    public static void cancelAttempt() {
        if (currentAttempt != null) {
            currentAttempt.finish();
            currentAttempt = null;
        }
    }

    public static void failAttempt() {
        if (currentAttempt != null && !currentAttempt.isFinished()) {
            finishFailure();
        }
    }

    private static void finishFailure() {
        if (currentAttempt != null) {
            currentAttempt.finish();
        }
        TransitionMode mode = currentAttempt != null ? currentAttempt.getMode() : TransitionMode.CLASSIC;
        AutoTuneManager.recordFailure(mode);
        NotificationRenderer.showNotification("Переход не удался");
        currentAttempt = null;
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('ё', 'е').trim();
    }
}
