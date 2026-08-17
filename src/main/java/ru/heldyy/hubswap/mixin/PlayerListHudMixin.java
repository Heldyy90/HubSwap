package ru.heldyy.hubswap.mixin;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heldyy.hubswap.gui.TransitionDetector;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {

    @Inject(method = "setHeader", at = @At("HEAD"), require = 0)
    private void hubswap$setHeader(Text header, CallbackInfo ci) {
        if (header != null) {
            TransitionDetector.onTabText(header.getString());
        }
    }

    @Inject(method = "setFooter", at = @At("HEAD"), require = 0)
    private void hubswap$setFooter(Text footer, CallbackInfo ci) {
        if (footer != null) {
            TransitionDetector.onTabText(footer.getString());
        }
    }
}
