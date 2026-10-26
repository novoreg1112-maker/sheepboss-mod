package com.example.sheepboss;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import org.joml.Vector3f;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BossSheepEntity extends SheepEntity {
    /** How many sheep each wave contains. */
    public static final int WAVE_SIZE = 30;
    /** Ticks between waves (20 ticks = 1 second, so 1200 = 1 minute). */
    public static final int WAVE_INTERVAL = 20 * 60;
    /** Delay before the very first wave. */
    public static final int FIRST_WAVE_DELAY = 20 * 8;

    private final ServerBossBar bossBar =
            new ServerBossBar(this.getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);
    private int nextWaveAge = FIRST_WAVE_DELAY;
    private boolean robotPhase = false;

    public BossSheepEntity(EntityType<? extends SheepEntity> type, World world) {
        super(type, world);
        this.setColor(DyeColor.RED);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createBossAttributes() {
        return SheepEntity.createSheepAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 14.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        super.initGoals(); // sets up fields vanilla sheep needs
        this.goalSelector.clear(goal -> true);
        this.targetSelector.clear(goal -> true);

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new LaserEyesGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 24.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        if (!robotPhase && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            robotPhase = true;
            this.playSound(SoundEvents.ENTITY_IRON_GOLEM_REPAIR, 3.0F, 0.55F);
            this.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 2.5F, 0.7F);
            if (this.getWorld() instanceof ServerWorld phaseWorld) {
                phaseWorld.spawnParticles(ParticleTypes.EXPLOSION, this.getX(), this.getBodyY(0.5), this.getZ(), 8, 1.0, 1.0, 1.0, 0.02);
                phaseWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getBodyY(0.5), this.getZ(), 70, 1.2, 1.2, 1.2, 0.12);
            }
            this.setCustomName(Text.literal("§c§lОВЦА-БОТ Mk.II"));
            for (PlayerEntity player : this.getWorld().getPlayers()) {
                if (this.squaredDistanceTo(player) < 64 * 64) player.sendMessage(Text.translatable("message.sheepboss.phase2"), true);
            }
        }

        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (this.age >= this.nextWaveAge) {
            PlayerEntity victim = world.getClosestPlayer(this, 48.0);
            if (victim != null) {
                summonWave(world, victim);
                this.nextWaveAge = this.age + WAVE_INTERVAL;
            }
        }
    }

    private void summonWave(ServerWorld world, PlayerEntity victim) {
        this.playSound(SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 4.0F, 0.6F);
        this.playSound(SoundEvents.ENTITY_SHEEP_AMBIENT, 4.0F, 0.4F);
        victim.sendMessage(Text.translatable("message.sheepboss.wave"), true);

        for (int i = 0; i < WAVE_SIZE; i++) {
            AngrySheepEntity sheep = new AngrySheepEntity(ModEntities.ANGRY_SHEEP, world);
            boolean placed = false;
            for (int attempt = 0; attempt < 8 && !placed; attempt++) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                double dist = 5.0 + this.random.nextDouble() * 8.0;
                sheep.refreshPositionAndAngles(
                        this.getX() + Math.cos(angle) * dist,
                        this.getY() + 0.5,
                        this.getZ() + Math.sin(angle) * dist,
                        this.random.nextFloat() * 360.0F, 0.0F);
                placed = world.isSpaceEmpty(sheep);
            }
            if (!placed) {
                sheep.refreshPositionAndAngles(this.getX(), this.getY() + 4.0, this.getZ(), 0.0F, 0.0F);
            }
            sheep.setColor(DyeColor.byId(this.random.nextInt(16)));
            // The flock dives from the sky instead of standing still on the ground.
            sheep.refreshPositionAndAngles(victim.getX() + (this.random.nextDouble() - 0.5) * 18.0,
                    victim.getY() + 12.0 + this.random.nextDouble() * 8.0,
                    victim.getZ() + (this.random.nextDouble() - 0.5) * 18.0,
                    this.random.nextFloat() * 360.0F, 0.0F);
            Vec3d dive = victim.getPos().add(0, victim.getHeight() * 0.5, 0)
                    .subtract(sheep.getPos()).normalize().multiply(0.72);
            sheep.launchAtPlayer(this, victim, dive);
            world.spawnEntity(sheep);
            world.spawnParticles(ParticleTypes.CLOUD, sheep.getX(), sheep.getY() + 0.5, sheep.getZ(),
                    8, 0.35, 0.3, 0.35, 0.02);
        }
    }

    /** Positions of the two "eyes" the lasers come from. */
    public Vec3d[] getEyeBeamOrigins() {
        Vec3d forward = Vec3d.fromPolar(this.getPitch(), this.getHeadYaw());
        Vec3d side = Vec3d.fromPolar(0.0F, this.getHeadYaw() + 90.0F);
        Vec3d base = this.getEyePos().add(forward.multiply(1.4));
        return new Vec3d[] { base.add(side.multiply(0.6)), base.add(side.multiply(-0.6)) };
    }

    @Override
    protected void dropLoot(DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(damageSource, causedByPlayer);
        this.dropStack(new ItemStack(ModItems.SHEEP_BAZOOKA));
        this.dropStack(new ItemStack(Items.BLACK_WOOL, 32)); // ammo
        if (this.getWorld() instanceof ServerWorld world) {
            ExperienceOrbEntity.spawn(world, this.getPos(), 500);
        }
    }

    // --- boss can't be sheared / fed ---
    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public boolean isShearable() {
        return false;
    }

    // --- boss bar plumbing (same idea as the Wither) ---
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void setCustomName(Text name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }
}
