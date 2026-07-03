package net.yeditepemc.bixisparkour.manager;

import net.yeditepemc.bixisparkour.BixisParkourPlugin;
import net.yeditepemc.bixisparkour.hologram.LeaderboardHologram;
import net.yeditepemc.bixisparkour.model.Checkpoint;
import net.yeditepemc.bixisparkour.model.Parkour;
import net.yeditepemc.bixisparkour.model.ParkourSession;
import net.yeditepemc.bixisparkour.model.RunRecord;
import net.yeditepemc.bixisparkour.storage.ParkourStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Parkurları, checkpoint'leri ve rekorları yöneten çekirdek sınıf.
 * Basılan pressure plate koordinatlarından checkpoint'e O(1) erişim için
 * blok bazlı bir index (plateIndex) tutar.
 */
public class ParkourManager {

    /** Başlangıç plate'ini işaret eden özel index. */
    public static final int START_INDEX = ParkourSession.AT_START;

    /** Bir blok koordinatının hangi parkur/checkpoint'e ait olduğunu tutar. */
    public record CheckpointRef(String parkourId, int index) {
        public boolean isStart() {
            return index == START_INDEX;
        }
    }

    private final BixisParkourPlugin plugin;
    private final ParkourStorage storage;
    private final Map<String, Parkour> parkours = new HashMap<>();
    private final Map<String, CheckpointRef> plateIndex = new HashMap<>();

