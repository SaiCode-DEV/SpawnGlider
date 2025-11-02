package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.commands.BoostCommand;
import de.kokoio01.spawnglider.commands.ToggleGliderCommand;
import de.kokoio01.spawnglider.commands.ZoneManagementCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;

import java.util.logging.Logger;

public class SpawnGlider implements ModInitializer {
    @Override
    public void onInitialize() {
        // Config loading
        SpawnElytraConfig CONFIG = SpawnElytraConfig.loadOrCreate();

        // Command registration
        ToggleGliderCommand.register();
        ZoneManagementCommand.register(CONFIG);
        BoostCommand.register(CONFIG);

        // Controller registration
        new RegionFlightController(CONFIG).register();
        
        // Boost handler registration
        BoostHandler.register(CONFIG);
        
        // Fall damage protection
        FallDamageProtection.register();
        
        // Clean up leftover interaction entities on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            BoostHandler.cleanupAllInteractionEntities(server);
        });
    }
}
