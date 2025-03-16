package io.github.unkn0wncvm1.smartmobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
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
    @Override
    public void onInitialize() {

        LOGGER.info("SmartMobs is initializing!");
        // Register tick event
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_ENTITY)
                .stream()
                .filter(Entity::hasVehicle)
                .forEach(this::giveMobsControl);
        }
    }

private void giveMobsControl(LivingEntity mob) {
    Entity vehicle = mob.getVehicle();

    if (vehicle instanceof BoatEntity boat) {
        boat.setYaw(mob.getYaw()); // Set boat direction based on mob yaw
        Vec3d mobVelocity = mob.getVelocity();
        double fixedyforboats = mobVelocity.y + 0.1; // Keep boat on water
        Vec3d boatVelocity = new Vec3d(mobVelocity.x, fixedyforboats, mobVelocity.z).multiply(2.0); // Increase multiplier for faster boat speed on ice
        boat.setVelocity(boatVelocity);
    } else if (vehicle instanceof MinecartEntity minecart) {
        Vec3d mobVelocity = mob.getVelocity();
        Vec3d minecartVelocity = new Vec3d(mobVelocity.x, mobVelocity.y, mobVelocity.z).multiply(1.5); // Adjust minecart velocity and ensure proper rail interaction
        minecart.setVelocity(minecartVelocity);
    } else if (vehicle instanceof MobEntity ridingMob) {
        ridingMob.getNavigation().startMovingTo(mob.getX(), mob.getY(), mob.getZ(), 1.5); // Increase navigation speed
    }
}
}
