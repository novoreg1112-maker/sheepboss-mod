package com.example.sheepboss;

import net.fabricmc.api.ModInitializer;

public class SheepBossMod implements ModInitializer {
    public static final String MOD_ID = "sheepboss";

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();
        BossRitual.register();
    }
}
