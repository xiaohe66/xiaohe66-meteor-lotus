package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.BepInventoryManager;
import com.xiaohe66.mc.meteor.lotus.util.BepRotationUtils;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author He
 * @since 2025.05.17 00:22
 */
public class PacketMinePlus extends BaseModule {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoMine = settings.createGroup("自动挖掘");
    private final SettingGroup sgRender = settings.createGroup("渲染");
    private final Setting<SpeedmineMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<SpeedmineMode>()
            .name("模式")
            .description("速度挖掘的挖掘模式")
            .defaultValue(SpeedmineMode.PACKET)
            .build());
    private final Setting<Boolean> multitaskConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("多任务")
            .description("允许在使用物品时挖掘")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    public final Setting<Boolean> doubleBreakConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("双重破坏")
            .description("允许你同时挖掘两个方块")
            .defaultValue(true)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
            .name("范围")
            .description("挖掘方块的范围")
            .defaultValue(4.5)
            .min(0.1)
            .sliderRange(0.1, 6.0)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<Double> speedConfig = sgGeneral.add(new DoubleSetting.Builder()
            .name("速度")
            .description("挖掘方块的速度")
            .defaultValue(1.0)
            .min(0.1)
            .sliderRange(0.1, 1.0)
            .build());
    private final Setting<Boolean> instantConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("瞬间挖掘")
            .description("立即挖掘已经破坏的方块")
            .defaultValue(true)
            .build());
    private final Setting<Keybind> instantToggleKey = sgGeneral.add(new KeybindSetting.Builder()
            .name("瞬间挖掘切换键")
            .description("切换瞬间挖掘选项的按键")
            .defaultValue(Keybind.none())
            .build());
    private final Setting<Boolean> persistentConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("持久模式")
            .description("即使模块被禁用也保持数据包挖掘漏洞激活（防止漏洞破坏）")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .onChanged(enabled -> {
                if (enabled && mc.player != null) {
                    mc.player.sendMessage(Text.literal("§7[§bBepMine§7] §a持久模式已启用！除非你关闭此模式或断开连接，否则模块无法被禁用。"), false);
                }
            })
            .build());
    private final Setting<Swap> swapConfig = sgGeneral.add(new EnumSetting.Builder<Swap>()
            .name("自动切换")
            .description("挖掘完成后切换到最佳工具")
            .defaultValue(Swap.SILENT)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("旋转")
            .description("挖掘方块时旋转")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<Boolean> switchResetConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("切换重置")
            .description("切换物品后重置挖掘")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<Boolean> grimConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("Grim")
            .description("使用Grim方块破坏速度")
            .defaultValue(true)
            .build());
    private final Setting<Boolean> grimNewConfig = sgGeneral.add(new BoolSetting.Builder()
            .name("Grim-v3")
            .description("使用新的Grim方块破坏速度")
            .defaultValue(true)
            .visible(grimConfig::get)
            .build());
    private final Setting<Boolean> miningFix = sgGeneral.add(new BoolSetting.Builder()
            .name("挖掘修复")
            .description("针对Grim v3的挖掘修复")
            .defaultValue(false)
            .visible(() -> grimConfig.get() && grimNewConfig.get())
            .build());
    private final Setting<Keybind> autoMineKey = sgAutoMine.add(new KeybindSetting.Builder()
            .name("自动挖掘键")
            .description("切换自动挖掘敌人的按键")
            .defaultValue(Keybind.none())
            .build());
    private final Setting<Boolean> autoMine = sgAutoMine.add(new BoolSetting.Builder()
            .name("自动挖掘")
            .description("自动挖掘附近敌人周围的方块")
            .defaultValue(false)
            .build());
    private final Setting<Double> enemyRange = sgAutoMine.add(new DoubleSetting.Builder()
            .name("敌人范围")
            .description("搜索敌方玩家的范围")
            .defaultValue(5.0)
            .min(1.0)
            .sliderRange(1.0, 10.0)
            .visible(autoMine::get)
            .build());
    private final Setting<Boolean> strictDirection = sgAutoMine.add(new BoolSetting.Builder()
            .name("严格方向")
            .description("只挖掘可见面上的方块")
            .defaultValue(false)
            .visible(autoMine::get)
            .build());
    private final Setting<Boolean> targetHead = sgAutoMine.add(new BoolSetting.Builder()
            .name("目标头部")
            .description("同时瞄准头部高度（Y+1）的方块")
            .defaultValue(false)
            .visible(autoMine::get)
            .build());
    private final Setting<Boolean> autoRotate = sgAutoMine.add(new BoolSetting.Builder()
            .name("自动旋转")
            .description("旋转到敌人方块（使用静默旋转）")
            .defaultValue(true)
            .visible(autoMine::get)
            .build());
    private final Setting<Boolean> antiCrawl = sgAutoMine.add(new BoolSetting.Builder()
            .name("防爬行")
            .description("爬行时自动挖掘头顶的方块以站立起来")
            .defaultValue(true)
            .visible(autoMine::get)
            .build());
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("渲染")
            .description("是否渲染正在挖掘的方块")
            .defaultValue(true)
            .build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("形状模式")
            .description("形状的渲染方式")
            .defaultValue(ShapeMode.Both)
            .build());
    private final Setting<SettingColor> colorConfig = sgRender.add(new ColorSetting.Builder()
            .name("挖掘颜色")
            .description("挖掘渲染颜色")
            .defaultValue(new SettingColor(java.awt.Color.BLUE))
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<SettingColor> colorDoneConfig = sgRender.add(new ColorSetting.Builder()
            .name("完成颜色")
            .description("完成渲染颜色")
            .defaultValue(new SettingColor(Color.CYAN))
            .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
            .build());
    private final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
            .name("淡出时间")
            .description("淡出时间")
            .defaultValue(250)
            .min(0)
            .sliderRange(0, 1000)
            .visible(() -> false)
            .build());

    private final Map<MiningData, Animation> fadeList = new HashMap<>();
    private FirstOutQueue<MiningData> miningQueue;
    private long lastBreak;
    private boolean instantTogglePressed = false;
    private boolean autoMineTogglePressed = false;
    private PlayerEntity currentTarget = null;
    private BlockPos lastAutoMineBlock = null;
    private long lastAutoMineTime = 0;
    private BlockPos lastAntiCrawlBlock = null;
    private long lastAntiCrawlTime = 0;
    private static final long AUTO_MINE_DELAY_MS = 250;
    private static final long ANTI_CRAWL_DELAY_MS = 100;
    private int swappedToSlot = -1;
    private int originalSlot = -1;
    private int swapBackTicks = 0;
    private BepInventoryManager inventoryManager;

    public PacketMinePlus() {
        super("Bep包挖掘", "移植自Bep");
    }

    public Setting<Double> getSpeedConfig() {
        return speedConfig;
    }

    public Setting<SpeedmineMode> getModeConfig() {
        return modeConfig;
    }

    @Override
    public void toggle() {
        if (isActive() && persistentConfig.get() && mc.getNetworkHandler() != null) {
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("§7[§bBepMine§7] §cCannot disable while Persistent mode is active! Disable Persistent first or disconnect from server."), false);
            }
            return;
        }
        super.toggle();
    }

    @Override
    public void onActivate() {
        if (doubleBreakConfig.get()) {
            miningQueue = new FirstOutQueue<>(2);
        } else {
            miningQueue = new FirstOutQueue<>(1);
        }
        if (swapConfig.get() == Swap.SILENT) {
            inventoryManager = BepInventoryManager.getInstance();
        }
        swappedToSlot = -1;
        originalSlot = -1;
        swapBackTicks = 0;
        lastAutoMineBlock = null;
        lastAutoMineTime = 0;
        lastAntiCrawlBlock = null;
        lastAntiCrawlTime = 0;
    }

    @Override
    public void onDeactivate() {
        if (persistentConfig.get() && mc.getNetworkHandler() != null) {
            return;
        }
        if (miningQueue != null) {
            miningQueue.clear();
        }
        fadeList.clear();
        if (swapConfig.get() == Swap.SILENT && inventoryManager != null) {
            inventoryManager.syncToClient();
        }
        swappedToSlot = -1;
        originalSlot = -1;
        swapBackTicks = 0;
        lastAutoMineBlock = null;
        lastAutoMineTime = 0;
        lastAntiCrawlBlock = null;
        lastAntiCrawlTime = 0;
        currentTarget = null;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (miningQueue != null) {
            miningQueue.clear();
        }
        fadeList.clear();
        swappedToSlot = -1;
        originalSlot = -1;
        swapBackTicks = 0;
        lastAutoMineBlock = null;
        lastAutoMineTime = 0;
        lastAntiCrawlBlock = null;
        lastAntiCrawlTime = 0;
        currentTarget = null;
    }

    @EventHandler
    public void onPlayerTick(final TickEvent.Pre event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (swapBackTicks > 0) {
            swapBackTicks--;
            if (swapBackTicks == 0 && swappedToSlot != -1 && originalSlot != -1) {
                swapBack(originalSlot);
                swappedToSlot = -1;
                originalSlot = -1;
            }
        }
        if (autoMineKey.get().isPressed() && mc.currentScreen == null) {
            if (!autoMineTogglePressed) {
                autoMineTogglePressed = true;
                autoMine.set(!autoMine.get());
                if (mc.player != null) {
                    String status = autoMine.get() ? "§aenabled" : "§cdisabled";
                    mc.player.sendMessage(Text.literal("§7[§bBepMine§7] §fAuto-mine " + status), false);
                }
            }
        } else {
            autoMineTogglePressed = false;
        }
        if (instantToggleKey.get().isPressed() && mc.currentScreen == null) {
            if (!instantTogglePressed) {
                instantTogglePressed = true;
                instantConfig.set(!instantConfig.get());
                if (!instantConfig.get()) {
                    miningQueue.clear();
                }
                if (mc.player != null) {
                    String status = instantConfig.get() ? "§aenabled" : "§cdisabled";
                    mc.player.sendMessage(Text.literal("§7[§bBepMine§7] §fInstant mining " + status), false);
                }
            }
        } else {
            instantTogglePressed = false;
        }
        if (modeConfig.get() == SpeedmineMode.DAMAGE) {
            return;
        }
        if (autoMine.get() && modeConfig.get() == SpeedmineMode.PACKET) {
            int maxQueueSize = doubleBreakConfig.get() ? 2 : 1;
            long currentTime = System.currentTimeMillis();
            if (antiCrawl.get() && mc.player.getPose() == EntityPose.SWIMMING) {
                if (miningQueue.size() < maxQueueSize && currentTime - lastAntiCrawlTime >= ANTI_CRAWL_DELAY_MS) {
                    BlockPos crawlBlock = getAntiCrawlBlock();
                    if (crawlBlock != null && !isMiningBlock(crawlBlock)) {
                        Direction direction = Direction.DOWN;
                        if (autoRotate.get() && rotateConfig.get()) {
                            float[] rotations = getRotationsTo(mc.player.getEyePos(), crawlBlock.toCenterPos());
                            if (grimConfig.get()) {
                                BepRotationUtils.getInstance().setRotationSilent(rotations[0], rotations[1]);
                            } else {
                                Rotations.rotate(rotations[0], rotations[1]);
                            }
                        }
                        MiningData data = new MiningData(crawlBlock, direction);
                        queueMiningData(data);
                        lastAntiCrawlBlock = crawlBlock;
                        lastAntiCrawlTime = currentTime;
                        if (miningQueue.isEmpty()) {
                            return;
                        }
                    }
                }
            }
            currentTarget = getClosestEnemy();
            if (currentTarget != null) {
                if (miningQueue.size() < maxQueueSize) {
                    if (currentTime - lastAutoMineTime >= AUTO_MINE_DELAY_MS) {
                        BlockPos targetBlock = findBestEnemyBlock(currentTarget);
                        if (targetBlock != null && !isMiningBlock(targetBlock)) {
                            Direction direction = getInteractDirection(targetBlock);
                            if (direction == null && strictDirection.get()) {
                            } else {
                                if (direction == null) direction = Direction.UP;
                                if (autoRotate.get() && rotateConfig.get()) {
                                    float[] rotations = getRotationsTo(mc.player.getEyePos(), targetBlock.toCenterPos());
                                    if (grimConfig.get()) {
                                        BepRotationUtils.getInstance().setRotationSilent(rotations[0], rotations[1]);
                                    } else {
                                        Rotations.rotate(rotations[0], rotations[1]);
                                    }
                                }
                                MiningData data = new MiningData(targetBlock, direction);
                                queueMiningData(data);
                                lastAutoMineBlock = targetBlock;
                                lastAutoMineTime = currentTime;
                            }
                        }
                    }
                }
            } else {
                lastAutoMineBlock = null;
            }
        }
        if (miningQueue.isEmpty()) {
            return;
        }
        java.util.List<MiningData> toRemove = new ArrayList<>();
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                data.resetBreakTime();
            }
            if (isDataPacketMine(data) && (data.getState().isAir() || data.hasAttemptedBreak() && data.passedAttemptedBreakTime(500))) {
                toRemove.add(data);
                continue;
            }
            final float damageDelta = calcBlockBreakingDelta(data.getState(), mc.world, data.getPos());
            data.damage(damageDelta);
            if (isDataPacketMine(data) && data.getBlockDamage() >= 1.0f && data.getSlot() != -1) {
                if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                    return;
                }
                if (!data.hasAttemptedBreak()) {
                    data.setAttemptedBreak(true);
                }
            }
        }
        miningQueue.removeAll(toRemove);
        MiningData miningData2 = miningQueue.getFirst();
        if (miningData2 == null) return;
        final double distance = mc.player.getEyePos().squaredDistanceTo(miningData2.getPos().toCenterPos());
        if (distance > rangeConfig.get() * rangeConfig.get()) {
            miningQueue.remove(miningData2);
            return;
        }
        if (miningData2.getState().isAir()) {
            return;
        }
        if (miningData2.getBlockDamage() >= speedConfig.get() && miningData2.hasAttemptedBreak() && miningData2.passedAttemptedBreakTime(500)) {
            abortMining(miningData2);
            miningQueue.remove(miningData2);
        }
        if (miningData2.getBlockDamage() >= speedConfig.get()) {
            if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                return;
            }
            stopMining(miningData2);
            if (!miningData2.hasAttemptedBreak()) {
                miningData2.setAttemptedBreak(true);
            }
            if (!instantConfig.get()) {
                miningQueue.remove(miningData2);
            }
        }
    }

    @EventHandler
    public void onAttackBlock(final StartBreakingBlockEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator() || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }
        event.cancel();
        BlockState blockState = mc.world.getBlockState(event.blockPos);
        if (blockState.getHardness(mc.world, event.blockPos) == -1.0f || blockState.isAir()) {
            return;
        }
        startManualMine(event.blockPos, event.direction);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet
                && packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
                && modeConfig.get() == SpeedmineMode.DAMAGE && grimConfig.get()) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().up(500), packet.getDirection()));
        }
        if (event.packet instanceof UpdateSelectedSlotC2SPacket && switchResetConfig.get()
                && modeConfig.get() == SpeedmineMode.PACKET) {
            for (MiningData data : miningQueue) {
                data.resetDamage();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }
        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            handleBlockUpdatePacket(packet);
        } else if (event.packet instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
                if (packet1 instanceof BlockUpdateS2CPacket packet2) {
                    handleBlockUpdatePacket(packet2);
                }
            }
        }
    }

    private void handleBlockUpdatePacket(BlockUpdateS2CPacket packet) {
        if (!packet.getState().isAir()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
                data.setAttemptedBreak(false);
            }
        }
    }

    @EventHandler
    public void onRenderWorld(final Render3DEvent event) {
        if (mc.player.isCreative() || modeConfig.get() != SpeedmineMode.PACKET || !render.get()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                continue;
            }
            if (!fadeList.containsKey(data)) {
                fadeList.put(data, new Animation(true, fadeTimeConfig.get()));
            }
        }
        for (Map.Entry<MiningData, Animation> entry : fadeList.entrySet()) {
            MiningData data = entry.getKey();
            boolean isActive = miningQueue.contains(data) && !data.getState().isAir();
            entry.getValue().setState(isActive);
        }
        for (Map.Entry<MiningData, Animation> set : fadeList.entrySet()) {
            MiningData data = set.getKey();
            int boxAlpha = (int) (40 * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());
            int boxColor;
            int lineColor;
            boxColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ?
                    colorDoneConfig.get().getPacked() : colorConfig.get().getPacked();
            lineColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ?
                    colorDoneConfig.get().getPacked() : colorConfig.get().getPacked();
            boxColor = (boxColor & 0x00FFFFFF) | (boxAlpha << 24);
            lineColor = (lineColor & 0x00FFFFFF) | (lineAlpha << 24);
            BlockPos mining = data.getPos();
            VoxelShape outlineShape = data.getState().getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
            Box render1 = outlineShape.getBoundingBox();
            Box render = new Box(mining.getX() + render1.minX, mining.getY() + render1.minY,
                    mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                    mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            net.minecraft.util.math.Vec3d center = render.getCenter();
            float total = isDataPacketMine(data) ? 1.0f : speedConfig.get().floatValue();
            float scale = data.getState().isAir() ? 1.0f : MathHelper.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.tickDelta) / total, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
            event.renderer.box(scaled.minX, scaled.minY, scaled.minZ, scaled.maxX, scaled.maxY, scaled.maxZ,
                    new SettingColor(boxColor), new SettingColor(lineColor), shapeMode.get(), 0);
        }
        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
    }

    private void startManualMine(BlockPos pos, Direction direction) {
        clickMine(new MiningData(pos, direction));
    }

    public void clickMine(MiningData miningData) {
        int queueSize = miningQueue.size();
        if (queueSize <= 2) {
            queueMiningData(miningData);
        }
    }

    public void queueMiningData(MiningData data) {
        if (data.getState().isAir()) {
            return;
        }
        if (startMining(data)) {
            if (miningQueue.stream().anyMatch(p1 -> data.getPos().equals(p1.getPos()))) {
                return;
            }
            miningQueue.addFirst(data);
        }
    }

    private boolean startMining(MiningData data) {
        if (data.isStarted()) {
            return false;
        }
        data.setStarted();
        if (grimNewConfig.get()) {
            if (!miningFix.get()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return true;
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        return true;
    }

    private void abortMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    private void stopMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        if (rotateConfig.get()) {
            float[] rotations = getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
            if (grimConfig.get()) {
                Rotations.rotate(rotations[0], rotations[1]);
            } else {
                Rotations.rotate(rotations[0], rotations[1]);
            }
        }
        int bestSlot = data.getSlot();
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        boolean needsSwap = bestSlot != -1 && bestSlot != currentSlot;
        if (needsSwap && swappedToSlot == -1) {
            originalSlot = currentSlot;
        }
        if (needsSwap) {
            swapTo(bestSlot);
            swappedToSlot = bestSlot;
            swapBackTicks = 3;
        } else if (swappedToSlot != -1) {
            swapBackTicks = 3;
        }
        stopMiningInternal(data);
        lastBreak = System.currentTimeMillis();
    }

    private void swapTo(int slot) {
        switch (swapConfig.get()) {
            case NORMAL -> {
                mc.player.getInventory().setSelectedSlot(slot);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
            case SILENT -> {
                if (inventoryManager == null) {
                    inventoryManager = BepInventoryManager.getInstance();
                }
                inventoryManager.setSlot(slot);
            }
        }
    }

    private void swapBack(int originalSlot) {
        switch (swapConfig.get()) {
            case NORMAL -> {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            }
            case SILENT -> {
                if (inventoryManager == null) {
                    inventoryManager = BepInventoryManager.getInstance();
                }
                inventoryManager.syncToClient();
            }
        }
    }

    private void stopMiningInternal(MiningData data) {
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    public boolean isBlockDelayGrim() {
        return System.currentTimeMillis() - lastBreak <= 280 && grimConfig.get();
    }

    private boolean isDataPacketMine(MiningData data) {
        return miningQueue.size() == 2 && data == miningQueue.getLast();
    }

    public float calcBlockBreakingDelta(BlockState state, BlockView world, BlockPos pos) {
        if (swapConfig.get() == Swap.OFF) {
            return state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        }
        float f = state.getHardness(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }

    private float getBlockBreakingSpeed(BlockState block) {
        int tool = getBestTool(block);
        float f = mc.player.getInventory().getStack(tool).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            ItemStack stack = mc.player.getInventory().getStack(tool);
            int i = 0;
            var enchantments = stack.getEnchantments();
            for (var entry : enchantments.getEnchantmentEntries()) {
                if (entry.getKey().matchesKey(Enchantments.EFFICIENCY)) {
                    i = entry.getIntValue();
                    break;
                }
            }
            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }
        if (StatusEffectUtil.hasHaste(mc.player)) {
            f *= 1.0f + (float) (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float g = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            f *= g;
        }
        if (mc.player.isSubmergedIn(FluidTags.WATER)) {
            boolean hasAquaAffinity = false;
            ItemStack helmet = mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                var enchantments = helmet.getEnchantments();
                for (var entry : enchantments.getEnchantmentEntries()) {
                    if (entry.getKey().matchesKey(Enchantments.AQUA_AFFINITY)) {
                        hasAquaAffinity = true;
                        break;
                    }
                }
            }
            if (!hasAquaAffinity) {
                f /= 5.0f;
            }
        }
        if (!mc.player.isOnGround()) {
            f /= 5.0f;
        }
        return f;
    }

    private boolean canHarvest(BlockState state) {
        if (state.isToolRequired()) {
            int tool = getBestTool(state);
            return mc.player.getInventory().getStack(tool).isSuitableFor(state);
        }
        return true;
    }

    private int getBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot == -1 ? mc.player.getInventory().getSelectedSlot() : bestSlot;
    }

    public boolean isMining() {
        return !miningQueue.isEmpty();
    }

    private static float[] getRotationsTo(net.minecraft.util.math.Vec3d src, net.minecraft.util.math.Vec3d dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(src).z,
                dest.subtract(src).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(src).y,
                Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
        return new float[]{
                MathHelper.wrapDegrees(yaw),
                MathHelper.wrapDegrees(pitch)
        };
    }

    private SettingColor interpolateColor(float value, SettingColor start, SettingColor end) {
        float sr = start.r / 255.0f;
        float sg = start.g / 255.0f;
        float sb = start.b / 255.0f;
        float sa = start.a / 255.0f;
        float er = end.r / 255.0f;
        float eg = end.g / 255.0f;
        float eb = end.b / 255.0f;
        float ea = end.a / 255.0f;
        return new SettingColor(
                (int) ((sr * value + er * (1.0f - value)) * 255),
                (int) ((sg * value + eg * (1.0f - value)) * 255),
                (int) ((sb * value + eb * (1.0f - value)) * 255),
                (int) ((sa * value + ea * (1.0f - value)) * 255)
        );
    }

    public class MiningData {

        private boolean attemptedBreak;
        private long breakTime;
        private final BlockPos pos;
        private final Direction direction;
        private float lastDamage;
        private float blockDamage;
        private boolean started;

        public MiningData(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
        }

        public void setAttemptedBreak(boolean attemptedBreak) {
            this.attemptedBreak = attemptedBreak;
            if (attemptedBreak) {
                resetBreakTime();
            }
        }

        public void resetBreakTime() {
            breakTime = System.currentTimeMillis();
        }

        public boolean hasAttemptedBreak() {
            return attemptedBreak;
        }

        public boolean passedAttemptedBreakTime(long time) {
            return System.currentTimeMillis() - breakTime >= time;
        }

        public float damage(final float dmg) {
            lastDamage = blockDamage;
            blockDamage += dmg;
            return blockDamage;
        }

        public void setDamage(float blockDamage) {
            this.blockDamage = blockDamage;
        }

        public void resetDamage() {
            started = false;
            blockDamage = 0.0f;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getSlot() {
            return getBestToolNoFallback(getState());
        }

        public BlockState getState() {
            return mc.world.getBlockState(pos);
        }

        public float getBlockDamage() {
            return blockDamage;
        }

        public float getLastDamage() {
            return lastDamage;
        }

        public boolean isStarted() {
            return started;
        }

        public void setStarted() {
            this.started = true;
        }

        private int getBestToolNoFallback(BlockState state) {
            int bestSlot = -1;
            float bestSpeed = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
            return bestSlot;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MiningData that = (MiningData) o;
            return pos.equals(that.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    private static class FirstOutQueue<T> extends java.util.ArrayList<T> {

        private final int maxSize;

        public FirstOutQueue(int maxSize) {
            this.maxSize = maxSize;
        }

        public void addFirst(T element) {
            add(0, element);
            while (size() > maxSize) {
                remove(size() - 1);
            }
        }

        public T getFirst() {
            return isEmpty() ? null : get(0);
        }

        public T getLast() {
            return isEmpty() ? null : get(size() - 1);
        }
    }

    private static class Animation {

        private boolean state;
        private long time;
        private final long duration;

        public Animation(boolean state, long duration) {
            this.state = state;
            this.duration = duration;
            this.time = System.currentTimeMillis();
        }

        public void setState(boolean state) {
            if (this.state != state) {
                this.state = state;
                this.time = System.currentTimeMillis();
            }
        }

        public float getFactor() {
            if (state) return 1.0f;
            long elapsed = System.currentTimeMillis() - time;
            float progress = Math.min(1.0f, elapsed / (float) duration);
            return 1.0f - progress;
        }
    }

    public enum SpeedmineMode {
        PACKET,
        DAMAGE
    }

    public enum Swap {
        NORMAL,
        SILENT,
        OFF
    }

    private PlayerEntity getClosestEnemy() {
        if (mc.world == null || mc.player == null) return null;
        PlayerEntity closest = null;
        double closestDist = enemyRange.get() * enemyRange.get();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.isSpectator() || player.isDead()) continue;
            if (Friends.get().isFriend(player)) continue;
            double dist = mc.player.squaredDistanceTo(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }

    private BlockPos findBestEnemyBlock(PlayerEntity enemy) {
        if (enemy == null) return null;
        BlockPos enemyPos = enemy.getBlockPos();
        BlockState feetState = mc.world.getBlockState(enemyPos);
        if (!feetState.isAir() && feetState.getHardness(mc.world, enemyPos) != -1.0f) {
            double feetDist = mc.player.getEyePos().squaredDistanceTo(enemyPos.toCenterPos());
            if (feetDist <= rangeConfig.get() * rangeConfig.get()) {
                if (!isMiningBlock(enemyPos) && isResistantBlock(feetState)) {
                    if (!isOwnSurroundBlock(enemyPos)) {
                        return enemyPos;
                    }
                }
            }
        }
        java.util.List<BlockPos> surroundBlocks = new ArrayList<>();
        surroundBlocks.add(enemyPos.north());
        surroundBlocks.add(enemyPos.south());
        surroundBlocks.add(enemyPos.east());
        surroundBlocks.add(enemyPos.west());
        BlockPos bestSurround = findBestBlock(surroundBlocks);
        if (bestSurround != null) {
            return bestSurround;
        }
        if (targetHead.get()) {
            BlockPos aboveHead = enemyPos.up(2);
            BlockState aboveState = mc.world.getBlockState(aboveHead);
            if (!aboveState.isAir() && aboveState.getHardness(mc.world, aboveHead) != -1.0f) {
                double dist = mc.player.getEyePos().squaredDistanceTo(aboveHead.toCenterPos());
                if (dist <= rangeConfig.get() * rangeConfig.get()) {
                    if (!isMiningBlock(aboveHead) && isResistantBlock(aboveState)) {
                        if (!isOwnSurroundBlock(aboveHead)) {
                            return aboveHead;
                        }
                    }
                }
            }
        }
        return null;
    }

    private BlockPos findBestBlock(List<BlockPos> positions) {
        BlockPos bestBlock = null;
        double bestDist = rangeConfig.get() * rangeConfig.get();
        for (BlockPos pos : positions) {
            double dist = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos());
            if (dist > rangeConfig.get() * rangeConfig.get()) continue;
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir() || state.getHardness(mc.world, pos) == -1.0f) continue;
            if (isMiningBlock(pos)) continue;
            if (isOwnSurroundBlock(pos)) continue;
            if (!isResistantBlock(state)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                bestBlock = pos;
            }
        }
        return bestBlock;
    }

    private BlockPos getAntiCrawlBlock() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos blockAbove = playerPos.up();
        BlockState state = mc.world.getBlockState(blockAbove);
        if (!state.isAir() && state.getHardness(mc.world, blockAbove) != -1.0f) {
            if (!state.isOf(Blocks.BEDROCK) &&
                    !state.isOf(Blocks.REINFORCED_DEEPSLATE) &&
                    !state.isOf(Blocks.BARRIER)) {
                double dist = mc.player.getEyePos().squaredDistanceTo(blockAbove.toCenterPos());
                if (dist <= rangeConfig.get() * rangeConfig.get()) {
                    return blockAbove;
                }
            }
        }
        return null;
    }

    private boolean isMiningBlock(BlockPos pos) {
        if (miningQueue == null) return false;
        for (MiningData data : miningQueue) {
            if (data.getPos().equals(pos)) return true;
        }
        return false;
    }

    private boolean isOwnSurroundBlock(BlockPos pos) {
        BlockPos playerPos = mc.player.getBlockPos();
        if (pos.equals(playerPos.north()) || pos.equals(playerPos.south()) ||
                pos.equals(playerPos.east()) || pos.equals(playerPos.west())) {
            return true;
        }
        if (pos.equals(playerPos.up()) || pos.equals(playerPos.up(2))) {
            return true;
        }
        return false;
    }

    private boolean isResistantBlock(BlockState state) {
        if (state.isOf(Blocks.BEDROCK) ||
                state.isOf(Blocks.REINFORCED_DEEPSLATE) ||
                state.isOf(Blocks.BARRIER) ||
                state.isOf(Blocks.COMMAND_BLOCK) ||
                state.isOf(Blocks.STRUCTURE_BLOCK)) {
            return false;
        }
        return state.isOf(Blocks.OBSIDIAN) ||
                state.isOf(Blocks.CRYING_OBSIDIAN) ||
                state.isOf(Blocks.ENDER_CHEST) ||
                state.isOf(Blocks.ANCIENT_DEBRIS) ||
                state.isOf(Blocks.RESPAWN_ANCHOR);
    }

    private Direction getInteractDirection(BlockPos pos) {
        Vec3d eyePos = mc.player.getEyePos();
        Vec3d posVec = Vec3d.ofCenter(pos);
        Direction bestDir = null;
        double bestDot = -1;
        for (Direction dir : Direction.values()) {
            Vec3d dirVec = Vec3d.of(dir.getVector());
            double dot = eyePos.subtract(posVec).normalize().dotProduct(dirVec);
            if (dot > bestDot) {
                bestDot = dot;
                bestDir = dir;
            }
        }
        return bestDir;
    }

    public BlockPos getLastAutoMineBlock() {
        return lastAutoMineBlock;
    }

    public BlockPos getLastAntiCrawlBlock() {
        return lastAntiCrawlBlock;
    }
}
