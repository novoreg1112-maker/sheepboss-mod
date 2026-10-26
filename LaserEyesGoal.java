package com.example.sheepboss;

import java.util.EnumSet;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/** Boss charges up for 1.5s, then fires two fiery beams from its eyes for 2.5s. */
public class LaserEyesGoal extends Goal {
    private static final int CHARGE_TICKS = 30;
    private static final int FIRE_TICKS = 50;
    private static final int COOLDOWN_TICKS = 20 * 7;
    private static final double RANGE = 40.0;
    private static final float DAMAGE = 5.0F;

    private final BossSheepEntity boss;
    private int nextAge = 20 * 5;
    private int startAge;
    private int lastHitAge;

    public LaserEyesGoal(BossSheepEntity boss) {
        this.boss = boss;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity target = boss.getTarget();
        return target != null
                && target.isAlive()
                && boss.age >= nextAge
                && boss.squaredDistanceTo(target) < RANGE * RANGE
                && boss.canSee(target);
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity target = boss.getTarget();
        return target != null && target.isAlive() && boss.age - startAge < CHARGE_TICKS + FIRE_TICKS;
    }

    @Override
    public void start() {
        startAge = boss.age;
        lastHitAge = boss.age;
        boss.getNavigation().stop();
        boss.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, 4.0F, 0.6F);
        boss.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0F, 1.4F);
    }

    @Override
    public void stop() {
        nextAge = boss.age + COOLDOWN_TICKS;
    }

    @Override
    public void tick() {
        LivingEntity target = boss.getTarget();
        if (target == null || !(boss.getWorld() instanceof ServerWorld world)) {
            return;
        }
        boss.getNavigation().stop();
        boss.getLookControl().lookAt(target, 40.0F, 40.0F);

        int t = boss.age - startAge;
        Vec3d[] eyes = boss.getEyeBeamOrigins();

        if (t < CHARGE_TICKS) {
            for (Vec3d eye : eyes) {
                world.spawnParticles(new DustParticleEffect(new Vector3f(1.0F, 0.015F, 0.025F), 1.8F), eye.x, eye.y, eye.z, 8, 0.08, 0.08, 0.08, 0.01);
                world.spawnParticles(ParticleTypes.END_ROD, eye.x, eye.y, eye.z, 2, 0.04, 0.04, 0.04, 0.01);
            }
            return;
        }

        Vec3d aim = target.getPos().add(0.0, target.getHeight() * 0.6, 0.0);
        boss.playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 0.7F, 0.55F);
        for (Vec3d eye : eyes) {
            drawBeam(world, eye, aim);
        }
        if (boss.age - lastHitAge >= 8 && boss.canSee(target)) {
            lastHitAge = boss.age;
            target.damage(boss.getDamageSources().mobAttack(boss), DAMAGE);
            target.setOnFireFor(3);
        }
    }

    private static void drawBeam(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        double length = delta.length();
        if (length < 0.001) {
            return;
        }
        Vec3d step = delta.normalize();
        for (double d = 0.0; d < length; d += 1.0) {
            Vec3d p = from.add(step.multiply(d));
            world.spawnParticles(new DustParticleEffect(new Vector3f(1.0F, 0.0F, 0.025F), 1.45F), p.x, p.y, p.z, 3, 0.035, 0.035, 0.035, 0.0);
            world.spawnParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, 0.025, 0.025, 0.025, 0.0);
        }
    }
}
