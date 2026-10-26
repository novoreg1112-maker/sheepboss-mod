package com.example.sheepboss;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Two jobs:
 *  1) minion of the boss: a sheep that hunts players (despawns after 3 minutes);
 *  2) "ammo" of the bazooka: a black sheep that flies straight and explodes on impact.
 */
public class AngrySheepEntity extends SheepEntity {
    private static final int MINION_LIFETIME = 20 * 180;
    private static final int MISSILE_MAX_AGE = 80;

    private boolean missile = false;
    private Vec3d missileVelocity = Vec3d.ZERO;
    private Entity owner;
    private int missileAge = 0;
    private boolean playerOnlyImpact = false;
    private boolean exploded = false;

    public AngrySheepEntity(EntityType<? extends SheepEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAngryAttributes() {
        return SheepEntity.createSheepAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    /** Turns this sheep into a flying bomb. */
    public void launchAsMissile(Entity owner, Vec3d velocity) {
        this.missile = true;
        this.owner = owner;
        this.missileVelocity = velocity;
        this.setAiDisabled(true);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPersistent();
        this.setVelocity(velocity);
    }

    /** A diving attack launched from above; only players trigger the impact. */
    public void launchAtPlayer(Entity owner, PlayerEntity target, Vec3d velocity) {
        launchAsMissile(owner, velocity);
        this.playerOnlyImpact = true;
        if (target != null) {
            target.sendMessage(Text.translatable("message.sheepboss.dodge"), true);
        }
    }

    @Override
    public void tick() {
        if (this.missile && !this.getWorld().isClient) {
            this.setVelocity(this.missileVelocity); // keep flying straight
            if (this.age % 2 == 0 && this.getWorld() instanceof ServerWorld sw) {
                sw.spawnParticles(ParticleTypes.FLAME, this.getX(), this.getBodyY(0.5), this.getZ(), 3, 0.12, 0.12, 0.12, 0.015);
                sw.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getBodyY(0.5), this.getZ(), 2, 0.08, 0.08, 0.08, 0.01);
            }
        }
        super.tick();
        if (this.getWorld().isClient) {
            return;
        }
        if (this.missile) {
            this.missileAge++;
            boolean hit = this.horizontalCollision
                    || this.verticalCollision
                    || this.missileAge > MISSILE_MAX_AGE
                    || touchesSomething();
            if (hit && this.missileAge > 1) {
                explode();
            }
        } else if (this.age > MINION_LIFETIME) {
            this.discard();
        }
    }

    private boolean touchesSomething() {
        // Query the full entity bounding box, not only LivingEntity: this catches
        // armor stands and other collidable entities as well as players/mobs.
        return !this.getWorld().getOtherEntities(this, this.getBoundingBox().expand(0.18), e -> {
            if (e == this.owner || !e.isAlive() || e.isSpectator()) return false;
            if (this.playerOnlyImpact) {
                // Sneaking during the warning window is the dodge mechanic.
                if (e instanceof PlayerEntity player && player.isSneaking()) return false;
                return e instanceof PlayerEntity;
            }
            return true;
        }).isEmpty();
    }

    private void explode() {
        if (this.exploded) return;
        this.exploded = true;
        if (this.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getBodyY(0.5), this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getBodyY(0.5), this.getZ(), 24, 0.45, 0.45, 0.45, 0.12);
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.0F, 0.8F);
        }
        this.getWorld().createExplosion(this, this.getX(), this.getBodyY(0.5), this.getZ(),
                3.5F, World.ExplosionSourceType.MOB);
        this.discard();
    }

    @Override
    public boolean shouldSave() {
        return !this.missile && super.shouldSave();
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        // no drops
    }
}
