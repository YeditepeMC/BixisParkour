package net.yeditepemc.bixisparkour;

import com.yeditepemc.bixiscore.api.BixisCoreAPI;
import net.yeditepemc.bixisnavigator.api.NavigatorAPI;
import net.yeditepemc.bixisparkour.command.ParkourAdminCommand;
import net.yeditepemc.bixisparkour.command.ParkourPlayCommand;
import net.yeditepemc.bixisparkour.hologram.LeaderboardHologram;
import net.yeditepemc.bixisparkour.listener.ParkourListener;
import net.yeditepemc.bixisparkour.manager.ParkourManager;
import net.yeditepemc.bixisparkour.manager.SessionManager;
import net.yeditepemc.bixisparkour.model.Parkour;
import net.yeditepemc.bixisparkour.model.ParkourSession;
import net.yeditepemc.bixisparkour.storage.ParkourStorage;
import net.yeditepemc.bixisparkour.util.Msg;
import net.yeditepemc.bixisparkour.util.ParkourItems;
import net.yeditepemc.bixisparkour.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * BixisParkour — YeditepeMC lobi parkur sistemi.
 */
public final class BixisParkourPlugin extends JavaPlugin {

    private ParkourStorage storage;
    private ParkourManager parkourManager;
    private SessionManager sessionManager;
    private LeaderboardHologram leaderboardHologram;

    /** Başlangıç plate'i spam'ini önlemek için kısa süreli cooldown (60 tick). */
    private final Set<UUID> startCooldown = new HashSet<>();

    /** Bu oturumda günlük ilk-parkur bonusunu almış oyuncular (quit'te sıfırlanır). */
    private final Set<UUID> claimedDailyParkourBonus = new HashSet<>();

    private final Random random = new Random();

    @Override
    public void onEnable() {
        ParkourItems.init(this);

        this.storage = new ParkourStorage(this);
        this.sessionManager = new SessionManager();

        // DecentHolograms (softdepend) entegrasyonu — manager'dan önce hazır olmalı.
        this.leaderboardHologram = new LeaderboardHologram(this);
        this.parkourManager = new ParkourManager(this, storage);

        // Verileri yükle (dünyalar bu noktada hazır).
        parkourManager.load();

        // Event dinleyici.
        getServer().getPluginManager().registerEvents(new ParkourListener(this), this);

        // Komutlar.
        registerCommand("parkour", new ParkourAdminCommand(this));
        registerCommand("p", new ParkourPlayCommand(this));

        getLogger().info("BixisParkour etkinlestirildi (" + parkourManager.getParkours().size() + " parkur).");
    }

    @Override
    public void onDisable() {
        // Aktif oturumları temizle (timer görevleri sunucu kapanınca zaten iptal olur).
        if (parkourManager != null) {
            parkourManager.saveAll();
        }
        getLogger().info("BixisParkour devre disi birakildi.");
    }

    // ---------------------------------------------------------------------
    // Oyun akışı: başlat / bitir
    // ---------------------------------------------------------------------

