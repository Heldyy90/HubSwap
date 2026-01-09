package ru.heldyy.hubswap.executor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;
import ru.heldyy.hubswap.gui.NotificationRenderer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnarchyExecutor {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final ModConfig config = HubSwap.getConfig();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * mode:
     *  - "классик"  -> кликаем режим Classic (слот 15) + выбираем классик-анархию
     *  - "лайт"     -> кликаем режим Lite (слот 12) + выбираем лайт-анархию (1..69)
     *  - "лайт120"  -> кликаем режим Lite 1.20 (слот 10) + выбираем сервер (1..3)
     */
    public static void executeSequence(String mode, int anarchyNumber) {
        if (client.player == null || client.interactionManager == null) {
            sendErrorMessage("Игрок или взаимодействие недоступны");
            return;
        }

        executor.execute(() -> {
            try {
                sendCommand("hub");
                sleep(config.getClassicDelay());
                sendCommand("menu");
                sleep(config.getClickDelay());

                if ("классик".equals(mode)) {
                    // По текущему меню на скрине классик-выбора есть 8 анархий.
                    if (anarchyNumber < 1 || anarchyNumber > 8) {
                        sendErrorMessage("Недопустимый номер анархии: " + anarchyNumber);
                        return;
                    }
                    clickSlot(15); // Classic (как ты указал)
                    sleep(config.getClickDelay());
                    clickSlot(getClassicTargetSlot(anarchyNumber));

                    sleep(2000L);
                    NotificationRenderer.showNotification("Вы перемещены на " + anarchyNumber + " классик анархию");
                    return;
                }

                if ("лайт".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 69) {
                        sendErrorMessage("Недопустимый номер анархии: " + anarchyNumber);
                        return;
                    }
                    clickSlot(12); // Lite
                    sleep(config.getClickDelay());
                    int[] slots = getLightTargetSlots(anarchyNumber);
                    clickSlot(slots[0]);
                    sleep(config.getClickDelay());
                    clickSlot(slots[1]);

                    sleep(2000L);
                    NotificationRenderer.showNotification("Вы перемещены на " + anarchyNumber + " лайт анархию");
                    return;
                }

                if ("лайт120".equals(mode)) {
                    if (anarchyNumber < 1 || anarchyNumber > 3) {
                        sendErrorMessage("Недопустимый номер сервера: " + anarchyNumber);
                        return;
                    }
                    clickSlot(10); // Lite 1.20 (как ты указал)
                    sleep(config.getClickDelay());
                    clickSlot(getLite120TargetSlot(anarchyNumber));

                    sleep(2000L);
                    NotificationRenderer.showNotification("Вы перемещены на Лайт 1.20 сервер №" + anarchyNumber);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sendErrorMessage("Ошибка выполнения последовательности");
            }
        });
    }

    /**
     * Classic menu slot mapping (по скрину "Выберите Классик. Анархию"):
     *  1..5 в ряд, 6..8 ниже по центру.
     */
    private static int getClassicTargetSlot(int number) {
        // Индексы слотов (0-based внутри screenHandler), рассчитано по раскладке как на скрине.
        // Если у вас на сервере смещено — скажешь, я поправлю по точным слотам.
        // По скрину «Выберите Классик. Анархию»:
        // 1..5 — ряд по центру (row2 col2..6) => 20..24
        // 6..8 — ряд ниже (row3 col3..5)      => 30..32
        int[] slots = new int[]{20, 21, 22, 23, 24, 30, 31, 32}; // index (number-1)
        if (number < 1 || number > slots.length) {
            sendErrorMessage("Недопустимый номер анархии: " + number);
            return slots[0];
        }
        return slots[number - 1];
    }

    /**
     * Lite mapping (новые диапазоны):
     *  page 0: 1..16
     *  page 1: 17..37
     *  page 2: 38..53
     *  page 3: 54..69 (64 отображается одинаково — кликаем по порядку)
     */
    private static int[] getLightTargetSlots(int number) {
        int pageSlot;
        int offset;

        if (number <= 16) {
            pageSlot = 0;
            offset = number - 1;
        } else if (number <= 37) {
            pageSlot = 1;
            offset = number - 17;
        } else if (number <= 53) {
            pageSlot = 2;
            offset = number - 38;
        } else {
            pageSlot = 3;
            offset = number - 54;
        }

        int targetSlot = 18 + offset;
        return new int[]{pageSlot, targetSlot};
    }

    /**
     * Lite 1.20 menu (скрин "Выберите сервер Лайт"):
     * серверы 1..3 стоят подряд.
     */
    private static int getLite120TargetSlot(int number) {
        // По скрину: 1,2,3 стоят во 2-й строке подряд.
        // Обычно это слоты 11,12,13 (если контейнер 3 ряда) или всё равно такие же индексы в меню.
        int[] slots = new int[]{0, 11, 12, 13};
        if (number < 1 || number >= slots.length) return slots[1];
        return slots[number];
    }

    private static void sendCommand(String command) {
        client.execute(() -> {
            if (client.player == null || client.getNetworkHandler() == null) {
                return;
            }

            String cmd = command == null ? "" : command.trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            if (cmd.isEmpty()) return;

            // 1.20.1+: правильный путь отправки команды с учётом подписанных чатов
            client.getNetworkHandler().sendChatCommand(cmd);
        });
    }

    private static void clickSlot(int slot) {
        client.execute(() -> {
            if (client.interactionManager != null && client.player != null && client.player.currentScreenHandler != null) {
                client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
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
