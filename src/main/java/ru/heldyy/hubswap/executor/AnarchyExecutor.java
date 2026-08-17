package ru.heldyy.hubswap.executor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;
import ru.heldyy.hubswap.gui.AutoTuneManager;
import ru.heldyy.hubswap.gui.TransitionDetector;
import ru.heldyy.hubswap.gui.TransitionMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnarchyExecutor {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean SEQUENCE_RUNNING = new AtomicBoolean(false);

    public static void executeSequence(String mode, int anarchyNumber) {
        ModConfig config = HubSwap.getConfig();
        if (client.player == null || client.interactionManager == null) {
            sendErrorMessage("Игрок или взаимодействие недоступны");
            return;
        }

        if (!SEQUENCE_RUNNING.compareAndSet(false, true)) {
            sendErrorMessage("Переход уже выполняется");
            return;
        }

        HubSwap.getStats().recordSwitch(mode, anarchyNumber);
        HubSwap.saveStats();

        try {
            executor.execute(() -> {
                try {
                if ("classic".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 5) {
                        sendErrorMessage("Недопустимый номер анархии: " + anarchyNumber);
                        return;
                    }

                    AutoTuneManager.Delays delays = AutoTuneManager.getLiveDelays(config, TransitionMode.CLASSIC);

                    boolean alreadyInHub = prepareHubTransition(
                            TransitionMode.CLASSIC,
                            anarchyNumber,
                            delays
                    );

                    if (!alreadyInHub && !waitUntilHubDetected(getClassicHubTimeout(delays))) {
                        TransitionDetector.cancelAttempt();
                        sendErrorMessage("Hub не определился");
                        return;
                    }

                    TransitionDetector.markOpeningMenu();
                    sendCommand("menu");
                    sleep(delays.clickDelay());

                    TransitionDetector.markClicking();
                    clickSlot(16, false);
                    sleep(delays.clickDelay() + 60L);

                    clickSlot(getClassicTargetSlot(anarchyNumber), true);
                    return;
                }

                if ("light".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 74) {
                        sendErrorMessage("Недопустимый номер анархии: " + anarchyNumber);
                        return;
                    }

                    AutoTuneManager.Delays delays = AutoTuneManager.getLiveDelays(config, TransitionMode.LIGHT);

                    boolean alreadyInHub = prepareHubTransition(
                            TransitionMode.LIGHT,
                            anarchyNumber,
                            delays
                    );

                    if (!alreadyInHub && !waitUntilHubDetected(3500L)) {
                        TransitionDetector.cancelAttempt();
                        sendErrorMessage("Hub не определился");
                        return;
                    }

                    TransitionDetector.markOpeningMenu();
                    sendCommand("lite");
                    sleep(delays.clickDelay());

                    int[] slots = getLightTargetSlots(anarchyNumber);

                    TransitionDetector.markClicking();
                    clickSlot(slots[0], false);
                    sleep(delays.clickDelay() + 60L);

                    clickSlot(slots[1], true);
                    return;
                }

                if ("light120".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 3) {
                        sendErrorMessage("Недопустимый номер сервера: " + anarchyNumber);
                        return;
                    }

                    AutoTuneManager.Delays delays = AutoTuneManager.getLiveDelays(config, TransitionMode.LIGHT120);

                    boolean alreadyInHub = prepareHubTransition(
                            TransitionMode.LIGHT120,
                            anarchyNumber,
                            delays
                    );

                    if (!alreadyInHub && !waitUntilHubDetected(3500L)) {
                        TransitionDetector.cancelAttempt();
                        sendErrorMessage("Hub не определился");
                        return;
                    }

                    TransitionDetector.markOpeningMenu();
                    sendCommand("lite120");
                    sleep(delays.clickDelay());

                    TransitionDetector.markClicking();
                    clickSlot(getLite120TargetSlot(anarchyNumber), true);
                    return;
                }

                if ("prime".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 9) {
                        sendErrorMessage("Недопустимый номер Prime: " + anarchyNumber);
                        return;
                    }

                    AutoTuneManager.Delays delays = AutoTuneManager.getLiveDelays(config, TransitionMode.PRIME);

                    boolean alreadyInHub = prepareHubTransition(
                            TransitionMode.PRIME,
                            anarchyNumber,
                            delays
                    );

                    if (!alreadyInHub && !waitUntilHubDetected(3500L)) {
                        TransitionDetector.cancelAttempt();
                        sendErrorMessage("Hub не определился");
                        return;
                    }

                    TransitionDetector.markOpeningMenu();
                    sendCommand("prime");
                    sleep(delays.clickDelay());

                    TransitionDetector.markClicking();
                    clickSlot(getPrimeTargetSlot(anarchyNumber), true);
                    return;
                }

                sendErrorMessage("Неизвестный режим: " + mode);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    sendErrorMessage("Ошибка выполнения последовательности");
                } catch (Exception e) {
                    sendErrorMessage("Сбой автоперехода: " + e.getClass().getSimpleName());
                } finally {
                    SEQUENCE_RUNNING.set(false);
                }
            });
        } catch (RuntimeException e) {
            SEQUENCE_RUNNING.set(false);
            throw e;
        }
    }


    private static boolean prepareHubTransition(TransitionMode mode, int anarchyNumber, AutoTuneManager.Delays delays) {
        boolean alreadyInHub = TransitionDetector.isInHub(client);

        TransitionDetector.startAttempt(
                mode,
                anarchyNumber,
                alreadyInHub ? 0 : delays.hubDelay(),
                delays.clickDelay(),
                delays.confirmDelay()
        );

        if (!alreadyInHub) {
            sendCommand("hub");
        }

        return alreadyInHub;
    }


    private static long getClassicHubTimeout(AutoTuneManager.Delays delays) {
        
        
        return Math.max(3500L, Math.min(6500L, delays.hubDelay() + 1500L));
    }

    private static boolean waitUntilHubDetected(long timeoutMs) throws InterruptedException {
        long started = System.currentTimeMillis();

        while (System.currentTimeMillis() - started < timeoutMs) {
            if (TransitionDetector.isInHub(client)) {
                return true;
            }

            sleep(100L);
        }

        return false;
    }

    private static int getClassicTargetSlot(int number) {
        int[] slots = new int[]{20, 21, 22, 23, 24};

        if (number < 1 || number > slots.length) {
            return slots[0];
        }

        return slots[number - 1];
    }

    private static int[] getLightTargetSlots(int number) {
        int pageSlot;
        int offset;

        if (number <= 17) {
            pageSlot = 0;
            offset = number - 1;
        } else if (number <= 38) {
            pageSlot = 1;
            offset = number - 18;
        } else if (number <= 57) {
            pageSlot = 2;
            offset = number - 39;
        } else {
            pageSlot = 3;
            offset = number - 58;
        }

        int targetSlot = 18 + offset;

        return new int[]{pageSlot, targetSlot};
    }

    private static int getLite120TargetSlot(int number) {
        int[] slots = new int[]{0, 11, 12, 13};

        if (number < 1 || number >= slots.length) {
            return slots[1];
        }

        return slots[number];
    }

    private static int getPrimeTargetSlot(int number) {
        int[] slots = new int[]{0, 10, 11, 12, 13, 14, 15, 16, 19, 20};

        if (number < 1 || number >= slots.length) {
            return slots[1];
        }

        return slots[number];
    }

    private static void sendCommand(String command) {
        client.execute(() -> {
            if (client.player == null || client.getNetworkHandler() == null) {
                return;
            }

            String cmd = command == null ? "" : command.trim();

            if (cmd.startsWith("/")) {
                cmd = cmd.substring(1);
            }

            if (cmd.isEmpty()) {
                return;
            }

            client.getNetworkHandler().sendChatCommand(cmd);
        });
    }

    private static void clickSlot(int slot, boolean targetClick) {
        client.execute(() -> {
            if (client.interactionManager != null
                    && client.player != null
                    && client.player.currentScreenHandler != null
                    && client.currentScreen != null) {

                if (targetClick) {
                    TransitionDetector.markTargetClicked();
                }

                client.interactionManager.clickSlot(
                        client.player.currentScreenHandler.syncId,
                        slot,
                        0,
                        SlotActionType.PICKUP,
                        client.player
                );
            } else {
                sendErrorMessage("Меню не открыто или синхронизация нарушена");
                TransitionDetector.failAttempt();
            }
        });
    }

    private static void sendErrorMessage(String message) {
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[HubSwap] Ошибка: " + message), false);
            }
        });
    }

    private static void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    public static void shutdown() {
        executor.shutdown();
    }
}