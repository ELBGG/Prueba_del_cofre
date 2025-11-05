package me.elb.prueba_del_cofre.mixin;

import me.elb.prueba_del_cofre.Prueba_del_cofre;
import me.elb.prueba_del_cofre.access.ChestBlockEntityDataAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityDataMixin implements ChestBlockEntityDataAccess {

    @Unique
    private boolean chestLoot$used = false;

    @Override
    public void chestLoot$setUsed(boolean used) {
        this.chestLoot$used = used;
    }

    @Override
    public boolean chestLoot$isUsed() {
        return this.chestLoot$used;
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void onLoad(CompoundTag compoundTag, CallbackInfo ci) {
        chestLoot$used = compoundTag.getBoolean("ChestLootUsed");
    }

    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void onSave(CompoundTag compoundTag, CallbackInfo ci) {
        compoundTag.putBoolean("ChestLootUsed", chestLoot$used);
    }

    @Inject(method = "startOpen", at = @At("HEAD"))
    private void onChestOpened(Player player, CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;

        if (chest.getLevel() instanceof ServerLevel serverLevel && !chestLoot$used) {
            RandomizableContainerBlockEntityAccessor accessor = (RandomizableContainerBlockEntityAccessor) chest;

            if (accessor.getLootTable() == null) {
                chestLoot$generateLootForChest(serverLevel, chest.getBlockPos(), chest, player);
                Prueba_del_cofre.LOGGER.debug(
                        "Generated loot table for chest at {} (first time opened)",
                        chest.getBlockPos()
                );
            } else {
                Prueba_del_cofre.LOGGER.debug(
                        "Chest at {} already has loot table: {}",
                        chest.getBlockPos(), accessor.getLootTable()
                );
            }

            chestLoot$used = true;
            chest.setChanged();
        }
    }

    @Unique
    private void chestLoot$generateLootForChest(ServerLevel level, BlockPos pos, ChestBlockEntity chest, Player player) {
        var configLoader = Prueba_del_cofre.getConfigLoader();
        var lootManager = Prueba_del_cofre.getLootTableManager();

        var biomeHolder = level.getBiome(pos);
        Registry<Biome> biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
        ResourceLocation biomeId = biomeRegistry.getKey(biomeHolder.value());

        if (biomeId != null) {
            List<ResourceLocation> lootTables = configLoader.getLootTablesForBiome(biomeId);

            if (!lootTables.isEmpty()) {
                ResourceLocation selectedLootTable = configLoader.getRandomLootTable(lootTables);

                if (selectedLootTable != null) {
                    if (selectedLootTable.getNamespace().equals("chestloot")) {
                        chestLoot$fillWithCustomLoot(level, chest, selectedLootTable, player);
                        Prueba_del_cofre.LOGGER.info(
                                "Filled chest at {} with custom loot table {} in biome {}",
                                pos, selectedLootTable, biomeId
                        );
                    } else {
                        chest.setLootTable(selectedLootTable, level.getRandom().nextLong());
                        Prueba_del_cofre.LOGGER.info(
                                "Assigned vanilla loot table {} to chest at {} in biome {}",
                                selectedLootTable, pos, biomeId
                        );
                    }
                }
            } else {
                Prueba_del_cofre.LOGGER.debug(
                        "No loot tables configured for biome {} at position {}",
                        biomeId, pos
                );
            }
        }
    }

    @Unique
    private void chestLoot$fillWithCustomLoot(ServerLevel level, ChestBlockEntity chest,
                                              ResourceLocation lootTableId, Player player) {
        var lootManager = Prueba_del_cofre.getLootTableManager();
        LootTable customLoot = lootManager.getLootTable(lootTableId);

        if (customLoot != null) {
            try {
                LootContext.Builder contextBuilder = new LootContext.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest.getBlockPos()))
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                        .withRandom(level.getRandom());

                LootContext context = contextBuilder.create(LootContextParamSets.CHEST);
                customLoot.fill(chest, context);

                Prueba_del_cofre.LOGGER.debug("Filled chest with custom loot: {}", lootTableId);
            } catch (Exception e) {
                Prueba_del_cofre.LOGGER.error("Failed to fill chest with custom loot: " + lootTableId, e);
            }
        } else {
            Prueba_del_cofre.LOGGER.warn("Custom loot table not found: {}", lootTableId);
        }
    }
}