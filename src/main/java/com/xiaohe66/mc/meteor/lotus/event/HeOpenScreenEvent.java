package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.block.entity.SignText;
import net.minecraft.client.gui.screen.Screen;

/**
 * @author He
 * @since 2025.06.28 21:31
 */
public class HeOpenScreenEvent extends Event {

    private static final HeOpenScreenEvent INSTANCE = new HeOpenScreenEvent();

    public Screen screen;

    private SignText signText;

    public HeOpenScreenEvent() {
        super(Stage.Post);
    }

    public SignText getSignText() {
        return signText;
    }

    public void setSignText(SignText signText) {
        this.signText = signText;
    }

    public static HeOpenScreenEvent get(Screen screen) {
        INSTANCE.signText = null;
        INSTANCE.setCancelled(false);
        INSTANCE.screen = screen;
        return INSTANCE;
    }
}
