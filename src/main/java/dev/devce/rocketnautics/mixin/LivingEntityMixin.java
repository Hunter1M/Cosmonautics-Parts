package dev.devce.rocketnautics.mixin;

import dev.devce.rocketnautics.RocketConfig;
import dev.devce.rocketnautics.api.FreeMotionEntity;
import dev.devce.rocketnautics.client.FreeMotionHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements FreeMotionEntity {

    @Override
    public boolean is6DOFEnabled() { return this.rocketnautics$6DOFEnabled; }

    @Override
    public void set6DOFEnabled(boolean enabled) {
        if (this.rocketnautics$6DOFEnabled && !enabled) {
            LivingEntity entity = (LivingEntity)(Object)this;
            entity.setSwimming(false);
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }
        rocketnautics$6DOFEnabled = enabled;
    }

    @Unique
    private boolean rocketnautics$6DOFEnabled;

    @Override
    public boolean isAmbulant() { return this.rocketnautics$ambulant; }

    @Override
    public void setAmbulant(boolean ambulant) {
        if (this.rocketnautics$ambulant && !ambulant && !this.rocketnautics$6DOFEnabled) {
            LivingEntity entity = (LivingEntity)(Object)this;
            entity.setSwimming(false);
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }
        rocketnautics$ambulant = ambulant;
    }

    @Unique
    private boolean rocketnautics$ambulant;

    @Override
    public float getMovementAcceleration() { return rocketnautics$movementAcceleration; }

    @Override
    public void setMovementAcceleration(float accel) { rocketnautics$movementAcceleration = accel; }

    @Unique
    private float rocketnautics$movementAcceleration = (float)RocketConfig.SERVER.jetpackThrust.getAsDouble();

    @Override
    public float getDampenerForce() { return rocketnautics$dampenerForce; }

    @Override
    public void setDampenerForce(float force) { rocketnautics$dampenerForce = force; }

    @Unique
    private float rocketnautics$dampenerForce;

    @Override
    public Quaternionf getOrientation() { return rocketnautics$orientation; }

    @Override
    public void setOrientation(Quaternionf quat) { rocketnautics$orientation = quat; }

    @Unique
    private Quaternionf rocketnautics$orientation = new Quaternionf();

    public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Shadow
    public abstract float getSpeed();

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void rocketnautics$6DOFMovement(Vec3 motionIn, CallbackInfo ci) {
        if (FreeMotionHandler.apply6DOFPhysics(motionIn.toVector3f(), (LivingEntity)(Object)this)) ci.cancel();
    }
}
