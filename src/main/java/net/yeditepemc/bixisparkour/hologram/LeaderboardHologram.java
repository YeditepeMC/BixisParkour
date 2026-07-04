package net.yeditepemc.bixisparkour.hologram;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import net.yeditepemc.bixisparkour.model.Checkpoint;
import net.yeditepemc.bixisparkour.model.Parkour;
import net.yeditepemc.bixisparkour.model.RunRecord;
import net.yeditepemc.bixisparkour.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * DecentHolograms entegrasyonu.
 * <p>
 * İki tür hologram yönetir:
 * <ul>
 *   <li>Marker'lar (start / checkpoint / bitiş) — ilgili plate üzerinde,
 *       komut ile otomatik oluşturulur.</li>
 *   <li>Leaderboard — {@code bixisparkour_lb_<id>} — sadece
 *       {@code /parkour placeleaderboard} ile oyuncunun konumuna konur ve
 *       rekor kırılınca güncellenir.</li>
 * </ul>
 * DecentHolograms yüklü değilse tüm işlemler sessizce atlanır (softdepend).
 */
public class LeaderboardHologram {

    private static final int TOP = 5;
    /** Marker hologramlar (start / checkpoint / bitiş) için Y ofseti. */
    private static final double MARKER_Y = 1.2;
    /** Leaderboard hologramı için Y ofseti. */
    private static final double LEADERBOARD_Y = 3.5;

    private final Plugin plugin;
    private final boolean enabled;

    public LeaderboardHologram(Plugin plugin) {
        this.plugin = plugin;
        Plugin dh = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        this.enabled = dh != null && dh.isEnabled();
        if (enabled) {
            plugin.getLogger().info("DecentHolograms bulundu, hologramlar aktif.");
        } else {
            plugin.getLogger().info("DecentHolograms bulunamadı, hologramlar devre dışı.");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ---------------------------------------------------------------------
    // Hologram isimleri
    // ---------------------------------------------------------------------

    private static String startName(String id) {
        return "bixisparkour_start_" + id;
    }

    private static String checkpointName(String id, int index) {
        return "bixisparkour_cp_" + id + "_" + index;
    }

    private static String endName(String id) {
        return "bixisparkour_end_" + id;
    }

    private static String leaderboardName(String id) {
        return "bixisparkour_lb_" + id;
    }

    // ---------------------------------------------------------------------
    // Marker hologramları
    // ---------------------------------------------------------------------

    /** Başlangıç plate'i üzerinde "BAŞLA!" hologramı. */
    public void placeStart(Parkour parkour) {
        if (!enabled || parkour.getStart() == null) {
            return;
        }
        List<String> lines = List.of(
                "&6&l" + parkour.getName(),
                "&a▶ Parkura Başla!");
        createOrReplace(startName(parkour.getId()), markerLoc(parkour.getStart()), lines);
    }

    /** Checkpoint plate'i üzerinde checkpoint hologramı. */
    public void placeCheckpoint(Parkour parkour, Checkpoint checkpoint) {
        if (!enabled) {
            return;
        }
        List<String> lines = List.of("&e⬛ Checkpoint #" + checkpoint.getIndex());
        createOrReplace(checkpointName(parkour.getId(), checkpoint.getIndex()),
                markerLoc(checkpoint), lines);
    }

    /** Bitiş plate'i üzerinde "BİTİŞ" hologramı. */
    public void placeEnd(Parkour parkour) {
        if (!enabled) {
            return;
        }
        Checkpoint end = parkour.getEndCheckpoint();
        if (end == null) {
            return;
        }
        createOrReplace(endName(parkour.getId()), markerLoc(end), List.of(
                "&6&l" + parkour.getName(),
                "&c&l✦ BİTİŞ ✦"));
    }

    // ---------------------------------------------------------------------
    // Leaderboard
    // ---------------------------------------------------------------------

    /** Leaderboard hologramını verilen konuma koyar (varsa taşır). */
    public void placeLeaderboard(Parkour parkour, Location location) {
        if (!enabled) {
            return;
        }
        createOrReplace(leaderboardName(parkour.getId()),
                location.clone().add(0, LEADERBOARD_Y, 0), buildLeaderboardLines(parkour));
    }

    /** Var olan leaderboard hologramının satırlarını günceller (yoksa dokunmaz). */
    public void updateLeaderboard(Parkour parkour) {
        if (!enabled) {
            return;
        }
        try {
            Hologram hologram = DHAPI.getHologram(leaderboardName(parkour.getId()));
            if (hologram != null) {
                DHAPI.setHologramLines(hologram, buildLeaderboardLines(parkour));
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Leaderboard güncellenemedi (" + parkour.getId() + "): " + t.getMessage());
        }
    }

    /** Leaderboard hologramını kaldırır. */
    public boolean removeLeaderboard(String parkourId) {
        return removeHologram(leaderboardName(parkourId));
    }

    // ---------------------------------------------------------------------
    // Toplu kaldırma
    // ---------------------------------------------------------------------

    /** Bir parkura ait tüm hologramları kaldırır (parkur silinince). */
    public void removeAll(Parkour parkour) {
        if (!enabled) {
            return;
        }
        removeHologram(startName(parkour.getId()));
        removeHologram(endName(parkour.getId()));
        removeHologram(leaderboardName(parkour.getId()));
        for (Checkpoint cp : parkour.getCheckpoints()) {
            removeHologram(checkpointName(parkour.getId(), cp.getIndex()));
        }
    }

    // ---------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------

    private List<String> buildLeaderboardLines(Parkour parkour) {
        List<String> lines = new ArrayList<>();
        lines.add("&6&l⏱ " + parkour.getName());
        lines.add("&8&m──────────────");

        List<RunRecord> best = parkour.getTopRecords(TOP);
        if (best.isEmpty()) {
            lines.add("&8(Henüz rekor yok)");
            return lines;
        }

        int rank = 1;
        for (RunRecord record : best) {
            String playerName = record.getPlayerName() != null ? record.getPlayerName() : "Bilinmiyor";
            lines.add("&e#" + rank + " &f" + playerName + " &7- &a" + TimeFormatter.format(record.getTimeMillis()));
            rank++;
        }
        return lines;
    }

    private static Location markerLoc(Checkpoint cp) {
        return cp.getLocation().clone().add(0.5, MARKER_Y, 0.5);
    }

    /** Hologram varsa kaldırıp yeniden oluşturur (konum + satır güncellemesi için). */
    private void createOrReplace(String name, Location loc, List<String> lines) {
        if (loc.getWorld() == null) {
            return;
        }
        try {
            if (DHAPI.getHologram(name) != null) {
                DHAPI.removeHologram(name);
            }
            // saveToFile=true → config (holograms/<name>.yml) oluşturulur ve
            // createHologram içeride save() çağırıp diske yazar (restart'ta kalır).
            Hologram hologram = DHAPI.createHologram(name, loc, true, lines);
            if (hologram != null) {
                hologram.setDefaultVisibleState(true);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Hologram oluşturulamadı (" + name + "): " + t.getMessage());
        }
    }

    private boolean removeHologram(String name) {
        if (!enabled) {
            return false;
        }
        try {
            if (DHAPI.getHologram(name) != null) {
                DHAPI.removeHologram(name);
                return true;
            }
        } catch (Throwable ignored) {
            // sessizce atla
        }
        return false;
    }
}
