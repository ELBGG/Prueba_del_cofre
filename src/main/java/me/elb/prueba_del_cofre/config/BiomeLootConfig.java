package me.elb.prueba_del_cofre.config;

import java.util.List;
import java.util.Map;

public class BiomeLootConfig {
    private Map<String, BiomeLootEntry> biomes;
    
    public BiomeLootConfig() {
    }
    
    public Map<String, BiomeLootEntry> getBiomes() {
        return biomes;
    }
    
    public void setBiomes(Map<String, BiomeLootEntry> biomes) {
        this.biomes = biomes;
    }
    
    public static class BiomeLootEntry {
        private List<String> biome_ids;
        private List<String> loot_tables;
        
        public BiomeLootEntry() {
        }
        
        public List<String> getBiomeIds() {
            return biome_ids;
        }
        
        public void setBiomeIds(List<String> biome_ids) {
            this.biome_ids = biome_ids;
        }
        
        public List<String> getLootTables() {
            return loot_tables;
        }
        
        public void setLootTables(List<String> loot_tables) {
            this.loot_tables = loot_tables;
        }
    }
}