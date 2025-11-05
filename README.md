# Prueba del Cofre - ChestLoot Mod

## Descripción

**Prueba del Cofre** (ChestLoot) es un mod avanzado de Minecraft para servidores Fabric que permite asignar automáticamente tablas de botín (loot tables) a los cofres según el bioma donde se encuentran. El mod genera dinámicamente el contenido de los cofres cuando un jugador los abre por primera vez, permitiendo una experiencia de juego personalizada y configurable.

## Información del Proyecto

- **Lenguaje principal:** Java 17
- **Framework:** Fabric Mod Loader
- **Build Tool:** Gradle
- **Entorno:** Servidor (Server-side)


## Funcionamiento

### Concepto Principal

El mod intercepta la apertura de cofres y, basándose en el bioma donde se encuentra el cofre, asigna automáticamente una tabla de botín personalizada. Esto permite crear experiencias únicas en diferentes áreas del mundo de Minecraft.

### Estructura del Código

```
src/main/java/me/elb/prueba_del_cofre/
├── Prueba_del_cofre.java                          # Clase principal del mod
├── access/
│   └── ChestBlockEntityDataAccess.java            # Interfaz para datos del cofre
├── commands/
│   └── ChestLootCommand.java                      # Comando /chestloot
├── config/
│   ├── BiomeLootConfig.java                       # Modelo de configuración
│   └── ConfigLoader.java                          # Cargador de configuración
├── loots/
│   └── CustomLootTableManager.java                # Gestor de loot tables
└── mixin/
    ├── ChestBlockEntityDataMixin.java             # Mixin principal de cofres
    ├── ChestBlockPlaceMixin.java                  # Mixin de colocación
    └── RandomizableContainerBlockEntityAccessor.java # Accessor de loot tables
```

---

## Componentes Detallados

### **1. Prueba_del_cofre.java** - Clase Principal
**Ubicación:** `me.elb.prueba_del_cofre.Prueba_del_cofre`

**Función:** Inicializa y coordina todos los componentes del mod

**Responsabilidades:**
- Inicializa el `ConfigLoader` para cargar la configuración de biomas
- Inicializa el `CustomLootTableManager` para gestionar loot tables personalizadas
- Registra el comando `/chestloot`
- Inyecta las loot tables personalizadas cuando el servidor inicia
- Proporciona acceso estático a los gestores de configuración y loot tables

**Código clave:**
```java
@Override
public void onInitialize() {
    configLoader = new ConfigLoader();
    configLoader.loadConfig();
    
    lootTableManager = new CustomLootTableManager();
    lootTableManager.loadCustomLootTables();
    
}
```

---

### **2. ChestBlockEntityDataMixin.java** - Mixin Principal
**Ubicación:** `me.elb.prueba_del_cofre.mixin.ChestBlockEntityDataMixin`

**Función:** Intercepta la apertura de cofres y genera botín dinámicamente

**Responsabilidades:**
- **Detecta cuando un jugador abre un cofre** (`startOpen`)
- **Determina el bioma** donde está ubicado el cofre
- **Consulta las loot tables** configuradas para ese bioma
- **Selecciona aleatoriamente** una loot table de las disponibles
- **Llena el cofre** con items según la loot table elegida
- **Marca el cofre como usado** para evitar regeneración

**Flujo de ejecución:**
```
1. Jugador abre cofre
2. ¿Es primera vez? → Verificar flag chestLoot$used
3. Obtener bioma del cofre
4. Buscar loot tables para ese bioma en configuración
5. Seleccionar loot table aleatoria
6. ¿Es custom (namespace chestloot)?
   → Sí: Llenar directamente con CustomLootTableManager
   → No: Asignar loot table vanilla
7. Marcar cofre como usado
8. Guardar estado
```

