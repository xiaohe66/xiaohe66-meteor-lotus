package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * @author He
 * @since 2025.08.22 23:51
 */
public class HandledScreenRenderEvent extends Event {

    private static final HandledScreenRenderEvent INSTANCE = new HandledScreenRenderEvent();

    private DrawContext drawContext;
    private TextRenderer textRenderer;
    private int mouseX;
    private int mouseY;

    public HandledScreenRenderEvent() {
        super(Stage.Post);
    }

    public static HandledScreenRenderEvent get(DrawContext drawContext, TextRenderer textRenderer, int mouseX, int mouseY) {
        INSTANCE.drawContext = drawContext;
        INSTANCE.textRenderer = textRenderer;
        INSTANCE.mouseX = mouseX;
        INSTANCE.mouseY = mouseY;
        return INSTANCE;
    }

    public DrawContext getDrawContext() {
        return drawContext;
    }

    public TextRenderer getTextRenderer() {
        return textRenderer;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }
}