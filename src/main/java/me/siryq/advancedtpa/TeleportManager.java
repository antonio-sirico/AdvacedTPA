package me.siryq.advancedtpa;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Supplier;

public class TeleportManager {

    private final AdvancedTPA plugin;

    public TeleportManager(AdvancedTPA plugin) {
        this.plugin = plugin;
    }

    /**
     * Teletrasporto verso un altro giocatore (TPA)
     */
    public void executeTeleport(Player jumper, Player destination) {
        // Passiamo false perché NON è una home
        startCountdown(jumper, () -> destination.isOnline() ? destination.getLocation() : null, destination.getName(), false);
    }

    /**
     * Teletrasporto verso una Location fissa (Home)
     */
    public void executeTeleport(Player jumper, Location targetLoc, String destinationName) {
        // Passiamo true perché è una home
        startCountdown(jumper, () -> targetLoc, destinationName, true);
    }

    /**
     * IL CUORE DEL SISTEMA: Metodo privato che gestisce il countdown per tutti
     * @param jumper Il giocatore che deve teletrasportarsi
     * @param targetLocation Un "Supplier" che restituisce la posizione finale (utile se il target si muove)
     * @param targetName Il nome da mostrare nel messaggio finale
     * @param isHome Specifica se il teletrasporto è verso una casa o un giocatore
     */
    private void startCountdown(Player jumper, Supplier<Location> targetLocation, String targetName, boolean isHome) {
        Location startLoc = jumper.getLocation();
        int delay = plugin.getConfig().getInt("teleport-delay", 5);

        // Se il delay è 0, teletrasportiamo istantaneamente
        if (delay <= 0) {
            performFinalTeleport(jumper, targetLocation.get(), targetName, isHome);
            return;
        }

        new BukkitRunnable() {
            int timeLeft = delay;

            @Override
            public void run() {
                // 1. Controllo se il giocatore è ancora online
                if (!jumper.isOnline()) {
                    this.cancel();
                    return;
                }

                // 2. Controllo se la destinazione esiste ancora
                Location currentTarget = targetLocation.get();
                if (currentTarget == null) {
                    this.cancel();
                    return;
                }

                // 3. Controllo movimento (tolleranza 0.1 per piccoli respiri/rotazioni)
                if (jumper.getLocation().distanceSquared(startLoc) > 0.1) {
                    // APPLICAZIONE PREFISSO DINAMICO PER ANNULLAMENTO
                    if (isHome) {
                        jumper.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("teleport-cancelled-move")));
                    } else {
                        jumper.sendMessage(ColorUtils.format(plugin, plugin.getMessage("teleport-cancelled-move")));
                    }

                    plugin.getSoundEffect().play(jumper, "sounds.teleport-cancelled");
                    plugin.getParticleEffect().spawn(jumper, "particles.teleport-cancelled");
                    this.cancel();
                    return;
                }

                // 4. Gestione Countdown
                if (timeLeft > 0) {
                    // APPLICAZIONE PREFISSO DINAMICO PER COUNTDOWN
                    String msg = plugin.getMessage("teleport-countdown").replace("%seconds%", String.valueOf(timeLeft));

                    if (isHome) {
                        jumper.sendMessage(ColorUtils.formatHome(plugin, msg));
                    } else {
                        jumper.sendMessage(ColorUtils.format(plugin, msg));
                    }

                    plugin.getSoundEffect().play(jumper, "sounds.teleport-countdown");
                    plugin.getParticleEffect().spawn(jumper, "particles.teleport-countdown");
                    timeLeft--;
                } else {
                    // 5. Teletrasporto finale
                    performFinalTeleport(jumper, currentTarget, targetName, isHome);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private void performFinalTeleport(Player jumper, Location loc, String name, boolean isHome) {
        if (loc == null) return;
        jumper.teleport(loc);

        // Determiniamo se è una home o un player per scegliere il messaggio corretto
        if (isHome) {
            // Se è una home, usiamo il messaggio specifico "home-teleport" (placeholder %name%)
            jumper.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-teleport").replace("%name%", name)));
        } else {
            // Se è un player, usiamo il messaggio standard "teleporting" (placeholder %player%)
            jumper.sendMessage(ColorUtils.format(plugin, plugin.getMessage("teleporting").replace("%player%", name)));
        }

        plugin.getSoundEffect().play(jumper, "sounds.teleport-success");
        plugin.getParticleEffect().spawn(jumper, "particles.teleport-success");
    }
}