**Métodos únicos:**
```java
@Unique
private void chestLoot$generateLootForChest(ServerLevel level, BlockPos pos, 
                                            ChestBlockEntity chest, Player player)

@Unique
private void chestLoot$fillWithCustomLoot(ServerLevel level, ChestBlockEntity chest,
                                          ResourceLocation lootTableId, Player player)
```

---

### **3. ChestBlockPlaceMixin.java** - Mixin de Colocación
**Ubicación:** `me.elb.prueba_del_cofre.mixin.ChestBlockPlaceMixin`

**Función:** Intercepta cuando un jugador coloca un cofre

**Responsabilidades:**
- **Detecta la colocación de cofres** por jugadores
- **Verifica el modo de juego** (Survival/Adventure vs Creative/Spectator)
- **Marca inmediatamente como "usado"** los cofres colocados en Survival/Adventure
- **No marca** los cofres colocados en Creative/Spectator

**Por qué es importante:**
Evita que los cofres colocados por jugadores generen botín automáticamente. Solo los cofres naturales (o generados) deben tener loot automático.

**Lógica:**
```java
if (gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE) {
    chestAccess.chestLoot$setUsed(true); // Marcar como usado
}
```

---

### **4. RandomizableContainerBlockEntityAccessor.java** - Accessor
**Ubicación:** `me.elb.prueba_del_cofre.mixin.RandomizableContainerBlockEntityAccessor`

**Función:** Proporciona acceso a campos privados de contenedores

**Responsabilidades:**
- **Accede al campo privado `lootTable`** de `RandomizableContainerBlockEntity`
- **Permite leer** la loot table actual del cofre
- **Permite escribir** una nueva loot table

**Uso de Mixin Accessor:**
```java
@Accessor("lootTable")
ResourceLocation getLootTable();

@Accessor("lootTable")
void setLootTable(ResourceLocation lootTable);
```

Esto es necesario porque Minecraft no expone estos métodos públicamente.

---

### **5. ChestBlockEntityDataAccess.java** - Interfaz de Datos
**Ubicación:** `me.elb.prueba_del_cofre.access.ChestBlockEntityDataAccess`

**Función:** Define métodos para rastrear si un cofre ya fue usado

**Responsabilidades:**
- **Almacena el estado "usado"** del cofre
- **Persiste el estado** en NBT (se guarda con el mundo)

**Implementación:**
```java
public interface ChestBlockEntityDataAccess {
    void chestLoot$setUsed(boolean used);
    boolean chestLoot$isUsed();
}
```

Este flag se guarda en NBT como `"ChestLootUsed": true/false`

---

### **6. ConfigLoader.java** - Cargador de Configuración
**Ubicación:** `me.elb.prueba_del_cofre.config.ConfigLoader`

**Función:** Carga y gestiona la configuración de biomas y loot tables

**Responsabilidades:**
- **Crea el archivo de configuración** si no existe
- **Genera configuración por defecto** con ejemplos
- **Mapea biomas a loot tables** (muchos a muchos)
- **Selecciona loot tables aleatoriamente** de las disponibles
- **Recarga la configuración** con el comando `/chestloot reload`

**Estructura de configuración:**
```json
{
  "biomes": {
    "plains_chests": {
      "biome_ids": [
        "minecraft:plains",
        "minecraft:sunflower_plains"
      ],
      "loot_tables": [
        "minecraft:chests/village/village_plains_house",
        "minecraft:chests/simple_dungeon"
      ]
    },
    "default": {
      "biome_ids": [],
      "loot_tables": []
    }
  }
}
```

**Ubicación del archivo:** `config/chestloot/biome_loot_config.json`

---

### **7. BiomeLootConfig.java** - Modelo de Datos
**Ubicación:** `me.elb.prueba_del_cofre.config.BiomeLootConfig`

**Función:** Define la estructura de datos de la configuración

