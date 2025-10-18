package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.config.SpawnElytraConfig;
import de.kokoio01.spawnglider.config.SpawnElytraConfig.Region;
import de.kokoio01.spawnglider.util.States;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RegionFlightController {
	private static final int GRACE_PERIOD_TICKS = 5;
	private static final int MIN_FLYING_TICKS_FOR_GRACE = 20;

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
			if (player.isCreative() || player.isSpectator()) continue;
			if (!States.isGlidingEnabled(player.getUuid())) continue;

			Identifier worldId = player.getEntityWorld().getRegistryKey().getValue();
			Region region = config.getRegion(worldId);

			boolean inside = false;
			if (region != null) {
				inside = region.contains(worldId, player.getX(), player.getY(), player.getZ());
			}

			if (inside && !player.isGliding() && !player.isOnGround()) {
				player.startGliding();
				States.setFlying(player.getUuid(), true);
				States.resetFlyingTicks(player.getUuid());
				continue;
			}

			if (States.isFlying(player.getUuid()) && !player.isOnGround()) {
				States.incrementFlyingTicks(player.getUuid());
				if (!player.isGliding()) {
					player.startGliding();
				}
				continue;
			}

			if (States.isFlying(player.getUuid()) && player.isOnGround()) {
				int flown = States.getFlyingTicks(player.getUuid());
				States.setFlying(player.getUuid(), false);
				if (flown >= MIN_FLYING_TICKS_FOR_GRACE) {
					States.startGracePeriod(player.getUuid(), GRACE_PERIOD_TICKS);
					if (!player.isGliding()) {
						player.startGliding();
					}
				}
				States.resetFlyingTicks(player.getUuid());
				continue;
			}

			if (States.isInGracePeriod(player.getUuid()) && !player.isGliding()) {
				player.startGliding();
				continue;
			}
		}
	}
}
