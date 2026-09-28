package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.HandledScreenRenderEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseClickEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseDragEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseReleaseEvent;
import com.xiaohe66.mc.meteor.lotus.event.MouseScrollEvent;
import com.xiaohe66.mc.meteor.lotus.event.ScreenCloseEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HandledScreen Mixin - 只负责发布事件
 * <p>
 * 获取需要的值并放入事件中传递，所有业务逻辑由订阅者处理
 *
 * @author He
 * @since 2026.04.03
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> implements ScreenHandlerProvider<T> {

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Inject(
            method = "renderMain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        HandledScreen<?> screen = (HandledScreen<?>) (Object) this;
        MeteorClient.EVENT_BUS.post(HandledScreenRenderEvent.get(context, screen.getTextRenderer(), mouseX, mouseY));
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        MouseScrollEvent event = MouseScrollEvent.get(mouseX, mouseY, verticalAmount, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {

        MouseClickEvent event = MouseClickEvent.get(click.x(), click.y(), click.button(), doubled, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        MouseReleaseEvent event = MouseReleaseEvent.get(click.x(), click.y(), click.button(), this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        MouseDragEvent event = MouseDragEvent.get(click.x(), click.y(), click.button(), offsetX, offsetY, this.x, this.y);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ScreenCloseEvent.get());
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ScreenCloseEvent.get());
    }

}
