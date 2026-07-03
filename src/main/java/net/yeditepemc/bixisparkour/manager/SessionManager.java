package net.yeditepemc.bixisparkour.manager;

import net.yeditepemc.bixisparkour.model.ParkourSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Aktif oyuncu oturumlarını yönetir. Bir oyuncu aynı anda yalnızca bir
 * parkurda olabilir.
 */
public class SessionManager {

    private final Map<UUID, ParkourSession> sessions = new HashMap<>();

    /** Oyuncu için yeni bir oturum başlatır (varsa öncekini değiştirir). */
    public ParkourSession start(UUID player, String parkourId) {
        ParkourSession session = new ParkourSession(player, parkourId);
        sessions.put(player, session);
        return session;
    }

    public ParkourSession get(UUID player) {
        return sessions.get(player);
    }

    public boolean hasSession(UUID player) {
        return sessions.containsKey(player);
    }

    /** Oturumu sonlandırır ve döndürür (yoksa null). */
    public ParkourSession end(UUID player) {
        return sessions.remove(player);
    }
}
