package io.github.unkn0wncvm1.smartmobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class SmartMobs implements ModInitializer {
    public static final String MOD_ID = "smartmobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @SuppressWarnings("unused") //intellij might be stupid
    private static int tickCount = 0;
    @Override
    public void onInitialize() {

        LOGGER.info("SmartMobs is initializing!");
        // Register tick event
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        tickCount++;
        for (ServerWorld world : server.getWorlds()) {
            world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_ENTITY)
                    .stream()
                    .filter(Entity::hasVehicle)
                    .filter(entity -> !(entity instanceof PlayerEntity))
                    .forEach(this::giveMobsControl);
        }
    }
private void giveMobsControl(LivingEntity mob) {
    // Safety checks
    if (mob.isRemoved() || mob.isDead()) return;
    Entity vehicle = mob.getVehicle();
    if (vehicle == null || vehicle.isRemoved()) return;

    // Tunable constants for target speeds (blocks per tick) and acceleration smoothing
    final double BOAT_TARGET_SPEED = 0.5;     // ~8.0 blocks/sec
    final double BOAT_ACCEL = 0.12;           // 0..1 higher = snappier
    final double MINECART_TARGET_SPEED = 0.35; // ~5.6 blocks/sec
    final double MINECART_ACCEL = 0.12;       // 0..1
    final double RIDING_MOB_TARGET_SPEED = 0.65; // ~10.4 blocks/sec
    final double RIDING_MOB_ACCEL = 0.18;       // snappier for responsiveness

    switch (vehicle) {
        case BoatEntity boat -> {
            // Face the boat where the mob is looking and push forward toward a target speed
            boat.setYaw(mob.getYaw());
            Vec3d current = boat.getVelocity();
            Vec3d forward = Vec3d.fromPolar(0, mob.getYaw()); // unit vector on XZ from yaw
            Vec3d target = new Vec3d(forward.x * BOAT_TARGET_SPEED, current.y, forward.z * BOAT_TARGET_SPEED);
            Vec3d newVel = current.add(target.subtract(current).multiply(BOAT_ACCEL));
            // Keep from dipping; boats handle buoyancy, avoid forcing downward Y
            if (newVel.y < 0.0) newVel = new Vec3d(newVel.x, 0.0, newVel.z);
            boat.setVelocity(newVel);
        }
        case MinecartEntity minecart -> {
            // Nudge minecart along the mob's look direction with a capped target speed
            Vec3d current = minecart.getVelocity();
            Vec3d forward = Vec3d.fromPolar(0, mob.getYaw());
            Vec3d target = new Vec3d(forward.x * MINECART_TARGET_SPEED, current.y, forward.z * MINECART_TARGET_SPEED);
            Vec3d newVel = current.add(target.subtract(current).multiply(MINECART_ACCEL));
            minecart.setVelocity(newVel);
        }
        case MobEntity ridingMob -> {
            // Prevent the ridden mob’s own AI/navigation from fighting our control
            if (ridingMob.getNavigation() != null) {
                ridingMob.getNavigation().stop();
            }

            // If the rider isn't actively pathing anywhere, don't force movement: gently damp the ridden mob to a stop
            if (mob instanceof MobEntity riderMob) {
                var riderNav = riderMob.getNavigation();
                boolean riderMoving = riderNav != null && riderNav.isFollowingPath();
                if (!riderMoving) {
                    Vec3d cur = ridingMob.getVelocity();
                    Vec3d damped = new Vec3d(cur.x * 0.5, cur.y, cur.z * 0.5);
                    ridingMob.setVelocity(damped);
                    return;
                }
            }

            // Align ridden mob’s orientation to the rider and move forward with smoothing
            float yaw = mob.getYaw();
            ridingMob.setYaw(yaw);
            ridingMob.setBodyYaw(yaw);
            ridingMob.setHeadYaw(yaw);

            Vec3d current = ridingMob.getVelocity();
            Vec3d forward = Vec3d.fromPolar(0, yaw);
            Vec3d target = new Vec3d(forward.x * RIDING_MOB_TARGET_SPEED, current.y, forward.z * RIDING_MOB_TARGET_SPEED);
            Vec3d newVel = current.add(target.subtract(current).multiply(RIDING_MOB_ACCEL));
            ridingMob.setVelocity(newVel);
        }
        default -> {
            // no-op for other vehicle types
        }
    }
}
}
