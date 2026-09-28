package com.xiaohe66.mc.meteor.lotus.event;

import net.minecraft.util.PlayerInput;

/**
 * @author He
 * @since 2025.05.10 11:09
 */
public class KeyboardInputTickEvent extends Event {

    private PlayerInput playerInput;

    public KeyboardInputTickEvent() {
        super(Stage.Post);
    }

    public PlayerInput getPlayerInput() {
        return playerInput;
    }

    public void setPlayerInput(PlayerInput playerInput) {
        this.playerInput = playerInput;
    }
}
