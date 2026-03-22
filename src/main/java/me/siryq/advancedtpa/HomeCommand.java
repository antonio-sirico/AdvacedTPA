package me.siryq.advancedtpa;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.List;

public class HomeCommand implements CommandExecutor, TabCompleter {

    private final AdvancedTPA plugin;
    private final HomeManager homeManager;
    private final TeleportManager teleportManager; // Aggiunto per gestire il countdown universale

    public HomeCommand(AdvancedTPA plugin, HomeManager homeManager, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.teleportManager = teleportManager;
    }

    // Gestione dei comandi delle Home
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        String cmd = command.getName().toLowerCase();
        // Se non viene specificato un nome, il nome di default è "home"
        String homeName = (args.length > 0) ? args[0] : "home";

        switch (cmd) {
            case "sethome" -> {
                if (!p.hasPermission("advancedtpa.sethome")) {
                    p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("no-permission")));
                    return true;
                }

                int maxHomes = getMaxHomes(p);
                int currentHomes = homeManager.getHomeCount(p.getUniqueId());

                // Se la casa non esiste e ha già raggiunto il limite, blocca
                if (homeManager.getHome(p.getUniqueId(), homeName) == null && currentHomes >= maxHomes) {
                    p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-limit-reached").replace("%limit%", String.valueOf(maxHomes))));
                    return true;
                }

                homeManager.setHome(p.getUniqueId(), homeName, p.getLocation());
                p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-set").replace("%name%", homeName)));
            }

            case "home" -> {
                Location loc;
                String displayName; // Variabile per il nome da mostrare nel countdown

                // Gestione del LETTO con permesso dedicato
                if (homeName.equalsIgnoreCase("bed")) {
                    if (!p.hasPermission("advancedtpa.homebed")) {
                        p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("no-permission")));
                        return true;
                    }
                    loc = homeManager.getBedHome(p);
                    displayName = "Bed";
                    if (loc == null) {
                        p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-bed-missing")));
                        return true;
                    }
                }
                // Gestione HOME NORMALE
                else {
                    if (!p.hasPermission("advancedtpa.home")) {
                        p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("no-permission")));
                        return true;
                    }
                    loc = homeManager.getHome(p.getUniqueId(), homeName);
                    displayName = homeName;
                    if (loc == null) {
                        p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-not-found").replace("%name%", homeName)));
                        return true;
                    }
                }

                teleportManager.executeTeleport(p, loc, displayName);
            }

            case "delhome" -> {
                if (!p.hasPermission("advancedtpa.delhome")) {
                    p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("no-permission")));
                    return true;
                }
                if (homeManager.deleteHome(p.getUniqueId(), homeName)) {
                    p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-deleted").replace("%name%", homeName)));
                } else {
                    p.sendMessage(ColorUtils.formatHome(plugin, plugin.getMessage("home-not-found").replace("%name%", homeName)));
                }
            }
        }
        return true;
    }

    // Gestione dei permessi delle multi-house dinamicamente
    private int getMaxHomes(Player p) {
        if (p.isOp()) return 100;
        int max = 1; // Default
        for (PermissionAttachmentInfo permission : p.getEffectivePermissions()) {
            String perm = permission.getPermission().toLowerCase();
            if (perm.startsWith("advancedtpa.home.")) {
                try {
                    int value = Integer.parseInt(perm.replace("advancedtpa.home.", ""));
                    if (value > max) max = value;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    // Gestione del completamento dei comandi
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return new ArrayList<>();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            String cmd = command.getName().toLowerCase();

            // Se il comando è /home o /delhome, suggeriamo le sue case salvate
            if (cmd.equals("home") || cmd.equals("delhome")) {
                // Prendiamo i nomi delle case dal manager
                java.util.Set<String> playerHomes = homeManager.getPlayerHomeNames(p.getUniqueId());

                for (String homeName : playerHomes) {
                    if (homeName.startsWith(input)) {
                        completions.add(homeName);
                    }
                }

                // Se è il comando /home, aggiungiamo anche "bed" ai suggerimenti
                if (cmd.equals("home") && "bed".startsWith(input) && p.hasPermission("advancedtpa.homebed")) {
                    completions.add("bed");
                }

                return completions;
            }
        }
        return new ArrayList<>();
    }
}