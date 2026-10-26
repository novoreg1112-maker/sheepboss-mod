package com.example.sheepboss.client;

import com.example.sheepboss.AngrySheepEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.SheepEntityRenderer;
import net.minecraft.util.Identifier;

/** Custom textured renderer for the explosive sheep. */
public class AngrySheepRenderer extends SheepEntityRenderer {
    private static final Identifier TEXTURE = Identifier.of("sheepboss", "textures/entity/angry_sheep.png");
    public AngrySheepRenderer(EntityRendererFactory.Context context) { super(context); }
    @Override public Identifier getTexture(net.minecraft.entity.passive.SheepEntity entity) { return TEXTURE; }
}
