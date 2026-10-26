package com.example.sheepboss;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Right click: fires an exploding black sheep. Ammo: 1 black wool (free in creative). */
public class SheepBazookaItem extends Item {
    public SheepBazookaItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!user.isCreative() && !consumeAmmo(user)) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("message.sheepboss.no_ammo"), true);
            }
            return TypedActionResult.fail(stack);
        }

        if (!world.isClient) {
            Vec3d look = user.getRotationVec(1.0F);
            Vec3d pos = user.getEyePos().add(look.multiply(1.5)).subtract(0.0, 0.4, 0.0);

            AngrySheepEntity sheep = new AngrySheepEntity(ModEntities.ANGRY_SHEEP, world);
            sheep.refreshPositionAndAngles(pos.x, pos.y, pos.z, user.getYaw(), user.getPitch());
            sheep.setBodyYaw(user.getYaw());
            sheep.setHeadYaw(user.getYaw());
            sheep.setColor(DyeColor.BLACK);
            sheep.launchAsMissile(user, look.multiply(2.35));
            world.spawnEntity(sheep);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.5F, 0.7F);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_SHEEP_AMBIENT, SoundCategory.PLAYERS, 1.0F, 0.6F);
        }

        user.getItemCooldownManager().set(this, 12);
        return TypedActionResult.success(stack);
    }

    private static boolean consumeAmmo(PlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.BLACK_WOOL)) {
                s.decrement(1);
                return true;
            }
        }
        return false;
    }
}
