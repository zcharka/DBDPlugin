package pl.dbd.generator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.dbd.DBDPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GeneratorManager {

    private final DBDPlugin plugin;
    private final File genFile;
    private FileConfiguration data;
    
    // Mapa: NazwaMapy -> Lista Generatorów
    private Map<String, List<Generator>> mapGenerators = new HashMap<>();
    
    // Lista aktywnych generatorów (na obecnej mapie)
    private List<Generator> activeGenerators = new ArrayList<>();
    
    // Śledzimy nazwę obecnej mapy, żeby wiedzieć co dodawać
    private String currentMapName = null;

    public GeneratorManager(DBDPlugin plugin) {
        this.plugin = plugin;
        this.genFile = new File(plugin.getDataFolder(), "generators.yml");
        load();
    }

    public void load() {
        if (!genFile.exists()) {
            try { genFile.createNewFile(); } catch (IOException ignored) {}
        }
        data = YamlConfiguration.loadConfiguration(genFile);
        
        mapGenerators.clear();
        activeGenerators.clear();
        currentMapName = null;
        
        // Ładowanie z podziałem na mapy
        if (data.contains("maps")) {
            ConfigurationSection mapsSection = data.getConfigurationSection("maps");
            for (String mapName : mapsSection.getKeys(false)) {
                List<Generator> gens = new ArrayList<>();
                ConfigurationSection genSection = mapsSection.getConfigurationSection(mapName + ".generators");
                
                if (genSection != null) {
                    for (String key : genSection.getKeys(false)) {
                        String locStr = genSection.getString(key + ".location");
                        Location loc = parseLocation(locStr);
                        if (loc != null) {
                            Generator gen = new Generator(loc, null); 
                            gens.add(gen);
                        }
                    }
                }
                mapGenerators.put(mapName, gens);
            }
        }
        plugin.getLogger().info("[Generators] Załadowano mapy: " + mapGenerators.keySet());
    }

    public void save() {
        data.set("maps", null); // Czyścimy sekcję map przed zapisem
        
        for (Map.Entry<String, List<Generator>> entry : mapGenerators.entrySet()) {
            String mapName = entry.getKey();
            List<Generator> gens = entry.getValue();
            
            for (int i = 0; i < gens.size(); i++) {
                Generator gen = gens.get(i);
                Location loc = gen.getLocation();
                String path = "maps." + mapName + ".generators." + i + ".location";
                data.set(path, loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ());
            }
        }
        try { data.save(genFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void activateMap(String mapName) {
        activeGenerators.clear(); // Czyścimy listę aktywnych
        currentMapName = mapName; // Ustawiamy obecną mapę
        
        if (mapGenerators.containsKey(mapName)) {
            List<Generator> gens = mapGenerators.get(mapName);
            for (Generator gen : gens) {
                gen.setProgress(0);
                gen.setCompleted(false);
                activeGenerators.add(gen);
            }
            plugin.getLogger().info("Aktywowano generatory dla mapy: " + mapName);
        } else {
            plugin.getLogger().warning("Brak generatorów dla mapy: " + mapName);
        }
    }
    
    public void deactivateMap() {
        activeGenerators.clear();
        currentMapName = null;
    }

    public void deactivateAll() {
        activeGenerators.clear();
        currentMapName = null;
    }
    
    public void resetAllGenerators() {
        for (Generator gen : activeGenerators) {
            gen.setProgress(0);
            gen.setCompleted(false);
            // Tu można dodać ewentualny reset wyglądu (np. zmiana bloku na zgaszony)
        }
    }

    // Komenda: /generator add <NazwaMapy>
    public void createGenerator(Player player, String mapName) {
        Location loc = player.getTargetBlock(null, 5).getLocation();
        
        mapGenerators.computeIfAbsent(mapName, k -> new ArrayList<>());
        
        // Sprawdzamy czy już tu nie ma gena (na dowolnej mapie, żeby nie było kolizji)
        for (List<Generator> gens : mapGenerators.values()) {
             for (Generator g : gens) {
                 if (g.getLocation().getBlock().equals(loc.getBlock())) {
                     player.sendMessage("§cTu już jest generator (może na innej mapie)!");
                     return;
                 }
             }
        }

        Generator newGen = new Generator(loc, null);
        mapGenerators.get(mapName).add(newGen);
        
        // POPRAWKA: Dodajemy do aktywnych TYLKO jeśli ta mapa jest obecnie załadowana
        if (mapName.equalsIgnoreCase(currentMapName)) {
            activeGenerators.add(newGen);
            player.sendMessage("§aGenerator dodany i widoczny (bo mapa " + mapName + " jest aktywna).");
        } else {
            player.sendMessage("§aGenerator zapisany do mapy: §e" + mapName);
            player.sendMessage("§7(Nie widać go teraz, bo załadowana jest inna mapa lub żadna).");
        }
        
        save();
    }

    public boolean removeGenerator(Location location) {
        boolean removedAny = false;
        
        // Przeszukujemy wszystkie mapy
        for (List<Generator> gens : mapGenerators.values()) {
            Iterator<Generator> it = gens.iterator();
            while (it.hasNext()) {
                Generator gen = it.next();
                if (gen.getLocation().getBlock().equals(location.getBlock())) {
                    it.remove();
                    // Jeśli był aktywny, też usuwamy
                    activeGenerators.remove(gen);
                    removedAny = true;
                }
            }
        }
        
        if (removedAny) {
            save();
            return true;
        }
        return false;
    }

    public Generator getGeneratorAt(Location location) {
        // Sprawdzamy tylko aktywne (dla obecnej mapy)
        for (Generator gen : activeGenerators) {
            if (gen.getLocation().getBlock().equals(location.getBlock())) {
                return gen;
            }
        }
        return null;
    }
    
    public Generator getGeneratorByEntity(org.bukkit.entity.Entity entity) {
        return null;
    }

    public List<Generator> getGenerators() { return activeGenerators; }
    public int getCompletedCount() { return (int) activeGenerators.stream().filter(Generator::isCompleted).count(); }
    public int getTotalCount() { return activeGenerators.size(); }
    public String getCurrentMapName() { return currentMapName; }

    private Location parseLocation(String s) {
        if (s == null) return null;
        String[] p = s.split(",");
        if (p.length < 4) return null;
        try {
            return new Location(Bukkit.getWorld(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (Exception e) { return null; }
    }
}