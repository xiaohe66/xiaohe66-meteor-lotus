package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.event.KeyboardInputTickEvent;
import com.xiaohe66.mc.meteor.lotus.modules.printer.PlaceBlockHelper;
import com.xiaohe66.mc.meteor.lotus.modules.printer.SortAlgorithm;
import com.xiaohe66.mc.meteor.lotus.modules.printer.SortingSecond;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author He
 * @since 2025.03.07 12:04
 */
public class LitematicaPrinter extends BaseModule {

    private static final Logger log = LoggerFactory.getLogger(LitematicaPrinter.class);


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    // private final SettingGroup sgWhitelist = settings.createGroup("白名单");
    private final SettingGroup sgRendering = settings.createGroup("渲染");

    private final Setting<Double> printingRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("放置范围(格)")
            .description("放置范围(格)")
            .defaultValue(4.5)
            .min(1).sliderMin(1)
            .max(6).sliderMax(6)
            .build());

    private final Setting<Double> yPrintingRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("放置高度(格)")
            .description("放置高度(格)")
            .defaultValue(1)
            .min(1)
            .sliderMax(6)
            .build());

    /*private final Setting<Integer> placeQtyPer = sgGeneral.add(new IntSetting.Builder()
            .name("放置数量/tick")
            .description("每tick放置数量")
            .defaultValue(1)
            .min(1)
            .sliderMax(100)
            // .visible(() -> false)
            .build());*/

    /*private final Setting<Boolean> advanced = sgGeneral.add(new BoolSetting.Builder()
            .name("智能")
            .description("尊重方块旋转（在单人游戏中把方块放在奇怪的地方，多人游戏应该没问题）.")
            .defaultValue(false)
            .build()
    );*/

    private final Setting<Boolean> airPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("空气放置-org")
            .description("空气放置, 副手绕过, 用于 2b2t.org")
            .defaultValue(false)
            .build());

    /*private final Setting<Boolean> placeThroughWall = sgGeneral.add(new BoolSetting.Builder()
            .name("穿墙放置")
            .description("允许穿墙放置")
            .defaultValue(true)
            .build()
    );*/

    private final Setting<Boolean> movePause = sgGeneral.add(new BoolSetting.Builder()
            .name("移动暂停")
            .description("移动时暂停放置")
            .defaultValue(true)
            .build());

    /*private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("挥手")
            .description("放置时挥动手")
            .defaultValue(true)
            .build()
    );*/

    private final Setting<Boolean> returnHand = sgGeneral.add(new BoolSetting.Builder()
            .name("复原物品栏")
            .description("复原物品栏")
            .defaultValue(false)
            .visible(() -> false)
            .build());

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("旋转")
            .description("旋转到正在放置的块")
            .defaultValue(true)
            .build());

    /*private final Setting<Boolean> clientSide = sgGeneral.add(new BoolSetting.Builder()
            .name("客户端旋转")
            .description("旋转到正在放置的块")
            .defaultValue(true)
            .visible(rotate::get)
            .build()
    );*/

    /*private final Setting<Boolean> dirtgrass = sgGeneral.add(new BoolSetting.Builder()
            .name("泥土代替草")
            .description("泥土代替草.")
            .defaultValue(true)
            .build());*/

    private final Setting<SortAlgorithm> firstAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortAlgorithm>()
            .name("优先放置模式")
            .description("优先放置模式")
            .defaultValue(SortAlgorithm.DownTop)
            .build());

    private final Setting<SortingSecond> secondAlgorithm = sgGeneral.add(new EnumSetting.Builder<SortingSecond>()
            .name("第二放置模式")
            .description("第二放置模式")
            .defaultValue(SortingSecond.最远的)
            .visible(() -> firstAlgorithm.get().applySecondSorting)
            .build());

    /*private final Setting<Boolean> whitelistenabled = sgWhitelist.add(new BoolSetting.Builder()
            .name("白名单(方块)")
            .description("只放置选择的方块")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("白名单")
            .description("放置的方块")
            .visible(whitelistenabled::get)
            .build()
    );*/

    private final Setting<List<Block>> blacklist = sgGeneral.add(new BlockListSetting.Builder()
            .name("黑名单")
            .description("不允许放置的方块")
            .build());

    private final Setting<Boolean> renderBlocks = sgRendering.add(new BoolSetting.Builder()
            .name("放置渲染")
            .description("放置渲染")
            .defaultValue(true)
            .visible(() -> false)
            .build());

    private final Setting<Integer> fadeTime = sgRendering.add(new IntSetting.Builder()
            .name("渲染淡出时间(tick)")
            .description("渲染淡出时间")
            .defaultValue(5)
            .sliderRange(1, 20)
            .sliderMin(1)
            .visible(renderBlocks::get)
            .build());

    private final Setting<SettingColor> colour = sgRendering.add(new ColorSetting.Builder()
            .name("颜色")
            .description("颜色")
            .defaultValue(new SettingColor(95, 190, 255))
            .visible(renderBlocks::get)
            .build());

    private final Setting<Boolean> debug = sgRendering.add(new BoolSetting.Builder()
            .name("调试模式")
            .description("调试模式")
            .defaultValue(false)
            .build());

    private int lastUsedSlot = -1;
    // private Vec3d lastPlayerPos = Vec3d.ZERO;
    private boolean isMoving;
    // private boolean canMove;
    private final List<PlaceBlockHelper> needPlaceBlockList = new ArrayList<>();
    private final List<Pair<Integer, BlockPos>> renderPosList = new ArrayList<>();

    public LitematicaPrinter() {
        super("投影打印", "grim可用，在不复杂场景下的使用, 适合建造刷怪塔、村民交易所等简单生电机器, 暂不支持楼梯、活板门等", 1);
    }

    @Override
    public void onActivate() {
        /*if (isReady()) {
            lastPlayerPos = mc.player.getPos();
        }*/
        isMoving = false;
        // canMove = true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onKeyboardInputTickEvent(KeyboardInputTickEvent event) {
        PlayerInput playerInput = event.getPlayerInput();
        this.isMoving = playerInput.forward() || playerInput.backward() || playerInput.left() || playerInput.right() || playerInput.jump();
        /*if (movePause.get() && isMoving && !canMove) {

            PlayerInput newPlayerInput = new PlayerInput(
                    false,
                    false,
                    false,
                    false,
                    false,
                    playerInput.sneak(),
                    playerInput.sprint());

            event.setPlayerInput(newPlayerInput);
        }*/
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isReady()) {
            renderPosList.clear();
            return;
        }

        renderPosList.forEach(s -> s.setLeft(s.getLeft() - 1));
        renderPosList.removeIf(s -> s.getLeft() <= 0);

        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        if (worldSchematic == null) {
            warning("未加载投影");
            renderPosList.clear();
            toggle();
            return;
        }

        if (!checkAndDecrement()) {
            return;
        }

        // canMove = true;

        if (movePause.get()) {
            // Vec3d currentPos = mc.player.getPos();
            // double distanceMoved = currentPos.distanceTo(lastPlayerPos);
            // boolean isMoving = distanceMoved > 0.05;
            // lastPlayerPos = currentPos;

            // 移动时关闭功能
            if (isMoving) {
                setDelay(5);
                return;
            }
        }

        needPlaceBlockList.clear();

        List<BlockPos> blockPosList = HeBlockUtils.listPosInSphere((int) (printingRange.get() + 1), (int) (yPrintingRange.get() + 1), mc.player.getBlockPos());

        for (BlockPos schematicBlockPos : blockPosList) {
            BlockState targetCurBlockState = mc.world.getBlockState(schematicBlockPos);

            BlockState schematicBlockState = worldSchematic.getBlockState(schematicBlockPos);
            Block targetBlock = targetCurBlockState.getBlock();

            // log.info("schematicBlockPos : {}", schematicBlockPos);
            // log.info("schematicBlockState : {}", schematicBlockState);
            // log.info("targetCurBlockState : {}", targetCurBlockState);

            boolean isNeedPlace = mc.player.getBlockPos().isWithinDistance(schematicBlockPos, printingRange.get())
                    && targetCurBlockState.isReplaceable()
                    && !schematicBlockState.isLiquid()
                    && !schematicBlockState.isAir()
                    && targetBlock != schematicBlockState.getBlock()
                    && DataManager.getRenderLayerRange().isPositionWithinRange(schematicBlockPos)
                    && !mc.player.getBoundingBox().intersects(Vec3d.of(schematicBlockPos), Vec3d.of(schematicBlockPos).add(1, 1, 1))
                    && schematicBlockState.canPlaceAt(mc.world, schematicBlockPos)
                    && !blacklist.get().contains(schematicBlockState.getBlock());

            for (Pair<Integer, BlockPos> posPair : renderPosList) {
                if (schematicBlockPos.equals(posPair.getRight())) {

                    isNeedPlace = false;
                    break;
                }
            }

            if (isNeedPlace) {

                // NOTE : BlockIterator 传过来的 pos 会变
                BlockPos blockPos = new BlockPos(schematicBlockPos.getX(), schematicBlockPos.getY(), schematicBlockPos.getZ());
                PlaceBlockHelper placeBlockHelper = new PlaceBlockHelper(debug.get(), blockPos, schematicBlockState, printingRange.get(), rotate.get());
                placeBlockHelper.setAirPlace(airPlace.get());

                // Direction visiblePlaceSide = BlockPlaceUtils.getVisiblePlaceSide(schematicBlockPos, schematicBlockState, printingRange.get());

                if (placeBlockHelper.getCanPlaceDirection() != null) {

                    printLog("BlockIterator add schematicBlockPos : {} {} {}", schematicBlockPos.getX(), schematicBlockPos.getY(), schematicBlockPos.getZ());

                    needPlaceBlockList.add(placeBlockHelper);
                }
            }
        }

        // info("size : " + needPlaceBlockList.size());

        if (!needPlaceBlockList.isEmpty()) {

            // 排序
            sortPlaceBlock();

            PlaceBlockHelper placeBlockHelper = needPlaceBlockList.getFirst();

            Item item = placeBlockHelper.getSchematicBlock().asItem();
            /*if (dirtgrass.get() && item == Items.GRASS_BLOCK) {
                item = Items.DIRT;
            }*/

            // 切换物品并放置
            boolean placeSuccess = switchItemAndPlace(item, placeBlockHelper);

            if (placeSuccess && renderBlocks.get()) {
                renderPosList.add(new Pair<>(fadeTime.get(), new BlockPos(placeBlockHelper.getSchematicBlockPos())));
            }

            // canMove = false;
            setDelay();
        }
    }

    private boolean switchItemAndPlace(Item item, PlaceBlockHelper placeBlockHelper) {

        int selectedSlot = getMainSlot();

        // 判断主手
        if (mc.player.getMainHandStack().getItem() == item) {

            lastUsedSlot = selectedSlot;

            return placeBlockHelper.tryPlace();
        }

        // 判断上一个放置的物品栏位
        if (lastUsedSlot != -1 && getItemStack(lastUsedSlot).getItem() == item) {
            InvUtils.swap(lastUsedSlot, returnHand.get());
            return false;
        }

        // 从背包里找
        FindItemResult result = InvUtils.find(item);
        if (!result.found()) {
            return false;
        }

        // 在热栏，切换一下
        if (result.isHotbar()) {
            lastUsedSlot = selectedSlot;
            InvUtils.swap(result.slot(), returnHand.get());
            return false;
        }

        // 判断是否在背包中，因为还有可能在副手或者盔甲位？
        if (result.isMain()) {

            // 找空的格子，范围是热栏+背包
            FindItemResult empty = InvUtils.findEmpty();

            if (empty.found() && empty.isHotbar()) {
                // 移动物品到热栏
                InvUtils.move().from(result.slot()).toHotbar(empty.slot());

                // TODO : 如果还会回弹，则注释这行代码
                // 切换主手位置
                InvUtils.swap(empty.slot(), returnHand.get());

            } else if (lastUsedSlot != -1) {
                // 移动物品到热栏
                InvUtils.move().from(result.slot()).toHotbar(lastUsedSlot);

                // TODO : 如果还会回弹，则注释这行代码
                // 切换主手位置
                InvUtils.swap(lastUsedSlot, returnHand.get());

            } else {
                warning("热栏需要留空");
            }
        }

        return false;
    }

    private void sortPlaceBlock() {
        if (firstAlgorithm.get() != SortAlgorithm.None) {
            if (firstAlgorithm.get().applySecondSorting) {
                if (secondAlgorithm.get() != SortingSecond.None) {
                    needPlaceBlockList.sort((o1, o2) -> secondAlgorithm.get().algorithm.compare(o1.getSchematicBlockPos(), o2.getSchematicBlockPos()));
                }
            }
            needPlaceBlockList.sort((o1, o2) -> firstAlgorithm.get().algorithm.compare(o1.getSchematicBlockPos(), o2.getSchematicBlockPos()));
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderPosList.forEach(s -> {
            Color a = new Color(colour.get().r, colour.get().g, colour.get().b, (int) (((float) s.getLeft() / (float) fadeTime.get()) * colour.get().a));
            event.renderer.box(s.getRight(), a, null, ShapeMode.Sides, 0);
        });
    }

    @Override
    public void onDeactivate() {
        // log.info("onDeactivate");
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
