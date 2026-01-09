package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;

import java.util.Locale;

/**
 * Поддержка ввода команд в русской раскладке и через точку:
 *  - ".дт 1" -> "/ln 1"
 *  - "/ст 2" -> "/cn 2"
 *  - ".ln 2" -> "/ln 2"
 *  - ".ст 4" -> "/cn 4"
 *
 * Важно: команды подставляются из конфига HubSwap (classic/light/light120).
 */
@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @ModifyVariable(
            method = "sendMessage(Ljava/lang/String;Z)Z",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private String hubswap$normalizeSlashDotCommands(String chatText) {
        return normalize(chatText, HubSwap.getConfig());
    }

    private static String normalize(String chatText, ModConfig cfg) {
        if (chatText == null || chatText.isEmpty() || cfg == null) return chatText;

        char prefix = chatText.charAt(0);
        if (prefix != '.' && prefix != '/') return chatText;

        int space = chatText.indexOf(' ');
        String cmdRaw = (space == -1) ? chatText.substring(1) : chatText.substring(1, space);
        if (cmdRaw.isEmpty()) return chatText;
        String rest = (space == -1) ? "" : chatText.substring(space);

        String cmd = cmdRaw.toLowerCase(Locale.ROOT);

        // Если набрали в русской раскладке (cn -> ст, ln -> дт)
        // Делаем минимальную конвертацию только нужных букв.
        cmd = cmd
                .replace('с', 'c')
                .replace('т', 'n')
                .replace('д', 'l');

        String classic = safeLower(cfg.getClassicCommand());
        String light = safeLower(cfg.getLightCommand());
        String light120 = safeLower(cfg.getLight120Command());

        String mapped = null;

        // Classic aliases
        if (cmd.equals(classic) || cmd.equals("cn")) {
            mapped = cfg.getClassicCommand();
        }

        // Light aliases
        if (mapped == null && (cmd.equals(light) || cmd.equals("ln"))) {
            mapped = cfg.getLightCommand();
        }

        // Light 1.20 aliases
        if (mapped == null && (cmd.equals(light120) || cmd.equals("ln120"))) {
            mapped = cfg.getLight120Command();
        }

        if (mapped == null) return chatText;

        // Точку превращаем в слэш (чтобы работало везде). Слэш оставляем.
        return "/" + mapped + rest;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
