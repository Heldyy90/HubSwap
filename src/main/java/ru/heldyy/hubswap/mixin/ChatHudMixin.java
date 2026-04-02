package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.gui.TransitionDetector;
import ru.heldyy.hubswap.linkify.ServerLinkifier;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text hubswap$processMessage1(Text message) {
        if (message != null) {
            TransitionDetector.onChatMessage(message.getString());
        }
        return ServerLinkifier.linkify(message, HubSwap.getConfig());
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0
    )
    private Text hubswap$processMessage2(Text message) {
        if (message != null) {
            TransitionDetector.onChatMessage(message.getString());
        }
        return ServerLinkifier.linkify(message, HubSwap.getConfig());
    }
}