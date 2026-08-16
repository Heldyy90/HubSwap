package ru.heldyy.hubswap.executor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;
import ru.heldyy.hubswap.gui.NotificationRenderer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnarchyExecutor {

    private enum State {
        IDLE,
        WAITING_HUB_WORLD,
        WAITING_MENU,
        WAITING_MENU1,
        WAITING_MENU2
    }

    private static State state = State.IDLE;
    private static String mode = "lite";
    private static int targetNumber = -1;
    private static String categoryKey = null;
    private static String serverKey = null;
    private static World prevWorld = null;
    private static int ticks = 0;
    private static int timeoutTicks = 400;

    private static final Map<String, Integer> currentNumbers = new HashMap<>();

    private static final Pattern LITE120_PATTERN = Pattern.compile("(?i)Лайт\\s*#?\\s*(\\d+)");
    private static final Pattern CLASSIC_PATTERN = Pattern.compile("(?i)Классик\\s*#?\\s*(\\d+)");
    private static final Pattern PRIME_PATTERN = Pattern.compile("(?i)Прайм\\s*#?\\s*(\\d+)");

    private static MinecraftClient client() {
        return MinecraftClient.getInstance();
    }

    public static void top(String modeName, int step) {
        if (step < 1) step = 1;
        ModConfig.ModeConfig modeCfg = getModeSafely(modeName);
        if (modeCfg == null) return;
        int max = modeCfg.getRanges().getTotal();
        if (max <= 0) {
            sendError("Для режима не задан допустимый диапазон серверов");
            return;
        }
        String normalizedMode = normalizeMode(modeName);
        int current = currentNumbers.getOrDefault(normalizedMode, 1);
        int newNumber = ((current - 1 + step) % max) + 1;
        start(normalizedMode, newNumber);
    }

    public static void down(String modeName, int step) {
        if (step < 1) step = 1;
        ModConfig.ModeConfig modeCfg = getModeSafely(modeName);
        if (modeCfg == null) return;
        int max = modeCfg.getRanges().getTotal();
        if (max <= 0) {
            sendError("Для режима не задан допустимый диапазон серверов");
            return;
        }
        String normalizedMode = normalizeMode(modeName);
        int current = currentNumbers.getOrDefault(normalizedMode, 1);
        int normalizedStep = step % max;
        int newNumber = ((current - 1 - normalizedStep + max) % max) + 1;
        start(normalizedMode, newNumber);
    }

    public static void start(String modeName, int number) {
        MinecraftClient client = client();
        if (client.player == null || client.getNetworkHandler() == null) {
            sendError("Вы не подключены к серверу");
            return;
        }

        ModConfig.ModeConfig modeCfg = getModeSafely(modeName);
        if (modeCfg == null) return;

        String normalizedMode = normalizeMode(modeName);
        if (!modeCfg.getRanges().isValid(number)) {
            sendError("Номер вне допустимого диапазона (1-" + modeCfg.getRanges().getMax() + ")");
            return;
        }

        reset();

        mode = normalizedMode;
        targetNumber = number;
        timeoutTicks = HubSwap.getConfig().getTimeoutTicks();
        prevWorld = client.world;
        ticks = 0;

        currentNumbers.put(mode, number);

        if ("lite".equals(mode)) {
            ModConfig.RangeEntry entry = modeCfg.getRanges().find(number);
            if (entry == null) {
                sendError("Не удалось определить категорию для #" + number);
                reset();
                return;
            }
            categoryKey = entry.key;
            serverKey = number == 1 ? "lanarchy" : "lanarchy" + number;
        }

        Screen screen = client.currentScreen;
        if (screen instanceof GenericContainerScreen handledScreen) {
            if ("lite".equals(mode)) {
                var handler = handledScreen.getScreenHandler();
                int containerSlots = getContainerSlotCount(handledScreen);
                boolean foundType = false;
                boolean foundServer = false;
                for (int i = 0; i < containerSlots; i++) {
                    ItemStack stack = handler.getSlot(i).getStack();
                    if (!stack.isEmpty()) {
                        String typeVal = readCustomData(stack, "advancedserverselecter:server-type");
                        String serverVal = readCustomData(stack, "advancedserverselecter:server");
                        if (categoryKey.equals(typeVal)) foundType = true;
                        if (serverKey.equals(serverVal)) foundServer = true;
                    }
                }
                if (foundType && foundServer) {
                    state = State.WAITING_MENU2;
                    return;
                } else if (foundType) {
                    state = State.WAITING_MENU1;
                    return;
                }
            } else {
                Pattern pattern = getPatternForMode(mode);
                if (pattern != null) {
                    int slot = findSlotByLore(handledScreen, pattern, targetNumber);
                    if (slot != -1) {
                        clickSlot(handledScreen, slot);
                        finishSuccess();
                        return;
                    }
                }
            }
        }

        client.getNetworkHandler().sendChatCommand("hub");
        state = State.WAITING_HUB_WORLD;
        ticks = 0;
    }

    public static void onChatMessage(String msg) {
        if (state != State.WAITING_HUB_WORLD || msg == null) return;
        MinecraftClient client = client();
        if (client.player == null || client.getNetworkHandler() == null) {
            reset();
            return;
        }
        String lower = msg.toLowerCase(Locale.ROOT);
        if (lower.contains("уже подключен") || lower.contains("вы уже в лобби") || lower.contains("уже подключены")) {
            sendMenuCommand();
        }
    }

    public static void tick() {
        if (state == State.IDLE) return;
        MinecraftClient client = client();
        if (client.player == null || client.getNetworkHandler() == null) {
            reset();
            return;
        }

        ticks++;
        if (ticks > timeoutTicks) {
            sendError("Таймаут при переходе на #" + targetNumber + " (режим " + mode + ")");
            reset();
            return;
        }

        switch (state) {
            case WAITING_HUB_WORLD -> {
                if (client.world != null && client.world != prevWorld) {
                    sendMenuCommand();
                }
            }
            case WAITING_MENU1 -> scanMenu1();
            case WAITING_MENU2 -> scanMenu2();
            case WAITING_MENU -> scanMenu();
            default -> { }
        }
    }

    private static void sendMenuCommand() {
        MinecraftClient client = client();
        if (client.getNetworkHandler() == null) {
            reset();
            return;
        }
        String menuCmd = switch (mode) {
            case "lite" -> "lite";
            case "lite120" -> "lite120";
            case "classic" -> "anarchy";
            case "prime" -> "prime";
            default -> "";
        };
        if (menuCmd.isEmpty()) {
            reset();
            return;
        }
        client.getNetworkHandler().sendChatCommand(menuCmd);

        state = "lite".equals(mode) ? State.WAITING_MENU1 : State.WAITING_MENU;
        ticks = 0;
    }

    private static void scanMenu1() {
        scanMenuByCustomData("advancedserverselecter:server-type", categoryKey, true);
    }

    private static void scanMenu2() {
        scanMenuByCustomData("advancedserverselecter:server", serverKey, false);
    }

    private static void scanMenu() {
        Screen screen = client().currentScreen;
        if (!(screen instanceof GenericContainerScreen handledScreen)) return;

        Pattern pattern = getPatternForMode(mode);
        if (pattern == null) {
            reset();
            return;
        }

        int slot = findSlotByLore(handledScreen, pattern, targetNumber);
        if (slot != -1) {
            clickSlot(handledScreen, slot);
            finishSuccess();
        }
    }

    private static void scanMenuByCustomData(String nbtKey, String expectedValue, boolean firstMenu) {
        Screen screen = client().currentScreen;
        if (!(screen instanceof GenericContainerScreen handledScreen) || expectedValue == null) return;

        var handler = handledScreen.getScreenHandler();
        int containerSlots = getContainerSlotCount(handledScreen);
        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty()) {
                String value = readCustomData(stack, nbtKey);
                if (expectedValue.equals(value)) {
                    clickSlot(handledScreen, i);
                    if (firstMenu) {
                        state = State.WAITING_MENU2;
                    } else {
                        finishSuccess();
                    }
                    ticks = 0;
                    return;
                }
            }
        }
    }

    private static int findSlotByLore(HandledScreen<?> screen, Pattern pattern, int number) {
        var handler = screen.getScreenHandler();
        int containerSlots = getContainerSlotCount(screen);
        for (int i = 0; i < containerSlots; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty()) {
                String loreText = getNameOrLoreText(stack);
                if (loreText != null && !loreText.isEmpty()) {
                    Matcher m = pattern.matcher(loreText);
                    if (m.find()) {
                        try {
                            int num = Integer.parseInt(m.group(1));
                            if (num == number) return i;
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
        }
        return -1;
    }

    private static int getContainerSlotCount(HandledScreen<?> screen) {
        return Math.max(0, screen.getScreenHandler().slots.size() - 36);
    }

    private static void clickSlot(HandledScreen<?> screen, int slot) {
        MinecraftClient client = client();
        if (client.interactionManager == null || client.player == null) return;
        client.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                client.player
        );
    }

    private static Pattern getPatternForMode(String mode) {
        return switch (mode) {
            case "lite120" -> LITE120_PATTERN;
            case "classic" -> CLASSIC_PATTERN;
            case "prime" -> PRIME_PATTERN;
            default -> null;
        };
    }

    private static String readCustomData(ItemStack stack, String key) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return null;

        NbtCompound root = customData.copyNbt();
        return root.getCompound("PublicBukkitValues")
                .flatMap(values -> values.getString(key))
                .filter(value -> !value.isEmpty())
                .orElse(null);
    }

    private static String getNameOrLoreText(ItemStack stack) {
        StringBuilder text = new StringBuilder();

        String name = stack.getName().getString();
        if (name != null && !name.isBlank()) {
            text.append(name);
        }

        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String value = line.getString();
                if (value == null || value.isBlank()) continue;
                if (!text.isEmpty()) text.append(' ');
                text.append(value);
            }
        }

        String result = text.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private static void finishSuccess() {
        HubSwap.getStats().recordSwitch(mode, targetNumber);
        HubSwap.getStats().onServerChange(mode);
        NotificationRenderer.showNotification("Успешный переход на " + mode + " #" + targetNumber);
        reset();
    }

    private static ModConfig.ModeConfig getModeSafely(String modeName) {
        try {
            return HubSwap.getConfig().getMode(modeName);
        } catch (IllegalArgumentException e) {
            sendError(e.getMessage());
            return null;
        }
    }

    private static String normalizeMode(String modeName) {
        if (modeName == null) return "lite";
        return switch (modeName.toLowerCase(Locale.ROOT)) {
            case "light" -> "lite";
            case "light120" -> "lite120";
            default -> modeName.toLowerCase(Locale.ROOT);
        };
    }

    private static void sendError(String msg) {
        MinecraftClient client = client();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[HubSwap] Ошибка: " + msg), false);
        }
        reset();
    }

    private static void reset() {
        state = State.IDLE;
        mode = "lite";
        targetNumber = -1;
        categoryKey = null;
        serverKey = null;
        prevWorld = null;
        ticks = 0;
    }

    public static boolean isBusy() {
        return state != State.IDLE;
    }
}
