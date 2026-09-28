package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.modules.enchantment.AutoEnchantmentType;
import com.xiaohe66.mc.meteor.lotus.modules.enchantment.AutoEnchantmentXpSource;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.EnchantmentUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.HePosUtils;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.EnchantmentNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentBookNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentEquipNode;
import com.xiaohe66.mc.meteor.lotus.util.enchantment.impl.EnchantmentMargeNode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.RenameItemC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author He
 * @since 2025.06.13 09:34
 */
public class AutoEnchantment extends StepModule {

    private static final Logger log = LoggerFactory.getLogger(AutoEnchantment.class);
    private static final int USE_STATE = -2;
    private static final int TAKE_BOOK_STATE = -3;

    // 可被附魔的物品缓存
    private static final Set<Item> ENCHANTABLE_ITEMS = Set.of(
            // 工具
            Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE,
            Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
            Items.SHEARS, Items.FLINT_AND_STEEL,
            // 武器
            Items.DIAMOND_SWORD, Items.BOW,
            Items.CROSSBOW, Items.TRIDENT, Items.MACE,
            // 护甲 - 头盔
            Items.DIAMOND_HELMET, Items.TURTLE_HELMET,
            // 护甲 - 胸甲
            Items.DIAMOND_CHESTPLATE, Items.ELYTRA,
            // 护甲 - 护腿
            Items.DIAMOND_LEGGINGS,
            // 护甲 - 靴子
            Items.DIAMOND_BOOTS,
            // 钓鱼竿
            Items.FISHING_ROD,
            // 书
            Items.BOOK, Items.ENCHANTED_BOOK
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<AutoEnchantmentXpSource> xpSource = sgGeneral.add(new EnumSetting.Builder<AutoEnchantmentXpSource>()
            .name("经验来源")
            .description("缺少经验时经验的来源类型")
            .defaultValue(AutoEnchantmentXpSource.杀戮光环)
            .build());

    public final Setting<Integer> dropXpDelay = sgGeneral.add(new IntSetting.Builder()
            .name("丢经验延迟")
            .description("是否给附魔后的物品改名")
            .sliderRange(0, 100)
            .defaultValue(5)
            .visible(() -> xpSource.get() == AutoEnchantmentXpSource.XP)
            .build());

    public final Setting<Boolean> rename = sgGeneral.add(new BoolSetting.Builder()
            .name("改名")
            .description("是否给附魔后的物品改名")
            .defaultValue(true)
            .build());

    public final Setting<String> newName = sgGeneral.add(new StringSetting.Builder()
            .name("新名称")
            .description("给附魔后的物品设置的新名称")
            .defaultValue("lotus打造的神兵")
            .build());

    public final Setting<Boolean> onlyRename = sgGeneral.add(new BoolSetting.Builder()
            .name("不附魔仅改名")
            .description("不做附魔仅做改名")
            .defaultValue(false)
            .onChanged(b -> {
                if (b) {
                    rename.set(true);
                }
            })
            .build());

    public final Setting<Item> targetItem = sgGeneral.add(new ItemSetting.Builder()
            .name("物品")
            .description("要附魔的物品")
            .defaultValue(Items.DIAMOND_PICKAXE)
            .filter(ENCHANTABLE_ITEMS::contains)
            .visible(() -> !onlyRename.get())
            .build());

    public final Setting<AutoEnchantmentType> presetType = sgGeneral.add(new EnumSetting.Builder<AutoEnchantmentType>()
            .name("预设")
            .description("选择预设配置，选择后会自动更新物品和附魔选项")
            .defaultValue(AutoEnchantmentType.自定义)
            .onChanged(this::onPresetChanged)
            .visible(() -> !onlyRename.get())
            .build());

    public final Setting<Set<RegistryKey<Enchantment>>> selectedEnchantments = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("附魔")
            .description("选择要附魔的附魔类型，选择预设时会自动更新")
            // .onChanged(this::onSelectedEnchantments)
            .visible(() -> !onlyRename.get())
            .build());

    public final Setting<Integer> pauseTime = sgGeneral.add(new IntSetting.Builder()
            .name("暂停时间")
            .sliderRange(0, 100)
            .defaultValue(20)
            .visible(() -> !onlyRename.get())
            .build());

    private AnvilScreenHandler anvilLevelHandler;

