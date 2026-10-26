package com.example.sheepboss.client;

import com.example.sheepboss.ModEntities;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.SheepEntityRenderer;

public class SheepBossClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BOSS_SHEEP, BossSheepRenderer::new);
        EntityRendererRegistry.register(ModEntities.ANGRY_SHEEP, AngrySheepRenderer::new);
    }
}
