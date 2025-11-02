package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.util.States;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.InteractionEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoostHandler {
    private static SpawnElytraConfig config;
    private static final int BOOST_COOLDOWN_TICKS = 10;
    private static final Map<UUID, InteractionEntity> playerInteractionEntities = new HashMap<>();

    public static void register(SpawnElytraConfig cfg) {
        config = cfg;
        
        // Listen for entity interactions (right-click on interaction entity)
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != Hand.MAIN_HAND) {
                return ActionResult.PASS;
            }
            
            // Check if it's an interaction entity belonging to this player
            if (!(entity instanceof InteractionEntity)) {
                return ActionResult.PASS;
            }
            
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            
            // Verify this is the player's boost entity
            InteractionEntity playerEntity = playerInteractionEntities.get(serverPlayer.getUuid());
            if (playerEntity == null || !entity.equals(playerEntity)) {
                return ActionResult.PASS;
            }
            
            // Only boost if flying
            if (!States.isFlying(serverPlayer.getUuid())) {
                return ActionResult.PASS;
            }
            
            if (!serverPlayer.isGliding()) {
                return ActionResult.PASS;
            }
            
            // Check cooldown
            long currentTick = world.getTime();
            long lastBoost = States.getLastBoostTime(serverPlayer.getUuid());
            
            if (currentTick - lastBoost < BOOST_COOLDOWN_TICKS) {
                return ActionResult.FAIL;
            }
            
            // Check boost count
            int boosts = States.getBoostCount(serverPlayer.getUuid());
            if (boosts <= 0) {
                serverPlayer.sendMessage(Text.literal(config.noBoostsMessage)
                        .formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            
            // Apply boost using player's view direction
            double strength = config.boostStrength;
            var lookDir = serverPlayer.getRotationVec(1.0f).normalize();
            
            // Set velocity directly to the look direction * strength
            serverPlayer.setVelocity(
                lookDir.x * strength,
                lookDir.y * strength,
                lookDir.z * strength
            );
            serverPlayer.velocityModified = true;
            
            // Spawn particles at player location
            if (world instanceof ServerWorld serverWorld) {
                for (int i = 0; i < 10; i++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
                    double offsetY = (world.random.nextDouble() - 0.5) * 0.5;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;
                    
                    serverWorld.spawnParticles(
                        ParticleTypes.FIREWORK,
                        serverPlayer.getX() + offsetX,
                        serverPlayer.getY() + offsetY,
                        serverPlayer.getZ() + offsetZ,
                        1,
                        0, 0, 0,
                        0.1
                    );
                }
                
                // Add smoke trail effect
                for (int i = 0; i < 5; i++) {
                    serverWorld.spawnParticles(
                        ParticleTypes.LARGE_SMOKE,
                        serverPlayer.getX(),
                        serverPlayer.getY(),
                        serverPlayer.getZ(),
                        1,
                        0.2, 0.2, 0.2,
                        0.01
                    );
                }
            }
            
            // Update state
            States.decrementBoost(serverPlayer.getUuid());
            States.setLastBoostTime(serverPlayer.getUuid(), currentTick);
            
            // Send feedback
            int remaining = States.getBoostCount(serverPlayer.getUuid());
            serverPlayer.sendMessage(Text.literal(config.boostSuccessMessage + " (" + remaining + ")")
                    .formatted(Formatting.AQUA), true);
            
            return ActionResult.SUCCESS;
        });
    }

    public static void spawnInteractionEntity(ServerPlayerEntity player) {
        ServerWorld serverWorld = (ServerWorld) player.getEntityWorld();

        // Remove old entity if exists
        removeInteractionEntity(player);

        // Create new interaction entity in front of player
        InteractionEntity interactionEntity = new InteractionEntity(EntityType.INTERACTION, serverWorld);
        
        // Position it in front of the player (4 blocks forward)
        var lookDir = player.getRotationVec(1.0f).normalize();
        double offsetDistance = 4;
        double x = player.getX() + lookDir.x * offsetDistance;
        double y = player.getY() + lookDir.y * offsetDistance - 1.5;
        double z = player.getZ() + lookDir.z * offsetDistance;
        
        interactionEntity.refreshPositionAndAngles(x, y, z, 0, 0);
        interactionEntity.setInteractionHeight(3f);
        interactionEntity.setInteractionWidth(3f);
        interactionEntity.addCommandTag("spawnglider_boost_entity");

        serverWorld.spawnEntity(interactionEntity);
        
        playerInteractionEntities.put(player.getUuid(), interactionEntity);
    }

    public static void updateInteractionEntity(ServerPlayerEntity player) {
        InteractionEntity entity = playerInteractionEntities.get(player.getUuid());
        if (entity != null && !entity.isRemoved()) {
            // Update position to stay in front of player
            var lookDir = player.getRotationVec(1.0f).normalize();
            double offsetDistance = 4;
            double x = player.getX() + lookDir.x * offsetDistance;
            double y = player.getY() + lookDir.y * offsetDistance - 1.5;
            double z = player.getZ() + lookDir.z * offsetDistance;
            
            entity.refreshPositionAndAngles(x, y, z, 0, 0);
        } else if (entity != null) {
            // Entity was removed, clean up
            playerInteractionEntities.remove(player.getUuid());
        }
    }

    public static void removeInteractionEntity(ServerPlayerEntity player) {
        InteractionEntity entity = playerInteractionEntities.remove(player.getUuid());
        if (entity != null && !entity.isRemoved()) {
            entity.discard();
        }
    }

    public static void cleanupAllInteractionEntities(net.minecraft.server.MinecraftServer server) {
        // Clear the map first
        playerInteractionEntities.clear();
        
        // Kill all interaction entities with our tag in all worlds
        for (var world : server.getWorlds()) {
            var entities = world.getEntitiesByType(
                EntityType.INTERACTION,
                entity -> entity.getCommandTags().contains("spawnglider_boost_entity")
            );
            
            for (var entity : entities) {
                entity.discard();
            }
        }
        
        server.sendMessage(Text.literal("[SpawnGlider] Cleaned up " + 
            server.getWorlds().iterator().next().getEntitiesByType(
                EntityType.INTERACTION,
                entity -> entity.getCommandTags().contains("spawnglider_boost_entity")
            ).size() + " leftover boost entities"));
    }
}
