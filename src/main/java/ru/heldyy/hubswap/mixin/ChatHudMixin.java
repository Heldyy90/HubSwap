package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.executor.AnarchyExecutor;
import ru.heldyy.hubswap.linkify.ServerLinkifier;

@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 1
    )
    private Text hubswap$processMessage(Text message) {
        if (message != null) {
            AnarchyExecutor.onChatMessage(message.getString());
        }
        return ServerLinkifier.linkify(message, HubSwap.getConfig());
    }
}
