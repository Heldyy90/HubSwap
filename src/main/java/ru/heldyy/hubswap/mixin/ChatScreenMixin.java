package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;

import java.util.Locale;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @ModifyVariable(
            method = "sendMessage(Ljava/lang/String;Z)V",
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

        cmd = cmd
                .replace('с', 'c')
                .replace('т', 'n')
                .replace('д', 'l')
                .replace('з', 'p');

        String classic = safeLower(cfg.getClassicCommand());
        String light = safeLower(cfg.getLightCommand());
        String light120 = safeLower(cfg.getLight120Command());
        String prime = safeLower(cfg.getPrimeCommand());

        String mapped = null;

        if (cmd.equals(classic) || cmd.equals("cn")) {
            mapped = cfg.getClassicCommand();
        }

        if (mapped == null && (cmd.equals(light) || cmd.equals("ln"))) {
            mapped = cfg.getLightCommand();
        }

        if (mapped == null && (cmd.equals(light120) || cmd.equals("ln120"))) {
            mapped = cfg.getLight120Command();
        }

        if (mapped == null && (cmd.equals(prime) || cmd.equals("pn"))) {
            mapped = cfg.getPrimeCommand();
        }

        if (mapped == null) return chatText;

        return "/" + mapped + rest;
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
