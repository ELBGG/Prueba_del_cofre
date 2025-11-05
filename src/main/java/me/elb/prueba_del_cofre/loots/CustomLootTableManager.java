package me.elb.prueba_del_cofre.loots;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import me.elb.prueba_del_cofre.Prueba_del_cofre;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Deserializers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class CustomLootTableManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path LOOT_TABLES_PATH = Paths.get("config", "chestloot", "loots");

    private final Map<ResourceLocation, JsonElement> customLootTables = new HashMap<>();
    private MinecraftServer server;

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public void loadCustomLootTables() {
        customLootTables.clear();

        try {
            Files.createDirectories(LOOT_TABLES_PATH);

            File lootsDir = LOOT_TABLES_PATH.toFile();
            File[] files = lootsDir.listFiles((dir, name) -> name.endsWith(".json"));

            if (files == null || files.length == 0) {
                createExampleLootTables();
            }

            files = lootsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    loadLootTableFile(file);
                }
            }

            Prueba_del_cofre.LOGGER.info("Loaded {} custom loot tables from config", customLootTables.size());

        } catch (IOException e) {
            Prueba_del_cofre.LOGGER.error("Failed to load custom loot tables", e);
        }
    }

    private void loadLootTableFile(File file) {
        try {
            String fileName = file.getName().replace(".json", "");
            ResourceLocation id = new ResourceLocation("chestloot", fileName);

            FileReader reader = new FileReader(file);
            JsonElement jsonElement = GSON.fromJson(reader, JsonElement.class);
            reader.close();

            customLootTables.put(id, jsonElement);
            Prueba_del_cofre.LOGGER.info("Loaded custom loot table: {}", id);

        } catch (Exception e) {
            Prueba_del_cofre.LOGGER.error("Failed to load loot table from file: " + file.getName(), e);
        }
    }

    public void injectLootTablesIntoServer() {
        if (server == null) {
            Prueba_del_cofre.LOGGER.warn("Server is null, cannot inject loot tables");
            return;
        }

        try {
            var lootTables = server.getLootTables();

            Field tablesField = lootTables.getClass().getDeclaredField("tables");
            tablesField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<ResourceLocation, LootTable> tablesMap =
                    (Map<ResourceLocation, LootTable>) tablesField.get(lootTables);

            Map<ResourceLocation, LootTable> mutableMap = new HashMap<>(tablesMap);

            int successCount = 0;
            for (Map.Entry<ResourceLocation, JsonElement> entry : customLootTables.entrySet()) {
                ResourceLocation id = entry.getKey();
                JsonElement json = entry.getValue();

                try {
                    LootTable lootTable = Deserializers.createLootTableSerializer().create()
                            .fromJson(json, LootTable.class);

                    if (lootTable != null) {
                        mutableMap.put(id, lootTable);
                        successCount++;
                        Prueba_del_cofre.LOGGER.info("Injected custom loot table into server: {}", id);
                    } else {
                        Prueba_del_cofre.LOGGER.error("Failed to deserialize loot table: {} (returned null)", id);
                    }
                } catch (Exception e) {
                    Prueba_del_cofre.LOGGER.error("Failed to inject loot table: " + id, e);
                    Prueba_del_cofre.LOGGER.warn("Consider using a datapack for loot table: {}", id);
                }
            }

            tablesField.set(lootTables, mutableMap);

            Prueba_del_cofre.LOGGER.info("Successfully injected {}/{} custom loot tables",
                    successCount, customLootTables.size());

        } catch (NoSuchFieldException e) {
            Prueba_del_cofre.LOGGER.error("Could not find 'tables' field in LootTables class. This might be due to obfuscation.", e);
            Prueba_del_cofre.LOGGER.warn("Custom loot tables will only work if you create a datapack with them.");
            suggestDatapackApproach();
        } catch (Exception e) {
            Prueba_del_cofre.LOGGER.error("Failed to inject custom loot tables into server", e);
            suggestDatapackApproach();
        }
    }

    public LootTable getLootTable(ResourceLocation id) {
        JsonElement json = customLootTables.get(id);
        if (json != null) {
            try {
                return Deserializers.createLootTableSerializer().create()
                        .fromJson(json, LootTable.class);
            } catch (Exception e) {
                Prueba_del_cofre.LOGGER.error("Failed to deserialize loot table: " + id, e);
            }
        }
        return null;
    }

    private void suggestDatapackApproach() {
        Prueba_del_cofre.LOGGER.info("==================================================");
        Prueba_del_cofre.LOGGER.info("Alternative: Create a datapack with your custom loot tables");
        Prueba_del_cofre.LOGGER.info("Place them in: <world>/datapacks/<packname>/data/chestloot/loot_tables/");
        Prueba_del_cofre.LOGGER.info("==================================================");
    }

    private void createExampleLootTables() throws IOException {
        String mineshaftLoot = """
{
  "type": "minecraft:chest",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:golden_apple",
          "weight": 20
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:enchanted_golden_apple",
          "weight": 1
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:name_tag",
          "weight": 30
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "function": "minecraft:enchant_randomly"
            }
          ],
          "name": "minecraft:book",
          "weight": 10
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:iron_pickaxe",
          "weight": 5
        }
      ],
      "rolls": 1.0
    },
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 5.0,
                "min": 1.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:iron_ingot",
          "weight": 10
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 3.0,
                "min": 1.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:gold_ingot",
          "weight": 5
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 9.0,
                "min": 4.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:redstone",
          "weight": 5
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 2.0,
                "min": 1.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:diamond",
          "weight": 3
        }
      ],
      "rolls": {
        "type": "minecraft:uniform",
        "max": 4.0,
        "min": 2.0
      }
    }
  ]
}
""";

        String bastionLoot = """
{
  "type": "minecraft:chest",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "functions": [
            {
              "function": "minecraft:enchant_randomly"
            }
          ],
          "name": "minecraft:diamond_pickaxe",
          "weight": 6
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:netherite_scrap",
          "weight": 4
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:ancient_debris",
          "weight": 12
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 17.0,
                "min": 6.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:golden_carrot",
          "weight": 12
        }
      ],
      "rolls": 1.0
    }
  ]
}
""";

        String shipwreckLoot = """
{
  "type": "minecraft:chest",
  "pools": [
    {
      "bonus_rolls": 0.0,
      "entries": [
        {
          "type": "minecraft:item",
          "functions": [
            {
              "add": false,
              "count": {
                "type": "minecraft:uniform",
                "max": 12.0,
                "min": 1.0
              },
              "function": "minecraft:set_count"
            }
          ],
          "name": "minecraft:paper",
          "weight": 8
        },
        {
          "type": "minecraft:item",
          "functions": [
            {
              "function": "minecraft:enchant_randomly"
            }
          ],
          "name": "minecraft:leather_helmet",
          "weight": 3
        },
        {
          "type": "minecraft:item",
          "name": "minecraft:tnt",
          "weight": 1
        }
      ],
      "rolls": {
        "type": "minecraft:uniform",
        "max": 10.0,
        "min": 3.0
      }
    }
  ]
}
""";

        Files.writeString(LOOT_TABLES_PATH.resolve("custom_mineshaft.json"), mineshaftLoot);
        Files.writeString(LOOT_TABLES_PATH.resolve("custom_bastion.json"), bastionLoot);
        Files.writeString(LOOT_TABLES_PATH.resolve("custom_shipwreck.json"), shipwreckLoot);

        Prueba_del_cofre.LOGGER.info("Created example custom loot tables");
    }

    public Map<ResourceLocation, JsonElement> getCustomLootTables() {
        return customLootTables;
    }

    public boolean hasCustomLootTable(ResourceLocation id) {
        return customLootTables.containsKey(id);
    }
}