    /**
     * Başlangıç plate'ine basıldığında çağrılır. Cooldown ve oturum kontrolü
     * yapar, uygunsa parkuru başlatır.
     */
    public void handleStartPlate(Player player, Parkour parkour) {
        // Parkur aktif değilse sessizce yoksay (bitiş ayarlanana kadar oynanamaz).
        if (!parkour.isActive()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        if (startCooldown.contains(uuid)) {
            return;
        }
        if (sessionManager.hasSession(uuid)) {
            Msg.prefixed(player, "&cZaten bir parkurdasin!");
            return;
        }
        startCooldown.add(uuid);
        startParkour(player, parkour);
        // 3 saniye (60 tick) sonra cooldown'ı kaldır.
        getServer().getScheduler().runTaskLater(this, () -> startCooldown.remove(uuid), 60L);
    }

    /**
     * Parkur başlatma sırası (setstart plate'ine basılınca çağrılır).
     * Oyuncu zaten bir parkurdaysa çağrılmamalıdır (kontrol handleStartPlate'de).
     */
    public void startParkour(Player player, Parkour parkour) {
        String name = player.getName();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gmenu reset all " + name);

        NavigatorAPI nav = getNavigatorAPI();
        if (nav != null) {
            nav.disableNav(player);
        }

        ParkourItems.give(player);

        ParkourSession session = sessionManager.start(player.getUniqueId(), parkour.getId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        startActionBarTimer(player, session);

        Msg.prefixed(player, "&a" + parkour.getName() + " &7parkuru basladi! Basarilar.");
    }

    /**
     * Parkuru sonlandırır (leave veya bitiş). Hotbar item'larını kaldırır,
     * bixisnav'ı geri açar, timer'ı iptal eder ve oturumu siler.
     *
     * @return sonlandırılan oturum, oyuncu parkurda değilse null
     */
    public ParkourSession stopParkour(Player player) {
        ParkourSession session = sessionManager.get(player.getUniqueId());
        if (session == null) {
            return null;
        }
        cancelActionBarTimer(session);
        ParkourItems.remove(player);

        NavigatorAPI nav = getNavigatorAPI();
        if (nav != null) {
            nav.enableNav(player);
        }

        sessionManager.end(player.getUniqueId());
        return session;
    }

    /**
     * Parkur bitirince ödül verir (submitTime/record işlendikten sonra çağrılır).
     * Günün ilk parkuru: 150 XP + rastgele kozmetik kasası. Sonraki parkurlar:
     * 15 XP + %25 ihtimalle kozmetik kasası (tier 1/2 rastgele).
     */
    public void grantFinishRewards(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();

        if (!claimedDailyParkourBonus.contains(uuid)) {
            claimedDailyParkourBonus.add(uuid);
            giveXP(player, 150);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gmysterybox give " + name + " 1");
            Msg.send(player, "&6✦ &eİlk günlük parkur bonusu! &a+150 XP &6+ Kozmetik Kasası!");
        } else {
            giveXP(player, 15);
            if (random.nextInt(4) == 0) {
                int tier = random.nextBoolean() ? 1 : 2;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gmysterybox give " + name + " 1 " + tier);
                Msg.send(player, "&b✦ &fParkur tamamlandı! &a+15 XP &b+ Kozmetik Kasası!");
            } else {
                Msg.send(player, "&7✦ &fParkur tamamlandı! &a+15 XP");
            }
        }
    }

    /** BixisCore ServicesManager üzerinden XP ekler (BixisCore yoksa sessizce atlar). */
    private void giveXP(Player player, long amount) {
        RegisteredServiceProvider<BixisCoreAPI> rsp =
                getServer().getServicesManager().getRegistration(BixisCoreAPI.class);
        if (rsp == null) {
            return;
        }
        rsp.getProvider().addXP(player, amount);
    }

    /** /p leave veya BARRIER item mantığı: parkurdan çıkar ve mesaj gönderir. */
    public void leaveParkour(Player player) {
        ParkourSession session = stopParkour(player);
        if (session == null) {
            Msg.prefixed(player, "&cSu an bir parkurda degilsin.");
        } else {
            Msg.prefixed(player, "&7Parkurdan ayrildin.");
        }
    }

    /** /p checkpoint veya FEATHER item mantığı: son geçilen noktaya ışınlar. */
    public void teleportToLastCheckpoint(Player player) {
        ParkourSession session = sessionManager.get(player.getUniqueId());
        if (session == null) {
            Msg.prefixed(player, "&cSu an bir parkurda degilsin.");
            return;
        }
        Parkour parkour = parkourManager.getParkour(session.getParkourId());
        if (parkour == null) {
            stopParkour(player);
            Msg.prefixed(player, "&cParkur artik mevcut degil.");
            return;
        }
        int idx = session.getCurrentCheckpointIndex();
        var target = idx == ParkourSession.AT_START
                ? parkour.getStart()
                : parkour.getCheckpoint(idx);
        if (target == null || target.getLocation() == null) {
            Msg.prefixed(player, "&cIsinlanacak checkpoint bulunamadi.");
            return;
        }
        Location dest = target.getLocation().clone().add(0.5, 1.0, 0.5);
        dest.setYaw(target.getYaw());
        dest.setPitch(target.getPitch());
        player.teleport(dest);
        String label = idx == ParkourSession.AT_START ? "baslangica" : ("checkpoint #" + idx + "'e");
        Msg.prefixed(player, "&aSon " + label + " isinlandin.");
    }

    /** Quit sırasında hafif temizlik (nav toggle yok — oyuncu çevrimdışı). */
    public void cleanupOnQuit(Player player) {
        // Günlük bonus takibini sıfırla (oturum olsun olmasın).
        claimedDailyParkourBonus.remove(player.getUniqueId());

        ParkourSession session = sessionManager.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        cancelActionBarTimer(session);
        ParkourItems.remove(player);
        sessionManager.end(player.getUniqueId());
    }

    // ---------------------------------------------------------------------
    // ActionBar timer
    // ---------------------------------------------------------------------

    private void startActionBarTimer(Player player, ParkourSession session) {
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !sessionManager.hasSession(player.getUniqueId())) {
                    cancel();
                    return;
                }
                ParkourSession current = sessionManager.get(player.getUniqueId());
                player.sendActionBar(Msg.color("&e⏱ &f" + TimeFormatter.formatShort(current.getElapsed())));
            }
        };
        task.runTaskTimer(this, 0L, 20L);
        session.setTimerTaskId(task.getTaskId());
    }

    private void cancelActionBarTimer(ParkourSession session) {
        if (session.getTimerTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(session.getTimerTaskId());
            session.setTimerTaskId(-1);
        }
    }

    // ---------------------------------------------------------------------
    // Yardımcı
    // ---------------------------------------------------------------------

    /**
     * BixisNavigator'ın ServicesManager'a kayıtlı NavigatorAPI sağlayıcısını
     * döner (yoksa null — softdepend).
     */
    private NavigatorAPI getNavigatorAPI() {
        RegisteredServiceProvider<NavigatorAPI> rsp =
                getServer().getServicesManager().getRegistration(NavigatorAPI.class);
        return rsp != null ? rsp.getProvider() : null;
    }

    /** Parkur item'larını işaretleyen PDC anahtarı. */
    public NamespacedKey getParkourItemKey() {
        return ParkourItems.getKey();
    }

    private void registerCommand(String name, Object handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Komut plugin.yml'de tanimli degil: " + name);
            return;
        }
        if (handler instanceof org.bukkit.command.CommandExecutor executor) {
            command.setExecutor(executor);
        }
        if (handler instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    public ParkourManager getParkourManager() {
        return parkourManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public LeaderboardHologram getLeaderboardHologram() {
        return leaderboardHologram;
    }
}
