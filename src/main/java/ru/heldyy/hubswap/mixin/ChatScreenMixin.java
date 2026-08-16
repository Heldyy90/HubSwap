package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.config.ModConfig;

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
            require = 0
    )
    private String hubswap$normalizeCommands(String chatText) {
        return normalize(chatText, HubSwap.getConfig());
    }

    private static String normalize(String chatText, ModConfig cfg) {
        if (chatText == null || chatText.isEmpty() || cfg == null) return chatText;

        char prefix = chatText.charAt(0);
        if (prefix != '.' && prefix != '/') return chatText;

        // Разбираем на команду и аргументы
        var matcher = COMMAND_PATTERN.matcher(chatText);
        if (!matcher.matches()) return chatText;

        String cmdRaw = matcher.group("cmd");
        String args = matcher.group("args");
        if (cmdRaw == null || cmdRaw.isEmpty()) return chatText;

        // Приводим к нижнему регистру и заменяем русские буквы
        String cmd = cmdRaw.toLowerCase(Locale.ROOT)
                .replace('с', 'c')
                .replace('т', 'n')
                .replace('д', 'l');

        // Карта соответствий: введённая команда -> целевая команда и режим
        // Сначала проверяем точные совпадения с алиасами
        String target = null;

        // Проверяем алиасы Lite
        for (String alias : cfg.getLite().getAliases()) {
            if (cmd.equals(alias.toLowerCase(Locale.ROOT))) {
                target = "/ln " + args;
                break;
            }
        }
        // Проверяем алиасы Lite120
        if (target == null) {
            for (String alias : cfg.getLite120().getAliases()) {
                if (cmd.equals(alias.toLowerCase(Locale.ROOT))) {
                    target = "/ln120 " + args;
                    break;
                }
            }
        }
        // Проверяем алиасы Classic
        if (target == null) {
            for (String alias : cfg.getClassic().getAliases()) {
                if (cmd.equals(alias.toLowerCase(Locale.ROOT))) {
                    target = "/cn " + args;
                    break;
                }
            }
        }
        // Проверяем алиасы Prime
        if (target == null) {
            for (String alias : cfg.getPrime().getAliases()) {
                if (cmd.equals(alias.toLowerCase(Locale.ROOT))) {
                    target = "/pm " + args;
                    break;
                }
            }
        }

        // Если не нашли, проверяем жёсткие команды (cn, ln, ln120, pm)
        if (target == null) {
            if (cmd.equals("cn")) {
                target = "/cn " + args;
            } else if (cmd.equals("ln")) {
                target = "/ln " + args;
            } else if (cmd.equals("ln120")) {
                target = "/ln120 " + args;
            } else if (cmd.equals("pm")) {
                target = "/pm " + args;
            }
        }

        // Если ничего не подошло, возвращаем исходный текст
        if (target == null) return chatText;

        // Если были аргументы, но target уже содержит аргументы, убираем дублирование
        // Но так как мы формируем "/cmd args", то всё ок
        return target;
    }
}