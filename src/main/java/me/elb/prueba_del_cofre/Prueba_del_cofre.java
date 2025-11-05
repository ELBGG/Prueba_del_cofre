package me.elb.prueba_del_cofre;

import me.elb.prueba_del_cofre.commands.ChestLootCommand;
import me.elb.prueba_del_cofre.config.ConfigLoader;
import me.elb.prueba_del_cofre.loots.CustomLootTableManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Prueba_del_cofre implements ModInitializer {
    public static final String MOD_ID = "prueba_del_cofre";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static ConfigLoader configLoader;
    private static CustomLootTableManager lootTableManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ChestLoot Mambo");

        configLoader = new ConfigLoader();
        configLoader.loadConfig();

        lootTableManager = new CustomLootTableManager();
        lootTableManager.loadCustomLootTables();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ChestLootCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            lootTableManager.setServer(server);
            lootTableManager.injectLootTablesIntoServer();
            LOGGER.info("ChestLoot custom loot tables loaded and injected on server start");
        });

        LOGGER.info("ChestLoot Mod initialized successfully");
    }

    public static ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public static CustomLootTableManager getLootTableManager() {
        return lootTableManager;
    }
}