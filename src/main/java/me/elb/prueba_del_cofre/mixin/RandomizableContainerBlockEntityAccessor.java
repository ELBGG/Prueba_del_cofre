package me.elb.prueba_del_cofre.access;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.class)
public interface RandomizableContainerBlockEntityAccessor {
    
    @Accessor("lootTable")
    ResourceLocation getLootTable();
    
    @Accessor("lootTable")
    void setLootTable(ResourceLocation lootTable);
}