    private final Map<RegistryKey<Enchantment>, BlockPos> enchantmentPosMap = new HashMap<>();
    private BlockPos supplyPos;
    private BlockPos grindPos;
    private BlockPos anvilPos;
    private BlockPos putPos;

    private final LinkedList<RegistryKey<Enchantment>> needTakeBook = new LinkedList<>();
    private int needLevel;
    private int index1;
    private int index2;
    private ItemStack itemStack1;
    private ItemStack itemStack2;
    private Item onlyRenameItem = Items.AIR;
    private int lastSlot = -1;

    public AutoEnchantment() {
        super("自动附魔", "自动附魔(祛魔)和改名, 附魔需要搭配附魔平台使用（但仅改名时可以随处使用）。快捷栏需要拿一些铁砧。");

        addStep(Steps.NEXT, this::next);
        addStep(Steps.TAKE_ITEM, this::takeEquip);
        addStep(Steps.GRIND, this::grind);
        addStep(Steps.TAKE_BOOK, this::takeBook);
        addStep(Steps.PLACE, this::placeAnvil);
        addStep(Steps.USE, this::useAnvil);
        addStep(Steps.LEVEL, this::level);
        addStep(Steps.PUT_ITEM, this::put);
    }

    @Override
    public void onActivate() {
        if (!isReady()) {
            return;
        }

        // 初始化
        anvilLevelHandler = new AnvilScreenHandler(-1, mc.player.getInventory());

        if (rename.get() && StringUtils.isBlank(newName.get())) {
            warning("改名时<新名称>不能为空");
            toggle();
            return;
        }

        if (onlyRename.get()) {
            // 物品改名
            if (!rename.get()) {
                warning("<仅改名>时需要勾选<改名>");
                toggle();
                return;
            }

            ItemStack onlyRenameItemStack = getItemStack(getMainSlot());
            if (onlyRenameItemStack.isEmpty()) {
                warning("主手需要拿着改名的物品");
                toggle();
                return;
            }

            // 搜索铁砧位置
            List<BlockPos> blockPosList = HeBlockUtils.listPosInSphere(4, 2, mc.player.getBlockPos());
            for (BlockPos blockPos : blockPosList) {
                BlockState blockState = mc.world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                    anvilPos = blockPos;

                    this.onlyRenameItem = onlyRenameItemStack.getItem();
                    info("开始改名:" + Names.get(onlyRenameItem));
                    delayNext(Steps.NEXT);
                    return;
                }
            }
            warning("身边没有铁砧");
            toggle();

        } else {
            enchantmentPosMap.clear();
            needTakeBook.clear();
            supplyPos = null;
            grindPos = null;
            anvilPos = null;
            putPos = null;

            // 搜索输入、输出的位置
            List<ItemFrameEntity> bookItemFrame = mc.world.getEntitiesByClass(
                    ItemFrameEntity.class,
                    mc.player.getBoundingBox().expand(6),
                    frame -> {
                        Item item = frame.getHeldItemStack().getItem();
                        return item == Items.REDSTONE || item == Items.REDSTONE_BLOCK;
                    });

            for (ItemFrameEntity itemFrame : bookItemFrame) {
                ItemStack itemStack = itemFrame.getHeldItemStack();
                if (itemStack.isEmpty()) {
                    continue;
                }
                if (itemStack.getItem() == Items.REDSTONE) {
                    Optional<BlockPos> chestPos = HePosUtils.getOtherChestPos(itemFrame);
                    chestPos.ifPresent(blockPos -> supplyPos = blockPos);

                } else if (itemStack.getItem() == Items.REDSTONE_BLOCK) {
                    Optional<BlockPos> chestPos = HePosUtils.getOtherChestPos(itemFrame);
                    chestPos.ifPresent(blockPos -> putPos = blockPos);
                }
            }

            // 搜索砂轮、铁砧的位置
            List<BlockPos> blockPosList = HeBlockUtils.listPosInSphere(5, 2, mc.player.getBlockPos());
            for (BlockPos blockPos : blockPosList) {
                BlockState blockState = mc.world.getBlockState(blockPos);
                Block block = blockState.getBlock();
                if (block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL) {
                    anvilPos = blockPos;
                } else if (block == Blocks.GRINDSTONE) {
                    grindPos = blockPos;
                }
            }

            if (supplyPos == null) {
                warning("未识别到输入位置");
                toggle();
                return;
            }
            if (putPos == null) {
                warning("未识别到输出位置");
                toggle();
                return;
            }
            if (anvilPos == null) {
                warning("未识别到铁砧");
                toggle();
                return;
            }
            if (grindPos == null) {
                warning("未识别到砂轮");
                toggle();
                return;
            }

            Map<ItemBo, BlockPos> takePosMap = HePosUtils.getItemFrameKitPosMap(6);
            if (takePosMap.isEmpty()) {
                warning("未识别到附魔书");
                toggle();
                return;
            }
            for (Map.Entry<ItemBo, BlockPos> entry : takePosMap.entrySet()) {
                enchantmentPosMap.put(entry.getKey().getEnchantment(), entry.getValue());
            }

            delayNext(Steps.NEXT);
        }
    }

    // ========== 预设联动逻辑 ==========

    private void onPresetChanged(AutoEnchantmentType newType) {
        // 更新附魔集合
        if (!newType.getAll().equals(selectedEnchantments.get())) {
            selectedEnchantments.set(newType.getAll());
        }
    }

    /*private void onSelectedEnchantments(Set<RegistryKey<Enchantment>> registryKeys) {
        if (presetType.get() != AutoEnchantmentType.自定义 && System.currentTimeMillis() - changeTime > 500) {
            presetType.set(AutoEnchantmentType.自定义);
        }
    }*/

    private void put() {
        openChest(putPos, inventory -> {
            ItemStack nextDoneStack = nextDoneStack();
            if (!nextDoneStack.isEmpty()) {
                info("卸货");

                // 放
                InvUtils.shiftClick().slot(getCurPlayerSlot());

                setDelay();
                return;
            }

            delayCloseNext(Steps.NEXT);
            // delayCloseNext(Steps.NEXT, () -> nextDoneStack().isEmpty());

            // 完成卸货后等待2秒，方便用户关闭功能
            setCloseScreenAfterDelay(pauseTime.get());
        });
    }

    private ItemStack nextDoneStack() {
        Item target = targetItem.get();
        return nextPlayerStack(itemStack -> {
            if (itemStack.getItem() == targetItem.get()) {
                Set<RegistryKey<Enchantment>> enchantmentSet = EnchantmentUtils.getEnchantment(itemStack);
                return enchantmentSet.equals(selectedEnchantments.get());
            }
            return false;
        });
    }

    private void level() {
        if (xpSource.get() == AutoEnchantmentXpSource.XP) {
            if (needLevel <= mc.player.experienceLevel) {
                delayNext(Steps.NEXT);
                return;
            }

            FindItemResult xpResult = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
            if (xpResult.found()) {
                if (xpResult.getHand() == null) {
                    // 切主手
                    InvUtils.swap(xpResult.slot(), false);
                } else {
                    // 丢XP
                    Rotations.rotate(mc.player.getYaw(), 90, () -> {
                        mc.interactionManager.interactItem(mc.player, xpResult.getHand());
                    });
                }
                // NOTE : 丢出的XP需要等一会才会被吸到身上, 所以至少要等5 tick
                setDelay(dropXpDelay.get());

            } else {
                FindItemResult xpResult2 = InvUtils.find(itemStack -> itemStack.getItem() == Items.EXPERIENCE_BOTTLE, 0, 36);
                if (xpResult2.found()) {
                    // 切主手
                    HeInvUtils.swapMainHand(xpResult2.slot());
                    setDelay();

                } else {
                    // TODO : 自动补给 XP ？
                    warning("缺少XP");
                    toggle();
                }
            }
            return;
        }

        KillAura killAura = Modules.get().get(KillAura.class);
        if (needLevel > mc.player.experienceLevel) {

            if (!killAura.isActive()) {

                info("开启杀戮");
                killAura.toggle();
            }

            setDelay();

        } else {
            // info("关闭杀戮");

            if (killAura.isActive()) {
                killAura.toggle();
            }

            delayNext(Steps.NEXT);
        }
    }

    private void placeAnvil() {

        FindItemResult anvilResult = InvUtils.find(Items.ANVIL);
        if (!anvilResult.found()) {
            warning("缺少铁砧");
            toggle();
            return;
        }

        if (!BlockUtils.canPlace(anvilPos, true)) {
            warning("无法放置铁砧");
            toggle();
            return;
        }

        if (InvUtils.testInMainHand(Items.ANVIL)) {
            info("放置铁砧");
            HeBlockUtils.place(anvilPos, anvilResult.slot(), true, Direction.DOWN);

            delayNext(Steps.USE);
        } else {

            HeInvUtils.swapMainHand(anvilResult.slot());
            setDelay();
        }
    }

    private void useAnvil() {

        // info("useAnvil");

        // 计算需要的经验
        ItemStack itemStack1 = getItemStack(index1);
        ItemStack itemStack2 = ItemStack.EMPTY;
        if (!onlyRename.get()) {
            itemStack2 = getItemStack(index2);
        }
        String newNameValue = null;
        if (rename.get()) {
            newNameValue = newName.get();
        }

        int needLevel = getLevelCost(itemStack1, itemStack2, newNameValue);
        if (needLevel > mc.player.experienceLevel) {
            this.needLevel = needLevel;
            delayCloseNext(Steps.LEVEL);
            return;
        }

        if (!(mc.player.currentScreenHandler instanceof AnvilScreenHandler screenHandler)) {

            // 用坏了要补
            if (mc.world.getBlockState(anvilPos).isAir()) {
                info("补放铁砧");
                delayNext(Steps.PLACE);
                return;
            }

            // info("打开铁砧");
            // log.info("open anvil : {}", anvilPos);
            rotateAndOpen(anvilPos);

            setDelay();

            return;
        }

        if (screenHandler.getSlot(0).getStack() == ItemStack.EMPTY) {

            // info("放入1 : " + index1);

            if (getItemStack(index1).getItem() != this.itemStack1.getItem()) {
                delayCloseNext(Steps.NEXT);
                return;
            }

            InvUtils.shiftClick().slot(index1);
            setDelay();

        } else if (!onlyRename.get() && screenHandler.getSlot(1).getStack() == ItemStack.EMPTY) {

            // info("放入2 : " + index2);

            if (getItemStack(index2).getItem() != itemStack2.getItem()) {
                delayCloseNext(Steps.NEXT);
                return;
            }

            InvUtils.shiftClick().slot(index2);
            setDelay();

        } else {

            if (rename.get()) {

                ItemStack itemStack = screenHandler.getSlot(2).getStack();
                if (itemStack.getItem() != Items.ENCHANTED_BOOK) {

                    Text customName = itemStack.getCustomName();
                    if (customName == null || !newNameValue.equals(customName.getString())) {

                        info("改名");

                        // 修改结果 ItemStack 的名称（服务端）
                        screenHandler.setNewItemName(newNameValue);
                        // 修改结果 ItemStack 的名称（客户端）
                        TextFieldWidget textFieldWidget = ((AnvilScreen) mc.currentScreen).nameField;
                        textFieldWidget.setText(newNameValue);
                        textFieldWidget.setCursorToEnd(false);

                        // 发包更新服务器，真正改名的代码
                        mc.player.networkHandler.sendPacket(new RenameItemC2SPacket(newNameValue));
                        setDelay();
                        return;
                    }
                }
            }

            if (screenHandler.getLevelCost() > mc.player.experienceLevel) {
                // 补经验
                // info("需要补经验");

                this.needLevel = screenHandler.getLevelCost();
                delayCloseNext(Steps.LEVEL);

            } else {

                // info("合成！");
                InvUtils.shiftClick().slotId(2);

                if (onlyRename.get()) {
                    delayNext(Steps.NEXT);

                } else {
                    delayCloseNext(Steps.NEXT);
                }
            }
        }
    }

    private void grind() {
        if (!(mc.player.currentScreenHandler instanceof GrindstoneScreenHandler screenHandler)) {
            if (mc.world.getBlockState(grindPos).getBlock() != Blocks.GRINDSTONE) {
                breakStep("砂轮位置错误");
                return;
            }

            rotateAndOpen(grindPos);
            setDelay();
            return;
        }

        if (screenHandler.getSlot(0).getStack() == ItemStack.EMPTY) {

            // info("放入1 : " + index1);

            if (getItemStack(index1).getItem() != itemStack1.getItem()) {
                delayCloseNext(Steps.NEXT);
                return;
            }

            InvUtils.shiftClick().slot(index1);
            setDelay();

        } else {

            info("祛魔！");

            InvUtils.shiftClick().slotId(2);

            delayCloseNext(Steps.NEXT);
        }
    }

    private void takeEquip() {
        Item target = targetItem.get();
        openChest(supplyPos, inventory -> {

            ItemStack nextItemStack = nextScreenStack(itemStack -> itemStack.getItem() == target);
            if (nextItemStack.isEmpty()) {
                warning("无法补给<" + Names.get(target) + ">");
                setDelay();
                return;
            }

            int curScreenSlot = getCurScreenSlot();
            // lastSlot = curScreenSlot;
            InvUtils.shiftClick().slotId(curScreenSlot);
            info("拿取装备");
            delayCloseNext(Steps.NEXT, () -> lastSlot == -1);
            // delayCloseNext(Steps.NEXT, () -> !nextScreenStack(itemStack -> itemStack.getItem() == target).isEmpty());
        });
    }

    private void takeBook() {
        if (needTakeBook.isEmpty()) {
            delayCloseNext(Steps.NEXT);
            return;
        }

        RegistryKey<Enchantment> needBook = needTakeBook.getFirst();
        BlockPos kitPos = enchantmentPosMap.get(needBook);

        if (mc.player.currentScreenHandler instanceof PlayerScreenHandler) {
            // info("打开补给");
            if (kitPos == null) {
                warning("找不到<" + Names.get(needBook) + ">容器");
                toggle();
                return;
            }

            // log.info("open book kit : {}", kitPos);
            if (!HeItemUtils.isShulkerBox(mc.world.getBlockState(kitPos).getBlock().asItem())) {
                warning("找不到<" + Names.get(needBook) + ">容器");
                setDelay();
                return;
            }
        }

        openKit(kitPos, Direction.UP, screenHandler -> {
            ItemStack nextScreenStack = nextNeedBook(needBook);

            if (nextScreenStack.isEmpty()) {
                warning("无法拿取<" + Names.get(needBook) + ">");
                setDelay();
                return;
            }

            // TODO : 检查背包有空位
            info("拿取<" + Names.get(needBook) + ">");

            int bookSlot = getCurScreenSlot();
            // TODO : 以包同步的方式去检查
            // lastSlot = bookSlot;
            // lastSlot = -1;
            InvUtils.shiftClick().slotId(bookSlot);

            needTakeBook.poll();

            if (needTakeBook.isEmpty()) {
                delayCloseNext(Steps.NEXT, () -> lastSlot == -1);
                // delayCloseNext(Steps.NEXT, () -> !nextNeedBook(needBook).isEmpty());

            } else {
                delayCloseNext(Steps.TAKE_BOOK);
                // delayCloseNext(Steps.TAKE_BOOK, () -> !nextNeedBook(needBook).isEmpty());
            }
        });
    }

    private ItemStack nextNeedBook(RegistryKey<Enchantment> needBook) {
        return nextScreenStack(itemStack -> {
            if (itemStack.getItem() == Items.ENCHANTED_BOOK) {
                Set<RegistryKey<Enchantment>> bookEnchantMentSet = EnchantmentUtils.getEnchantment(itemStack);
                return bookEnchantMentSet.contains(needBook);
            } else {
                return false;
            }
        });
    }

    private void next() {
        if (onlyRename.get()) {
            ItemStack nextStack = nextPlayerStack(itemStack -> {
                if (itemStack.getItem() == onlyRenameItem) {
                    Text customName = itemStack.getCustomName();
                    boolean done = customName != null && newName.get().equals(customName.getString());
                    return !done;
                }
                return false;
            });

            if (nextStack.isEmpty()) {
                breakStep("改名完毕");
                closeScreen();
                toggle();

            } else {
                setIndex1(getCurPlayerSlot());
                delayNext(Steps.USE);
            }
            return;
        }

        ItemStack equipItemStack = nextPlayerStack(itemStack -> itemStack.getItem() == targetItem.get());
        if (equipItemStack.isEmpty()) {
            // 拿装备
            delayNext(Steps.TAKE_ITEM);
            return;
        } else {
            // 找到需要附魔的装备了
        }

        Set<RegistryKey<Enchantment>> targetEnchants = selectedEnchantments.get();
        Set<RegistryKey<Enchantment>> enchantmentSet = EnchantmentUtils.getEnchantment(equipItemStack);
        if (!targetEnchants.containsAll(enchantmentSet)) {
            // 如果存在不包含的附魔, 则去魔
            setIndex1(getCurPlayerSlot());
            delayNext(Steps.GRIND);
            return;
        }

        Set<RegistryKey<Enchantment>> haveEnchantmentSet = EnchantmentUtils.getEnchantment(equipItemStack, true);
        if (haveEnchantmentSet.containsAll(targetEnchants)) {
            delayNext(Steps.PUT_ITEM);
            return;
        }

        Set<RegistryKey<Enchantment>> missEnchantmentSet = new HashSet<>(targetEnchants);
        missEnchantmentSet.removeAll(haveEnchantmentSet);

        EnchantmentMargeNode margeNode = EnchantmentUtils.bestStepSimple(equipItemStack, missEnchantmentSet);
        getSlotOrMarge(margeNode);
    }

    private int getSlotOrMarge(EnchantmentNode node) {
        if (node instanceof EnchantmentMargeNode margeNode) {
            // 需要合并的
            if (margeNode.getLeft() instanceof EnchantmentEquipNode) {
                int rightSlot = getSlotOrMarge(margeNode.getRight());
                if (rightSlot >= 0) {
                    int leftSlot = getCurPlayerSlot();
                    setIndex1(leftSlot);
                    setIndex2(rightSlot);
                    step = Steps.USE;
                    return USE_STATE;

                } else if (rightSlot == TAKE_BOOK_STATE) {
                    step = Steps.TAKE_BOOK;
                    return TAKE_BOOK_STATE;

                } else if (rightSlot == USE_STATE) {
                    return USE_STATE;

                } else {
                    error("意外的状态(equip), right :" + rightSlot);
                    return rightSlot;
                }
            } else {
                int bookSlot = HeInvUtils.findBookSlot(margeNode.getAllEnchantmentKey());
                if (bookSlot >= 0) {
                    // 找到了合并的书
                    return bookSlot;
                }

                // 需要合并
                int leftSlot = getSlotOrMarge(margeNode.getLeft());
                if (leftSlot == USE_STATE) {
                    return USE_STATE;
                }

                int rightSlot = getSlotOrMarge(margeNode.getRight());
                if (rightSlot == USE_STATE) {
                    return USE_STATE;
                }

                if (leftSlot == TAKE_BOOK_STATE || rightSlot == TAKE_BOOK_STATE) {
                    step = Steps.TAKE_BOOK;
                    return TAKE_BOOK_STATE;
                }

                if (leftSlot < 0) {
                    error("意外的状态(book), left :" + leftSlot);
                    return leftSlot;
                }

                if (rightSlot < 0) {
                    error("意外的状态(book), right :" + rightSlot);
                    return rightSlot;
                }

                setIndex1(leftSlot);
                setIndex2(rightSlot);
                step = Steps.USE;
                return USE_STATE;
            }

        } else if (node instanceof EnchantmentBookNode bookNode) {
            int slot = HeInvUtils.findBookSlot(bookNode.getEnchantmentKey());
            if (slot >= 0) {
                // 找到了书
                return slot;
            }

            // 需要拿书
            needTakeBook.add(bookNode.getEnchantmentKey());
            return TAKE_BOOK_STATE;

        } else {
            throw new IllegalStateException("不可能的情况");
        }
    }

    private void setIndex1(int slot) {
        this.index1 = slot;
        this.itemStack1 = getItemStack(slot);
    }

    private void setIndex2(int slot) {
        this.index2 = slot;
        this.itemStack2 = getItemStack(slot);
    }

    private int getLevelCost(ItemStack itemStack, ItemStack itemStack2, String newName) {
        anvilLevelHandler.getSlot(0).setStack(itemStack);
        if (!itemStack2.isEmpty()) {
            anvilLevelHandler.getSlot(1).setStack(itemStack2);
        }
        if (newName != null) {
            anvilLevelHandler.setNewItemName(newName);
        }

        anvilLevelHandler.updateResult();
        return anvilLevelHandler.getLevelCost();
    }

    @Override
    public void onDeactivate() {
        step = Steps.NONE;
        KillAura killAura = Modules.get().get(KillAura.class);
        if (killAura.isActive()) {
            killAura.toggle();
        }
    }
}
