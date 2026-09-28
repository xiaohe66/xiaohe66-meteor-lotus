package com.xiaohe66.mc.meteor.lotus.modules;

import com.xiaohe66.mc.meteor.lotus.bo.ItemBo;
import com.xiaohe66.mc.meteor.lotus.bo.StorageItem;
import com.xiaohe66.mc.meteor.lotus.bo.StoragePos;
import com.xiaohe66.mc.meteor.lotus.modules.clearup.ClearUpMapping;
import com.xiaohe66.mc.meteor.lotus.modules.step.Steps;
import com.xiaohe66.mc.meteor.lotus.util.HeBlockUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeInvUtils;
import com.xiaohe66.mc.meteor.lotus.util.HeItemUtils;
import com.xiaohe66.mc.meteor.lotus.util.ShulkerBoxReader;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.settings.ItemListSettingScreen;
import meteordevelopment.meteorclient.gui.screens.settings.ItemSettingScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.ItemSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author He
 * @since 2025.08.26 17:37
 */
public class AutoClearUp extends WalkModule {

    private final Setting<Integer> scanRange = sgGeneral.add(new IntSetting.Builder()
            .name("扫描范围")
            .description("检测展示框和箱子的范围")
            .defaultValue(50)
            .min(1)
            .sliderMax(100)
            .build());

    private final Setting<Integer> qty = sgGeneral.add(new IntSetting.Builder()
            .name("操作数")
            .description("每次操作的物品数量")
            .min(1)
            .sliderMax(9)
            .defaultValue(9)
            .build());

    private final Setting<Item> emptyKitItem = sgGeneral.add(new ItemSetting.Builder()
            .name("空盒标识")
            .description("空盒箱子展示框上的物品")
            .defaultValue(Items.WHITE_SHULKER_BOX)
            .build());

    private final Setting<Item> takeKitItem = sgGeneral.add(new ItemSetting.Builder()
            .name("待整理标识")
            .description("待整理箱子展示框上的物品")
            .defaultValue(Items.SHULKER_BOX)
            .build());

    private final Setting<Item> miscKitItem = sgGeneral.add(new ItemSetting.Builder()
            .name("无法整理标识")
            .description("无法整理箱子展示框上的物品")
            .defaultValue(Items.BLUE_SHULKER_BOX)
            .build());

    private final Setting<List<Item>> needItems = sgGeneral.add(new ItemListSetting.Builder()
            .name("提取整盒物品")
            .description("将这里的物品提取出来打包成盒")
            .defaultValue(
                    // 鞘翅
                    Items.ELYTRA,
                    // 附魔金苹果
                    Items.ENCHANTED_GOLDEN_APPLE,
                    // 金萝卜
                    Items.GOLDEN_CARROT,
                    // XP
                    Items.EXPERIENCE_BOTTLE,
                    // 黑曜石
                    Items.OBSIDIAN,
                    // 末影箱
                    Items.ENDER_CHEST,
                    // 珍珠
                    Items.ENDER_PEARL,
                    // 打火石
                    Items.FLINT_AND_STEEL,
                    // 水晶
                    Items.END_CRYSTAL,
                    // 重生锚
                    Items.RESPAWN_ANCHOR,
                    // 荧石
                    Items.GLOWSTONE,
                    // 图腾
                    Items.TOTEM_OF_UNDYING,
                    // 各种矿物块
                    Items.COAL_BLOCK,
                    Items.GOLD_BLOCK,
                    Items.IRON_BLOCK,
                    Items.REDSTONE_BLOCK,
                    Items.EMERALD_BLOCK,
                    Items.DIAMOND_BLOCK,
                    Items.LAPIS_BLOCK,
                    Items.COPPER_BLOCK,
                    Items.QUARTZ_BLOCK,
                    // 下界合金矿
                    Items.ANCIENT_DEBRIS,
                    Items.NETHERITE_SCRAP,
                    Items.NETHERITE_INGOT,
                    Items.NETHERITE_BLOCK,
                    // 下界合金装备
                    Items.NETHERITE_SWORD,
                    Items.NETHERITE_AXE,
                    Items.NETHERITE_HOE,
                    Items.NETHERITE_PICKAXE,
                    Items.NETHERITE_SHOVEL,
                    Items.NETHERITE_HELMET,
                    Items.NETHERITE_CHESTPLATE,
                    Items.NETHERITE_LEGGINGS,
                    Items.NETHERITE_BOOTS,
                    // 钻石装备
                    Items.DIAMOND_SWORD,
                    Items.DIAMOND_AXE,
                    Items.DIAMOND_HOE,
                    Items.DIAMOND_PICKAXE,
                    Items.DIAMOND_SHOVEL,
                    Items.DIAMOND_HELMET,
                    Items.DIAMOND_CHESTPLATE,
                    Items.DIAMOND_LEGGINGS,
                    Items.DIAMOND_BOOTS
            )
            .build());

    private final Setting<Boolean> initBtn = sgGeneral.add(new BoolSetting.Builder()
            .name("初始化")
            .description("使用前需要先初始化，保存所有箱子位置")
            .defaultValue(false)
            .onChanged(this::init)
            .build());

    private final Map<Item, ClearUpMapping> clearUpMappings = new HashMap<>();

    private final Map<Item, StoragePos> itemPosMap = new HashMap<>();
    private StoragePos emptyKitPos;
    private StoragePos takeKitPos;
    private StoragePos miscKitPos;
    private final Map<Item, StoragePos> needPosMap = new HashMap<>();
    private final Map<Item, StoragePos> multiItemPosMap = new HashMap<>();
    private final Map<Item, Item> relatedToTargetMap = new HashMap<>();

    private StoragePos curOperationPos;
    private int takeQty = 0;
    private int putKitIndex = -1;

    public AutoClearUp() {
        super("自动分类", "有价值物品打包、散装物品装箱, 需要在lotus简易仓库才可使用。由资深猎人<jiafog199>友情赞助开发");

        addStep(Steps.NEXT, this::nextStep);
        addStep(Steps.PUT_KIT, this::putKit);
        addStep(Steps.PUT_ITEM, this::putItem);
        addStep(Steps.TAKE_ITEM, this::takeItem);
        addStep(Steps.PLACE_EMPTY_KIT, this::placeEmptyKit);
        addStep(Steps.TAKE_EMPTY_KIT, this::takeEmptyKit);
        addStep(Steps.PLACE_KIT, this::placeKit);
        addStep(Steps.TAKE_KIT, this::takeKit);
        addStep(Steps.TAKE_LOOSE_ITEM, this::takeLooseItem);
        addStep(Steps.BREAK_KIT, this::breakKit);

        initDefaultMappings();
    }

