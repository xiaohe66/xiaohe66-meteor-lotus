package com.xiaohe66.mc.meteor.lotus.util;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * @author He
 * @since 2025.06.25 13:01
 */
public class HeItemUtils {

    public static boolean isShulkerBox(Item item) {
        return item == Items.SHULKER_BOX
                || item == Items.WHITE_SHULKER_BOX
                || item == Items.ORANGE_SHULKER_BOX
                || item == Items.MAGENTA_SHULKER_BOX
                || item == Items.LIGHT_BLUE_SHULKER_BOX
                || item == Items.YELLOW_SHULKER_BOX
                || item == Items.LIME_SHULKER_BOX
                || item == Items.PINK_SHULKER_BOX
                || item == Items.GRAY_SHULKER_BOX
                || item == Items.LIGHT_GRAY_SHULKER_BOX
                || item == Items.CYAN_SHULKER_BOX
                || item == Items.PURPLE_SHULKER_BOX
                || item == Items.BLUE_SHULKER_BOX
                || item == Items.BROWN_SHULKER_BOX
                || item == Items.GREEN_SHULKER_BOX
                || item == Items.RED_SHULKER_BOX
                || item == Items.BLACK_SHULKER_BOX;
    }

}
