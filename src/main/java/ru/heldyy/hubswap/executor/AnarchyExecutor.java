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

public class AnarchyExecutor {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void executeSequence(String mode, int anarchyNumber) {
        ModConfig config = HubSwap.getConfig();
        AutoTuneManager.Delays delays = AutoTuneManager.getLiveDelays(config);

        if (client.player == null || client.interactionManager == null) {
            sendErrorMessage("Игрок или взаимодействие недоступны");
            return;
        }

        HubSwap.getStats().recordSwitch(mode, anarchyNumber);
        HubSwap.saveStats();

        executor.execute(() -> {
            try {
                if ("classic".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 5) {
                        sendErrorMessage("Недопустимый номер анархии: " + anarchyNumber);
                        return;
                    }

                    TransitionDetector.startAttempt(
                            TransitionMode.CLASSIC,
                            anarchyNumber,
                            delays.hubDelay(),
                            delays.clickDelay(),
                            delays.confirmDelay()
                    );

                    sendCommand("hub");
                    sleep(delays.hubDelay());

                    sendCommand("menu");
                    sleep(delays.clickDelay());

                    clickSlot(15);
                    sleep(delays.clickDelay() + 60L);

                    clickSlot(getClassicTargetSlot(anarchyNumber));
                    return;
                }

                if ("light".equals(mode)) {
                    // Было максимум 69, теперь максимум 70
                    if (anarchyNumber < 1 || anarchyNumber > 70) {
                        sendErrorMessage("Недопустимый номер анархии: " + anarchyNumber);
                        return;
                    }

                    TransitionDetector.startAttempt(
                            TransitionMode.LIGHT,
                            anarchyNumber,
                            delays.hubDelay(),
                            delays.clickDelay(),
                            delays.confirmDelay()
                    );

                    sendCommand("hub");
                    sleep(delays.hubDelay());

                    sendCommand("menu");
                    sleep(delays.clickDelay());

                    clickSlot(12);
                    sleep(delays.clickDelay() + 60L);

                    int[] slots = getLightTargetSlots(anarchyNumber);

                    clickSlot(slots[0]);
                    sleep(delays.clickDelay() + 60L);

                    clickSlot(slots[1]);
                    return;
                }

                if ("light120".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 3) {
                        sendErrorMessage("Недопустимый номер сервера: " + anarchyNumber);
                        return;
                    }

                    TransitionDetector.startAttempt(
                            TransitionMode.LIGHT120,
                            anarchyNumber,
                            delays.hubDelay(),
                            delays.clickDelay(),
                            delays.confirmDelay()
                    );

                    sendCommand("hub");
                    sleep(delays.hubDelay());

                    sendCommand("menu");
                    sleep(delays.clickDelay());

                    clickSlot(10);
                    sleep(delays.clickDelay() + 60L);

                    clickSlot(getLite120TargetSlot(anarchyNumber));
                    return;
                }

                sendErrorMessage("Неизвестный режим: " + mode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendErrorMessage("Ошибка выполнения последовательности");
            } catch (Exception e) {
                sendErrorMessage("Сбой автоперехода: " + e.getClass().getSimpleName());
            }
        });
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

        if (number <= 16) {
            // СолоЛайт #1-#16
            pageSlot = 0;
            offset = number - 1;
        } else if (number <= 37) {
            // ДуоЛайт #17-#37
            pageSlot = 1;
            offset = number - 17;
        } else if (number <= 53) {
            // ТриоЛайт #38-#53
            pageSlot = 2;
            offset = number - 38;
        } else {
            // КланЛайт #54-#70
            pageSlot = 3;
            offset = number - 54;
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

    private static void clickSlot(int slot) {
        client.execute(() -> {
            if (client.interactionManager != null
                    && client.player != null
                    && client.player.currentScreenHandler != null) {

                client.interactionManager.clickSlot(
                        client.player.currentScreenHandler.syncId,
                        slot,
                        0,
                        SlotActionType.PICKUP,
                        client.player
                );
            } else {
                sendErrorMessage("Меню не открыто или синхронизация нарушена");
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
