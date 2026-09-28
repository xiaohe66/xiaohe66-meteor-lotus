package com.xiaohe66.mc.meteor.lotus.modules;

import baritone.api.event.listener.AbstractGameEventListener;
import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerEntityWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerSettingWarp;
import com.xiaohe66.mc.meteor.lotus.modules.villager.VillagerType;
import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeRotationUtils;
import com.xiaohe66.mc.meteor.lotus.util.TaskUtils;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author He
 * @since 2025.06.08 14:18
 */
public class VillagerTrader extends WalkModule implements AbstractGameEventListener {

    private static final Logger log = LoggerFactory.getLogger(VillagerTrader.class);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("调试模式")
            .description("调试模式会将一些信息输出到log中")
            .defaultValue(false)
            .build());

    public final Setting<Integer> supplyRange = sgGeneral.add(new IntSetting.Builder()
            .name("搜索容器范围")
            .description("开启功能时, 搜索容器的范围")
            .min(6)
            .sliderMax(100)
            .defaultValue(32)
            .build());

    public final Setting<Integer> minDistance = sgGeneral.add(new IntSetting.Builder()
            .name("操作范围")
            .description("补给、卸货的距离")
            .min(2)
            .sliderMax(3)
            .defaultValue(2)
            .build());

    private final VillagerSettingWarp villagerSettingWarp1 = new VillagerSettingWarp(settings,
            sgGeneral,
            "牧师",
            VillagerType.牧师,
            24);

    private final VillagerSettingWarp villagerSettingWarp2 = new VillagerSettingWarp(settings,
            sgGeneral,
            "农民",
            VillagerType.农民,
            8);

    private final VillagerSettingWarp villagerSettingWarp3 = new VillagerSettingWarp(settings,
            sgGeneral,
            "图书管理员",
            VillagerType.图书管理员,
            1);

    private final VillagerSettingWarp villagerSettingWarp4 = new VillagerSettingWarp(settings,
            sgGeneral,
            "盔甲匠",
            VillagerType.盔甲匠,
            1);

    private final VillagerSettingWarp villagerSettingWarp5 = new VillagerSettingWarp(settings,
            sgGeneral,
            "武器匠",
            VillagerType.武器匠,
            1);

    private final VillagerSettingWarp villagerSettingWarp6 = new VillagerSettingWarp(settings,
            sgGeneral,
            "工具匠",
            VillagerType.工具匠,
            1);

    private final VillagerSettingWarp villagerSettingWarp7 = new VillagerSettingWarp(settings,
            sgGeneral,
            "其他",
            VillagerType.石匠,
            6);

    public final Setting<Integer> restartDelay = sgGeneral.add(new IntSetting.Builder()
            .name("重启延时(秒)")
            .description("重启延时")
            .min(1)
            .sliderMax(200)
            .defaultValue(2)
            .build());

    private final Setting<Boolean> initBtn = sgGeneral.add(new BoolSetting.Builder()
            .name("初始化")
            .description("使用前需要先初始化, 否则无法使用")
            .defaultValue(false)
            .onChanged(this::init)
            .build());

    private static final int firstTraderTime = 2000;
    private static final int secondTraderTime = 9000;

    private final Map<VillagerType, VillagerSettingWarp> villagerSettingWarpMap;

    private List<VillagerEntityWarp> villagerList = Collections.emptyList();
    private Map<ItemBo, BlockPos> putPosMap = Collections.emptyMap();
    private BlockPos moneyPos;
    private BlockPos bookPos;

    private VillagerSettingWarp curVillagerSettingWarp;

    private BlockPos curPutPos;
    private ItemBo curPutVillagerItem;
    private BlockPos curTakePos;
    private Item curTakeItem;

    private List<ItemBo> buyVillagerItemList = Collections.emptyList();
    private VillagerEntityWarp currentVillager;
    private long waitStartTime;
    private int tradeIndex = 0;

    private ScheduledFuture<?> restartFuture;

    public VillagerTrader() {
        super("村民交易", "自动和村民交易。展示框下2格的盒子卸货、最近的木桶补绿宝石, 展示框放书的箱子补给书");

        addStep(Steps.GOTO_PUT_ITEM, this::gotoPutIfNeed);
        addStep(Steps.PUT_ITEM, this::put);
        addStep(Steps.GOTO_TAKE_ITEM, this::gotoTakeIfNotFull);
        addStep(Steps.TAKE_ITEM, this::take);
        // case GotoGoods -> gotoGoodsIfNotFull();
        // case TakeGoods -> takeGoods();
        addStep(Steps.NEXT, this::next);
        addStep(Steps.WALKING, this::none);
        addStep(Steps.OPEN_TRADE, this::openTrade);
        addStep(Steps.EXECUTE_TRADE, this::executeTrade);
        addStep(Steps.WAIT, this::waitTrade);

        villagerSettingWarpMap = new LinkedHashMap<>();
        villagerSettingWarpMap.put(VillagerType.牧师, villagerSettingWarp1);
        villagerSettingWarpMap.put(VillagerType.农民, villagerSettingWarp2);
        villagerSettingWarpMap.put(VillagerType.图书管理员, villagerSettingWarp3);
        villagerSettingWarpMap.put(VillagerType.盔甲匠, villagerSettingWarp4);
        villagerSettingWarpMap.put(VillagerType.武器匠, villagerSettingWarp5);
        villagerSettingWarpMap.put(VillagerType.工具匠, villagerSettingWarp6);
    }

    @Override
    public void onActivate() {
        if (!isReady()) {
            toggle();
            return;
        }
        GameMode currentGameMode = mc.interactionManager.getCurrentGameMode();
        if (currentGameMode != GameMode.CREATIVE && currentGameMode != GameMode.SURVIVAL) {
            return;
        }
        super.onActivate();
        if (villagerList.isEmpty()) {
            warning("请先进行初始化");
            toggle();
            return;
        }

        delayStart();
    }

    private void init(Boolean b) {
        if (!Boolean.TRUE.equals(b)) {
            return;
        }

        initBtn.set(false);
        villagerList.clear();

        List<VillagerEntityWarp> villagerList = getVillagerEntity();

        if (villagerList.isEmpty()) {
            warning("附近没有合适村民");
            return;
        }

        Map<ItemBo, BlockPos> putPosMap = HePosUtils.getItemFrameKitPosMap(supplyRange.get());
        if (putPosMap.isEmpty()) {
            warning("找不到<卸货容器>");
            return;
        }

        // 书补给容器
        if (villagerSettingWarp3.needBuyEnchantment() || villagerSettingWarp7.needBuyEnchantment()) {

            BlockPos takeBookPos = getTakeBookPos();
            if (takeBookPos == null) {
                warning("找不到<书>补给位置");
                return;
            }
            this.bookPos = takeBookPos;
        }

        BlockPos moneyPos = null;
        double min1 = Double.MAX_VALUE;
        // 拿最近的木桶
        for (BlockEntity blockEntity : Utils.blockEntities()) {

            BlockEntityType<?> type = blockEntity.getType();
            if (BlockEntityType.BARREL.equals(type)) {
                double distanceTo = mc.player.getEntityPos().distanceTo(blockEntity.getPos().toCenterPos());
                if (distanceTo < supplyRange.get() && distanceTo < min1) {
                    moneyPos = blockEntity.getPos();
                    min1 = distanceTo;
                }
            }
        }

        if (moneyPos == null) {
            warning("找不到<绿宝石>补给位置");
            return;
        }

        List<ItemBo> buyVillagerItemList = new ArrayList<>();
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp1);
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp2);
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp3);
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp4);
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp5);
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp6);
        fillBuyItemList(buyVillagerItemList, villagerSettingWarp7);

        if (buyVillagerItemList.isEmpty()) {
            warning("未配置<购买物>");
            return;
        }

        // 检查卸货容器
        for (ItemBo villagerItem : buyVillagerItemList) {
            if (!putPosMap.containsKey(villagerItem)) {
                warning("缺少卸货容器:" + villagerItem.getName());
                return;
            }
        }

        // TODO : 需要在初始化时, 将各种配置都记录下来
        this.villagerList = villagerList;
        this.putPosMap = putPosMap;
        this.moneyPos = moneyPos;
        this.buyVillagerItemList = buyVillagerItemList;

        this.waitStartTime = -1;
        this.tradeIndex = 0;
        this.curVillagerSettingWarp = villagerSettingWarp1;

        info("初始化完毕");
    }

    private BlockPos getTakeBookPos() {
        List<ItemFrameEntity> bookItemFrame = mc.world.getEntitiesByClass(
                ItemFrameEntity.class,
                mc.player.getBoundingBox().expand(supplyRange.get()),
                frame -> frame.getHeldItemStack().getItem() == Items.BOOK);

        for (ItemFrameEntity itemFrame : bookItemFrame) {
            Optional<BlockPos> chestPos = HePosUtils.getOtherChestPos(itemFrame);
            if (chestPos.isPresent()) {
                return chestPos.get();
            }
        }
        return null;
    }

    private void fillBuyItemList(List<ItemBo> buyVillagerItemList, VillagerSettingWarp villagerSettingWarp) {
        if (villagerSettingWarp.isOpen()) {
            for (Item item : villagerSettingWarp.getBuyItem()) {
                buyVillagerItemList.add(new ItemBo(item));
            }
            if (villagerSettingWarp.getType() == VillagerType.图书管理员) {
                for (RegistryKey<Enchantment> enchantment : villagerSettingWarp.getBuyEnchantment()) {
                    buyVillagerItemList.add(new ItemBo(Items.ENCHANTED_BOOK, enchantment));
                }
            }
        }
    }

    private void waitTrade() {

        long waitStartDay = waitStartTime / 24000;
        long waitStartTimeOfDay = waitStartTime % 24000;

        long time = mcTime();
        long day = time / 24000;
        long timeOfDay = time % 24000;

        boolean ok = false;
        if (waitStartTimeOfDay > secondTraderTime) {
            if (day > waitStartDay && timeOfDay > firstTraderTime + 20) {
                ok = true;
            }
        } else if (waitStartTimeOfDay > firstTraderTime) {
            if (day > waitStartDay || timeOfDay > secondTraderTime) {
                ok = true;
            }
        } else {
            if (day > waitStartDay || timeOfDay > firstTraderTime + 20) {
                ok = true;
            }
        }

        if (ok) {
            info("开始交易");
            waitStartTime = -1;
            step = Steps.GOTO_PUT_ITEM;

        } else {
            printLog("waitTrade, waitStartDay : {}, day : {}, waitStartTimeOfDay : {}, timeOfDay : {}", waitStartDay, day, waitStartTimeOfDay, timeOfDay);
            setDelay();
        }
    }

    private boolean needClean() {
        FindItemResult findItemResult = InvUtils.find(Items.EMERALD);
        int count = findItemResult.found() ? findItemResult.count() : 0;
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler screenHandler) {
            ItemStack itemStack = screenHandler.getSlot(0).getStack();

            printLog("needClear itemStack : " + itemStack);

            if (itemStack.getItem() == Items.EMERALD) {
                count += itemStack.getCount();
            }
        }

        boolean needBuyEnchantment = curVillagerSettingWarp.needBuyEnchantment();
        if (needBuyEnchantment) {
            // 小于1组时补给
            int need = Math.min(curVillagerSettingWarp.getMaxMoney() * 12, 64);
            if (count < need) {
                return true;
            }

            // 补给书
            FindItemResult findBookResult = InvUtils.find(Items.BOOK);
            int bookCount = findBookResult.found() ? findBookResult.count() : 0;
            if (bookCount < 12) {
                return true;
            }

            // 空背包
            PlayerInventory playerInventory = mc.player.getInventory();
            int emptyQty = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = playerInventory.getStack(i);
                if (itemStack.isEmpty()) {
                    emptyQty++;
                }
            }

            // 交易附魔书时需要清空至少 12 个空位才行
            return emptyQty < 12;

        } else {
            // 小于1组时补给
            int need = Math.min(curVillagerSettingWarp.getMaxMoney() * 12, 64);
            if (count < need) {
                return true;
            }

            // 空背包
            PlayerInventory playerInventory = mc.player.getInventory();
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = playerInventory.getStack(i);
                if (itemStack.isEmpty()) {
                    return false;
                }
            }
            return true;
        }
    }

    private void executeTrade() {
        if (!(mc.player.currentScreenHandler instanceof MerchantScreenHandler)) {
            // ChatUtils.error("交易界面未打开, 稍候重试");

            setDelay();
            step = Steps.OPEN_TRADE;
            return;
        }

        MerchantScreenHandler handler = (MerchantScreenHandler) mc.player.currentScreenHandler;
        TradeOfferList tradeOfferList = handler.getRecipes();

        for (; tradeIndex < tradeOfferList.size(); tradeIndex++) {

            TradeOffer trade = tradeOfferList.get(tradeIndex);
            ItemStack sellItemStack = trade.getSellItem();
            Item sellItemValue = sellItemStack.getItem();
            TradedItem firstBuyItem = trade.getFirstBuyItem();

            if (trade.getMaxUses() <= trade.getUses()) {
                // 不能交易了
                continue;
            }

            ItemStack firstBuyItemStamp = firstBuyItem.itemStack();
            Item firstBuyItemValue = firstBuyItemStamp.getItem();

            boolean need = false;
            if (firstBuyItemValue == Items.EMERALD) {
                if (curVillagerSettingWarp.getBuyItem().contains(sellItemValue)) {
                    // 买普通物品
                    need = true;

                } else if (curVillagerSettingWarp.getType() == VillagerType.图书管理员 && sellItemValue == Items.ENCHANTED_BOOK) {
                    // 买附魔书
                    RegistryKey<Enchantment> enchantmentOne = EnchantmentUtils.getEnchantmentOne(sellItemStack);
                    need = curVillagerSettingWarp.getBuyEnchantment().contains(enchantmentOne);
                }
            }

            if (need) {

                // 实际交易价格 = 基础价格 + (基础价格 * 价格乘数 * 总需求调整) + specialPrice
                // 实际交易价格 = originCount + (originCount * (priceMultiplier * (demandBonus))) + specialPrice

                // ItemStack itemStack = trade.getDisplayedFirstBuyItem();

                int originCount = trade.getOriginalFirstBuyItem().getCount();
                int sellCount = originCount + trade.getSpecialPrice();
                if (trade.getDemandBonus() > 0) {
                    sellCount += originCount * trade.getPriceMultiplier() * trade.getDemandBonus();
                }

                printLog("priceMultiplier : " + trade.getPriceMultiplier());
                printLog("demandBonus : " + trade.getDemandBonus());
                printLog("firstBuyItemCount : " + firstBuyItem.count());
                printLog("originCount : " + originCount);
                printLog("specialPrice : " + trade.getSpecialPrice());
                printLog("sellCount : " + sellCount);

                if (sellCount > curVillagerSettingWarp.getMaxMoney()) {
                    warning("价格超过设定上限, 不交易");
                    continue;
                }

                // 清理、补给
                if (needClean()) {
                    delayCloseNext(Steps.GOTO_PUT_ITEM);
                    return;
                }

                // 执行交易
                handler.setRecipeIndex(tradeIndex);
                handler.switchTo(tradeIndex);

                // 发包选择索引
                MinecraftClient.getInstance().getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(tradeIndex));

                // 点击交易(成品在第2格)
                // TODO : 交易书的单价过高, 且有其他书时, 可能会买错书(服务器问题?)
                // TODO : 价格过高时，可能会有多次交易的bug
                InvUtils.shiftClick().slotId(2);

                // 多次交易
                setDelay();

                return;
            }
        }

        currentVillager.setLastTradeTime(mcTime());
        this.tradeIndex = 0;

        delayCloseNext(Steps.NEXT);
    }

    private void openTrade() {

        VillagerEntity villager = currentVillager.getVillager();
        if (villager == null) {
            currentVillager.setLastTradeTime(mcTime());
            next();
            return;
        }

        Vec3d villagerPos = villager.getEntityPos();
        Vec3d playerPos = mc.player.getEntityPos();

        double distance = villagerPos.distanceTo(playerPos);

        if (distance > minDistance.get() + 0.5) {
            // 距离不够, 跳过
            printLog("distance false");

            currentVillager.setLastFailTime(mcTime());
            next();
            return;
        }

        if (!villager.isAlive()) {
            info("村民挂了?");
            currentVillager.setLastTradeTime(mcTime());
            next();
            return;
        }

        printLog("openTrade");

        // 射线检测
        EntityHitResult entityHitResult = ProjectileUtil.raycast(mc.player, playerPos, villagerPos, villager.getBoundingBox(), Entity::canHit, playerPos.squaredDistanceTo(villagerPos));
        if (entityHitResult == null) {

            // NOTE : 这里是中间被挡了, 导致无法与村民互动
            if (debug.get()) {
                info("射线检测不通过");
            }

            // 确保玩家面向村民
            HeRotationUtils.rotate(villager.getEyePos(), () -> {
                mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);
                step = Steps.EXECUTE_TRADE;
            });
        } else {

            if (debug.get()) {
                info("射线检测通过");
            }

            // 面向碰撞点
            HeRotationUtils.rotate(entityHitResult.getPos(), () -> {
                ActionResult actionResult = mc.interactionManager.interactEntityAtLocation(mc.player, villager, entityHitResult, Hand.MAIN_HAND);
                if (!actionResult.isAccepted()) {
                    mc.interactionManager.interactEntity(mc.player, villager, Hand.MAIN_HAND);

                    step = Steps.EXECUTE_TRADE;

                } else {
                    ChatUtils.error("无法打开交易界面，重试中...");
                }
            });
        }

        // NOTE : 因为打开界面会比较慢
        setDelay(delay.get() * 2);
    }

    private void next() {
        if (needClean()) {
            boolean needPut = tryPut();
            if (needPut) {
                return;
            }
        }

        // 拿所有的村民，判断是否没有交易满的村民
        // 最后交易时间在昨天下班前的, 立刻可以开始交易
        // 最后交易时间在昨天下班后的，需要至少等到 8点后才可以开始交易
        // 最后交易时间在今天下班前的，下班后可以交易

        long time = mcTime();
        long timeOfDay = time % 24000;
        long day = time / 24000;

        printLog("day : " + day);
        printLog("timeOfDay : " + timeOfDay);

        List<VillagerEntityWarp> validVillagerList;
        if (timeOfDay > secondTraderTime) {
            // 今天下班前的
            validVillagerList = villagerList.stream()
                    .filter(warp -> warp.getDay() < day || warp.getTimeOfDay() < secondTraderTime - 100)
                    .filter(warp -> warp.getLastFailTime() < time - 100)
                    .collect(Collectors.toList());

        } else if (timeOfDay > firstTraderTime + 20) {
            // 今天上班前的
            validVillagerList = villagerList.stream()
                    .filter(warp -> warp.getDay() < day || warp.getTimeOfDay() < firstTraderTime)
                    .filter(warp -> warp.getLastFailTime() < time - 100)
                    .collect(Collectors.toList());
        } else {
            // 昨天下班前的
            validVillagerList = villagerList.stream()
                    .filter(warp -> warp.getDay() < day && warp.getTimeOfDay() < secondTraderTime - 100)
                    .filter(warp -> warp.getLastFailTime() < time - 100)
                    .collect(Collectors.toList());
        }

        // 按顺序执行交易
        Map<VillagerType, List<VillagerEntityWarp>> typeListMap = validVillagerList.stream().collect(Collectors.groupingBy(VillagerEntityWarp::getVillagerType));

        VillagerSettingWarp settingWarp = null;
        for (Map.Entry<VillagerType, VillagerSettingWarp> entry : villagerSettingWarpMap.entrySet()) {
            if (typeListMap.containsKey(entry.getKey())) {
                validVillagerList = typeListMap.get(entry.getKey());
                settingWarp = entry.getValue();
                break;
            }
        }

        if (settingWarp != null) {
            curVillagerSettingWarp = settingWarp;
        } else {
            validVillagerList = typeListMap.getOrDefault(villagerSettingWarp7.getType(), Collections.emptyList());
            if (!validVillagerList.isEmpty()) {
                curVillagerSettingWarp = villagerSettingWarp7;
            }
        }

        // 最近的
        VillagerEntityWarp best = null;
        Vec3d playerPos = mc.player.getEntityPos();
        double minDistance = Double.MAX_VALUE;
        for (VillagerEntityWarp warp : validVillagerList) {

            printLog("warpDay : {}, timeOfDay : {}", warp.getDay(), warp.getTimeOfDay());

            double distance = warp.getOperatePosCenter().distanceTo(playerPos);
            if (distance < minDistance) {
                best = warp;
                minDistance = distance;
            }
        }

        if (best == null) {
            info("准备待机");
            waitStartTime = mcTime();
            step = Steps.GOTO_PUT_ITEM;
            return;
        }

        printLog("find next : {}", best.getOperatePosCenter());

        currentVillager = best;

        BlockPos targetPos = currentVillager.getOperatePos();
        if (!targetPos.isWithinDistance(mc.player.getEntityPos(), 300)) {
            warning("寻路距离过远");
            toggle();
            return;
        }
        gotoTargetIfNeed(targetPos, 0, Steps.OPEN_TRADE, null);
    }

    private void tryNext() {
        step = waitStartTime > 0 ? Steps.WAIT : Steps.NEXT;
    }

    private void take() {
        openChest(curTakePos, screenHandler -> {
            FindItemResult findItemResult = InvUtils.find(curTakeItem);

            int needSupplyQty = curTakeItem == Items.BOOK ? 1 : curVillagerSettingWarp.getSupplyQty();
            if (findItemResult.found() && findItemResult.count() >= 64 * needSupplyQty) {
                // 数量够了, 不补给了
                HeInvUtils.closeCurScreen();
                if (curTakeItem == Items.EMERALD) {
                    gotoTakeIfNotFull();
                } else {
                    tryNext();
                }
                return;
            }

            // 要补给
            ItemStack nextStack = nextScreenStack(itemStack -> itemStack.getItem() == curTakeItem);
            if (nextStack.isEmpty()) {
                warning("无法补给");
                stop();
                delayStart();

            } else {
                info("拿取:" + Names.get(curTakeItem));
                InvUtils.shiftClick().slotId(getCurScreenSlot());
                setDelay();
            }
        });
    }

    private void gotoTakeIfNotFull() {
        FindItemResult findItemResult = InvUtils.find(Items.EMERALD);
        if (!findItemResult.found() || findItemResult.count() < 64 * curVillagerSettingWarp.getSupplyQty()/* || waitStartTime > 0*/) {
            curTakePos = moneyPos;
            curTakeItem = Items.EMERALD;
            gotoTargetIfNeed(moneyPos, minDistance.get(), Steps.TAKE_ITEM, "补给绿宝石");
            return;
        }

        boolean needBuyEnchantment = curVillagerSettingWarp.needBuyEnchantment();
        if (needBuyEnchantment) {
            FindItemResult findBookResult = InvUtils.find(Items.BOOK);
            if (!findBookResult.found() || findBookResult.count() < 64/* || waitStartTime > 0*/) {
                curTakePos = bookPos;
                curTakeItem = Items.BOOK;
                gotoTargetIfNeed(bookPos, minDistance.get(), Steps.TAKE_ITEM, "补给书");
                return;
            }
        }

        tryNext();
    }

    private boolean tryPut() {
        // 先卸货最近的
        Vec3d playerPos = mc.player.getEntityPos();
        ItemBo best = null;
        double minDis = Double.MAX_VALUE;
        for (ItemBo buyVillagerItem : buyVillagerItemList) {
            ItemStack nextPlayerStack = nextPlayerStack(buyVillagerItem::isSameItem);
            if (!nextPlayerStack.isEmpty()) {
                BlockPos putPos = putPosMap.get(buyVillagerItem);
                double distance = putPos.toCenterPos().distanceTo(playerPos);
                if (distance < minDis) {
                    best = buyVillagerItem;
                    minDis = distance;
                }
            }
        }

        if (best != null) {
            curPutPos = putPosMap.get(best);
            curPutVillagerItem = best;
            gotoTargetIfNeed(curPutPos, minDistance.get() + 1, Steps.PUT_ITEM, "前往卸货");
            return true;
        }

        return false;
    }

    private void put() {
        openKit(curPutPos, inventory -> {
            ItemStack nextStack = nextPlayerStack(itemStack -> curPutVillagerItem.isSameItem(itemStack));

            if (nextStack.isEmpty()) {
                // 身上没有需要放的物品了
                FindItemResult emptyResult = InvUtils.findEmpty();
                if (emptyResult.found()) {
                    // 有空位, 下一步
                    HeInvUtils.closeCurScreen();

                    gotoPutIfNeed();

                } else {
                    // 没空位了, 停止
                    HeInvUtils.closeCurScreen();
                    warning("无法清空背包");
                    stop();
                    delayStart();
                }
            } else {
                // 放
                InvUtils.shiftClick().slot(getCurPlayerSlot());
                setDelay();
            }
        });
    }

    private void gotoPutIfNeed() {
        boolean needPut = tryPut();
        if (!needPut) {
            gotoTakeIfNotFull();
        }
    }

    private List<VillagerEntityWarp> getVillagerEntity() {

        List<VillagerEntityWarp> villagerList = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {

            // TODO : 限制一定范围内
            // 是村民
            if (EntityType.VILLAGER.equals(entity.getType())) {

                double y = entity.getEntityPos().getY() - mc.player.getY();

                // 排除高度差太多的村民
                if (!(y >= -2) || !(y <= 2)) {
                    continue;
                }

                VillagerEntity villager = (VillagerEntity) entity;

                // 职业
                VillagerType currentType = VillagerType.fromEntry(villager.getVillagerData().profession());

                if (currentType == null) {
                    continue;
                }

                VillagerSettingWarp villagerSettingWarp = villagerSettingWarpMap.getOrDefault(currentType, villagerSettingWarp7);
                if ((villagerSettingWarp.isOpen() && currentType == villagerSettingWarp.getType())) {
                    BlockPos pos = getOperatePos(villager, currentType, Direction.EAST);
                    if (pos == null) {
                        pos = getOperatePos(villager, currentType, Direction.SOUTH);
                    }
                    if (pos == null) {
                        pos = getOperatePos(villager, currentType, Direction.WEST);
                    }
                    if (pos == null) {
                        pos = getOperatePos(villager, currentType, Direction.NORTH);
                    }
                    if (pos != null) {
                        villagerList.add(new VillagerEntityWarp(currentType, villager.getUuid(), pos));
                    }
                }
            }
        }
        return villagerList;
    }

    private BlockPos getOperatePos(VillagerEntity villager, VillagerType currentType, Direction direction) {

        BlockPos blockPos = villager.getBlockPos().offset(direction);
        BlockState blockState = mc.world.getBlockState(blockPos);

        if (blockState.getBlock().asItem() == currentType.getItem()) {
            BlockPos pos = blockPos.offset(direction);
            // 检查操作位置的上一个方块位置
            BlockPos checkPos = pos.add(0, 1, 0);
            if (mc.world.getBlockState(checkPos).isAir()) {
                return pos;
            }
        }

        return null;
    }

    private void delayStart() {
        // TODO : 需要重新检查村民的状态？
        if (restartFuture != null) {
            restartFuture.cancel(false);
        }

        // 检查当前模式是生存
        GameMode gameMode = mc.interactionManager.getCurrentGameMode();

        printLog("gameMode : " + gameMode);

        if (gameMode == GameMode.SPECTATOR || gameMode == GameMode.ADVENTURE) {
            return;
        }

        this.restartFuture = TaskUtils.run(() -> {
            info("启动");
            step = Steps.NEXT;
            this.restartFuture = null;
        }, restartDelay.get(), TimeUnit.SECONDS);
    }

    private void stop() {
        info("停止");
        step = Steps.NONE;
        if (restartFuture != null) {
            restartFuture.cancel(false);
        }
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
        stop();
    }

    private void printLog(String str, Object arg) {
        if (debug.get()) {
            log.info(str, arg);
        }
    }

    private void printLog(String str, Object... args) {
        if (debug.get()) {
            log.info(str, args);
        }
    }
}