    private void initDefaultMappings() {
        // 1. 红石粉 -> 红石类
        clearUpMappings.put(Items.REDSTONE, new ClearUpMapping(Items.REDSTONE, List.of(
                Items.REDSTONE, Items.REDSTONE_BLOCK, Items.REPEATER, Items.COMPARATOR,
                Items.PISTON, Items.STICKY_PISTON, Items.DISPENSER, Items.DROPPER,
                Items.HOPPER, Items.OBSERVER, Items.REDSTONE_TORCH, Items.REDSTONE_LAMP,
                Items.NOTE_BLOCK, Items.TARGET, Items.LEVER, Items.TRIPWIRE_HOOK,
                Items.DAYLIGHT_DETECTOR, Items.LECTERN, Items.CALIBRATED_SCULK_SENSOR,
                Items.SCULK_SENSOR
        ), true));

        // 2. 白桦木板 -> 所有木头制品
        List<Item> woodItems = new ArrayList<>(List.of(
                Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
                Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS,
                Items.BAMBOO_PLANKS, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS,
                Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
                Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CHERRY_LOG,
                Items.BAMBOO_BLOCK, Items.CRIMSON_STEM, Items.WARPED_STEM,
                Items.STRIPPED_OAK_LOG, Items.STRIPPED_SPRUCE_LOG, Items.STRIPPED_BIRCH_LOG,
                Items.STRIPPED_JUNGLE_LOG, Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_DARK_OAK_LOG,
                Items.STRIPPED_MANGROVE_LOG, Items.STRIPPED_CHERRY_LOG, Items.STRIPPED_BAMBOO_BLOCK,
                Items.STRIPPED_CRIMSON_STEM, Items.STRIPPED_WARPED_STEM,
                Items.OAK_WOOD, Items.SPRUCE_WOOD, Items.BIRCH_WOOD, Items.JUNGLE_WOOD,
                Items.ACACIA_WOOD, Items.DARK_OAK_WOOD, Items.MANGROVE_WOOD, Items.CHERRY_WOOD,
                Items.CRIMSON_HYPHAE, Items.WARPED_HYPHAE,
                Items.STRIPPED_OAK_WOOD, Items.STRIPPED_SPRUCE_WOOD, Items.STRIPPED_BIRCH_WOOD,
                Items.STRIPPED_JUNGLE_WOOD, Items.STRIPPED_ACACIA_WOOD, Items.STRIPPED_DARK_OAK_WOOD,
                Items.STRIPPED_MANGROVE_WOOD, Items.STRIPPED_CHERRY_WOOD,
                Items.STRIPPED_CRIMSON_HYPHAE, Items.STRIPPED_WARPED_HYPHAE,
                Items.OAK_SLAB, Items.SPRUCE_SLAB, Items.BIRCH_SLAB, Items.JUNGLE_SLAB,
                Items.ACACIA_SLAB, Items.DARK_OAK_SLAB, Items.MANGROVE_SLAB, Items.CHERRY_SLAB,
                Items.BAMBOO_SLAB, Items.CRIMSON_SLAB, Items.WARPED_SLAB,
                Items.OAK_STAIRS, Items.SPRUCE_STAIRS, Items.BIRCH_STAIRS, Items.JUNGLE_STAIRS,
                Items.ACACIA_STAIRS, Items.DARK_OAK_STAIRS, Items.MANGROVE_STAIRS, Items.CHERRY_STAIRS,
                Items.BAMBOO_STAIRS, Items.CRIMSON_STAIRS, Items.WARPED_STAIRS,
                Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE, Items.JUNGLE_FENCE,
                Items.ACACIA_FENCE, Items.DARK_OAK_FENCE, Items.MANGROVE_FENCE, Items.CHERRY_FENCE,
                Items.BAMBOO_FENCE, Items.CRIMSON_FENCE, Items.WARPED_FENCE,
                Items.OAK_FENCE_GATE, Items.SPRUCE_FENCE_GATE, Items.BIRCH_FENCE_GATE, Items.JUNGLE_FENCE_GATE,
                Items.ACACIA_FENCE_GATE, Items.DARK_OAK_FENCE_GATE, Items.MANGROVE_FENCE_GATE, Items.CHERRY_FENCE_GATE,
                Items.BAMBOO_FENCE_GATE, Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE,
                Items.OAK_DOOR, Items.SPRUCE_DOOR, Items.BIRCH_DOOR, Items.JUNGLE_DOOR,
                Items.ACACIA_DOOR, Items.DARK_OAK_DOOR, Items.MANGROVE_DOOR, Items.CHERRY_DOOR,
                Items.BAMBOO_DOOR, Items.CRIMSON_DOOR, Items.WARPED_DOOR,
                Items.OAK_TRAPDOOR, Items.SPRUCE_TRAPDOOR, Items.BIRCH_TRAPDOOR, Items.JUNGLE_TRAPDOOR,
                Items.ACACIA_TRAPDOOR, Items.DARK_OAK_TRAPDOOR, Items.MANGROVE_TRAPDOOR, Items.CHERRY_TRAPDOOR,
                Items.BAMBOO_TRAPDOOR, Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR,
                Items.OAK_PRESSURE_PLATE, Items.SPRUCE_PRESSURE_PLATE, Items.BIRCH_PRESSURE_PLATE,
                Items.JUNGLE_PRESSURE_PLATE, Items.ACACIA_PRESSURE_PLATE, Items.DARK_OAK_PRESSURE_PLATE,
                Items.MANGROVE_PRESSURE_PLATE, Items.CHERRY_PRESSURE_PLATE, Items.BAMBOO_PRESSURE_PLATE,
                Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE,
                Items.OAK_BUTTON, Items.SPRUCE_BUTTON, Items.BIRCH_BUTTON, Items.JUNGLE_BUTTON,
                Items.ACACIA_BUTTON, Items.DARK_OAK_BUTTON, Items.MANGROVE_BUTTON, Items.CHERRY_BUTTON,
                Items.BAMBOO_BUTTON, Items.CRIMSON_BUTTON, Items.WARPED_BUTTON,
                Items.STICK, Items.CRAFTING_TABLE, Items.CHEST, Items.BARREL, Items.LECTERN,
                Items.COMPOSTER, Items.FLETCHING_TABLE, Items.LOOM, Items.SMITHING_TABLE,
                Items.CARTOGRAPHY_TABLE, Items.BOOKSHELF, Items.BOWL,
                Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE, Items.WOODEN_SWORD,
                Items.OAK_SIGN, Items.SPRUCE_SIGN, Items.BIRCH_SIGN, Items.JUNGLE_SIGN,
                Items.ACACIA_SIGN, Items.DARK_OAK_SIGN, Items.MANGROVE_SIGN, Items.CHERRY_SIGN,
                Items.BAMBOO_SIGN, Items.CRIMSON_SIGN, Items.WARPED_SIGN,
                Items.OAK_HANGING_SIGN, Items.SPRUCE_HANGING_SIGN, Items.BIRCH_HANGING_SIGN,
                Items.JUNGLE_HANGING_SIGN, Items.ACACIA_HANGING_SIGN, Items.DARK_OAK_HANGING_SIGN,
                Items.MANGROVE_HANGING_SIGN, Items.CHERRY_HANGING_SIGN, Items.BAMBOO_HANGING_SIGN,
                Items.CRIMSON_HANGING_SIGN, Items.WARPED_HANGING_SIGN,
                Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT,
                Items.ACACIA_BOAT, Items.DARK_OAK_BOAT, Items.MANGROVE_BOAT, Items.CHERRY_BOAT,
                Items.BAMBOO_RAFT, Items.OAK_CHEST_BOAT, Items.SPRUCE_CHEST_BOAT, Items.BIRCH_CHEST_BOAT,
                Items.JUNGLE_CHEST_BOAT, Items.ACACIA_CHEST_BOAT, Items.DARK_OAK_CHEST_BOAT,
                Items.MANGROVE_CHEST_BOAT, Items.CHERRY_CHEST_BOAT, Items.BAMBOO_CHEST_RAFT
        ));
        clearUpMappings.put(Items.BIRCH_PLANKS, new ClearUpMapping(Items.BIRCH_PLANKS, woodItems, true));

        // 3. 铁锭 -> 所有矿物
        clearUpMappings.put(Items.IRON_INGOT, new ClearUpMapping(Items.IRON_INGOT, List.of(
                Items.RAW_IRON, Items.IRON_NUGGET, Items.IRON_INGOT, Items.IRON_BLOCK, Items.RAW_IRON_BLOCK,
                Items.RAW_COPPER, Items.COPPER_INGOT, Items.COPPER_BLOCK, Items.RAW_COPPER_BLOCK,
                Items.RAW_GOLD, Items.GOLD_NUGGET, Items.GOLD_INGOT, Items.GOLD_BLOCK, Items.RAW_GOLD_BLOCK,
                Items.COAL, Items.CHARCOAL, Items.COAL_BLOCK,
                Items.REDSTONE, Items.REDSTONE_BLOCK,
                Items.LAPIS_LAZULI, Items.LAPIS_BLOCK,
                Items.DIAMOND, Items.DIAMOND_BLOCK,
                Items.EMERALD, Items.EMERALD_BLOCK,
                Items.QUARTZ, Items.QUARTZ_BLOCK,
                Items.AMETHYST_SHARD, Items.AMETHYST_BLOCK,
                Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK
        ), true));

        // 4. 小麦种子 -> 所有植物类型
        clearUpMappings.put(Items.WHEAT_SEEDS, new ClearUpMapping(Items.WHEAT_SEEDS, List.of(
                Items.WHEAT, Items.WHEAT_SEEDS,
                Items.CARROT, Items.POTATO, Items.BEETROOT, Items.BEETROOT_SEEDS,
                Items.PUMPKIN_SEEDS, Items.MELON_SEEDS, Items.TORCHFLOWER_SEEDS,
                Items.PITCHER_PLANT, Items.PITCHER_POD,
                Items.BAMBOO, Items.SUGAR_CANE, Items.CACTUS, Items.VINE,
                Items.LILY_PAD, Items.KELP, Items.SEAGRASS, Items.SEA_PICKLE,
                Items.CORNFLOWER, Items.POPPY, Items.DANDELION, Items.BLUE_ORCHID,
                Items.ALLIUM, Items.AZURE_BLUET, Items.OXEYE_DAISY,
                Items.LILY_OF_THE_VALLEY, Items.SUNFLOWER, Items.LILAC,
                Items.ROSE_BUSH, Items.PEONY, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN,
                Items.DEAD_BUSH, Items.GLOW_LICHEN, Items.HANGING_ROOTS,
                Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
                Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING,
                Items.MANGROVE_PROPAGULE, Items.CHERRY_SAPLING, Items.AZALEA,
                Items.FLOWERING_AZALEA, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM,
                Items.CRIMSON_FUNGUS, Items.WARPED_FUNGUS
        ), true));

        // 5. 圆石 -> 所有圆石、石头类型
        clearUpMappings.put(Items.COBBLESTONE, new ClearUpMapping(Items.COBBLESTONE, List.of(
                Items.COBBLESTONE, Items.MOSSY_COBBLESTONE, Items.COBBLESTONE_SLAB, Items.COBBLESTONE_STAIRS, Items.COBBLESTONE_WALL,
                Items.STONE, Items.SMOOTH_STONE, Items.STONE_SLAB, Items.SMOOTH_STONE_SLAB,
                Items.STONE_BRICKS, Items.MOSSY_STONE_BRICKS, Items.CRACKED_STONE_BRICKS, Items.CHISELED_STONE_BRICKS,
                Items.STONE_BRICK_SLAB, Items.STONE_BRICK_STAIRS, Items.STONE_BRICK_WALL,
                Items.ANDESITE, Items.POLISHED_ANDESITE, Items.ANDESITE_SLAB, Items.ANDESITE_STAIRS, Items.ANDESITE_WALL,
                Items.DIORITE, Items.POLISHED_DIORITE, Items.DIORITE_SLAB, Items.DIORITE_STAIRS, Items.DIORITE_WALL,
                Items.GRANITE, Items.POLISHED_GRANITE, Items.GRANITE_SLAB, Items.GRANITE_STAIRS, Items.GRANITE_WALL,
                Items.COBBLED_DEEPSLATE, Items.COBBLED_DEEPSLATE_SLAB, Items.COBBLED_DEEPSLATE_STAIRS, Items.COBBLED_DEEPSLATE_WALL,
                Items.DEEPSLATE, Items.POLISHED_DEEPSLATE, Items.DEEPSLATE_BRICKS, Items.CRACKED_DEEPSLATE_BRICKS,
                Items.DEEPSLATE_TILES, Items.CRACKED_DEEPSLATE_TILES, Items.CHISELED_DEEPSLATE,
                Items.REINFORCED_DEEPSLATE,
                Items.TUFF, Items.POLISHED_TUFF, Items.TUFF_BRICKS, Items.CHISELED_TUFF,
                Items.SMOOTH_BASALT, Items.BASALT, Items.POLISHED_BASALT,
                Items.BLACKSTONE, Items.POLISHED_BLACKSTONE, Items.BLACKSTONE_SLAB, Items.BLACKSTONE_STAIRS, Items.BLACKSTONE_WALL,
                Items.POLISHED_BLACKSTONE_BRICKS, Items.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                Items.END_STONE, Items.END_STONE_BRICKS,
                Items.SANDSTONE, Items.RED_SANDSTONE,
                Items.CHISELED_SANDSTONE, Items.CUT_SANDSTONE, Items.SMOOTH_SANDSTONE,
                Items.CHISELED_RED_SANDSTONE, Items.CUT_RED_SANDSTONE, Items.SMOOTH_RED_SANDSTONE
        ), true));

        // 6. 玻璃 -> 所有玻璃，沙子
        clearUpMappings.put(Items.GLASS, new ClearUpMapping(Items.GLASS, List.of(
                Items.GLASS, Items.TINTED_GLASS,
                Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.MAGENTA_STAINED_GLASS,
                Items.LIGHT_BLUE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS,
                Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS, Items.LIGHT_GRAY_STAINED_GLASS,
                Items.CYAN_STAINED_GLASS, Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
                Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS, Items.RED_STAINED_GLASS,
                Items.BLACK_STAINED_GLASS,
                Items.GLASS_PANE, Items.WHITE_STAINED_GLASS_PANE, Items.ORANGE_STAINED_GLASS_PANE,
                Items.MAGENTA_STAINED_GLASS_PANE, Items.LIGHT_BLUE_STAINED_GLASS_PANE,
                Items.YELLOW_STAINED_GLASS_PANE, Items.LIME_STAINED_GLASS_PANE,
                Items.PINK_STAINED_GLASS_PANE, Items.GRAY_STAINED_GLASS_PANE,
                Items.LIGHT_GRAY_STAINED_GLASS_PANE, Items.CYAN_STAINED_GLASS_PANE,
                Items.PURPLE_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE,
                Items.BROWN_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE,
                Items.RED_STAINED_GLASS_PANE, Items.BLACK_STAINED_GLASS_PANE,
                Items.SAND, Items.RED_SAND
        ), true));

        // 7. 火把 -> 所有照明方块
        clearUpMappings.put(Items.TORCH, new ClearUpMapping(Items.TORCH, List.of(
                Items.TORCH, Items.SOUL_TORCH, Items.REDSTONE_TORCH,
                Items.LANTERN, Items.SOUL_LANTERN,
                Items.GLOWSTONE, Items.SEA_LANTERN, Items.JACK_O_LANTERN,
                Items.CAMPFIRE, Items.SOUL_CAMPFIRE,
                Items.CANDLE, Items.WHITE_CANDLE, Items.ORANGE_CANDLE, Items.MAGENTA_CANDLE,
                Items.LIGHT_BLUE_CANDLE, Items.YELLOW_CANDLE, Items.LIME_CANDLE,
                Items.PINK_CANDLE, Items.GRAY_CANDLE, Items.LIGHT_GRAY_CANDLE,
                Items.CYAN_CANDLE, Items.PURPLE_CANDLE, Items.BLUE_CANDLE,
                Items.BROWN_CANDLE, Items.GREEN_CANDLE, Items.RED_CANDLE, Items.BLACK_CANDLE,
                Items.END_ROD, Items.CRYING_OBSIDIAN,
                Items.OCHRE_FROGLIGHT, Items.VERDANT_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT,
                Items.GLOW_LICHEN, Items.SHROOMLIGHT
        ), true));

        // 8. 骨头 -> 所有怪物掉落物
        clearUpMappings.put(Items.BONE, new ClearUpMapping(Items.BONE, List.of(
                Items.BONE, Items.BONE_MEAL,
                Items.ROTTEN_FLESH, Items.GUNPOWDER, Items.STRING, Items.SPIDER_EYE,
                Items.ENDER_PEARL, Items.BLAZE_ROD, Items.GHAST_TEAR,
                Items.SLIME_BALL, Items.MAGMA_CREAM, Items.LEATHER,
                Items.FEATHER, Items.ARROW, Items.SPECTRAL_ARROW, Items.TIPPED_ARROW,
                Items.INK_SAC, Items.GLOW_INK_SAC,
                Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS,
                Items.NETHER_STAR, Items.WITHER_SKELETON_SKULL,
                Items.SHULKER_SHELL, Items.PHANTOM_MEMBRANE,
                Items.RABBIT_HIDE, Items.RABBIT_FOOT, Items.TURTLE_SCUTE,
                Items.ARMADILLO_SCUTE, Items.WOLF_ARMOR, Items.HEAVY_CORE,
                Items.ENDER_EYE, Items.FERMENTED_SPIDER_EYE,
                Items.BREEZE_ROD, Items.WIND_CHARGE
        ), true));
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = super.toTag();
        NbtList list = new NbtList();
        for (ClearUpMapping mapping : clearUpMappings.values()) {
            list.add(mapping.toTag());
        }
        tag.put("clearUpMappings", list);
        return tag;
    }

