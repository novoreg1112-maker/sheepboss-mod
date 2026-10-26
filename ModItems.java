package com.example.sheepboss;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    private ModItems() {}

    public static final Item SHEEP_BAZOOKA = Registry.register(
            Registries.ITEM,
            Identifier.of(SheepBossMod.MOD_ID, "sheep_bazooka"),
            new SheepBazookaItem(new Item.Settings().maxCount(1)));

    public static final Item BOSS_SHEEP_SPAWN_EGG = Registry.register(
            Registries.ITEM,
            Identifier.of(SheepBossMod.MOD_ID, "boss_sheep_spawn_egg"),
            new SpawnEggItem(ModEntities.BOSS_SHEEP, 0xB71C1C, 0xFFFFFF, new Item.Settings()));

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(SHEEP_BAZOOKA));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(entries -> entries.add(BOSS_SHEEP_SPAWN_EGG));
    }
}
