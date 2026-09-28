package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.ScreenRenderEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author He
 * @since 2025.08.22 23:50
 */
@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        Screen screen = (Screen) (Object) this;
        MeteorClient.EVENT_BUS.post(ScreenRenderEvent.get(context, screen.getTextRenderer(), mouseX, mouseY));
    }
}