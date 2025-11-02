package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.config.SpawnElytraConfig.Region;
import de.kokoio01.spawnglider.util.States;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class RegionFlightController {
	private static final int GRACE_PERIOD_TICKS = 5 * 20;
	private static final int MIN_FLYING_TICKS_FOR_GRACE = 20;
	private static final int MIN_AIRBORNE_TICKS = 10; // ~0.5 seconds before activating glide
	private static final double MIN_FALL_DISTANCE = 1.0; // Minimum fall distance in blocks

	private final SpawnElytraConfig config;

	public RegionFlightController(SpawnElytraConfig config) {
		this.config = config;
	}

	public void register() {
		ServerTickEvents.END_SERVER_TICK.register(this::onEndTick);
	}

	private void onEndTick(MinecraftServer server) {
		States.GracePeriodEndTimes.entrySet().removeIf(entry -> 
			System.currentTimeMillis() >= entry.getValue());
		
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isCreative() || player.isSpectator()) {
				if (States.isFlying(player.getUuid())) {
					States.setFlying(player.getUuid(), false);
					States.resetFlyingTicks(player.getUuid());
					States.setBoostCount(player.getUuid(), 0);
					States.clearGracePeriod(player.getUuid());
					BoostHandler.removeInteractionEntity(player);
				}
				continue;
			}
			if (!States.isGlidingEnabled(player.getUuid())) continue;

			Identifier worldId = player.getEntityWorld().getRegistryKey().getValue();
			Region region = config.getRegion(worldId);

			boolean inside = false;
			if (region != null) {
				inside = region.contains(worldId, player.getX(), player.getY(), player.getZ());
			}

			// Track airborne time for players in region
			if (inside && !player.isOnGround()) {
				if (!States.isFlying(player.getUuid())) {
					States.incrementFlyingTicks(player.getUuid());
				}
				
				// Only activate gliding after minimum airborne time AND fall distance
				if (!player.isGliding() && 
					States.getFlyingTicks(player.getUuid()) >= MIN_AIRBORNE_TICKS &&
					player.fallDistance >= MIN_FALL_DISTANCE) {
					player.startGliding();
					States.setFlying(player.getUuid(), true);
					States.resetFlyingTicks(player.getUuid());
					
					// Initialize boosts and spawn interaction entity
					int boosts = States.getBoostCount(player.getUuid());
					if (boosts == 0) {
						boosts = config.maxBoosts;
						States.setBoostCount(player.getUuid(), boosts);
					}
					
					BoostHandler.spawnInteractionEntity(player);
					player.sendMessage(Text.literal(config.boostActivationMessage)
							.formatted(Formatting.GOLD), true);
					continue;
				}
			} else if (inside && player.isOnGround() && !States.isFlying(player.getUuid())) {
				// Reset airborne counter when on ground and not flying
				States.resetFlyingTicks(player.getUuid());
			}

			// Refresh boosts when in spawn region (on ground or flying)
			if (inside && player.isOnGround()) {
				if (States.getBoostCount(player.getUuid()) < config.maxBoosts) {
					States.resetBoosts(player.getUuid(), config.maxBoosts);
				}
			}

			if (States.isFlying(player.getUuid()) && !player.isOnGround()) {
				States.incrementFlyingTicks(player.getUuid());
				if (!player.isGliding()) {
					player.startGliding();
				}

				// Refresh boosts while flying in spawn region
				if (inside && States.getBoostCount(player.getUuid()) < config.maxBoosts) {
					States.resetBoosts(player.getUuid(), config.maxBoosts);
				}

				// Update interaction entity position
				BoostHandler.updateInteractionEntity(player);

				continue;
			}

			if (States.isFlying(player.getUuid()) && player.isOnGround()) {
				States.setFlying(player.getUuid(), false);
				
				// Remove interaction entity and boosts when landing (not in spawn)
				BoostHandler.removeInteractionEntity(player);
				if (!inside) {
					States.setBoostCount(player.getUuid(), 0);
				}
				
				States.startGracePeriod(player.getUuid(), GRACE_PERIOD_TICKS);
				
				States.resetFlyingTicks(player.getUuid());
				continue;
			}

			if (States.isInGracePeriod(player.getUuid())) {
				if (!player.isOnGround() && !player.isGliding()) {
					player.startGliding();
				}
				continue;
			}
		}
	}
}
