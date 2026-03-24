package me.siryq.advancedtpa;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AdvancedTPA extends JavaPlugin {

    private FileConfiguration langConfig;
    private SoundEffect soundEffect;
    private ParticleEffect particleEffect;
    private HomeManager homeManager;
    private TeleportManager teleportManager;

    @Override
    public void onEnable() {
        // Inizializzazione file
        saveDefaultConfig();
        loadLang();

        // Inizializzazione classi helper
        this.soundEffect = new SoundEffect(this);
        this.particleEffect = new ParticleEffect(this);
        this.homeManager = new HomeManager(this);
        this.teleportManager = new TeleportManager(this);

        // Registrazione comandi TPA
        TpaCommand tpaExecutor = new TpaCommand(this, teleportManager);
        getCommand("tpa").setExecutor(tpaExecutor);
        getCommand("tpahere").setExecutor(tpaExecutor);
        getCommand("tpaccept").setExecutor(tpaExecutor);
        getCommand("tpadeny").setExecutor(tpaExecutor);

        // Tab Completer per i nomi dei giocatori
        getCommand("tpa").setTabCompleter(tpaExecutor);
        getCommand("tpahere").setTabCompleter(tpaExecutor);

        // Registrazione comandi Home
        HomeCommand homeHandler = new HomeCommand(this, homeManager, teleportManager);
        getCommand("sethome").setExecutor(homeHandler);
        getCommand("home").setExecutor(homeHandler);
        getCommand("delhome").setExecutor(homeHandler);

        // Tab Completer per i nomi delle case
        getCommand("home").setTabCompleter(homeHandler);
        getCommand("delhome").setTabCompleter(homeHandler);

        // Registrazione comando di reload (Pulito con ColorUtils)
        getCommand("tpareload").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("advancedtpa.reload")) {
                sender.sendMessage(ColorUtils.format(this, getMessage("no-permission")));
                return true;
            }
            reloadPluginConfig();
            sender.sendMessage(ColorUtils.format(this, getMessage("reload-success")));
            return true;
        });

        pluginEnabledMessage();
    }

    public void loadLang() {
        String lang = getConfig().getString("language", "it");
        String fileName = "lang_" + lang + ".yml";
        File langFile = new File(getDataFolder(), fileName);

        if (!langFile.exists()) {
            saveResource(fileName, false);
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String path) {
        return langConfig.getString(path, "&c[Messaggio mancante: " + path + "]");
    }

    public String getPrefix() {
        return langConfig.getString("prefix", "&b&lTPA &8» &r");
    }

    public String getHomePrefix() { return langConfig.getString("prefix-home", "&6&lHOME &8» &r"); }

    public void reloadPluginConfig() {
        reloadConfig();
        loadLang();
        this.soundEffect = new SoundEffect(this);
        this.particleEffect = new ParticleEffect(this);
        // ! Nota: non ricarichiamo l'homeManager per non resettare la cache binaria bruscamente
        getLogger().info("Configurazione e messaggi ricaricati con successo!");
    }

    public SoundEffect getSoundEffect() { return soundEffect; }
    public ParticleEffect getParticleEffect() { return particleEffect; }

    private void pluginEnabledMessage() {
        org.bukkit.command.ConsoleCommandSender console = Bukkit.getConsoleSender();

        String version= getPluginMeta().getVersion();

        String[] banner = {
                "&b&l╾━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╼",
                "",
                " &b&lAdvancedTPA &a&labilitato correttamente! ",
                " &f&lVersione: " +version+ " - &4&lBy Antonio        ",
                "",
                "&b&l╾━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╼"
        };

        for (String line : banner) {
            console.sendMessage(ColorUtils.formatSimple(line));
        }
    }
}