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

public class SmartMobs implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register tick event
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            world.getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class), EntityPredicates.VALID_ENTITY)
                .stream()
                .filter(entity -> entity.hasVehicle())
                .forEach(this::giveMobsControl);
        }
    }

    private void giveMobsControl(LivingEntity mob) {
        Entity vehicle = mob.getVehicle();

        if (vehicle instanceof BoatEntity) {
            BoatEntity boat = (BoatEntity) vehicle;
            boat.setYaw(mob.getYaw()); // Set boat direction based on mob yaw
            boat.setVelocity(mob.getVelocity().multiply(2.0)); // Increase boat velocity based on mob velocity
            boat.setPos(mob.getX(), mob.getY(), mob.getZ()); // Ensure the boat stays above water
        } else if (vehicle instanceof MinecartEntity) {
            MinecartEntity minecart = (MinecartEntity) vehicle;
            minecart.setVelocity(mob.getVelocity().multiply(2.0)); // Increase minecart velocity based on mob velocity
        } else if (vehicle instanceof MobEntity) {
            MobEntity ridingMob = (MobEntity) vehicle;
            ridingMob.getNavigation().startMovingTo(mob.getX(), mob.getY(), mob.getZ(), 2.0); // Increase navigation speed
        }
    }
}
