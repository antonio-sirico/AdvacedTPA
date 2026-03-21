package me.siryq.advancedtpa;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ParticleEffect {
    private final AdvancedTPA plugin;

    public ParticleEffect(AdvancedTPA plugin) {
        this.plugin = plugin;
    }

    /**
     * Genera particelle attorno a un giocatore.
     * @param p Il giocatore interessato.
     * @param configPath Il percorso nel config (es. "particles.teleport-success").
     */
    public void spawn(Player p, String configPath) {
        if (p == null || !p.isOnline()) return;

        String particleName = plugin.getConfig().getString(configPath);
        if (particleName == null || particleName.isEmpty()) return;

        try {
            // In 1.21.11 usiamo Particle.valueOf() o Registry.PARTICLE_TYPES
            // Nota: valueOf è ancora lo standard sicuro per gli Enum delle particelle
            Particle particle = Particle.valueOf(particleName.toUpperCase());

            int amount = plugin.getConfig().getInt("particles.amount", 20);
            double offset = plugin.getConfig().getDouble("particles.offset", 0.5);

            // Spawna le particelle attorno al corpo del player (altezza occhi/centro)
            Location loc = p.getLocation().add(0, 1, 0);

            p.getWorld().spawnParticle(particle, loc, amount, offset, offset, offset, 0.1);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Particella non valida nel config: " + particleName);
        }
    }
}