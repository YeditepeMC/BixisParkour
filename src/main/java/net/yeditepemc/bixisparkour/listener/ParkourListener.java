package net.yeditepemc.bixisparkour.listener;

import net.yeditepemc.bixisparkour.BixisParkourPlugin;
import net.yeditepemc.bixisparkour.manager.ParkourManager;
import net.yeditepemc.bixisparkour.manager.SessionManager;
import net.yeditepemc.bixisparkour.model.Checkpoint;
import net.yeditepemc.bixisparkour.model.Parkour;
import net.yeditepemc.bixisparkour.model.ParkourSession;
import net.yeditepemc.bixisparkour.util.Msg;
import net.yeditepemc.bixisparkour.util.ParkourItems;
import net.yeditepemc.bixisparkour.util.TimeFormatter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Parkur olay dinleyicileri.
 * <p>
 * Plate tespiti PlayerInteractEvent + Action.PHYSICAL üzerinden O(1) yapılır.
 * Ayrıca parkur hotbar item'larının kullanımı ve korunması burada işlenir.
 */
public class ParkourListener implements Listener {

    private final BixisParkourPlugin plugin;
    private final ParkourManager parkourManager;
    private final SessionManager sessionManager;

    public ParkourListener(BixisParkourPlugin plugin) {
        this.plugin = plugin;
        this.parkourManager = plugin.getParkourManager();
        this.sessionManager = plugin.getSessionManager();
    }

    // ---------------------------------------------------------------------
    // Plate + item etkileşimi
    // ---------------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            handlePlate(event);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            handleItem(event);
        }
    }

    private void handlePlate(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        // Basılan blok kayıtlı bir plate mi? O(1)
        ParkourManager.CheckpointRef ref = parkourManager.lookup(block.getLocation());
        if (ref == null) {
            return;
        }
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Parkour parkour = parkourManager.getParkour(ref.parkourId());
        if (parkour == null) {
            return;
        }
        ParkourSession session = sessionManager.get(uuid);

        // Başlangıç plate'i (cooldown + oturum kontrolü plugin'de).
        if (ref.isStart()) {
            plugin.handleStartPlate(player, parkour);
            return;
        }

        // Progression checkpoint / bitiş.
        if (session == null || !session.getParkourId().equals(ref.parkourId())) {
            return;
        }
        int expected = session.getCurrentCheckpointIndex() + 1;
        // Sıradaki checkpoint değilse (geri basma veya atlama) yoksay.
        if (ref.index() != expected) {
            return;
        }
        Checkpoint cp = parkour.getCheckpoint(ref.index());
        if (cp == null) {
            return;
        }
        session.setCurrentCheckpointIndex(ref.index());

        if (cp.isEnd()) {
            long time = session.getElapsed();
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            boolean record = parkourManager.submitTime(parkour, player, time);
            plugin.stopParkour(player);

            Msg.prefixed(player, "&a" + parkour.getName() + " &7parkurunu bitirdin!");
            Msg.prefixed(player, "&7Sure: &e" + TimeFormatter.format(time));
            if (record) {
                Msg.prefixed(player, "&6&l✦ Yeni rekor!");
            }
            plugin.grantFinishRewards(player);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            Msg.prefixed(player, "&aCheckpoint &e" + ref.index() + " &ageçildi! &7("
                    + TimeFormatter.format(session.getElapsed()) + ")");
        }
    }

    private void handleItem(PlayerInteractEvent event) {
        // Sadece ana el ile bir kez işle.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getItem();
        ParkourItems.Type type = ParkourItems.typeOf(item);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();

        // Oturumu olmayan biri item taşıyorsa temizle.
        if (!sessionManager.hasSession(player.getUniqueId())) {
            ParkourItems.remove(player);
            return;
        }

        switch (type) {
            case CHECKPOINT -> plugin.teleportToLastCheckpoint(player);
            case LEAVE -> plugin.leaveParkour(player);
        }
    }

    // ---------------------------------------------------------------------
    // Item koruması (BixisNavigator deseni)
    // ---------------------------------------------------------------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (ParkourItems.isParkourItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Sayı tuşu ile hotbar takası (number key swap) korumasi.
        if (event.getAction() == InventoryAction.HOTBAR_SWAP) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory()
                    .getItem(event.getHotbarButton());
            if (isParkourItem(event.getCurrentItem()) || isParkourItem(hotbarItem)) {
                event.setCancelled(true);
                return;
            }
        }
        if (ParkourItems.isParkourItem(event.getCurrentItem())
                || ParkourItems.isParkourItem(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        for (ItemStack item : event.getNewItems().values()) {
            if (ParkourItems.isParkourItem(item)) {
                event.setCancelled(true);
                return;
            }
        }
        if (ParkourItems.isParkourItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        ItemStack main = event.getMainHandItem();
        ItemStack off = event.getOffHandItem();
        if (isParkourItem(main) || isParkourItem(off)) {
            event.setCancelled(true);
        }
    }

    private boolean isParkourItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getParkourItemKey(), PersistentDataType.STRING);
    }

    // ---------------------------------------------------------------------
    // Çıkış
    // ---------------------------------------------------------------------

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Oyuncu parkurdayken çıkarsa oturumu ve item'ları temizle.
        plugin.cleanupOnQuit(event.getPlayer());
    }
}
