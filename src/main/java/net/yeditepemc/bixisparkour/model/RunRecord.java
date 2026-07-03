package net.yeditepemc.bixisparkour.model;

import java.util.UUID;

/**
 * Bir parkur bitirme kaydı. Aynı oyuncu birden fazla kez kayıt oluşturabilir
 * (her koşu ayrı bir RunRecord'dur — kişisel en iyi ile sınırlı değil).
 */
public class RunRecord {

    private UUID uuid;
    private String playerName;
    private long timeMillis;
    private long timestamp;

    /** Gson için no-arg constructor. */
    public RunRecord() {
    }

    public RunRecord(UUID uuid, String playerName, long timeMillis, long timestamp) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.timeMillis = timeMillis;
        this.timestamp = timestamp;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
