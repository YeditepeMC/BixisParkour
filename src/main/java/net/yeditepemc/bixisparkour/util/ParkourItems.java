package net.yeditepemc.bixisparkour.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Parkur sırasında oyuncuya verilen hotbar item'ları (BixisNavigator deseni).
 * Her item PersistentDataContainer ile "bixisparkour:item" anahtarı taşır;
 * değeri item tipini (CHECKPOINT / LEAVE) belirtir.
 */
public final class ParkourItems {

    public enum Type {
        CHECKPOINT, LEAVE
    }

    public static final int SLOT_CHECKPOINT = 0;
    public static final int SLOT_LEAVE = 8;

    private static NamespacedKey key;

    private ParkourItems() {
    }

    /** Plugin etkinleştiğinde çağrılır; PDC anahtarını hazırlar. */
    public static void init(Plugin plugin) {
        key = new NamespacedKey(plugin, "item"); // -> "bixisparkour:item"
    }

    /** Parkur item'larını işaretleyen PDC anahtarı ("bixisparkour:item"). */
    public static NamespacedKey getKey() {
        return key;
    }

    /** Parkur hotbar item'larını verir (slot 0 ve 8). */
    public static void give(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(SLOT_CHECKPOINT, checkpointItem());
        inv.setItem(SLOT_LEAVE, leaveItem());
        player.updateInventory();
    }

    /** Envanterdeki tüm parkur item'larını temizler. */
    public static void remove(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isParkourItem(contents[i])) {
                inv.setItem(i, null);
            }
        }
        player.updateInventory();
    }

    /** ItemStack bir parkur item'ı mı? */
    public static boolean isParkourItem(ItemStack item) {
        if (key == null || item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    /** Item tipini döner (parkur item'ı değilse null). */
    public static Type typeOf(ItemStack item) {
        if (!isParkourItem(item)) {
            return null;
        }
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String raw = pdc.get(key, PersistentDataType.STRING);
        try {
            return raw == null ? null : Type.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------------

    private static ItemStack checkpointItem() {
        return build(Material.FEATHER, Type.CHECKPOINT,
                "&bSon Checkpoint", "&7Son geçtiğin noktaya dön");
    }

    private static ItemStack leaveItem() {
        return build(Material.BARRIER, Type.LEAVE,
                "&cParkuru Terk Et", "&7Parkurdan çık");
    }

    private static ItemStack build(Material material, Type type, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(noItalic(Msg.color(name)));
        meta.lore(List.of(noItalic(Msg.color(lore))));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());
        item.setItemMeta(meta);
        return item;
    }

    private static Component noItalic(Component c) {
        return c.decoration(TextDecoration.ITALIC, false);
    }
}
