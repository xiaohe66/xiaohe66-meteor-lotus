package com.xiaohe66.mc.meteor.lotus.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author He
 * @since 2025.06.08 11:26
 */
public class ModsCheck {

    private static final Logger log = LoggerFactory.getLogger(ModsCheck.class);

    private static Boolean worldSchematic;
    private static Boolean baritone;
    private static Boolean xaeros;

    public static boolean hasWorldSchematic() {

        if (worldSchematic == null) {

            try {
                Class.forName("fi.dy.masa.litematica.world.WorldSchematic");
                worldSchematic = true;

            } catch (ClassNotFoundException e) {
                log.warn("缺少<投影>");
                worldSchematic = false;
            }
        }

        return worldSchematic;
    }

    public static boolean hasBaritone() {

        if (baritone == null) {

            try {
                Class.forName("baritone.api.pathing.goals.Goal");
                baritone = true;

            } catch (ClassNotFoundException e) {
                log.warn("缺少<男中音>");
                baritone = false;
            }
        }

        return baritone;
    }

    public static boolean hasXaeros() {

        if (xaeros == null) {

            try {
                Class.forName("com.github.benmanes.caffeine.cache.Caffeine");
                xaeros = true;

            } catch (ClassNotFoundException e) {
                log.warn("缺少<地图三件套>");
                xaeros = false;
            }
        }

        return xaeros;
    }


}
