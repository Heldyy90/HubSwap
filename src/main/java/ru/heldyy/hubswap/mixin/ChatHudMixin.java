package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.linkify.ServerLinkifier;

/**
 * Надёжный перенос функционала Click-Anarchy:
 * перехватываем добавление сообщений в чат и подменяем Text на "кликабельный",
 * при этом сохраняем исходные стили/цвета всего сообщения (кроме найденного совпадения).
 * 
 * Версия для Minecraft 1.20.1
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    // Основной метод для 1.20.1: addMessage(Text)
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text hubswap$linkify$1(Text message) {
        return ServerLinkifier.linkify(message, HubSwap.getConfig());
    }

    // Метод с подписью сообщения для 1.19+/1.20.x
    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text hubswap$linkify$2(Text message) {
        return ServerLinkifier.linkify(message, HubSwap.getConfig());
    }
}