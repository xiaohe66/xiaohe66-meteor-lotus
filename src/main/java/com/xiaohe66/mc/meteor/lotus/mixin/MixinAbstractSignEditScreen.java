package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.HeOpenScreenEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.entity.SignText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AbstractSignEditScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen extends Screen {

    protected MixinAbstractSignEditScreen(Text textComponent) {
        super(textComponent);
    }

    @Shadow
    private SignText text;

    @Shadow
    @Final
    private String[] messages;

    @Inject(method = "init", at = @At("RETURN"))
    private void preventGuiOpen(CallbackInfo ci) {

        MinecraftClient mc = MinecraftClient.getInstance();

        HeOpenScreenEvent event = HeOpenScreenEvent.get(mc.currentScreen);
        MeteorClient.EVENT_BUS.post(event);

        if (event.getSignText() != null) {
            this.text = event.getSignText();

            for (int i = 0; i < this.messages.length; i++) {
                this.messages[i] = text.getMessage(i, false).getString();
            }
        }
    }
}
