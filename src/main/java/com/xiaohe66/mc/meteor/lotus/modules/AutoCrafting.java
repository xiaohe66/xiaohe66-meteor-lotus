package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.NetworkRecipeId;
import net.minecraft.recipe.RecipeDisplayEntry;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_V;

/**
 * @author He
 * @since 2025.11.06 22:51
 */
public class AutoCrafting extends BaseModule {

    private static final Logger log = LoggerFactory.getLogger(AutoCrafting.class);

    private static final Map<Item, Item> kitColorMap;

    private final Setting<List<Item>> craftingItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("合成物品")
            .description("需要合成的物品")
            .defaultValue(List.of())
            .build());

    /*public final Setting<Boolean> remember = sgGeneral.add(new BoolSetting.Builder()
            .name("记住合成物品")
            .description("将合成过的物品自动添加到合成物品中")
            .defaultValue(true)
            .visible(() -> false)
            .build());*/

    private final Setting<Keybind> keybind1 = sgGeneral.add(new KeybindSetting.Builder()
            .name("合成键")
            .description("合成键")
            .defaultValue(Keybind.fromKey(GLFW_KEY_C))
            .action(() -> keydown(false))
            .build());

    private final Setting<Keybind> keybind2 = sgGeneral.add(new KeybindSetting.Builder()
            .name("丢出合成键")
            .description("丢出合成键")
            .defaultValue(Keybind.fromKey(GLFW_KEY_V))
            .action(() -> keydown(true))
            .build());

    private final Setting<FireworkSpeed> fireworkSpeed = sgGeneral.add(new EnumSetting.Builder<FireworkSpeed>()
            .name("烟花速度")
            .description("合成烟花时使用的火药数量（决定飞行速度）")
            .defaultValue(FireworkSpeed.三速)
            .build());

    private final Setting<Boolean> closeScreen = sgGeneral.add(new BoolSetting.Builder()
            .name("关闭界面")
            .description("在合成完毕后关闭界面")
            .defaultValue(true)
            .build());

    private static final long KIT_PLACE_TIMEOUT_MS = 1000;

    private Item needItem;

    private boolean doing;
    private boolean isDrop;

    // 防止高延迟时重复放入盒子：标记是否已经尝试放入盒子但服务器尚未响应
    private boolean kitPlacedPending;
    private long kitPlacedTime;

    private int needGunpowderCount;

    static {
        Map<Item, Item> map = new HashMap<>();
        map.put(Items.WHITE_SHULKER_BOX, Items.WHITE_DYE);
        map.put(Items.ORANGE_SHULKER_BOX, Items.ORANGE_DYE);
        map.put(Items.MAGENTA_SHULKER_BOX, Items.MAGENTA_DYE);
        map.put(Items.LIGHT_BLUE_SHULKER_BOX, Items.LIGHT_BLUE_DYE);
        map.put(Items.YELLOW_SHULKER_BOX, Items.YELLOW_DYE);
        map.put(Items.LIME_SHULKER_BOX, Items.LIME_DYE);
        map.put(Items.PINK_SHULKER_BOX, Items.PINK_DYE);
        map.put(Items.GRAY_SHULKER_BOX, Items.GRAY_DYE);
        map.put(Items.LIGHT_GRAY_SHULKER_BOX, Items.LIGHT_GRAY_DYE);
        map.put(Items.CYAN_SHULKER_BOX, Items.CYAN_DYE);
        map.put(Items.PURPLE_SHULKER_BOX, Items.PURPLE_DYE);
        map.put(Items.BLUE_SHULKER_BOX, Items.BLUE_DYE);
        map.put(Items.BROWN_SHULKER_BOX, Items.BROWN_DYE);
        map.put(Items.GREEN_SHULKER_BOX, Items.GREEN_DYE);
        map.put(Items.RED_SHULKER_BOX, Items.RED_DYE);
        map.put(Items.BLACK_SHULKER_BOX, Items.BLACK_DYE);

        kitColorMap = Collections.unmodifiableMap(map);
    }

    public AutoCrafting() {
        super("自动合成", "必须在工作台使用, 可自动识别工作台上的配方, 或者手动选择合成物品。（计划未来支持锻造台）", 3);
    }

    public enum FireworkSpeed {
        一速, 二速, 三速;

        public int getGunpowderCount() {
            return ordinal() + 1;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!checkAndDecrement()) {
            return;
        }

        if (!doing || craftingItems.get().isEmpty()) {
            return;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler craftingScreenHandler) {
            doCrafting(craftingScreenHandler);
        } else {
            finish();
        }
    }

    private void doCrafting(CraftingScreenHandler craftingScreenHandler) {
        // 烟花合成特殊处理
        if (needItem == Items.FIREWORK_ROCKET) {
            doFireworkCrafting(craftingScreenHandler);
            return;

        } else if (HeItemUtils.isShulkerBox(needItem)) {
            margeKit(craftingScreenHandler);
            return;

        } else {
            // 合成书
            Map<Item, NetworkRecipeId> recipeIdMap = getRecipeIdMap();
            if (recipeIdMap.containsKey(needItem)) {
                NetworkRecipeId networkRecipeId = recipeIdMap.get(needItem);
                mc.interactionManager.clickRecipe(craftingScreenHandler.syncId, networkRecipeId, true);
                SlotActionType actionType = isDrop ? SlotActionType.THROW : SlotActionType.QUICK_MOVE;
                mc.interactionManager.clickSlot(craftingScreenHandler.syncId, 0, 1, actionType, mc.player);
                setDelay();
                return;
            }
        }
        finish();
    }

    private void margeKit(CraftingScreenHandler craftingScreenHandler) {
        ItemStack outItemStack = craftingScreenHandler.getOutputSlot().getStack();
        if (outItemStack.getItem() == needItem) {
            // 合成成功，重置盒子放入标记
            kitPlacedPending = false;
            // 合成
            if (isDrop) {
                InvUtils.drop().slotId(0);
            } else {
                InvUtils.shiftClick().slotId(0);
            }
            setDelay();
            return;
        }

        Item colorItem = kitColorMap.get(needItem);
        boolean needKit = true;
        boolean needColor = true;
        for (int i = 1; i <= 9; i++) {
            Item item = craftingScreenHandler.getSlot(i).getStack().getItem();
            if (HeItemUtils.isShulkerBox(item)) {
                needKit = false;
            }
            if (item == colorItem) {
                needColor = false;
            }
        }

        if (needKit) {
            // 避免高延迟时重复放入盒子：如果已经尝试放入但服务器尚未响应，则等待
            if (kitPlacedPending) {
                // 检查是否超时，超时则重置标记允许重试
                if (System.currentTimeMillis() - kitPlacedTime < KIT_PLACE_TIMEOUT_MS) {
                    setDelay();
                    return;
                }
                // 超时，重置标记继续执行
                kitPlacedPending = false;
            }
            // 盒子放上去
            ItemStack nextStack = nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()) && itemStack.getItem() != needItem);
            if (nextStack.isEmpty()) {
                // 没有盒子了
                finish();
            } else {
                InvUtils.shiftClick().slot(getCurPlayerSlot());
                kitPlacedPending = true;
                kitPlacedTime = System.currentTimeMillis();
                setDelay();
            }
        } else if (needColor) {
            // 盒子已确认放入，重置标记
            kitPlacedPending = false;
            // 颜色放上去
            ItemStack nextStack = nextPlayerStack(itemStack -> itemStack.getItem() == colorItem);
            if (nextStack.isEmpty()) {
                // 没有染料了
                finish();
            } else {
                InvUtils.shiftClick().slot(getCurPlayerSlot());
                setDelay();
            }
        } else {
            // 因网络延迟导致合成栏上有材料, 但是结果没有显示的时候就会走到这个分支, 再等一下就好了
            setDelay();
        }
    }

    /**
     * 获取烟花的飞行时间（速度）
     *
     * @return 飞行时间（1-3），如果不是烟花或没有该组件则返回 -1
     */
    private int getFireworkFlightDuration(ItemStack stack) {
        if (stack.getItem() != Items.FIREWORK_ROCKET) {
            return -1;
        }
        FireworksComponent component = stack.get(DataComponentTypes.FIREWORKS);
        if (component == null) {
            return -1;
        }
        return component.flightDuration();
    }

    /**
     * 烟花合成处理
     * 根据设置项决定目标飞行速度（1-3），自动调整合成栏中的火药数量
     * 支持不均等平均分配材料以实现连续合成
     */
    private void doFireworkCrafting(CraftingScreenHandler craftingScreenHandler) {
        ItemStack outputStack = craftingScreenHandler.getOutputSlot().getStack();
        // 合成结果已出现，检查速度是否匹配
        if (outputStack.getItem() == Items.FIREWORK_ROCKET) {
            int flightDuration = getFireworkFlightDuration(outputStack);
            // 只有速度匹配目标速度时才取出
            if (flightDuration == needGunpowderCount) {
                if (isDrop) {
                    InvUtils.drop().slotId(0);
                } else {
                    InvUtils.shiftClick().slotId(0);
                }
                setDelay();
                return;
            }
            // 速度不匹配，继续等待正确的材料放置
        }

        // 统计合成栏中的材料
        int paperCount = 0;
        int gunpowderCount = 0;
        for (int i = 1; i <= 9; i++) {
            ItemStack stack = craftingScreenHandler.getSlot(i).getStack();
            if (stack.getItem() == Items.PAPER) {
                paperCount++;
            } else if (stack.getItem() == Items.GUNPOWDER) {
                gunpowderCount++;
            } else if (!stack.isEmpty()) {
                // 存在其他类型的材料, 需要纠正
                warning("工作台上存在错误的材料");
                finish();
                return;
            }
        }

        // 放置纸（只需要1格）
        if (paperCount <= 0) {
            ItemStack paperStack = nextPlayerStack(s -> s.getItem() == Items.PAPER);
            if (paperStack.isEmpty()) {
                // 没有纸了
                finish();
            } else {
                InvUtils.shiftClick().slot(getCurPlayerSlot());
                setDelay();
            }
        } else if (gunpowderCount > needGunpowderCount) {
            // 火药过多，取回一个
            for (int i = 1; i <= 9; i++) {
                if (craftingScreenHandler.getSlot(i).getStack().getItem() == Items.GUNPOWDER) {
                    InvUtils.shiftClick().slotId(i);
                    setDelay();
                    return;
                }
            }
        } else if (gunpowderCount < needGunpowderCount) {
            ItemStack nextStack = nextPlayerStack(s -> s.getItem() == Items.GUNPOWDER);
            if (nextStack.isEmpty()) {
                // 没有火药了
                finish();
            } else {
                InvUtils.shiftClick().slot(getCurPlayerSlot());
                setDelay();
            }
        } else {
            // 材料已正确放置，等待服务器响应
            setDelay();
        }
    }

    private void keydown(boolean isDrop) {
        if (!isReady()) {
            return;
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler craftingScreenHandler) {

            if (!isDrop) {
                FindItemResult emptyResult = InvUtils.find(ItemStack::isEmpty, 0, 35);

                if (!emptyResult.found()) {
                    info("需要至少留一个空位");
                    return;
                }
            }

            // NOTE : 每次只合一种成品
            // 优先合成已放置的配方
            Item needItem = Items.AIR;

            Slot outputSlot = craftingScreenHandler.getOutputSlot();
            ItemStack outItemStack = outputSlot.getStack();
            if (!outItemStack.isEmpty()) {
                if (!craftingItems.get().contains(outItemStack.getItem())) {
                    craftingItems.get().add(outItemStack.getItem());
                }
                needItem = outItemStack.getItem();
            }

            if (needItem == Items.AIR) {
                Map<Item, NetworkRecipeId> recipeIdMap = getRecipeIdMap();
                for (Map.Entry<Item, NetworkRecipeId> entry : recipeIdMap.entrySet()) {
                    Item item = entry.getKey();
                    if (craftingItems.get().contains(item)) {
                        needItem = item;
                        if (kitColorMap.containsKey(item)) {
                            // 优先给kit染色
                            break;
                        }
                    }
                }
            }

            if (needItem == Items.AIR) {
                return;
            }

            if (needItem == Items.FIREWORK_ROCKET) {
                // 合成烟花，根据设置的速度决定火药数量
                this.needGunpowderCount = fireworkSpeed.get().getGunpowderCount();
            }

            this.needItem = needItem;
            this.doing = true;
            this.isDrop = isDrop;
            this.kitPlacedPending = false;
            info("合成物品: " + Names.get(needItem));

        } else {
            this.doing = false;
        }
    }

    private void closeCurScreenIfNeed() {
        if (closeScreen.get()) {
            HeInvUtils.closeCurScreen();
        }
    }

    private void finish() {
        info("合成结束");
        doing = false;
        closeCurScreenIfNeed();
    }

    private Map<Item, NetworkRecipeId> getRecipeIdMap() {
        Map<Item, NetworkRecipeId> recipeIdMap = new HashMap<>();
        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        for (RecipeResultCollection recipeResultCollection : recipeBook.getOrderedResults()) {
            List<RecipeDisplayEntry> craftRecipes = recipeResultCollection.filter(RecipeResultCollection.RecipeFilterMode.CRAFTABLE);
            for (RecipeDisplayEntry recipe : craftRecipes) {
                RecipeDisplay recipeDisplay = recipe.display();
                List<ItemStack> resultStacks = recipeDisplay.result().getStacks(SlotDisplayContexts.createParameters(mc.world));
                for (ItemStack resultStack : resultStacks) {
                    Item item = resultStack.getItem();
                    if (craftingItems.get().contains(item)) {
                        recipeIdMap.put(item, recipe.id());
                    }
                }
            }
        }
        return recipeIdMap;
    }
}