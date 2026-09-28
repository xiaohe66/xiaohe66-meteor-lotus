package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerEntityWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 刷附魔书模块
 * 自动寻找失业村民+岩浆块组合，放置讲台刷取指定附魔书
 *
 * @author He
 * @since 2026.04.13
 */
public class VillagerBookRoller extends WalkModule {

    private static final Logger log = LoggerFactory.getLogger(VillagerBookRoller.class);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<RegistryKey<Enchantment>>> targetEnchants = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("目标附魔")
            .description("想要刷取的附魔书类型（等级自动使用最高等级），默认排除消失诅咒、绑定诅咒、冰霜行者")
            .defaultValue(
                    // 护甲保护类
                    Enchantments.PROTECTION,             // 保护
                    Enchantments.FEATHER_FALLING,        // 摔落保护
                    Enchantments.BLAST_PROTECTION,       // 爆炸保护
                    Enchantments.RESPIRATION,            // 水下呼吸
                    Enchantments.AQUA_AFFINITY,          // 水下速掘
                    Enchantments.DEPTH_STRIDER,          // 深海探索者
                    Enchantments.THORNS,                 // 荆棘
                    // 武器类
                    Enchantments.SHARPNESS,              // 锋利
                    Enchantments.KNOCKBACK,              // 击退
                    Enchantments.FIRE_ASPECT,            // 火焰附加
                    Enchantments.LOOTING,                // 抢夺
                    Enchantments.SWEEPING_EDGE,          // 横扫之刃
                    // 工具类
                    Enchantments.EFFICIENCY,             // 效率
                    Enchantments.SILK_TOUCH,             // 精准采集
                    Enchantments.FORTUNE,                // 时运
                    Enchantments.MENDING,                // 经验修补
                    Enchantments.UNBREAKING             // 耐久
            )
            .build());

    private final Setting<Integer> maxCost = sgGeneral.add(new IntSetting.Builder()
            .name("价格上限")
            .description("可接受的最高价格（绿宝石数量）")
            .range(1, 64)
            .sliderRange(1, 64)
            .defaultValue(26)
            .build());

    private final Setting<Integer> searchRange = sgGeneral.add(new IntSetting.Builder()
            .name("搜索范围")
            .description("搜索村民和岩浆块的范围")
            .range(8, 64)
            .sliderRange(8, 64)
            .defaultValue(32)
            .build());

    private final Setting<Integer> professionTimeout = sgGeneral.add(new IntSetting.Builder()
            .name("职业获取超时")
            .description("等待村民获得职业的毫秒数")
            .range(1000, 10000)
            .sliderRange(1000, 10000)
            .defaultValue(5000)
            .build());

    private final Setting<Boolean> removeWhenFound = sgGeneral.add(new BoolSetting.Builder()
            .name("找到后移除")
            .description("找到目标附魔书后从列表中移除，继续刷其他附魔")
            .defaultValue(false)
            .build());

    private final Setting<Boolean> playSound = sgGeneral.add(new BoolSetting.Builder()
            .name("播放提示音")
            .description("找到目标附魔书时播放提示音")
            .defaultValue(true)
            .build());

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("调试模式")
            .description("输出调试信息到日志")
            .defaultValue(false)
            .build());

    // 当前处理的村民
    private VillagerEntityWarp currentTarget;

    // 等待计时
    private long professionWaitStartTime;
    private long clearProfessionWaitStartTime;

    // 交易索引
    private int tradeIndex;

    public VillagerBookRoller() {
        super("刷附魔书", "自动寻找失业村民，放置讲台刷取指定附魔书. (村民旁边是岩浆块且上方未放置工作方块)");

        addStep(Steps.FINDING_TARGET, this::findingTarget);
        addStep(Steps.GOTO_TARGET, this::gotoTarget);
        addStep(Steps.PLACE_LECTERN, this::placeLectern);
        addStep(Steps.WAIT_PROFESSION, this::waitProfession);
        addStep(Steps.OPEN_TRADE, this::openTrade);
        addStep(Steps.CHECK_TRADES, this::checkTrades);
        addStep(Steps.EXECUTE_TRADE, this::executeTrade);
        addStep(Steps.BREAK_LECTERN, this::breakLectern);
        addStep(Steps.WAIT_PROFESSION_CLEAR, this::waitProfessionClear);
    }

    @Override
    public void onActivate() {
        super.onActivate();

        if (targetEnchants.get().isEmpty()) {
            warning("未设置目标附魔");
            toggle();
            return;
        }

        step = Steps.FINDING_TARGET;
        info("开始刷附魔书");
    }

    /**
     * 步骤：寻找目标村民
     */
    private void findingTarget() {
        // 检查是否所有附魔都已找到
        if (targetEnchants.get().isEmpty()) {
            info("§a所有目标附魔书已找到！");
            toggle();
            return;
        }

        currentTarget = findNearestValidVillager();

        if (currentTarget == null) {
            warning("附近没有符合条件的失业村民");
            toggle();
            return;
        }

        printLog("找到目标村民: " + currentTarget.getUuid() + " 朝向: " + currentTarget.getFacing());

        delayNext(Steps.GOTO_TARGET);
    }

    /**
     * 步骤：前往目标位置
     */
    private void gotoTarget() {
        if (currentTarget == null) {
            step = Steps.FINDING_TARGET;
            return;
        }

        // 检查是否已到达操作位置附近
        double distance = mc.player.getEntityPos().distanceTo(currentTarget.getOperatePos().toCenterPos());
        if (distance <= 1.5) {

            delayNext(Steps.PLACE_LECTERN);
            return;
        }

        // 使用 Baritone 寻路
        gotoTargetIfNeed(currentTarget.getOperatePos(), 0, Steps.PLACE_LECTERN, "前往目标村民");
    }

    /**
     * 步骤：放置讲台
     */
    private void placeLectern() {
        if (currentTarget == null || currentTarget.getWorkPos() == null) {
            step = Steps.FINDING_TARGET;
            return;
        }

        BlockState workPosState = mc.world.getBlockState(currentTarget.getWorkPos());
        if (!workPosState.isAir()) {
            // 检查是否书讲台
            if (workPosState.getBlock() instanceof LecternBlock) {
                step = Steps.WAIT_PROFESSION;

            } else {
                breakStep("<讲台位置>被占用");
            }
            return;
        }

        // 检查背包中是否有讲台
        FindItemResult lecternResult = InvUtils.findInHotbar(Items.LECTERN);
        if (!lecternResult.found()) {
            warning("背包中没有讲台，请先准备讲台");
            toggle();
            return;
        }

        BlockPos lecternPos = currentTarget.getWorkPos();

        // 面向讲台位置
        Vec3d lecternCenter = lecternPos.toCenterPos();
        HeRotationUtils.rotate(lecternCenter, () -> {
            // 放置讲台
            if (BlockUtils.place(lecternPos, lecternResult, true, 5)) {
                printLog("放置讲台成功");
                professionWaitStartTime = System.currentTimeMillis();
                step = Steps.WAIT_PROFESSION;
            } else {
                warning("放置讲台失败，重试中...");
            }
        });

        setDelay();
    }

    /**
     * 步骤：等待村民获得职业
     */
    private void waitProfession() {
        if (currentTarget == null) {
            step = Steps.FINDING_TARGET;
            return;
        }

        VillagerEntity villager = currentTarget.getVillager();
        if (villager == null) {
            step = Steps.FINDING_TARGET;
            return;
        }

        // 检查是否超时
        if (System.currentTimeMillis() - professionWaitStartTime > professionTimeout.get()) {
            warning("等待村民获得职业超时，挖掉讲台重试");
            delayNext(Steps.BREAK_LECTERN);
            return;
        }

        // 检查村民职业
        Optional<RegistryKey<VillagerProfession>> professionOpt = villager.getVillagerData().profession().getKey();
        if (professionOpt.isPresent() && professionOpt.get() == VillagerProfession.LIBRARIAN) {
            printLog("村民已成为图书管理员");
            delayNext(Steps.OPEN_TRADE);
            return;
        }

        setDelay();
    }

    /**
     * 步骤：打开交易界面
     */
    private void openTrade() {
        if (currentTarget == null) {
            step = Steps.FINDING_TARGET;
            return;
        }

        VillagerEntity villager = currentTarget.getVillager();
        if (villager == null) {
            step = Steps.FINDING_TARGET;
            return;
        }

        if (!villager.isAlive()) {
            warning("村民已死亡");
            delayCloseNext(Steps.FINDING_TARGET);
            return;
        }

        Vec3d playerPos = mc.player.getEyePos();
        Vec3d villagerPos = villager.getEyePos();

        // 射线检测
        EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, playerPos, villagerPos, villager.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos));

        if (entityHitResult == null) {
            HeRotationUtils.rotate(villager.getEyePos(), () -> {
                mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);
                step = Steps.CHECK_TRADES;
            });
        } else {
            HeRotationUtils.rotate(entityHitResult.getPos(), () -> {
                ActionResult actionResult = mc.interactionManager.interactEntityAtLocation(
                        mc.player, villager, entityHitResult, Hand.MAIN_HAND);
                if (!actionResult.isAccepted()) {
                    mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);
                }
                step = Steps.CHECK_TRADES;
            });
        }
    }

    /**
     * 步骤：检查交易
     */
    private void checkTrades() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            delayNext(Steps.OPEN_TRADE);
            return;
        }

        TradeOfferList offers = handler.getRecipes();
        Set<RegistryKey<Enchantment>> targets = targetEnchants.get();

        // 遍历所有交易
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            ItemStack sellItem = offer.getSellItem();

            // 检查是否是附魔书
            if (!sellItem.isOf(Items.ENCHANTED_BOOK)) {
                continue;
            }

            // 检查是否已售罄
            if (offer.getUses() >= offer.getMaxUses()) {
                continue;
            }

            // 获取附魔书上的附魔
            var storedEnchants = sellItem.get(DataComponentTypes.STORED_ENCHANTMENTS);
            if (storedEnchants == null) {
                continue;
            }

            for (var entry : storedEnchants.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> enchantEntry = entry.getKey();
                int level = entry.getIntValue();

                Optional<RegistryKey<Enchantment>> enchantKeyOpt = enchantEntry.getKey();
                if (enchantKeyOpt.isEmpty()) {
                    continue;
                }

                RegistryKey<Enchantment> enchantKey = enchantKeyOpt.get();

                // 检查是否是我们想要的附魔
                if (!targets.contains(enchantKey)) {
                    continue;
                }

                // 检查等级是否是最高等级
                int maxLevel = enchantEntry.value().getMaxLevel();
                if (level < maxLevel) {
                    printLog("找到附魔但等级不足: " + enchantKey.getValue() + " " + level + "/" + maxLevel);
                    continue;
                }

                // 检查价格
                int cost = offer.getOriginalFirstBuyItem().getCount();
                if (cost > maxCost.get()) {
                    printLog("找到附魔但价格过高: " + cost + " > " + maxCost.get());
                    continue;
                }

                // 找到目标附魔书！
                String enchantmentName = Names.get(enchantKey);
                info("§a找到目标附魔书: §f" + enchantmentName + " §a价格: §f" + cost);

                // 播放提示音
                if (playSound.get()) {
                    mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.0f));
                }

                // 如果设置了找到后移除，从目标列表中移除
                if (removeWhenFound.get()) {
                    targets.remove(enchantKey);
                    printLog("已从目标列表中移除: " + enchantmentName);
                }

                // 准备执行交易
                this.tradeIndex = i;
                delayNext(Steps.EXECUTE_TRADE);
                return;
            }
        }

        // 没有找到目标附魔书，关闭界面，挖掉讲台重试
        printLog("未找到目标附魔书，挖掉讲台重试");
        delayCloseNext(Steps.BREAK_LECTERN);
    }

    /**
     * 步骤：执行交易（锁定交易）
     */
    private void executeTrade() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            delayNext(Steps.OPEN_TRADE);
            return;
        }

        TradeOfferList offers = handler.getRecipes();
        if (tradeIndex >= offers.size()) {
            delayCloseNext(Steps.BREAK_LECTERN);
            return;
        }

        TradeOffer offer = offers.get(tradeIndex);

        // 检查绿宝石是否足够
        FindItemResult emeraldResult = InvUtils.find(Items.EMERALD);
        int needEmerald = offer.getOriginalFirstBuyItem().getCount();
        int hasEmerald = emeraldResult.found() ? emeraldResult.count() : 0;

        // 检查快捷栏中的绿宝石
        ItemStack slot0 = handler.getSlot(0).getStack();
        if (slot0.isOf(Items.EMERALD)) {
            hasEmerald += slot0.getCount();
        }

        if (hasEmerald < needEmerald) {
            warning("绿宝石不足，需要 " + needEmerald + " 个");
            toggle();
            return;
        }

        // 检查书
        if (offer.getSecondBuyItem().isPresent()) {
            ItemStack secondItem = offer.getSecondBuyItem().get().itemStack();
            if (secondItem.isOf(Items.BOOK)) {
                FindItemResult bookResult = InvUtils.find(Items.BOOK);
                if (!bookResult.found()) {
                    warning("需要书作为交易材料，但背包中没有");
                    toggle();
                    return;
                }
            }
        }

        // 选择交易
        handler.setRecipeIndex(tradeIndex);
        handler.switchTo(tradeIndex);
        mc.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(tradeIndex));

        // 执行交易：将结果槽的附魔书移动到背包空槽
        FindItemResult emptyResult = InvUtils.findEmpty();
        if (!emptyResult.found()) {
            warning("背包没有格子了");
            toggle();
            return;
        }

        // 从结果槽(slotId=2)移动附魔书到背包空槽
        InvUtils.move().fromId(2).to(emptyResult.slot());

        info("§a已锁定交易！");

        // 关闭界面，挖掉讲台，去下一个村民
        delayCloseNext(Steps.FINDING_TARGET);
    }

    /**
     * 步骤：挖掉讲台
     */
    private void breakLectern() {
        if (currentTarget == null || currentTarget.getWorkPos() == null) {
            delayCloseNext(Steps.FINDING_TARGET);
            return;
        }

        BlockPos lecternPos = currentTarget.getWorkPos();

        // 检查是否还有讲台
        BlockState state = mc.world.getBlockState(lecternPos);
        if (!state.isOf(Blocks.LECTERN)) {
            // 讲台已经被挖掉了，等待失业
            clearProfessionWaitStartTime = System.currentTimeMillis();
            delayNext(Steps.WAIT_PROFESSION_CLEAR);
            return;
        }

        // 切换到斧头（优先顺序：下界合金 > 钻石 > 铁 > 金 > 石 > 木）
        switchToAxe();

        // 使用 BlockUtils 挖讲台
        BlockUtils.breakBlock(lecternPos, true);
    }

    /**
     * 步骤：等待村民失业
     */
    private void waitProfessionClear() {
        if (currentTarget == null) {
            delayCloseNext(Steps.FINDING_TARGET);
            return;
        }

        VillagerEntity villager = currentTarget.getVillager();
        if (villager == null) {
            delayCloseNext(Steps.FINDING_TARGET);
            return;
        }

        // 检查是否超时
        if (System.currentTimeMillis() - clearProfessionWaitStartTime > professionTimeout.get()) {
            printLog("等待失业超时，继续下一步");
            delayCloseNext(Steps.FINDING_TARGET);
            return;
        }

        // 检查村民是否失业
        Optional<RegistryKey<VillagerProfession>> professionOpt = villager.getVillagerData().profession().getKey();
        if (professionOpt.isPresent() && professionOpt.get() == VillagerProfession.NONE) {
            printLog("村民已失业，继续刷取");
            // 直接重新放置讲台，不限制重试次数
            delayNext(Steps.PLACE_LECTERN);
            return;
        }

        setDelay();
    }

    /**
     * 查找最近的符合条件的失业村民
     */
    private VillagerEntityWarp findNearestValidVillager() {
        VillagerEntityWarp nearest = null;
        double minDistance = Double.MAX_VALUE;

        List<VillagerEntity> itemFrames = mc.world.getEntitiesByClass(
                VillagerEntity.class,
                mc.player.getBoundingBox().expand(searchRange.get()),
                frame -> true);

        for (VillagerEntity villager : itemFrames) {
            // 检查是否是失业状态
            Optional<RegistryKey<VillagerProfession>> professionOpt = villager.getVillagerData().profession().getKey();
            if (professionOpt.isEmpty() || professionOpt.get() != VillagerProfession.NONE) {
                continue;
            }

            BlockPos villagerPos = villager.getBlockPos();

            // 检查4个方向是否有岩浆块（岩浆块在村民旁边位置的下方一格）
            Direction validDirection = null;
            BlockPos workPos = null;
            for (Direction dir : Direction.Type.HORIZONTAL) {
                // 岩浆块位置：村民旁边位置的下方一格
                BlockPos lecternPos = villagerPos.offset(dir);
                BlockPos magmaPos = lecternPos.down();
                BlockState magmaState = mc.world.getBlockState(magmaPos);
                if (!magmaState.isOf(Blocks.MAGMA_BLOCK)) {
                    continue;
                }

                BlockState lecternState = mc.world.getBlockState(lecternPos);

                // 如果已经有讲台，说明正在被处理，跳过
                if (!lecternState.isAir()) {
                    continue;
                }

                // 检查操作位置是否可站立
                BlockPos opPos = lecternPos.offset(dir);
                BlockState opState = mc.world.getBlockState(opPos);
                BlockState opUpState = mc.world.getBlockState(opPos.up());
                if (!opState.isAir() || !opUpState.isAir()) {
                    continue;
                }

                validDirection = dir;
                workPos = lecternPos;
                break;
            }

            if (validDirection == null) {
                continue;
            }

            // 计算距离
            double distance = mc.player.getEntityPos().distanceTo(villagerPos.toCenterPos());
            if (distance < minDistance) {
                minDistance = distance;
                // 操作位置：岩浆块前方
                BlockPos operatePos = workPos.offset(validDirection);
                nearest = new VillagerEntityWarp(VillagerType.图书管理员, villager.getUuid(), operatePos, validDirection, workPos);
            }
        }

        return nearest;
    }

    private void printLog(String msg) {
        if (debug.get()) {
            log.info(msg);
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        currentTarget = null;
    }

    @Override
    public String getInfoString() {
        if (currentTarget != null) {
            return "刷取中";
        }
        return "搜索中";
    }

    /**
     * 切换到斧头（优先顺序：下界合金 > 钻石 > 铁 > 金 > 石 > 木）
     * 支持从背包移动到热栏
     */
    private void switchToAxe() {
        // 斧头优先级列表
        var axeTypes = List.of(
                Items.NETHERITE_AXE,
                Items.DIAMOND_AXE,
                Items.IRON_AXE,
                Items.GOLDEN_AXE,
                Items.STONE_AXE,
                Items.WOODEN_AXE
        );

        // 先尝试切换到热栏中的斧头
        for (var axe : axeTypes) {
            FindItemResult result = InvUtils.findInHotbar(axe);
            if (result.found()) {
                InvUtils.swap(result.slot(), false);
                return;
            }
        }

        // 如果没有在热栏，尝试从背包移动到主手
        for (var axe : axeTypes) {
            FindItemResult result = InvUtils.find(axe);
            if (result.found()) {
                // 将斧头从背包移动到当前选中的热栏槽位
                InvUtils.move().from(result.slot()).to(HeInvUtils.getMainSlot());
                return;
            }
        }
    }
}
