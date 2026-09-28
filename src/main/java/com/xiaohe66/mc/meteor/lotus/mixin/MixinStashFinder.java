package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.event.StorageFinderEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.world.StashFinder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * @author He
 * @since 2025.07.06 19:09
 */
@Mixin(value = StashFinder.class, remap = false)
public class MixinStashFinder {

    @Shadow
    @Final
    private Setting<List<BlockEntityType<?>>> storageBlocks;

    @Unique
    private BlockEntity blockEntity;

    /**
     * NOTE : Redirect 是重写目标方法, 这个可以做到取消一行代码的作用。 而 @Inject 的取消会中断整个方法的调用
     */
    @Redirect(method = "onChunkData", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"))
    private boolean isStorage(List<?> list, Object element) {

        boolean isStorage = storageBlocks.get().contains(element);

        if (isStorage) {
            StorageFinderEvent event = StorageFinderEvent.get(blockEntity);
            MeteorClient.EVENT_BUS.post(event);
            if (event.isCancel()) {
                isStorage = false;
            }
        }
        return isStorage;
    }

    @ModifyVariable(method = "onChunkData",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", shift = At.Shift.BEFORE))
    private BlockEntity captureBlockEntity(BlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        return blockEntity;
    }
}