    @Override
    public Module fromTag(NbtCompound tag) {
        super.fromTag(tag);
        if (tag.contains("clearUpMappings")) {
            NbtList list = tag.getListOrEmpty("clearUpMappings");
            clearUpMappings.clear();
            for (NbtElement e : list) {
                if (e.getType() != NbtElement.COMPOUND_TYPE) continue;
                ClearUpMapping mapping = new ClearUpMapping().fromTag((NbtCompound) e);
                clearUpMappings.put(mapping.getTargetItem(), mapping);
            }
        }
        return this;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        fillWidget(theme, list);
        return list;
    }

    private void fillWidget(GuiTheme theme, WVerticalList list) {
        WSection section = list.add(theme.section("散装物品分类 (" + clearUpMappings.size() + ")")).expandX().widget();

        WTable table = section.add(theme.table()).expandX().widget();
        // table.add(theme.label(""));
        // table.add(theme.label("目标物品"));
        // table.add(theme.label("关联物品"));
        // table.add(theme.label("启用"));
        // table.add(theme.label("删除"));
        // table.row();

        List<ClearUpMapping> mappings = new ArrayList<>(clearUpMappings.values());
        for (ClearUpMapping mapping : mappings) {
            final Item oldTarget = mapping.getTargetItem();

            ItemStack targetStack = mapping.getTargetItem().getDefaultStack();
            table.add(theme.item(targetStack));

            WButton changeTarget = table.add(theme.button(Names.get(mapping.getTargetItem()))).expandX().widget();
            changeTarget.action = () -> {
                ItemSetting tempSetting = new ItemSetting.Builder()
                        .name("target-item")
                        .description("")
                        .defaultValue(mapping.getTargetItem())
                        .onChanged(item -> {
                            clearUpMappings.remove(oldTarget);
                            mapping.setTargetItem(item);
                            clearUpMappings.put(item, mapping);
                            list.clear();
                            fillWidget(theme, list);
                        })
                        .build();
                mc.setScreen(new ItemSettingScreen(theme, tempSetting));
            };

            String relatedText = mapping.getRelatedItems().isEmpty() ? "空" : mapping.getRelatedItems().size() + " 个物品";
            WButton editRelated = table.add(theme.button(relatedText)).expandX().widget();
            editRelated.action = () -> {
                ItemListSetting tempSetting = new ItemListSetting.Builder()
                        .name("related-items")
                        .description("")
                        .defaultValue(mapping.getRelatedItems().toArray(new Item[0]))
                        .onChanged(items -> {
                            mapping.setRelatedItems(new HashSet<>(items));
                        })
                        .build();
                ItemListSettingScreen screen = new ItemListSettingScreen(theme, tempSetting);
                screen.onClosed(() -> {
                    list.clear();
                    fillWidget(theme, list);
                });
                mc.setScreen(screen);
            };

            WCheckbox enabled = table.add(theme.checkbox(mapping.isEnabled())).widget();
            enabled.action = () -> {
                mapping.setEnabled(enabled.checked);
            };

            WMinus del = table.add(theme.minus()).widget();
            del.action = () -> {
                clearUpMappings.remove(mapping.getTargetItem());
                list.clear();
                fillWidget(theme, list);
            };

            table.row();
        }

        WTable controls = list.add(theme.table()).expandX().widget();

        WButton add = controls.add(theme.button("新增分类")).expandX().widget();
        add.action = () -> {
            clearUpMappings.put(Items.AIR, new ClearUpMapping());
            list.clear();
            fillWidget(theme, list);
        };

        WButton resetDefaults = controls.add(theme.button("恢复默认")).expandX().widget();
        resetDefaults.action = () -> {
            clearUpMappings.clear();
            initDefaultMappings();
            list.clear();
            fillWidget(theme, list);
        };

        controls.row();
    }

