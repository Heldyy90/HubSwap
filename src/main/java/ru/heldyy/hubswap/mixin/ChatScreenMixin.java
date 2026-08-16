package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^(?<prefix>[./])\\s*(?<cmd>\\S+)\\s*(?<args>.*)"
    );

    @ModifyVariable(
            method = "sendMessage(Ljava/lang/String;Z)Z",
            at = @At("HEAD"),
            argsOnly = true,
            require = 1
    )
    private String hubswap$normalizeCommands(String chatText) {
        return normalize(chatText, HubSwap.getConfig());
    }

    private static String normalize(String chatText, ModConfig cfg) {
        if (chatText == null || chatText.isEmpty() || cfg == null) return chatText;

        char prefix = chatText.charAt(0);
        if (prefix != '.' && prefix != '/') return chatText;

        var matcher = COMMAND_PATTERN.matcher(chatText);
        if (!matcher.matches()) return chatText;

        String cmdRaw = matcher.group("cmd");
        String args = matcher.group("args");
        if (cmdRaw == null || cmdRaw.isEmpty()) return chatText;

        String cmd = normalizeCommandToken(cmdRaw);
        String target = matchAliases(cmd, cfg.getLite().getAliases(), "ln", args);
        if (target == null) target = matchAliases(cmd, cfg.getLite120().getAliases(), "ln120", args);
        if (target == null) target = matchAliases(cmd, cfg.getClassic().getAliases(), "cn", args);
        if (target == null) target = matchAliases(cmd, cfg.getPrime().getAliases(), "pm", args);

        if (target == null) {
            target = switch (cmd) {
                case "cn" -> buildCommand("cn", args);
                case "ln" -> buildCommand("ln", args);
                case "ln120" -> buildCommand("ln120", args);
                case "pm", "pn" -> buildCommand("pm", args);
                default -> null;
            };
        }

        return target == null ? chatText : target;
    }

    private static String matchAliases(String normalizedCommand, List<String> aliases, String targetCommand, String args) {
        if (aliases == null) return null;
        for (String alias : aliases) {
            if (alias == null || alias.isBlank()) continue;
            if (normalizedCommand.equals(normalizeCommandToken(alias))) {
                return buildCommand(targetCommand, args);
            }
        }
        return null;
    }

    private static String buildCommand(String command, String args) {
        String cleanArgs = args == null ? "" : args.trim();
        return cleanArgs.isEmpty() ? "/" + command : "/" + command + " " + cleanArgs;
    }

    private static String normalizeCommandToken(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            out.append(switch (lower.charAt(i)) {
                case 'й' -> 'q'; case 'ц' -> 'w'; case 'у' -> 'e'; case 'к' -> 'r';
                case 'е' -> 't'; case 'н' -> 'y'; case 'г' -> 'u'; case 'ш' -> 'i';
                case 'щ' -> 'o'; case 'з' -> 'p'; case 'ф' -> 'a'; case 'ы' -> 's';
                case 'в' -> 'd'; case 'а' -> 'f'; case 'п' -> 'g'; case 'р' -> 'h';
                case 'о' -> 'j'; case 'л' -> 'k'; case 'д' -> 'l'; case 'я' -> 'z';
                case 'ч' -> 'x'; case 'с' -> 'c'; case 'м' -> 'v'; case 'и' -> 'b';
                case 'т' -> 'n'; case 'ь' -> 'm';
                default -> lower.charAt(i);
            });
        }
        return out.toString();
    }
}
