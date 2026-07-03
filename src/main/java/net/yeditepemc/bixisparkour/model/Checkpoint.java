package net.yeditepemc.bixisparkour.model;

import org.bukkit.Location;

/**
 * Bir parkurdaki tek bir checkpoint.
 * index=0 başlangıç, isEnd=true olan checkpoint bitiştir.
 * location, basılması gereken pressure plate'in blok konumudur.
 * yaw/pitch, checkpoint kaydedilirken admin'in bakış yönüdür; ışınlanınca
 * oyuncu bu yöne bakar.
 */
public class Checkpoint {

    private final int index;
    private final Location location;
    private boolean end;
    private float yaw;
    private float pitch;

    public Checkpoint(int index, Location location, boolean end) {
        this(index, location, end, 0f, 0f);
    }

    public Checkpoint(int index, Location location, boolean end, float yaw, float pitch) {
        this.index = index;
        this.location = location;
        this.end = end;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public int getIndex() {
        return index;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
