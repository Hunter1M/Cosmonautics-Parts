package dev.devce.rocketnautics.content.physics;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import dev.devce.rocketnautics.RocketNautics;
import dev.devce.rocketnautics.content.items.JetpackItem;
import dev.devce.rocketnautics.content.items.LegThrustersItem;
import dev.devce.rocketnautics.registry.RocketSounds;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core physics handler for high-speed sonic boom effects and backtank timer visibility.
 */
public class GlobalSpacePhysicsHandler {

    /**
     * Initializes the physics tick listener using Sable's physics system.
     */
    public static void init() {
        SableEventPlatform.INSTANCE.onPhysicsTick((physicsSystem, timeStep) -> {
            ServerLevel level = physicsSystem.getLevel();
            SubLevelContainer container = SubLevelContainer.getContainer(level);

            if (container == null) return;

            for (SubLevel subLevel : container.getAllSubLevels()) {
                if (subLevel instanceof ServerSubLevel serverSubLevel) {
                    RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);
                    if (handle != null && handle.isValid()) {
                        applySonicBoom(serverSubLevel, handle, level);
                    }
                }
            }
        });
    }

    private static final Map<UUID, Boolean> SUPERSONIC_SHIPS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vector3d> LAST_SHIPS_VELOCITIES = new ConcurrentHashMap<>();

    private static void applySonicBoom(ServerSubLevel subLevel, RigidBodyHandle handle, ServerLevel level) {
        if (!dev.devce.rocketnautics.RocketConfig.SERVER.boomEnabled.get() || !handle.isValid()) {
            SUPERSONIC_SHIPS.remove(subLevel.getUniqueId());
            LAST_SHIPS_VELOCITIES.remove(subLevel.getUniqueId());
            return;
        }

        Vector3d worldPos = subLevel.logicalPose().position();
        if (worldPos == null) return;

        Vector3d velocity;
        try {
            velocity = new Vector3d(handle.getLinearVelocity());
        } catch (RuntimeException e) {
            SUPERSONIC_SHIPS.remove(subLevel.getUniqueId());
            LAST_SHIPS_VELOCITIES.remove(subLevel.getUniqueId());
            return;
        }
        double speed = velocity.length();
        
        // Mach 1 is configured by the server settings (default: 80.0 blocks per second).
        // Hysteresis allows re-triggering if it slows down below 85% of Mach 1
        double mach1 = dev.devce.rocketnautics.RocketConfig.SERVER.sonicBoomSpeedThreshold.get();
        double machReset = mach1 * 0.85;
        
        UUID uuid = subLevel.getUniqueId();
        boolean wasSupersonic = SUPERSONIC_SHIPS.getOrDefault(uuid, false);

        // Filter out extreme acceleration jumps typical of creative physics gun drags / teleports
        Vector3d lastVel = LAST_SHIPS_VELOCITIES.get(uuid);
        LAST_SHIPS_VELOCITIES.put(uuid, new Vector3d(velocity));
        
        boolean abnormalAcceleration = false;
        if (lastVel != null) {
            double accel = new Vector3d(velocity).sub(lastVel).length() * 20.0; // accel in m/s^2 (20 ticks/sec)
            if (accel > 150.0) {
                abnormalAcceleration = true;
            }
        } else {
            abnormalAcceleration = true; // Skip first tick to prevent boom on spawning/teleporting
        }

        if (speed >= mach1) {
            if (!wasSupersonic) {
                SUPERSONIC_SHIPS.put(uuid, true);
                if (!abnormalAcceleration) {
                    triggerSonicBoomEffects(subLevel, level, worldPos, velocity);
                }
            }
        } else if (speed < machReset) {
            if (wasSupersonic) {
                SUPERSONIC_SHIPS.put(uuid, false);
            }
        }
    }

    private static void triggerSonicBoomEffects(ServerSubLevel subLevel, ServerLevel level, Vector3d worldPos, Vector3d velocity) {
        // Play the sonic boom sound at a very large range (volume 12.0f translates to ~192 blocks range)
        level.playSound(
                null,
                worldPos.x(), worldPos.y(), worldPos.z(),
                RocketSounds.SONIC_BOOM.get(),
                SoundSource.NEUTRAL,
                12.0f,
                1.0f
        );

        // Spawn vapor shockwave ring perpendicular to the velocity direction
        Vector3d dir = new Vector3d(velocity);
        double velLength = dir.length();
        if (velLength > 0.001) {
            dir.normalize();
        } else {
            dir.set(0, 1, 0); // Default to up if no velocity
        }

        // Find two perpendicular vectors to draw a circle around the travel axis
        Vector3d u = new Vector3d();
        Vector3d v = new Vector3d();
        if (Math.abs(dir.x) > 0.9) {
            u.set(0, 1, 0);
        } else {
            u.set(1, 0, 0);
        }
        dir.cross(u, v).normalize();
        dir.cross(v, u).normalize();

        // Expand shockwave rings of cloud particles
        for (double radius = 1.5; radius <= 8.5; radius += 1.75) {
            int points = (int) (radius * 10);
            for (int i = 0; i < points; i++) {
                double angle = (2.0 * Math.PI * i) / points;
                double dx = (u.x * Math.cos(angle) + v.x * Math.sin(angle)) * radius;
                double dy = (u.y * Math.cos(angle) + v.y * Math.sin(angle)) * radius;
                double dz = (u.z * Math.cos(angle) + v.z * Math.sin(angle)) * radius;

                level.sendParticles(
                        ParticleTypes.CLOUD,
                        worldPos.x() + dx,
                        worldPos.y() + dy,
                        worldPos.z() + dz,
                        1,
                        0.15, 0.15, 0.15,
                        0.02
                );
            }
        }

        // Spawn visual flash and dramatic explosion shockwave at center
        level.sendParticles(
                ParticleTypes.FLASH,
                worldPos.x(), worldPos.y(), worldPos.z(),
                6,
                0.5, 0.5, 0.5,
                0.0
        );

        level.sendParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                worldPos.x(), worldPos.y(), worldPos.z(),
                3,
                1.0, 1.0, 1.0,
                0.0
        );
    }

    public static boolean shouldDisplayTimer(Player player) {
        if (BacktankUtil.getAllWithAir(player).isEmpty()) return false;
        return JetpackItem.isActive(player) || LegThrustersItem.legThrustersActive(player);
    }
}
