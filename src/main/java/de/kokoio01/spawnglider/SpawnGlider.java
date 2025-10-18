package de.kokoio01.spawnglider;

import de.kokoio01.spawnglider.commands.ToggleGliderCommand;
import de.kokoio01.spawnglider.commands.ZoneManagementCommand;
import net.fabricmc.api.ModInitializer;
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

        // Controller registration
        new RegionFlightController(CONFIG).register();
        
        // Fall damage protection
        FallDamageProtection.register();
    }
}
