package net.yeditepemc.bixisparkour.model;

import java.util.UUID;

/**
 * Bir oyuncunun aktif parkur oturumu.
 * <p>
 * currentCheckpointIndex = -1 → başlangıçta, henüz hiçbir checkpoint geçilmedi.
 * İlk progression checkpoint index'i 0'dır.
 */
public class ParkourSession {

    /** Henüz checkpoint geçilmediğini belirten değer. */
    public static final int AT_START = -1;

    private final UUID playerUUID;
    private final String parkourId;
    private int currentCheckpointIndex;
    private final long startTime;
    private int timerTaskId = -1;

    public ParkourSession(UUID playerUUID, String parkourId) {
        this.playerUUID = playerUUID;
        this.parkourId = parkourId;
        this.currentCheckpointIndex = AT_START;
        this.startTime = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getParkourId() {
        return parkourId;
    }

    public int getCurrentCheckpointIndex() {
        return currentCheckpointIndex;
    }

    public void setCurrentCheckpointIndex(int currentCheckpointIndex) {
        this.currentCheckpointIndex = currentCheckpointIndex;
    }

    public long getStartTime() {
        return startTime;
    }

    /** Oturum başından bu yana geçen süre (ms). */
    public long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }

    /** ActionBar timer görevinin id'si (-1 = yok). */
    public int getTimerTaskId() {
        return timerTaskId;
    }

    public void setTimerTaskId(int timerTaskId) {
        this.timerTaskId = timerTaskId;
    }
}