    /**
     * 初始化方法 - 扫描展示框保存位置
     */
    private void init(Boolean enabled) {
        if (!Boolean.TRUE.equals(enabled)) {
            return;
        }

        // 重置按钮状态
        initBtn.set(false);

        if (mc.player == null || mc.world == null) {
            warning("玩家或世界未加载");
            return;
        }

        clear();

        // 扫描展示框识别所有存储位置
        boolean scanPositionsSuccess = scanPositions();
        if (!scanPositionsSuccess) {
            clear();
            return;
        }

        // 需要的物品位置
        for (Item item : needItems.get()) {
            if (itemPosMap.containsKey(item)) {
                needPosMap.put(item, itemPosMap.get(item));
            }
        }

        // 多物品分类位置
        for (ClearUpMapping mapping : clearUpMappings.values()) {
            if (!mapping.isEnabled()) continue;
            Item targetItem = mapping.getTargetItem();
            if (itemPosMap.containsKey(targetItem)) {
                multiItemPosMap.put(targetItem, itemPosMap.get(targetItem));
                for (Item relatedItem : mapping.getRelatedItems()) {
                    relatedToTargetMap.put(relatedItem, targetItem);
                }
            }
        }

        if (needPosMap.isEmpty() && multiItemPosMap.isEmpty()) {
            warning("未检测到需要的<物品>位置>");
            clear();
            return;
        }

        // 输出检测到的物品
        String itemNames = needPosMap.keySet().stream()
                .map(Names::get)
                .collect(Collectors.joining(","));

        if (!itemNames.isEmpty()) {
            info("成功识别打包物品: " + itemNames);
        }

        String multiItemNames = multiItemPosMap.keySet().stream()
                .map(Names::get)
                .collect(Collectors.joining(","));

        if (!multiItemNames.isEmpty()) {
            info("成功识别多物品分类: " + multiItemNames);
        }

        info("初始化完毕！");
    }

