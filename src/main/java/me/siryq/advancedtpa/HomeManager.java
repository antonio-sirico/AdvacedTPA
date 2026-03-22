package me.siryq.advancedtpa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeManager {

    private final File dataFile;
    // Struttura: UUID Giocatore -> (Nome Casa -> Dati Posizione)
    private Map<UUID, Map<String, Map<String, Object>>> homesCache = new HashMap<>();

    public HomeManager(AdvancedTPA plugin) {
        this.dataFile = new File(plugin.getDataFolder(), "homes.dat");
        loadHomes();
    }

    // Logica del sethome
    public void setHome(UUID uuid, String homeName, Location loc) {
        Map<UUID, Map<String, Map<String, Object>>> data = homesCache;
        data.computeIfAbsent(uuid, k -> new HashMap<>());

        Map<String, Object> locMap = new HashMap<>();
        locMap.put("world", loc.getWorld().getName());
        locMap.put("x", loc.getX());
        locMap.put("y", loc.getY());
        locMap.put("z", loc.getZ());
        locMap.put("yaw", loc.getYaw());
        locMap.put("pitch", loc.getPitch());

        data.get(uuid).put(homeName.toLowerCase(), locMap);
        saveHomes();
    }


    public Location getHome(UUID uuid, String homeName) {
        if (!homesCache.containsKey(uuid)) return null;
        Map<String, Object> data = homesCache.get(uuid).get(homeName.toLowerCase());
        if (data == null) return null;

        return new Location(
                Bukkit.getWorld((String) data.get("world")),
                ((Number) data.get("x")).doubleValue(),
                ((Number) data.get("y")).doubleValue(),
                ((Number) data.get("z")).doubleValue(),
                ((Number) data.get("yaw")).floatValue(),
                ((Number) data.get("pitch")).floatValue()
        );
    }



    public boolean deleteHome(UUID uuid, String homeName) {
        if (homesCache.containsKey(uuid) && homesCache.get(uuid).containsKey(homeName.toLowerCase())) {
            homesCache.get(uuid).remove(homeName.toLowerCase());
            saveHomes();
            return true;
        }
        return false;
    }

    public int getHomeCount(UUID uuid) {
        return homesCache.containsKey(uuid) ? homesCache.get(uuid).size() : 0;
    }

    public Location getBedHome(Player player) {
        // Minecraft restituisce la posizione solo se il letto esiste ed è agibile
        Location bed = player.getBedSpawnLocation();

        if (bed == null) return null;

        // Opzionale: aggiungiamo 0.5 a X e Z per spawnare al centro del blocco e non sullo spigolo
        return bed.clone().add(0.5, 0.1, 0.5);
    }

    @SuppressWarnings("unchecked")
    private void loadHomes() {
        if (!dataFile.exists()) return;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            homesCache = (Map<UUID, Map<String, Map<String, Object>>>) ois.readObject();
        } catch (Exception e) {
            Bukkit.getLogger().warning("AdvancedTPA » Errore caricamento homes.dat");
        }
    }

    private void saveHomes() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(homesCache);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public java.util.Set<String> getPlayerHomeNames(UUID uuid) {
        if (homesCache.containsKey(uuid)) {
            return homesCache.get(uuid).keySet();
        }
        return java.util.Collections.emptySet();
    }
}