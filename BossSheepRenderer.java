package com.example.sheepboss.client;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SheepEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.util.Identifier;
import com.example.sheepboss.BossSheepEntity;

/** Vanilla sheep renderer, just 3x bigger. */
public class BossSheepRenderer extends SheepEntityRenderer {
    private static final float SCALE = 3.0F;

    public BossSheepRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.7F * SCALE;
    }

    @Override
    public Identifier getTexture(SheepEntity entity) {
        if (entity instanceof BossSheepEntity boss && boss.getHealth() <= boss.getMaxHealth() * 0.5F) {
            return Identifier.of("sheepboss", "textures/entity/boss_sheep_robot.png");
        }
        return Identifier.of("minecraft", "textures/entity/sheep/sheep.png");
    }

    @Override
    protected void scale(SheepEntity entity, MatrixStack matrices, float amount) {
        matrices.scale(SCALE, SCALE, SCALE);
    }
}
