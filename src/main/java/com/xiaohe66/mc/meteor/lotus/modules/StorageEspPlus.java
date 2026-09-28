package com.xiaohe66.mc.meteor.lotus.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.xiaohe66.mc.meteor.lotus.event.StorageEspLineEvent;
import com.xiaohe66.mc.meteor.lotus.event.StorageFinderEvent;
import com.xiaohe66.mc.meteor.lotus.util.Const;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author He
 * @since 2025.05.29 12:57
 */
public class StorageEspPlus extends Module {

    private static final Cache<String, Boolean> storageStatusCache = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .expireAfterAccess(5, TimeUnit.SECONDS)
            .build();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> storageEspEnable = sgGeneral.add(new BoolSetting.Builder()
            .name("存储透视增强")
            .description("减少存储透视的连接线")
            .defaultValue(true)
            .build());

    public final Setting<Boolean> stashFinderEnable = sgGeneral.add(new BoolSetting.Builder()
            .name("存储查找器增强")
            .description("减少存储查找器的提示")
            .defaultValue(true)
            .build());

    private final Setting<List<Block>> excludeBlock = sgGeneral.add(new BlockListSetting.Builder()
            .name("排除方块")
            .description("附近有指定方块时不显示连接线")
            .defaultValue(
                    Blocks.WAXED_COPPER_BLOCK, //涂腊的铜块
                    Blocks.WAXED_OXIDIZED_COPPER, //涂腊的氧化铜块
                    Blocks.MOSSY_COBBLESTONE, //苔石
                    Blocks.MOSSY_STONE_BRICKS, // 苔石砖
                    Blocks.NETHER_BRICKS, // 下界砖块
                    Blocks.BLACKSTONE, // 黑石
                    Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS // 裂纹磨制黑石砖
            )
            .build());

    private final Setting<Integer> excludeRange = sgGeneral.add(new IntSetting.Builder()
            .name("排除范围")
            .description("排除范围")
            .min(1)
            .sliderMax(5)
            .defaultValue(3)
            .build());

    private final Setting<Integer> excludeValue = sgGeneral.add(new IntSetting.Builder()
            .name("排除阈值")
            .description("达到阈值后不显示连接线")
            .min(1)
            .sliderMax(10)
            .defaultValue(5)
            .build());

    private final Setting<Integer> storageQty = sgGeneral.add(new IntSetting.Builder()
            .name("强制容器数")
            .description("当附近的容器达到指定数量时，强制显示连接线")
            .min(1)
            .sliderMax(10)
            .defaultValue(3)
            .build());

    public StorageEspPlus() {
        super(Const.CATEGORY, "存储查找增强", "给[存储透视]和[存储查找器]增加排除遗迹的选项");
    }

    @EventHandler
    private void onEvent(StorageEspLineEvent event) {
        if (storageEspEnable.get() && testInStructure(event.getBlockEntity())) {
            event.cancel();
        }
    }

    @EventHandler
    private void onEvent(StorageFinderEvent event) {
        if (stashFinderEnable.get() && testInStructure(event.getBlockEntity())) {
            event.cancel();
        }
    }

    private boolean testInStructure(BlockEntity blockEntity) {
        BlockPos originPos = blockEntity.getPos();
        String key = originPos.getX() + "_" + originPos.getY() + "_" + originPos.getZ();
        try {
            return storageStatusCache.get(key, () -> doTestInStructure(blockEntity));

        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        }

        // return doTestInStructure(blockEntity);
    }

    private boolean doTestInStructure(BlockEntity blockEntity) {

        int range = excludeRange.get() - 1;
        List<Block> blocks = excludeBlock.get();

        Set<Item> blockItemSet = blocks.stream().map(Block::asItem).collect(Collectors.toSet());

        int count = 0;
        int storageCount = 0;

        BlockPos originPos = blockEntity.getPos();
        int startX = originPos.getX() - range;
        int endX = originPos.getX() + range;
        int startY = originPos.getY() - range;
        int endY = originPos.getY() + range;
        int startZ = originPos.getZ() - range;
        int endZ = originPos.getZ() + range;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {

                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState blockState = MeteorClient.mc.world.getBlockState(blockPos);
                    Item item = blockState.getBlock().asItem();

                    if (item == Items.CHEST || item == Items.TRAPPED_CHEST || item == Items.HOPPER) {
                        storageCount++;

                    } else if (blockItemSet.contains(item)) {
                        count++;
                    }
                }
            }
        }

        return storageCount < storageQty.get() && count >= excludeValue.get();
    }

}
