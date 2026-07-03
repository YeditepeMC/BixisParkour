package net.yeditepemc.bixisparkour.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tek bir parkur: sıralı checkpoint listesi ve koşu kayıtları.
 */
public class Parkour {

    private final String id;
    private String name;
    private Checkpoint start;
    private boolean active;
    private final List<Checkpoint> checkpoints;
    private final List<RunRecord> records;

    public Parkour(String id, String name) {
        this.id = id;
        this.name = name;
        this.checkpoints = new ArrayList<>();
        this.records = new ArrayList<>();
    }

    /**
     * Başlangıç checkpoint'i (setstart ile ayarlanır, üzerine basılınca parkur
     * başlar). Progression checkpoint listesinden ayrı tutulur; index = -1.
     */
    public Checkpoint getStart() {
        return start;
    }

    public void setStart(Checkpoint start) {
        this.start = start;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /** Parkur oynanabilir mi? setend ile true olur. */
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Checkpoint> getCheckpoints() {
        return checkpoints;
    }

    public List<RunRecord> getRecords() {
        return records;
    }

    /** Bitiş olarak işaretlenmiş checkpoint (yoksa null). */
    public Checkpoint getEndCheckpoint() {
        for (Checkpoint cp : checkpoints) {
            if (cp.isEnd()) {
                return cp;
            }
        }
        return checkpoints.isEmpty() ? null : checkpoints.get(checkpoints.size() - 1);
    }

    /** index'e göre checkpoint (yoksa null). */
    public Checkpoint getCheckpoint(int index) {
        if (index < 0 || index >= checkpoints.size()) {
            return null;
        }
        return checkpoints.get(index);
    }

    /**
     * Yeni bir koşu kaydı ekler. Her zaman eklenir — mükerrer/kişisel-en-iyi
     * kontrolü YAPILMAZ; aynı oyuncu birden fazla kez listeye girebilir.
     */
    public void addRecord(UUID uuid, String playerName, long timeMillis) {
        records.add(new RunRecord(uuid, playerName, timeMillis, System.currentTimeMillis()));
    }

    /**
     * En hızlı n kaydı döner (süreye göre artan sıralı).
     */
    public List<RunRecord> getTopRecords(int n) {
        return records.stream()
                .sorted(Comparator.comparingLong(RunRecord::getTimeMillis))
                .limit(n)
                .collect(Collectors.toList());
    }
}