    @Override
    public void onActivate() {
        super.onActivate();

        // 检查是否已初始化
        if (takeKitPos == null || emptyKitPos == null || (needPosMap.isEmpty() && multiItemPosMap.isEmpty())) {
            warning("请先进行初始化");
            toggle();
            return;
        }

        nextStep();
    }

    private void nextStep() {
        ItemStack nextKit = nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
        if (!nextKit.isEmpty()) {
            putKitIndex = getCurPlayerSlot();
            ShulkerBoxReader reader = new ShulkerBoxReader(nextKit);
            if (reader.isEmpty()) {
                // 记录拿空盒放置的时候，不要走 next 方法, 否则会在这里循环
                curOperationPos = emptyKitPos;
                gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放空盒");

            } else {
                // 单一物品放到对应箱子中
                if (reader.isSameItem()) {
                    Item firstItem = reader.getFirstItem();
                    if (itemPosMap.containsKey(firstItem)) {
                        curOperationPos = itemPosMap.get(firstItem);
                        gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
                        return;
                    }
                }
                List<ItemStack> condensed = reader.getCondensed();
                boolean need = false;
                for (ItemStack itemStack : condensed) {
                    Item item = itemStack.getItem();
                    if (needPosMap.containsKey(item) || relatedToTargetMap.containsKey(item)) {
                        need = true;
                        break;
                    }
                }
                if (need) {
                    curOperationPos = takeKitPos;
                    gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放满盒");
                    return;
                }
                curOperationPos = miscKitPos;
                gotoTargetIfNeed(curOperationPos.getBtnPos(), 0, Steps.PUT_KIT, "存放杂盒");
            }
            return;
        }

        // 检查地上的
        List<ItemEntity> shulkerEntities = mc.world.getEntitiesByClass(
                ItemEntity.class,
                mc.player.getBoundingBox().expand(5),
                e -> HeItemUtils.isShulkerBox(e.getStack().getItem()));

        if (!shulkerEntities.isEmpty()) {
            // 捡kit
            BlockPos targetPos = shulkerEntities.getFirst().getBlockPos();
            if (targetPos.getY() != mc.player.getBlockY()) {
                BlockPos testPos = new BlockPos(targetPos.getX(), mc.player.getBlockY(), targetPos.getZ());
                BlockPos canStandPos = HeBlockUtils.getCanStandPos(testPos, 1);
                if (canStandPos != null) {
                    targetPos = canStandPos;
                }
            }
            gotoTargetIfNeed(targetPos, 0, Steps.NEXT, "捡kit");
            return;
        }

        step = Steps.PUT_ITEM;
    }

