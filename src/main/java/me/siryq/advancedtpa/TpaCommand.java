package me.siryq.advancedtpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final AdvancedTPA plugin;
    private final TeleportManager teleportManager;
    // Mappe per le richieste: Destinatario -> Mittente
    private final HashMap<UUID, UUID> tpaRequests = new HashMap<>();
    private final HashMap<UUID, UUID> tpaHereRequests = new HashMap<>();

    public TpaCommand(AdvancedTPA plugin, TeleportManager teleportManager) {
        this.plugin = plugin;
        this.teleportManager = teleportManager;
    }

    // Metodo che gestisce tutti i comandi del TPA
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ColorUtils.format(plugin, plugin.getMessage("only-players")));
            return true;
        }

        String cmd = command.getName().toLowerCase();

        if (!p.hasPermission("advancedtpa." + cmd)) {
            p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("no-permission")));
            return true;
        }

        switch (cmd) {
            case "tpa", "tpahere" -> {
                if (args.length == 0) {
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage(cmd + "-usage")));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("player-not-found")));
                    return true;
                }

                if (target.equals(p)) {
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("no-self-tpa")));
                    return true;
                }

                UUID targetUUID = target.getUniqueId();
                UUID senderUUID = p.getUniqueId();

                if (cmd.equals("tpa")) {
                    tpaRequests.put(targetUUID, senderUUID);
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-sent").replace("%player%", target.getName())));
                    sendClickableRequest(target, p.getName(), "request-received");
                } else {
                    tpaHereRequests.put(targetUUID, senderUUID);
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("tpahere-sent").replace("%player%", target.getName())));
                    sendClickableRequest(target, p.getName(), "tpahere-received");
                }

                plugin.getSoundEffect().play(target, "sounds.request-received");
                startTimeoutTask(targetUUID, p.getName(), cmd.equals("tpa"));
            }

            case "tpaccept" -> {
                if (tpaRequests.containsKey(p.getUniqueId())) {
                    UUID requesterUUID = tpaRequests.remove(p.getUniqueId());
                    Player requester = Bukkit.getPlayer(requesterUUID);
                    if (requester != null) {
                        p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-accepted").replace("%player%", requester.getName())));
                        plugin.getSoundEffect().play(p, "sounds.request-accepted");
                        teleportManager.executeTeleport(requester, p);
                    }
                } else if (tpaHereRequests.containsKey(p.getUniqueId())) {
                    UUID requesterUUID = tpaHereRequests.remove(p.getUniqueId());
                    Player requester = Bukkit.getPlayer(requesterUUID);
                    if (requester != null) {
                        p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-accepted").replace("%player%", requester.getName())));
                        plugin.getSoundEffect().play(p, "sounds.request-accepted");
                        teleportManager.executeTeleport(p, requester);
                    }
                } else {
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("no-pending-requests")));
                }
            }

            case "tpadeny" -> {
                UUID requesterID = tpaRequests.containsKey(p.getUniqueId()) ?
                        tpaRequests.remove(p.getUniqueId()) : tpaHereRequests.remove(p.getUniqueId());

                if (requesterID != null) {
                    Player req = Bukkit.getPlayer(requesterID);
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-denied-target")
                            .replace("%player%", req != null ? req.getName() : "Player")));

                    if (req != null && req.isOnline()) {
                        req.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-denied-sender").replace("%player%", p.getName())));
                        plugin.getSoundEffect().play(req, "sounds.request-denied");
                        plugin.getParticleEffect().spawn(req, "particles.request-denied");
                    }
                    plugin.getSoundEffect().play(p, "sounds.request-denied");
                } else {
                    p.sendMessage(ColorUtils.format(plugin, plugin.getMessage("no-request-to-deny")));
                }
            }
        }
        return true;
    }

    // Suggerimento nomi durante la scrittura del comando (Tab Completion)
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String cmd = command.getName().toLowerCase();
            if (cmd.equals("tpa") || cmd.equals("tpahere")) {
                List<String> completions = new ArrayList<>();
                String input = args[0].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(input) && !p.equals(sender)) {
                        completions.add(p.getName());
                    }
                }
                return completions;
            }
        }
        return new ArrayList<>();
    }

    // Metodo che gestisce il timeout del TPA
    private void startTimeoutTask(UUID targetUUID, String senderName, boolean isTpa) {
        int timeoutTicks = plugin.getConfig().getInt("timeout", 60) * 20;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isTpa) {
                    if (tpaRequests.containsKey(targetUUID)) {
                        tpaRequests.remove(targetUUID);
                        notifyTimeout(targetUUID, senderName);
                    }
                } else {
                    if (tpaHereRequests.containsKey(targetUUID)) {
                        tpaHereRequests.remove(targetUUID);
                        notifyTimeout(targetUUID, senderName);
                    }
                }
            }
        }.runTaskLater(plugin, timeoutTicks);
    }

    // Metodo che manda il messaggio di timeout
    private void notifyTimeout(UUID targetUUID, String senderName) {
        Player target = Bukkit.getPlayer(targetUUID);
        Player sender = Bukkit.getPlayer(senderName);
        if (target != null && target.isOnline()) {
            target.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-expired-target").replace("%player%", senderName)));
        }
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(ColorUtils.format(plugin, plugin.getMessage("request-expired-sender")));
        }
    }

    // Metodo che gestisce i messaggi cliccabili
    private void sendClickableRequest(Player target, String senderName, String langKey) {
        Component message = ColorUtils.format(plugin, plugin.getMessage(langKey).replace("%player%", senderName));

        Component acceptBtn = ColorUtils.formatSimple(plugin.getMessage("accept-button"))
                .clickEvent(ClickEvent.runCommand("/tpaccept"))
                .hoverEvent(HoverEvent.showText(ColorUtils.formatSimple(plugin.getMessage("accept-hover"))));

        Component denyBtn = ColorUtils.formatSimple(plugin.getMessage("deny-button"))
                .clickEvent(ClickEvent.runCommand("/tpadeny"))
                .hoverEvent(HoverEvent.showText(ColorUtils.formatSimple(plugin.getMessage("deny-hover"))));

        target.sendMessage(message);
        target.sendMessage(acceptBtn.append(Component.text("   ")).append(denyBtn));
    }
}