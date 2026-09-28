package com.xiaohe66.mc.meteor.lotus;

import com.xiaohe66.mc.meteor.lotus.modules.*;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import com.xiaohe66.mc.meteor.lotus.util.ModsCheck;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * @author He
 * @since 2025.03.07 12:03
 */
public class MeteorLotus extends MeteorAddon {

    private static final Logger log = LoggerFactory.getLogger(MeteorLotus.class);

    @Override
    public void onInitialize() {

        log.info("Initializing xiaohe66-meteor-lotus");

        // Modules
        Modules modules = Modules.get();
        modules.add(new AutoCrafting());
        modules.add(new AutoEnchantment());
        modules.add(new AutoHelmet());
        modules.add(new AutoPlaceMap());
        modules.add(new AutoSignPlus());
        // modules.add(new ElytraAssist());
        modules.add(new ElytraFlyPlus());
        modules.add(new EntityList());
        modules.add(new GatherOrder());
        modules.add(new RedstoneAssist());
        modules.add(new ItemClearUp());
        modules.add(new PacketMinePlus());
        modules.add(new PreviewTool());
        modules.add(new RaidHelper());
        modules.add(new SafeWalkPlus());
        modules.add(new StorageEspPlus());

        if (ModsCheck.hasWorldSchematic()) {
            modules.add(new LitematicaPrinter());
        }

        if (ModsCheck.hasBaritone()) {
            modules.add(new AutoClearUp());
            modules.add(new AutoKit());
            modules.add(new VillagerTrader());
            modules.add(new VillagerBookRoller());
        }

        // trouser
        // modules.add(new ActivatedSpawnerDetector());
        // modules.add(new NewerNewChunks());
        // modules.add(new HoleAndTunnelAndStairsESP());

        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    private void onGameJoinedEvent(GameJoinedEvent event) {
        ChatUtils.warning("已加载 xiaohe66-meteor-lotus, 此插件免费");
        if (!ModsCheck.hasBaritone()) {
            ChatUtils.warning("缺少男中音");
        }

        GatherOrder gatherOrder = Modules.get().get(GatherOrder.class);
        if (gatherOrder != null && gatherOrder.isPermanentlyDisabled() && !gatherOrder.isActive()) {
            gatherOrder.toggle();
        }
    }

    private void tryAddModule(String className, String msg, Supplier<Module> module) {
        Modules modules = Modules.get();

        log.info("try load : {}", className);
        try {
            Class.forName(className);
            log.info("try load success : {}", className);
            modules.add(module.get());

        } catch (ClassNotFoundException ignore) {
            log.warn(msg);
        }
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Const.CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.xiaohe66.mc.meteor.lotus";
    }
}