**Clases:**
```java
public class BiomeLootConfig {
    private Map<String, BiomeLootEntry> biomes;
    
    public static class BiomeLootEntry {
        private List<String> biome_ids;      // IDs de biomas
        private List<String> loot_tables;    // Tablas de botín
    }
}
```

Utilizado por Gson para serializar/deserializar JSON.

---

### **8. CustomLootTableManager.java** - Gestor de Loot Tables
**Ubicación:** `me.elb.prueba_del_cofre.loots.CustomLootTableManager`

**Función:** Gestiona loot tables personalizadas creadas por el usuario

**Responsabilidades:**
- **Carga archivos JSON** de `config/chestloot/loots/`
- **Crea loot tables de ejemplo** si no existen
- **Inyecta loot tables en el servidor** mediante reflexión
- **Deserializa JSON** a objetos `LootTable` de Minecraft
- **Sugiere usar datapacks** si la inyección falla

**Loot tables de ejemplo generadas:**
1. `custom_mineshaft.json` - Cofre tipo mina
2. `custom_bastion.json` - Cofre tipo bastión
3. `custom_shipwreck.json` - Cofre tipo barco hundido

**Ubicación:** `config/chestloot/loots/*.json`

**Namespace:** `chestloot:custom_mineshaft` (ejemplo)

**Inyección mediante reflexión:**
```java
Field tablesField = lootTables.getClass().getDeclaredField("tables");
tablesField.setAccessible(true);
Map<ResourceLocation, LootTable> tablesMap = (Map) tablesField.get(lootTables);
// Añadir custom loot tables al mapa
```

---

### **9. ChestLootCommand.java** - Comando del Servidor
**Ubicación:** `me.elb.prueba_del_cofre.commands.ChestLootCommand`

**Función:** Proporciona comandos de administración

**Comando disponible:**
```
/chestloot reload
```

**Requisitos:**
- Nivel de permisos: 2 (Operador)

**Funcionalidad:**
- **Recarga la configuración** sin reiniciar el servidor
- **Muestra mensaje de éxito** en verde
- **Muestra mensaje de error** en rojo si falla

**Uso:**
```
/chestloot reload
→ §e[ChestLoot] Configuration reloaded successfully!
→ §e[ChestLoot] Custom loot tables reloaded successfully!
```

---

## Flujo de Funcionamiento Completo

### **Escenario 1: Servidor Inicia**
```
1. Fabric carga el mod
2. onInitialize() ejecuta
3. ConfigLoader carga biome_loot_config.json
4. CustomLootTableManager carga archivos de config/chestloot/loots/
5. Comando /chestloot se registra
6. Servidor termina de iniciar
7. CustomLootTableManager inyecta loot tables personalizadas
8. Mod listo para usar
```

### **Escenario 2: Jugador Abre Cofre Natural (Primera Vez)**
```
1. Jugador hace clic derecho en cofre
2. ChestBlockEntityDataMixin intercepta startOpen()
3. ¿chestLoot$used == false? → Continuar
4. ¿Tiene lootTable ya asignada? → No, generar nueva
5. Obtener bioma: level.getBiome(pos) → minecraft:plains
6. ConfigLoader busca loot tables para minecraft:plains
7. Encuentra: [village_plains_house, simple_dungeon]
8. Selecciona aleatoriamente: simple_dungeon
9. ¿Es namespace "chestloot"? → No, es vanilla
10. chest.setLootTable(simple_dungeon, randomSeed)
11. chestLoot$used = true
12. Guardar estado en NBT
13. Cofre se abre con items generados
```

### **Escenario 3: Jugador Abre Cofre con Loot Custom**
```
1. Jugador abre cofre
2. Bioma detectado: minecraft:desert
3. Loot table seleccionada: chestloot:custom_bastion
4. ¿Namespace == "chestloot"? → Sí
5. CustomLootTableManager.getLootTable(custom_bastion)
6. Crear LootContext con parámetros del cofre
7. customLoot.fill(chest, context)
8. Items aparecen instantáneamente en el cofre
9. Marcar como usado
```