    public ParkourManager(BixisParkourPlugin plugin, ParkourStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    private LeaderboardHologram holo() {
        return plugin.getLeaderboardHologram();
    }

    // ---------------------------------------------------------------------
    // Yükleme / kaydetme
    // ---------------------------------------------------------------------

    public void load() {
        parkours.clear();
        parkours.putAll(storage.load());
        rebuildIndex();
    }

    public void saveParkours() {
        storage.saveParkours(parkours);
    }

    public void saveRecords() {
        storage.saveRecords(parkours);
    }

    public void saveAll() {
        saveParkours();
        saveRecords();
    }

    private void rebuildIndex() {
        plateIndex.clear();
        for (Parkour parkour : parkours.values()) {
            if (parkour.getStart() != null) {
                plateIndex.put(key(parkour.getStart().getLocation()),
                        new CheckpointRef(parkour.getId(), START_INDEX));
            }
            for (Checkpoint cp : parkour.getCheckpoints()) {
                plateIndex.put(key(cp.getLocation()), new CheckpointRef(parkour.getId(), cp.getIndex()));
            }
        }
    }

    // ---------------------------------------------------------------------
    // Erişim
    // ---------------------------------------------------------------------

    public Parkour getParkour(String id) {
        return parkours.get(id.toLowerCase());
    }

    public boolean exists(String id) {
        return parkours.containsKey(id.toLowerCase());
    }

    public Collection<Parkour> getParkours() {
        return parkours.values();
    }

    /** Basılan bloğun kayıtlı bir checkpoint/başlangıç olup olmadığını O(1) sorgular. */
    public CheckpointRef lookup(Location location) {
        return plateIndex.get(key(location));
    }

    // ---------------------------------------------------------------------
    // Yönetim işlemleri
    // ---------------------------------------------------------------------

    /** Yeni parkur oluşturur. Zaten varsa false döner. */
    public boolean createParkour(String id, String name) {
        String key = id.toLowerCase();
        if (parkours.containsKey(key)) {
            return false;
        }
        parkours.put(key, new Parkour(key, name));
        saveParkours();
        return true;
    }

    /** Parkuru siler ve tüm hologramlarını kaldırır. */
    public boolean deleteParkour(String id) {
        Parkour removed = parkours.remove(id.toLowerCase());
        if (removed == null) {
            return false;
        }
        if (holo() != null) {
            holo().removeAll(removed);
        }
        rebuildIndex();
        saveAll();
        return true;
    }

    /**
     * Başlangıç plate'ini ayarlar (varsa değiştirir) ve start hologramını koyar.
     * yaw/pitch, ışınlanınca oyuncunun bakacağı yöndür.
     */
    public Checkpoint setStart(Parkour parkour, Location plateLocation, float yaw, float pitch) {
        if (parkour.getStart() != null) {
            plateIndex.remove(key(parkour.getStart().getLocation()));
        }
        Checkpoint cp = new Checkpoint(START_INDEX, normalize(plateLocation), false, yaw, pitch);
        parkour.setStart(cp);
        plateIndex.put(key(cp.getLocation()), new CheckpointRef(parkour.getId(), START_INDEX));
        saveParkours();
        if (holo() != null) {
            holo().placeStart(parkour);
        }
        return cp;
    }

    /**
     * Verilen konuma bir progression checkpoint'i ekler ve hologramını koyar.
     */
    public Checkpoint addCheckpoint(Parkour parkour, Location plateLocation, float yaw, float pitch) {
        int index = parkour.getCheckpoints().size();
        Checkpoint cp = new Checkpoint(index, normalize(plateLocation), false, yaw, pitch);
        parkour.getCheckpoints().add(cp);
        plateIndex.put(key(cp.getLocation()), new CheckpointRef(parkour.getId(), index));
        saveParkours();
        if (holo() != null) {
            holo().placeCheckpoint(parkour, cp);
        }
        return cp;
    }

    /**
     * Verilen konuma bitiş checkpoint'i ekler ve bitiş hologramını koyar.
     * Önceki bitiş işaretleri kaldırılır. (Leaderboard OTOMATİK oluşturulmaz.)
     */
    public Checkpoint setEnd(Parkour parkour, Location plateLocation, float yaw, float pitch) {
        for (Checkpoint cp : parkour.getCheckpoints()) {
            cp.setEnd(false);
        }
        int index = parkour.getCheckpoints().size();
        Checkpoint cp = new Checkpoint(index, normalize(plateLocation), true, yaw, pitch);
        parkour.getCheckpoints().add(cp);
        plateIndex.put(key(cp.getLocation()), new CheckpointRef(parkour.getId(), index));
        // Bitiş ayarlandı → parkur artık oynanabilir.
        parkour.setActive(true);
        saveParkours();
        if (holo() != null) {
            holo().placeEnd(parkour);
        }
        return cp;
    }

    /** Oyuncunun bu parkurdaki tüm kayıtlarını siler. */
    public boolean resetRecord(Parkour parkour, UUID player) {
        boolean removed = parkour.getRecords().removeIf(r -> player.equals(r.getUuid()));
        if (removed) {
            saveRecords();
            if (holo() != null) {
                holo().updateLeaderboard(parkour);
            }
        }
        return removed;
    }

    /**
     * Bir bitiş süresini kaydeder (HER ZAMAN eklenir — mükerrer kontrolü yok) ve
     * leaderboard hologramını günceller.
     *
     * @return bu süre parkurun yeni en iyisi (rekoru) ise true
     */
    public boolean submitTime(Parkour parkour, Player player, long millis) {
        long previousBest = parkour.getRecords().stream()
                .mapToLong(RunRecord::getTimeMillis)
                .min()
                .orElse(Long.MAX_VALUE);
        parkour.addRecord(player.getUniqueId(), player.getName(), millis);
        saveRecords();
        if (holo() != null) {
            holo().updateLeaderboard(parkour);
        }
        return millis < previousBest;
    }

    // ---------------------------------------------------------------------
    // Koordinat yardımcıları
    // ---------------------------------------------------------------------

    /** Konumu blok merkezine (tam sayı koordinatlara) indirger. */
    private static Location normalize(Location loc) {
        return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /** Blok bazlı benzersiz anahtar. */
    private static String key(Location loc) {
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "?";
        return world + ':' + loc.getBlockX() + ':' + loc.getBlockY() + ':' + loc.getBlockZ();
    }
}
