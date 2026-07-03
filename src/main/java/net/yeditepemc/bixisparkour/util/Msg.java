package net.yeditepemc.bixisparkour.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * '&' renk kodlu metinleri Adventure Component'e çevirip gönderen küçük yardımcı.
 */
public final class Msg {

    private static final String PREFIX = "&8[&bParkur&8] &r";

    private Msg() {
    }

    public static Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static void send(CommandSender to, String text) {
        to.sendMessage(color(text));
    }

    public static void prefixed(CommandSender to, String text) {
        to.sendMessage(color(PREFIX + text));
    }
}
