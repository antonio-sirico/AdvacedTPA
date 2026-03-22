package me.siryq.advancedtpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    /**
     * Formatta un messaggio aggiungendo il prefisso del plugin.
     */
    public static Component format(AdvancedTPA plugin, String text) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getPrefix() + text);
    }

    /**
     * Formatta un messaggio con il prefisso HOME dedicato.
     */
    public static Component formatHome(AdvancedTPA plugin, String text) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(plugin.getHomePrefix() + text);
    }

    /**
     * Formatta un messaggio senza aggiungere il prefisso.
     */
    public static Component formatSimple(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * Converte i codici colore & in § per la console o log vecchi.
     */
    public static String toLegacy(String text) {
        return text.replace("&", "§");
    }
}