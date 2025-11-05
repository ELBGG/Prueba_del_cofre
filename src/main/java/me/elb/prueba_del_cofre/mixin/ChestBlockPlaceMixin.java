package me.elb.prueba_del_cofre.mixin;

import me.elb.prueba_del_cofre.Prueba_del_cofre;
import me.elb.prueba_del_cofre.access.ChestBlockEntityDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlock.class)
public class ChestBlockPlaceMixin {

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void onChestPlaced(Level level, BlockPos pos, BlockState state,
                               @Nullable LivingEntity placer, ItemStack stack, CallbackInfo ci) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!(placer instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GameType gameMode = serverPlayer.gameMode.getGameModeForPlayer();

        if (gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof ChestBlockEntity chest) {
                if (chest instanceof ChestBlockEntityDataAccess chestAccess) {
                    chestAccess.chestLoot$setUsed(true);
                    chest.setChanged();

                    Prueba_del_cofre.LOGGER.debug(
                            "Marked chest at {} as used (placed by {} in {} mode)",
                            pos, serverPlayer.getName().getString(), gameMode
                    );
                } else {
                    Prueba_del_cofre.LOGGER.error(
                            "ChestBlockEntity at {} does not implement ChestBlockEntityDataAccess!",
                            pos
                    );
                }
            }
        } else {
            Prueba_del_cofre.LOGGER.debug(
                    "Chest at {} placed by {} in {} mode - NOT marked as used (creative/spectator)",
                    pos, serverPlayer.getName().getString(), gameMode
            );
        }
    }
}