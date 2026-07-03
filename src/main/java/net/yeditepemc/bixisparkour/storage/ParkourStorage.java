package net.yeditepemc.bixisparkour.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.yeditepemc.bixisparkour.model.Checkpoint;
import net.yeditepemc.bixisparkour.model.Parkour;
import net.yeditepemc.bixisparkour.model.RunRecord;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Parkur ve rekor verilerinin JSON okuma/yazma katmanı.
 * - parkours.json : parkur yapısı (checkpoint'ler dahil)
 * - records.json  : oyuncu rekorları (parkourId -> uuid -> ms)
 * <p>
 * Location doğrudan serileştirilemediği için dünya adı + blok koordinatlarına
 * indirgenir.
 */
public class ParkourStorage {

    private final Plugin plugin;
    private final Gson gson;
    private final File parkoursFile;
    private final File recordsFile;

    public ParkourStorage(Plugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        File dir = plugin.getDataFolder();
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Veri klasörü oluşturulamadı: " + dir);
        }
        this.parkoursFile = new File(dir, "parkours.json");
        this.recordsFile = new File(dir, "records.json");
    }

    // ---------------------------------------------------------------------
    // JSON DTO'ları
    // ---------------------------------------------------------------------

    private static final class CheckpointDto {
        int index;
        String world;
        int x;
        int y;
        int z;
        float yaw;
        float pitch;
        boolean end;
    }

    private static final class ParkourDto {
        String id;
        String name;
        boolean active;
        CheckpointDto start;
        List<CheckpointDto> checkpoints = new ArrayList<>();
    }

    private static final class ParkoursFileDto {
        List<ParkourDto> parkours = new ArrayList<>();
    }

    private static final class RecordsFileDto {
        // parkourId -> koşu kayıtları listesi
        Map<String, List<RunRecord>> records = new LinkedHashMap<>();
    }

    // ---------------------------------------------------------------------
    // Yükleme
    // ---------------------------------------------------------------------

    /**
     * Tüm parkurları yükler ve rekorları içine yerleştirir.
     */
    public Map<String, Parkour> load() {
        Map<String, Parkour> result = new LinkedHashMap<>();

        ParkoursFileDto data = readJson(parkoursFile, ParkoursFileDto.class);
        if (data != null && data.parkours != null) {
            for (ParkourDto pd : data.parkours) {
                if (pd.id == null) {
                    continue;
                }
                Parkour parkour = new Parkour(pd.id, pd.name == null ? pd.id : pd.name);
                parkour.setActive(pd.active);
                if (pd.start != null) {
                    Location startLoc = toLocation(pd.start, pd.id);
                    if (startLoc != null) {
                        parkour.setStart(new Checkpoint(pd.start.index, startLoc, pd.start.end,
                                pd.start.yaw, pd.start.pitch));
                    }
                }
                if (pd.checkpoints != null) {
                    for (CheckpointDto cd : pd.checkpoints) {
                        Location loc = toLocation(cd, pd.id);
                        if (loc == null) {
                            continue;
                        }
                        parkour.getCheckpoints().add(new Checkpoint(cd.index, loc, cd.end, cd.yaw, cd.pitch));
                    }
                    parkour.getCheckpoints().sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
                }
                result.put(parkour.getId(), parkour);
            }
        }

        // Koşu kayıtlarını yerleştir
        RecordsFileDto recData = readJson(recordsFile, RecordsFileDto.class);
        if (recData != null && recData.records != null) {
            for (Map.Entry<String, List<RunRecord>> e : recData.records.entrySet()) {
                Parkour parkour = result.get(e.getKey());
                if (parkour == null || e.getValue() == null) {
                    continue;
                }
                for (RunRecord record : e.getValue()) {
                    if (record != null && record.getUuid() != null) {
                        parkour.getRecords().add(record);
                    }
                }
            }
        }

        return result;
    }

    // ---------------------------------------------------------------------
    // Kaydetme
    // ---------------------------------------------------------------------

    /** Parkur yapısını (checkpoint'ler) diske yazar. */
    public void saveParkours(Map<String, Parkour> parkours) {
        ParkoursFileDto data = new ParkoursFileDto();
        for (Parkour parkour : parkours.values()) {
            ParkourDto pd = new ParkourDto();
            pd.id = parkour.getId();
            pd.name = parkour.getName();
            pd.active = parkour.isActive();
            if (parkour.getStart() != null) {
                pd.start = toDto(parkour.getStart());
            }
            for (Checkpoint cp : parkour.getCheckpoints()) {
                pd.checkpoints.add(toDto(cp));
            }
            data.parkours.add(pd);
        }
        writeJson(parkoursFile, data);
    }

    /** Koşu kayıtlarını diske yazar. */
    public void saveRecords(Map<String, Parkour> parkours) {
        RecordsFileDto data = new RecordsFileDto();
        for (Parkour parkour : parkours.values()) {
            if (parkour.getRecords().isEmpty()) {
                continue;
            }
            data.records.put(parkour.getId(), new ArrayList<>(parkour.getRecords()));
        }
        writeJson(recordsFile, data);
    }

    // ---------------------------------------------------------------------
    // Yardımcılar
    // ---------------------------------------------------------------------

    private Location toLocation(CheckpointDto cd, String parkourId) {
        World world = Bukkit.getWorld(cd.world);
        if (world == null) {
            plugin.getLogger().warning("Parkur '" + parkourId + "' checkpoint " + cd.index
                    + " için dünya bulunamadı: " + cd.world);
            return null;
        }
        return new Location(world, cd.x, cd.y, cd.z);
    }

    private CheckpointDto toDto(Checkpoint cp) {
        CheckpointDto cd = new CheckpointDto();
        cd.index = cp.getIndex();
        Location loc = cp.getLocation();
        cd.world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        cd.x = loc.getBlockX();
        cd.y = loc.getBlockY();
        cd.z = loc.getBlockZ();
        cd.yaw = cp.getYaw();
        cd.pitch = cp.getPitch();
        cd.end = cp.isEnd();
        return cd;
    }

    private <T> T readJson(File file, Class<T> type) {
        if (!file.exists()) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "JSON okunamadı: " + file.getName(), ex);
            return null;
        }
    }

    private void writeJson(File file, Object data) {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "JSON yazılamadı: " + file.getName(), ex);
        }
    }
}
