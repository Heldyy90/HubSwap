package ru.heldyy.hubswap.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.HotkeySlot;
import ru.heldyy.hubswap.executor.AnarchyExecutor;
import ru.heldyy.hubswap.gui.ConfigScreen;
import ru.heldyy.hubswap.gui.AutoTuneManager;
import ru.heldyy.hubswap.gui.MinecraftStatsHelper;
import ru.heldyy.hubswap.gui.NotificationRenderer;
import ru.heldyy.hubswap.gui.TransitionDetector;
import ru.heldyy.hubswap.updater.AnarchyRangeUpdater;
import ru.heldyy.hubswap.updater.UpdateChecker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

public class HubSwapClient implements ClientModInitializer {
    private static KeyBinding configMenuKey;
    private static final KeyBinding.Category HUBSWAP_CATEGORY = KeyBinding.Category.create(Identifier.of("hubswap", "main"));
    private static CommandDispatcher<FabricClientCommandSource> DISPATCHER;

    private static final Map<Integer, Boolean> hotkeyPressed = new HashMap<>();

    private static final Map<Character, Character> EN_TO_RU = new HashMap<>();

    static {
        EN_TO_RU.put('q', 'й'); EN_TO_RU.put('w', 'ц'); EN_TO_RU.put('e', 'у');
        EN_TO_RU.put('r', 'к'); EN_TO_RU.put('t', 'е'); EN_TO_RU.put('y', 'н');
        EN_TO_RU.put('u', 'г'); EN_TO_RU.put('i', 'ш'); EN_TO_RU.put('o', 'щ');
        EN_TO_RU.put('p', 'з'); EN_TO_RU.put('a', 'ф'); EN_TO_RU.put('s', 'ы');
        EN_TO_RU.put('d', 'в'); EN_TO_RU.put('f', 'а'); EN_TO_RU.put('g', 'п');
        EN_TO_RU.put('h', 'р'); EN_TO_RU.put('j', 'о'); EN_TO_RU.put('k', 'л');
        EN_TO_RU.put('l', 'д'); EN_TO_RU.put('z', 'я'); EN_TO_RU.put('x', 'ч');
        EN_TO_RU.put('c', 'с'); EN_TO_RU.put('v', 'м'); EN_TO_RU.put('b', 'и');
        EN_TO_RU.put('n', 'т'); EN_TO_RU.put('m', 'ь');
    }

    @Override
    public void onInitializeClient() {
        registerKeybinds();
        registerCommands();
        registerTickHandler();
        registerLifecycleEvents();
        NotificationRenderer.register();
    }

    private void registerKeybinds() {
        configMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hubswap.config",
                295,
                HUBSWAP_CATEGORY
        ));
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            DISPATCHER = dispatcher;
            registerConfiguredCommands();
        });
    }

    public static void registerConfiguredCommands() {
        if (DISPATCHER == null) return;

        registerClassicCommand(HubSwap.getConfig().getClassicCommand());
        registerLightCommand(HubSwap.getConfig().getLightCommand());
        registerLight120Command(HubSwap.getConfig().getLight120Command());
        registerPrimeCommand(HubSwap.getConfig().getPrimeCommand());
        registerUtilityCommands();
    }

    private static void registerClassicCommand(String literal) {
        registerWithRuAlias(literal, 1, 5, anarchy -> AnarchyExecutor.executeSequence("classic", anarchy));
    }

    private static void registerLightCommand(String literal) {
        registerWithRuAlias(literal, 1, 9999, anarchy -> AnarchyExecutor.executeSequence("light", anarchy));
    }

    private static void registerLight120Command(String literal) {
        registerWithRuAlias(literal, 1, 3, server -> AnarchyExecutor.executeSequence("light120", server));
    }

    private static void registerPrimeCommand(String literal) {
        registerWithRuAlias(literal, 1, 9, server -> AnarchyExecutor.executeSequence("prime", server));
    }

    private static void registerUtilityCommands() {
        registerSimpleLiteralIfAbsent("hshide", UpdateChecker::hideCurrentNotice);
        registerSimpleLiteralIfAbsent("hsdownload", UpdateChecker::downloadCurrentNotice);
        registerSimpleLiteralIfAbsent("hscheck", UpdateChecker::forceCheck);
    }

    private static void registerSimpleLiteralIfAbsent(String literal, Runnable action) {
        if (DISPATCHER == null || DISPATCHER.getRoot().getChild(literal) != null) return;

        DISPATCHER.register(ClientCommandManager.literal(literal).executes(ctx -> {
            action.run();
            return 1;
        }));
    }

    private static void registerWithRuAlias(String literal, int min, int max, IntConsumer action) {
        if (literal == null || literal.isBlank()) return;

        String base = literal.trim();

        registerLiteralIfAbsent(base, min, max, action);

        String ru = toRussianLayout(base);

        if (!ru.equalsIgnoreCase(base)) {
            registerLiteralIfAbsent(ru, min, max, action);
        }
    }

    private static void registerLiteralIfAbsent(String literal, int min, int max, IntConsumer action) {
        if (literal == null || literal.isBlank() || DISPATCHER == null) return;
        if (DISPATCHER.getRoot().getChild(literal) != null) return;

        DISPATCHER.register(ClientCommandManager.literal(literal)
                .then(ClientCommandManager.argument("номер", IntegerArgumentType.integer(min, max))
                        .executes(ctx -> {
                            int value = IntegerArgumentType.getInteger(ctx, "номер");
                            action.accept(value);
                            return 1;
                       })));
    }

    private static String toRussianLayout(String s) {
        StringBuilder out = new StringBuilder(s.length());

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            char lower = Character.toLowerCase(ch);
            Character mapped = EN_TO_RU.get(lower);

            out.append(mapped != null ? mapped : ch);
        }

        return out.toString();
    }

    private void registerTickHandler() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MinecraftStatsHelper.onClientTick();
            TransitionDetector.onClientTick(client);
            UpdateChecker.onClientTick(client);
            AnarchyRangeUpdater.onClientTick(client);

            if (configMenuKey.wasPressed()) {
                client.setScreen(new ConfigScreen(null));

                if (client.player != null) {
                    Formatting themeColor = HubSwap.getConfig().getColorTheme().getFormatting();

                    client.player.sendMessage(
                            Text.literal("[HubSwap] ").formatted(themeColor)
                                    .append(Text.literal("Открыто меню настроек").formatted(Formatting.WHITE)),
                            false
                    );
                }
            }

            if (client.currentScreen == null && client.player != null) {
                List<HotkeySlot> slots = HubSwap.getConfig().getHotkeySlots();

                for (HotkeySlot slot : slots) {
                    if (!slot.isEnabled() || slot.getKeyCode() < 0) continue;

                    int code = slot.getKeyCode();
                    boolean nowDown = InputUtil.isKeyPressed(client.getWindow(), code);
                    boolean wasDown = hotkeyPressed.getOrDefault(code, false);

                    if (nowDown && !wasDown) {
                        AnarchyExecutor.executeSequence(slot.getMode(), slot.getServerNumber());
                    }

                    hotkeyPressed.put(code, nowDown);
                }
            }
        });
    }

    private void registerLifecycleEvents() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> HubSwap.saveStats());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            AutoTuneManager.onServerJoin();
            UpdateChecker.onServerJoin();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            AutoTuneManager.onServerDisconnect();
            TransitionDetector.onDisconnect();
            UpdateChecker.onDisconnect();
        });
    }
}