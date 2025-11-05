package me.elb.prueba_del_cofre.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.elb.prueba_del_cofre.Prueba_del_cofre;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ConfigLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "chestloot", "biome_loot_config.json");

    private BiomeLootConfig config;
    private final Map<ResourceLocation, List<ResourceLocation>> biomeLootMap;

    public ConfigLoader() {
        this.biomeLootMap = new HashMap<>();
    }

    public void loadConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());

            if (!Files.exists(CONFIG_PATH)) {
                createDefaultConfig();
            }

            Reader reader = new FileReader(CONFIG_PATH.toFile());
            config = GSON.fromJson(reader, BiomeLootConfig.class);
            reader.close();

            processConfig();

            Prueba_del_cofre.LOGGER.info("Configuration loaded successfully");
        } catch (Exception e) {
            Prueba_del_cofre.LOGGER.error("Failed to load configuration", e);
            config = new BiomeLootConfig();
            config.setBiomes(new HashMap<>());
        }
    }

    private void createDefaultConfig() throws IOException {
        BiomeLootConfig defaultConfig = new BiomeLootConfig();
        Map<String, BiomeLootConfig.BiomeLootEntry> biomes = new HashMap<>();

        BiomeLootConfig.BiomeLootEntry defaultEntry = new BiomeLootConfig.BiomeLootEntry();
        defaultEntry.setBiomeIds(new ArrayList<>());
        defaultEntry.setLootTables(new ArrayList<>());
        biomes.put("default", defaultEntry);

        BiomeLootConfig.BiomeLootEntry plainsEntry = new BiomeLootConfig.BiomeLootEntry();
        plainsEntry.setBiomeIds(Arrays.asList(
                "minecraft:plains",
                "minecraft:sunflower_plains"
        ));
        plainsEntry.setLootTables(Arrays.asList(
                "minecraft:chests/village/village_plains_house",
                "minecraft:chests/simple_dungeon"
        ));
        biomes.put("plains_chests", plainsEntry);

        BiomeLootConfig.BiomeLootEntry desertEntry = new BiomeLootConfig.BiomeLootEntry();
        desertEntry.setBiomeIds(List.of(
                "minecraft:desert"
        ));
        desertEntry.setLootTables(Arrays.asList(
                "minecraft:chests/village/village_desert_house",
                "minecraft:chests/desert_pyramid"
        ));
        biomes.put("desert_chests", desertEntry);

        defaultConfig.setBiomes(biomes);

        Writer writer = new FileWriter(CONFIG_PATH.toFile());
        GSON.toJson(defaultConfig, writer);
        writer.close();

        Prueba_del_cofre.LOGGER.info("Created default configuration file");
    }

    private void processConfig() {
        biomeLootMap.clear();

        if (config == null || config.getBiomes() == null) {
            return;
        }

        for (Map.Entry<String, BiomeLootConfig.BiomeLootEntry> entry : config.getBiomes().entrySet()) {
            BiomeLootConfig.BiomeLootEntry lootEntry = entry.getValue();

            if (lootEntry.getBiomeIds() == null || lootEntry.getLootTables() == null) {
                continue;
            }

            List<ResourceLocation> lootTables = new ArrayList<>();
            for (String lootTable : lootEntry.getLootTables()) {
                try {
                    lootTables.add(ResourceLocation.tryParse(lootTable));
                } catch (Exception e) {
                    Prueba_del_cofre.LOGGER.warn("Invalid loot table identifier: {}", lootTable);
                }
            }
            for (String biomeId : lootEntry.getBiomeIds()) {
                try {
                    ResourceLocation biome = ResourceLocation.tryParse(biomeId);
                    biomeLootMap.put(biome, lootTables);
                } catch (Exception e) {
                    Prueba_del_cofre.LOGGER.warn("Invalid biome identifier: {}", biomeId);
                }
            }
        }

        Prueba_del_cofre.LOGGER.info("Processed {} biome mappings", biomeLootMap.size());
    }

    public List<ResourceLocation> getLootTablesForBiome(ResourceLocation biomeId) {
        List<ResourceLocation> lootTables = biomeLootMap.get(biomeId);

        if (lootTables == null || lootTables.isEmpty()) {
            BiomeLootConfig.BiomeLootEntry defaultEntry = config.getBiomes().get("default");
            if (defaultEntry != null && defaultEntry.getLootTables() != null) {
                lootTables = new ArrayList<>();
                for (String lootTable : defaultEntry.getLootTables()) {
                    try {
                        lootTables.add(ResourceLocation.tryParse(lootTable));
                    } catch (Exception e) {
                        Prueba_del_cofre.LOGGER.warn("Invalid default loot table: {}", lootTable);
                    }
                }
            }
        }

        return lootTables != null ? lootTables : new ArrayList<>();
    }

    public ResourceLocation getRandomLootTable(List<ResourceLocation> lootTables) {
        if (lootTables.isEmpty()) {
            return null;
        }

        Random random = new Random();
        return lootTables.get(random.nextInt(lootTables.size()));
    }
}