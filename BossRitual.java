package com.example.sheepboss;

import java.util.List;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Ritual: a "+" made of 5 wool blocks (any colours, lying flat or standing up),
 * then right-click the CENTER wool block with a redstone torch.
 */
public final class BossRitual {
    private BossRitual() {}

    private static final Direction[][] PLANES = {
            {Direction.EAST, Direction.SOUTH},
            {Direction.EAST, Direction.UP},
            {Direction.SOUTH, Direction.UP}
    };

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || player.isSpectator()) {
                return ActionResult.PASS;
            }
            ItemStack held = player.getStackInHand(hand);
            if (!held.isOf(Items.REDSTONE_TORCH)) {
                return ActionResult.PASS;
            }
            BlockPos center = hit.getBlockPos();
            List<BlockPos> cross = findCross(world, center);
            if (cross == null) {
                return ActionResult.PASS;
            }

            ServerWorld sw = (ServerWorld) world;
            for (BlockPos p : cross) {
                sw.removeBlock(p, false);
            }
            if (!player.isCreative()) {
                held.decrement(1);
            }

            BossSheepEntity boss = new BossSheepEntity(ModEntities.BOSS_SHEEP, sw);
            boss.refreshPositionAndAngles(center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                    player.getYaw() + 180.0F, 0.0F);
            for (int i = 0; i < 4 && !sw.isSpaceEmpty(boss); i++) {
                boss.setPosition(boss.getX(), boss.getY() + 1.0, boss.getZ());
            }
            sw.spawnEntity(boss);

            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(sw);
            if (bolt != null) {
                bolt.setPosition(Vec3d.ofBottomCenter(center));
                bolt.setCosmetic(true);
                sw.spawnEntity(bolt);
            }
            sw.playSound(null, center, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 1.0F, 1.0F);
            sw.getServer().getPlayerManager().broadcast(Text.translatable("message.sheepboss.awaken"), false);
            return ActionResult.SUCCESS;
        });
    }

    private static List<BlockPos> findCross(World world, BlockPos center) {
        if (!world.getBlockState(center).isIn(BlockTags.WOOL)) {
            return null;
        }
        for (Direction[] plane : PLANES) {
            Direction a = plane[0];
            Direction b = plane[1];
            List<BlockPos> cross = List.of(
                    center,
                    center.offset(a), center.offset(a.getOpposite()),
                    center.offset(b), center.offset(b.getOpposite()));
            boolean ok = true;
            for (BlockPos p : cross) {
                if (!world.getBlockState(p).isIn(BlockTags.WOOL)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return cross;
            }
        }
        return null;
    }
}
