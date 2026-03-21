package me.siryq.advancedtpa;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AdvancedTPA extends JavaPlugin {

    private FileConfiguration langConfig;
    private SoundEffect soundEffect;
    private ParticleEffect particleEffect;

    @Override
    public void onEnable() {
        // Inizializzazione file
        saveDefaultConfig();
        loadLang();

        // Inizializzazione classi helper
        this.soundEffect = new SoundEffect(this);
        this.particleEffect = new ParticleEffect(this);

        // Registrazione comandi
        TpaCommand tpaExecutor = new TpaCommand(this);
        getCommand("tpa").setExecutor(tpaExecutor);
        getCommand("tpahere").setExecutor(tpaExecutor);
        getCommand("tpaccept").setExecutor(tpaExecutor);
        getCommand("tpadeny").setExecutor(tpaExecutor);

        // Registrazione comando di reload
        getCommand("tpareload").setExecutor((sender, command, label, args) -> {
            if (!sender.hasPermission("advancedtpa.reload")) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage("no-permission")));
                return true;
            }
            reloadPluginConfig();
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getMessage("reload-success")));
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

    /**
     * Recupera il messaggio dal file lang.
     * Restituisce la stringa con i codici colore ancora in formato & (verranno puliti nel TpaCommand)
     */
    public String getMessage(String path) {
        return langConfig.getString(path, "&c[Messaggio mancante: " + path + "]");
    }

    /**
     * Recupera il prefisso configurato
     */
    public String getPrefix() {
        return langConfig.getString("prefix", "&b&lTPA &8» &r");
    }

    public void reloadPluginConfig() {
        reloadConfig(); // Ricarica config.yml
        loadLang();     // Ricarica il file di lingua
        // Re-inizializziamo gli effetti per sicurezza se avessimo cache (opzionale)
        this.soundEffect = new SoundEffect(this);
        this.particleEffect = new ParticleEffect(this);
        getLogger().info("Configurazione e messaggi ricaricati con successo!");
    }

    public SoundEffect getSoundEffect() {
        return soundEffect;
    }

    public ParticleEffect getParticleEffect() {
        return particleEffect;
    }

    private void pluginEnabledMessage() {
        getLogger().info("╾━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╼");
        getLogger().info(" AdvancedTPA abilitato correttamente! ");
        getLogger().info(" Versione: 1.21.11 - By Antonio        ");
        getLogger().info("╾━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╼");
    }
}