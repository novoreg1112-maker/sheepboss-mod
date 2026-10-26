package com.example.sheepboss;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private ModEntities() {}

    public static final EntityType<BossSheepEntity> BOSS_SHEEP = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(SheepBossMod.MOD_ID, "boss_sheep"),
            EntityType.Builder.create(BossSheepEntity::new, SpawnGroup.MONSTER)
                    .dimensions(2.7F, 3.9F)
                    .build("boss_sheep"));

    public static final EntityType<AngrySheepEntity> ANGRY_SHEEP = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(SheepBossMod.MOD_ID, "angry_sheep"),
            EntityType.Builder.create(AngrySheepEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.9F, 1.3F)
                    .build("angry_sheep"));

    public static void register() {
        FabricDefaultAttributeRegistry.register(BOSS_SHEEP, BossSheepEntity.createBossAttributes());
        FabricDefaultAttributeRegistry.register(ANGRY_SHEEP, AngrySheepEntity.createAngryAttributes());
    }
}
