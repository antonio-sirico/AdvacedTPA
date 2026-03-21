package me.siryq.advancedtpa;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.logging.Level;

public class SoundEffect {
    private final AdvancedTPA plugin;

    public SoundEffect(AdvancedTPA plugin) {
        this.plugin = plugin;
    }

    public void play(Player p, String configPath) {
        if (p == null || !p.isOnline()) return;

        // Recuperiamo la stringa dal config
        String soundName = plugin.getConfig().getString(configPath);
        if (soundName == null || soundName.isEmpty()) return;

        try {
            // Puliamo la stringa: tutto minuscolo e rimuoviamo "minecraft:" se presente
            String cleanedKey = soundName.toLowerCase().trim().replace("minecraft:", "");
            NamespacedKey key = NamespacedKey.minecraft(cleanedKey);

            // Recuperiamo il suono dal Registro
            Sound sound = Registry.SOUNDS.get(key);

            if (sound != null) {
                float volume = (float) plugin.getConfig().getDouble("sounds.volume", 1.0);
                float pitch = (float) plugin.getConfig().getDouble("sounds.pitch", 1.0);

                // Riproduce il suono
                p.playSound(p.getLocation(), sound, volume, pitch);
            } else {
                plugin.getLogger().log(Level.WARNING, "[Sound] Il suono '{0}' non esiste nel registro 1.21.11!", soundName);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "[Sound] Formato chiave non valido nel config per: {0}", soundName);
        }
    }
}