    private void putKit() {
        if (notInOperationRange(curOperationPos)) {
            gotoBtnPos(curOperationPos, "<存kit>距离不够，尝试移动", Steps.PUT_KIT);
            return;
        }

        // 找到背包中的潜影盒
        ItemStack kitItemStack = getItemStack(putKitIndex);
        if (!HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
            // 是放kit的状态, 但是身上却没有kit
            warning("放kit, 但身上没有...");
            step = Steps.NEXT;
            return;
        }

        openChest(curOperationPos.getPutPos(), inventory -> {
            // TODO : 这个是找箱子的, 还是找玩家背包的？
            FindItemResult emptyResult = InvUtils.findEmpty();
            if (emptyResult.found()) {
                // 放入
                InvUtils.shiftClick().slot(putKitIndex);
                delayCloseNext(Steps.NEXT);

            } else {
                // 满了, 中断操作
                breakStep("<存kit>箱满，无法存放");
            }
        });
    }

    private void takeEmptyKit() {
        // 检查距离
        if (notInOperationRange(emptyKitPos)) {
            gotoBtnPos(emptyKitPos, "<拿空盒>距离不够, 尝试移动", Steps.TAKE_EMPTY_KIT);
            return;
        }

        openChest(emptyKitPos.getTakePos(), inventory -> {

            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack kitItemStack = inventory.getStack(slot);
                if (!HeItemUtils.isShulkerBox(kitItemStack.getItem())) {
                    continue;
                }

                ShulkerBoxReader reader = new ShulkerBoxReader(kitItemStack);
                if (reader.isEmpty()) {
                    // 拿
                    InvUtils.shiftClick().slotId(slot);

                    // 去放kit
                    delayCloseNext(Steps.PLACE_EMPTY_KIT);
                    return;
                }
            }

            info("<空盒箱>缺少盒子");
            toggle();
        });
    }

    private void placeEmptyKit() {
        BlockState emptyKitState = mc.world.getBlockState(curOperationPos.getKitPos());

        // 1. 判断是否存在kit, 若存在则 next
        if (emptyKitState.getBlock() instanceof ShulkerBoxBlock) {
            // kit已存在
            step = Steps.PUT_ITEM;
            return;
        }

        // 检查位置是否被其他方块占用
        if (!emptyKitState.isAir()) {
            breakStep("<位置>被占用: " + Names.get(curOperationPos.getItem().getItem()));
            return;
        }

        // 2. 判断身上是否存在空kit, 若不存在则去拿空kit
        FindItemResult findItemResult = HeInvUtils.findShulkerBox(Items.AIR);
        if (!findItemResult.found()) {
            // 身上没有空盒，去拿空盒
            step = Steps.TAKE_EMPTY_KIT;
            return;
        }

        // 空盒需要在快捷栏上才能放置
        if (!findItemResult.isMainHand()) {
            // 将空盒切换到主手
            HeInvUtils.swap(findItemResult.slot(), getMainSlot());
            setDelay();
            return;
        }

        // 3. 判断距离放空kit是否够, 不够需要移动过去
        if (notInOperationRange(curOperationPos)) {
            gotoTarget(curOperationPos.getBtnPos(), 0, Steps.PLACE_EMPTY_KIT);
            return;
        }

        // 4. 放置空kit, 设置next
        info("放置空盒");
        HeBlockUtils.place(curOperationPos.getKitPos(), getMainSlot(), true, Direction.DOWN);

        // 重置放置索引，准备开始装配
        // putItemIndex = 0;
        // currentTakeIndex = 0;
        // setCurrent(0);
        delayNext(Steps.PUT_ITEM);
    }

    /**
     * 就是拿待分类的kit
     */
    private void takeKit() {
        if (notInOperationRange(takeKitPos)) {
            gotoBtnPos(takeKitPos, "<拿kit>距离不够，尝试移动", Steps.TAKE_KIT);
            return;
        }

        openChest(takeKitPos.getTakePos(), inventory -> {
            ItemStack nextScreenStack = nextScreenStack(itemStack -> {
                if (HeItemUtils.isShulkerBox(itemStack.getItem())) {
                    // 读取盒子内容
                    ShulkerBoxReader reader = new ShulkerBoxReader(itemStack);
                    List<ItemStack> condensed = reader.getCondensed();
                    for (ItemStack stack : condensed) {
                        Item item = stack.getItem();
                        if (needPosMap.containsKey(item) || relatedToTargetMap.containsKey(item)) {
                            return true;
                        }
                    }
                }
                return false;
            });

            if (nextScreenStack.isEmpty()) {
                breakStep("分类完毕...");
                return;
            }

            info("拿取kit");
            InvUtils.shiftClick().slotId(getCurScreenSlot());

            delayCloseNext(Steps.PLACE_KIT);
        });
    }

    /**
     * 放整盒
     */
    private void placeKit() {
        BlockState finishedKitBlockState = mc.world.getBlockState(takeKitPos.getKitPos());
        if (!finishedKitBlockState.isAir()) {
            // 检查是否是潜影盒
            if (finishedKitBlockState.getBlock() instanceof ShulkerBoxBlock) {
                step = Steps.TAKE_ITEM;

            } else {
                breakStep("<待分类位置>被占用");
            }
            return;
        }
        // 检查身上是否存在kit
        ItemStack nextItemStack = nextPlayerStack(itemStack -> HeItemUtils.isShulkerBox(itemStack.getItem()));
        if (nextItemStack.isEmpty()) {
            // 身上没有, 去拿kit
            step = Steps.TAKE_KIT;
            return;
        }

        // 判断距离是否够, 不够则需要移动过去
        if (notInOperationRange(takeKitPos)) {
            gotoBtnPos(takeKitPos, "去拿kit", Steps.PLACE_KIT);
            return;
        }

        // 切主手
        if (swapToMainHand(getCurPlayerSlot())) {
            return;
        }

        HeBlockUtils.place(takeKitPos.getKitPos(), getMainSlot(), true, Direction.DOWN);

        delayNext(Steps.TAKE_ITEM);
    }

    /**
     * 从待整理取出箱子中拿取散装物品（包括打包物品和多物品分类）
     */
    private void takeLooseItem() {
        if (takeQty >= qty.get()) {
            takeQty = 0;
            step = Steps.PUT_ITEM;
            return;
        }

        if (notInOperationRange(takeKitPos)) {
            gotoTarget(takeKitPos.getBtnPos(), 0, Steps.TAKE_LOOSE_ITEM);
            return;
        }

        openChest(takeKitPos.getTakePos(), inventory -> {
            ItemStack nextLooseItem = nextScreenStack(itemStack -> {
                Item item = itemStack.getItem();
                return needPosMap.containsKey(item) || relatedToTargetMap.containsKey(item);
            });

            if (!nextLooseItem.isEmpty()) {
                info("拿取散装物品: " + Names.get(nextLooseItem));
                InvUtils.shiftClick().slotId(getCurScreenSlot());
                takeQty++;
                if (takeQty >= qty.get()) {
                    delayCloseNext(Steps.PUT_ITEM);
                } else {
                    setDelay();
                }
            } else {
                delayCloseNext(Steps.TAKE_ITEM);
            }
        });
    }

    private void takeItem() {
        // 检查是否需要停止拿取（已达到操作数限制）
        if (takeQty >= qty.get()) {
            step = Steps.PUT_ITEM;
            return;
        }

        // TODO : 检查身上空格？

        // 检查是否存在kit
        BlockState takeKitPosState = mc.world.getBlockState(this.takeKitPos.getKitPos());
        if (takeKitPosState.isAir()) {
            // 不存在, 需要放盒子
            step = Steps.PLACE_KIT;
            return;
        }

        // 检查是否是潜影盒
        if (!(takeKitPosState.getBlock() instanceof ShulkerBoxBlock)) {
            breakStep("<kit位置>被占用");
            return;
        }

        // 判断距离是否够, 不够则需要移动过去
        if (notInOperationRange(this.takeKitPos)) {
            gotoTarget(this.takeKitPos.getBtnPos(), 0, Steps.TAKE_ITEM);
            return;
        }

        openKit(this.takeKitPos.getKitPos(), screenHandler -> {
            ItemStack nextItemStack = nextScreenStack(itemStack -> {
                Item item = itemStack.getItem();
                return needPosMap.containsKey(item) || relatedToTargetMap.containsKey(item);
            });
            if (nextItemStack.isEmpty()) {
                // 盒子里没有了，需要挖掉并重新拿取
                info("待分类kit已空，挖掉重放");
                curOperationPos = takeKitPos;
                delayCloseNext(Steps.BREAK_KIT);
                return;
            }

            info("拿取物品: " + Names.get(nextItemStack));
            InvUtils.shiftClick().slotId(getCurScreenSlot());

            takeQty++;

            // 模板的物品都拿完了, 去放物品
            if (takeQty >= qty.get()) {
                delayCloseNext(Steps.PUT_ITEM);

            } else {
                // 下一个物品
                setDelay();
            }
        });
    }

    /**
     * 将物品放入盒子里或直接放入箱子
     * 1. 优先处理打包物品放入潜影盒
     * 2. 再处理多物品分类直接放入对应箱子
     * 3. 身上没有物品时，去拿新的（先拿散装再拿盒装）
     */
    private void putItem() {
        // 1. 优先处理打包物品（放入潜影盒）
        Set<Item> needItemSet = new HashSet<>();
        for (int i = 0; i < 36; i++) {
            Item item = getItemStack(i).getItem();
            if (needPosMap.containsKey(item)) {
                needItemSet.add(item);
            }
        }

        if (!needItemSet.isEmpty()) {
            putKitItem(needItemSet);
            return;
        }

        // 2. 处理多物品分类（直接放入箱子）
        Set<Item> multiItemSet = new HashSet<>();
        for (int i = 0; i < 36; i++) {
            Item item = getItemStack(i).getItem();
            if (relatedToTargetMap.containsKey(item)) {
                multiItemSet.add(item);
            }
        }

        if (!multiItemSet.isEmpty()) {
            putMultiItem(multiItemSet);
            return;
        }

        // 身上没有物品了, 去拿新的
        takeQty = 0;
        delayCloseNext(multiItemPosMap.isEmpty() ? Steps.TAKE_ITEM : Steps.TAKE_LOOSE_ITEM);
    }

    /**
     * 将打包物品放入潜影盒
     */
    private void putKitItem(Set<Item> needItemSet) {
        // 找出最近的
        Vec3d playerPos = mc.player.getEntityPos();
        double minDistance = Double.MAX_VALUE;
        Item putItem = Items.AIR;
        for (Item item : needItemSet) {
            StoragePos storagePos = needPosMap.get(item);
            double newDistance = playerPos.distanceTo(storagePos.getBtnPos().toCenterPos());
            if (newDistance < minDistance) {
                minDistance = newDistance;
                putItem = item;
            }
        }
        StoragePos storagePos = needPosMap.get(putItem);

        // 检查位置是否存在kit
        BlockState putItemKitPos = mc.world.getBlockState(storagePos.getKitPos());
        if (putItemKitPos.isAir()) {
            // 不存在, 需要去拿空盒, 然后走放置kit流程
            curOperationPos = storagePos;
            closeScreen();
            step = Steps.PLACE_EMPTY_KIT;
            return;
        }

        // 检查是否是潜影盒
        if (!(putItemKitPos.getBlock() instanceof ShulkerBoxBlock)) {
            closeScreen();
            breakStep("<" + Names.get(putItem) + "位置>被占用");
            return;
        }

        // 判断距离是否够, 不够则需要移动过去
        if (notInOperationRange(storagePos)) {
            closeScreen();
            gotoBtnPos(storagePos, "放<" + Names.get(putItem) + ">距离不够, 尝试移动", Steps.PUT_ITEM);
            return;
        }

        curOperationPos = storagePos;
        Item finalPutItem = putItem;
        openKit(curOperationPos.getKitPos(), screenHandler -> {
            // 检查kit是否已满
            if (hasScreenFull()) {
                info("kit已满，挖掉存放");
                delayCloseNext(Steps.BREAK_KIT);
                return;
            }
            nextPlayerStack(itemStack -> itemStack.getItem() == finalPutItem);
            InvUtils.shiftClick().slot(getCurPlayerSlot());

            if (hasScreenFull()) {
                info("kit已满，挖掉存放");
                delayCloseNext(Steps.BREAK_KIT);

            } else {
                setDelay();
            }
        });
    }

    /**
     * 将多物品分类物品直接放入对应箱子
     */
    private void putMultiItem(Set<Item> multiItemSet) {
        // 找出最近的
        Vec3d playerPos = mc.player.getEntityPos();
        double minDistance = Double.MAX_VALUE;
        Item putItem = Items.AIR;
        StoragePos targetPos = null;
        for (Item item : multiItemSet) {
            Item targetItem = relatedToTargetMap.get(item);
            StoragePos storagePos = multiItemPosMap.get(targetItem);
            if (storagePos == null) continue;
            double newDistance = playerPos.distanceTo(storagePos.getBtnPos().toCenterPos());
            if (newDistance < minDistance) {
                minDistance = newDistance;
                putItem = item;
                targetPos = storagePos;
            }
        }

        if (targetPos == null) {
            delayCloseNext(Steps.PUT_ITEM);
            return;
        }

        curOperationPos = targetPos;
        Item finalPutItem = putItem;

        // 判断距离是否够
        if (notInOperationRange(targetPos)) {
            closeScreen();
            gotoBtnPos(targetPos, "放<" + Names.get(finalPutItem) + ">距离不够, 尝试移动", Steps.PUT_ITEM);
            return;
        }

        // 检查目标位置是否为箱子
        BlockState putPosState = mc.world.getBlockState(targetPos.getPutPos());
        if (putPosState.getBlock() != Blocks.CHEST) {
            closeScreen();
            breakStep("<" + Names.get(finalPutItem) + "位置>箱子不存在");
            return;
        }

        openChest(targetPos.getPutPos(), inventory -> {
            if (isContainerFull()) {
                breakStep("<" + Names.get(finalPutItem) + ">箱子已满");
                return;
            }
            nextPlayerStack(itemStack -> itemStack.getItem() == finalPutItem);
            InvUtils.shiftClick().slot(getCurPlayerSlot());
            setDelay();
        });
    }

    private void breakKit() {
        // 检查距离
        if (notInOperationRange(curOperationPos)) {
            gotoBtnPos(curOperationPos, "<挖盒子>距离不够, 尝试移动", Steps.BREAK_KIT);
            return;
        }

        // 计算点击位置
        Vec3d hitPos = Vec3d.ofCenter(curOperationPos.getBtnPos()).add(0, -0.5, 0);

        // 创建 BlockHitResult
        BlockHitResult hitResult = new BlockHitResult(hitPos, Direction.UP, curOperationPos.getBtnPos(), false);

        // 创建并发送交互数据包
        PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0);

        // 点击按钮推掉盒子
        mc.getNetworkHandler().sendPacket(packet);

        step = Steps.NEXT;

        setDelay(40);
    }

    /**
     * 扫描展示框识别所有存储位置（用于初始化）
     */
    private boolean scanPositions() {
        List<ItemFrameEntity> itemFrames = mc.world.getEntitiesByClass(
                ItemFrameEntity.class,
                mc.player.getBoundingBox().expand(scanRange.get()),
                frame -> !frame.getHeldItemStack().isEmpty());

        int playerY = mc.player.getBlockPos().getY();
        Vec3d playerPos = mc.player.getEntityPos();

        for (ItemFrameEntity frame : itemFrames) {
            BlockPos attachedBlockPos = frame.getAttachedBlockPos();

            // 范围限制：只需要玩家相同或高4格范围内的
            if (attachedBlockPos.getY() < playerY || attachedBlockPos.getY() > playerY + 4) {
                continue;
            }

            // 距离限制
            double distance = attachedBlockPos.toCenterPos().distanceTo(playerPos);
            if (distance > scanRange.get()) {
                continue;
            }

            ItemStack frameHeldItemStack = frame.getHeldItemStack();
            Item item = frameHeldItemStack.getItem();

            StoragePos pos = checkAndBuildStoragePos(frame, StorageItem.valueOf(new ItemBo(frameHeldItemStack)));
            if (pos == null) {
                continue;
            }

            // 识别待整理位置（待整理箱子）
            if (item == takeKitItem.get()) {
                takeKitPos = keepNearer(pos, takeKitPos, playerPos);
            }
            // 识别空盒位置
            else if (item == emptyKitItem.get()) {
                emptyKitPos = keepNearer(pos, emptyKitPos, playerPos);
            }
            // 识别杂盒位置
            else if (item == miscKitItem.get()) {
                miscKitPos = keepNearer(pos, miscKitPos, playerPos);
            }
            // 普通物品位置（非潜影盒）
            else if (!HeItemUtils.isShulkerBox(item)) {
                StoragePos oldPos = itemPosMap.get(item);
                StoragePos nearerPos = keepNearer(pos, oldPos, playerPos);
                if (nearerPos == pos) {
                    itemPosMap.put(item, pos);
                }
            }
        }

        // 验证必要位置
        if (emptyKitPos == null) {
            warning("未检测到<空盒位置>，请在对应展示框放置" + Names.get(emptyKitItem.get()));
            return false;
        }
        if (takeKitPos == null) {
            warning("未检测到<待整理位置>，请在对应展示框放置" + Names.get(takeKitItem.get()));
            return false;
        }
        if (miscKitPos == null) {
            warning("未检测到<无法整理位置>，请在对应展示框放置" + Names.get(miscKitItem.get()));
            return false;
        }
        if (itemPosMap.isEmpty()) {
            warning("未检测到<物品>位置>");
            return false;
        }

        return true;
    }

    /**
     * 保留距离玩家更近的位置
     *
     * @return 如果 newPos 更近则返回 newPos，否则返回 oldPos
     */
    private StoragePos keepNearer(StoragePos newPos, StoragePos oldPos, Vec3d playerPos) {
        if (oldPos == null) {
            return newPos;
        }
        double newDist = newPos.getBtnPos().getSquaredDistance(playerPos);
        double oldDist = oldPos.getBtnPos().getSquaredDistance(playerPos);
        return newDist < oldDist ? newPos : oldPos;
    }

    private void clear() {
        itemPosMap.clear();
        takeKitPos = null;
        emptyKitPos = null;
        miscKitPos = null;
        needPosMap.clear();
        multiItemPosMap.clear();
        relatedToTargetMap.clear();
        curOperationPos = null;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        curOperationPos = null;
        takeQty = 0;

        HeInvUtils.closeCurScreen();
    }
}