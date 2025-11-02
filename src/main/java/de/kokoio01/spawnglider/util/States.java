package de.kokoio01.spawnglider.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class States {
	public static Map<UUID, Boolean> GlidingEnabled = new HashMap<>();
	public static Set<UUID> FlyingPlayers = new HashSet<>();
	public static Map<UUID, Long> GracePeriodEndTimes = new HashMap<>();
	public static Map<UUID, Integer> FlyingTicks = new HashMap<>();
	public static Map<UUID, Integer> BoostCount = new HashMap<>();
	public static Map<UUID, Long> LastBoostTime = new HashMap<>();

	public static boolean isGlidingEnabled(UUID uuid) {
		return GlidingEnabled.getOrDefault(uuid, true);
	}

	public static void setGlidingEnabled(UUID uuid, boolean value) {
		GlidingEnabled.put(uuid, value);
	}

	public static boolean isFlying(UUID uuid) {
		return FlyingPlayers.contains(uuid);
	}

	public static void setFlying(UUID uuid, boolean flying) {
		if (flying) {
			FlyingPlayers.add(uuid);
		} else {
			FlyingPlayers.remove(uuid);
		}
	}

	public static int getFlyingTicks(UUID uuid) {
		return FlyingTicks.getOrDefault(uuid, 0);
	}

	public static void incrementFlyingTicks(UUID uuid) {
		FlyingTicks.put(uuid, getFlyingTicks(uuid) + 1);
	}

	public static void resetFlyingTicks(UUID uuid) {
		FlyingTicks.remove(uuid);
	}

	public static boolean isInGracePeriod(UUID uuid) {
		Long endTime = GracePeriodEndTimes.get(uuid);
		if (endTime == null) {
			return false;
		}
		return System.currentTimeMillis() < endTime;
	}

	public static void startGracePeriod(UUID uuid, long ticks) {
		// Convert ticks to milliseconds (1 tick = 50ms)
		long durationMs = ticks * 50;
		GracePeriodEndTimes.put(uuid, System.currentTimeMillis() + durationMs);
	}

	public static void clearGracePeriod(UUID uuid) {
		GracePeriodEndTimes.remove(uuid);
	}

	public static int getBoostCount(UUID uuid) {
		return BoostCount.getOrDefault(uuid, 0);
	}

	public static void setBoostCount(UUID uuid, int count) {
		BoostCount.put(uuid, count);
	}

	public static void decrementBoost(UUID uuid) {
		int current = getBoostCount(uuid);
		if (current > 0) {
			setBoostCount(uuid, current - 1);
		}
	}

	public static void resetBoosts(UUID uuid, int maxBoosts) {
		setBoostCount(uuid, maxBoosts);
	}

	public static long getLastBoostTime(UUID uuid) {
		return LastBoostTime.getOrDefault(uuid, 0L);
	}

	public static void setLastBoostTime(UUID uuid, long time) {
		LastBoostTime.put(uuid, time);
	}
}