### **Escenario 4: Jugador Coloca Cofre (Survival)**
```
1. Jugador coloca cofre en el mundo
2. ChestBlockPlaceMixin intercepta setPlacedBy()
3. ¿Es ServerLevel? → Sí
4. ¿Es ServerPlayer? → Sí
5. ¿Modo de juego == Survival/Adventure? → Sí
6. chestLoot$setUsed(true)
7. chest.setChanged()
8. Cofre marcado como usado (no generará loot)
```

### **Escenario 5: Admin Recarga Configuración**
```
1. Admin ejecuta: /chestloot reload
2. ChestLootCommand.reload() ejecuta
3. ConfigLoader.loadConfig() vuelve a leer JSON
4. biomeLootMap se actualiza
5. Mensaje de éxito se muestra
6. Nuevos cofres usarán la configuración actualizada
```

---

## Archivos de Configuración

### **1. prueba_del_cofre.mixins.json**
```json
{
  "package": "me.elb.prueba_del_cofre.mixin",
  "compatibilityLevel": "JAVA_17",
  "mixins": [
    "ChestBlockEntityDataMixin",
    "ChestBlockPlaceMixin",
    "RandomizableContainerBlockEntityAccessor"
  ]
}
```

### **2. biome_loot_config.json** (Generado automáticamente)
**Ubicación:** `config/chestloot/biome_loot_config.json`

```json
{
  "biomes": {
    "default": {
      "biome_ids": [],
      "loot_tables": []
    },
    "plains_chests": {
      "biome_ids": [
        "minecraft:plains",
        "minecraft:sunflower_plains"
      ],
      "loot_tables": [
        "minecraft:chests/village/village_plains_house",
        "minecraft:chests/simple_dungeon"
      ]
    }
  }
}
```

---

## Instalación y Configuración

### **Requisitos Previos**
- Java 17 o superior
- Servidor Minecraft con Fabric Loader
- Fabric API instalada


## ⚙️ Configuración Avanzada

### **Añadir Nuevos Biomas**

Edita `config/chestloot/biome_loot_config.json`:

```json
{
  "biomes": {
    "jungle_chests": {
      "biome_ids": [
        "minecraft:jungle",
        "minecraft:bamboo_jungle"
      ],
      "loot_tables": [
        "minecraft:chests/jungle_temple",
        "chestloot:custom_jungle"
      ]
    }
  }
}
```

### **Crear Loot Table Personalizada**

Crea `config/chestloot/loots/custom_jungle.json`:

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": 3,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "minecraft:emerald",
          "weight": 10,
          "functions": [
            {
              "function": "minecraft:set_count",
              "count": {
                "min": 1,
                "max": 5
              }
            }
          ]
        }
      ]
    }
  ]
}
```

### **Recargar Configuración**

```bash
/chestloot reload
```

## Tecnologías Utilizadas

- **Fabric Mod Loader** - Framework de modding
- **Mixin** - Inyección de código en runtime
- **Brigadier** - Sistema de comandos
- **Gson** - Serialización JSON
- **Java Reflection** - Inyección de loot tables


## Licencia

All Rights Reserved - Ver `LICENSE.txt`

---

## Autor

**ELBGG** 
- [GitHub](https://github.com/ELBGG)
- [X](https://x.com/ELBGG_)
- [Youtube](https://www.youtube.com/@MamboPost)

---

## Enlaces Útiles

- [Documentación Fabric](https://fabricmc.net/wiki/)
- [Loot Table Generator](https://misode.github.io/loot-table/?version=1.20)
- [Loot Table Wiki](https://minecraft.wiki/w/Loot_table)
- [Mixin Wiki](https://github.com/SpongePowered/Mixin/wiki)
- [Mappings](https://linkie.shedaniel.me/mappings?namespace=yarn&version=1.19.2&search=)
```
