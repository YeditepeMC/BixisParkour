package net.yeditepemc.bixisparkour.command;

import net.yeditepemc.bixisparkour.BixisParkourPlugin;
import net.yeditepemc.bixisparkour.manager.ParkourManager;
import net.yeditepemc.bixisparkour.model.Checkpoint;
import net.yeditepemc.bixisparkour.model.Parkour;
import net.yeditepemc.bixisparkour.model.RunRecord;
import net.yeditepemc.bixisparkour.util.Msg;
import net.yeditepemc.bixisparkour.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * /parkour admin komutu.
 */
public class ParkourAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "create", "delete", "setstart", "addcheckpoint", "setend",
            "placeleaderboard", "removehologram", "list", "info", "tp", "reset");

    /** İlk argümanı <id> olan alt komutlar (tab tamamlama için). */
    private static final Set<String> ID_FIRST = Set.of(
            "delete", "setstart", "addcheckpoint", "setend",
            "placeleaderboard", "removehologram", "info", "tp");

    private final BixisParkourPlugin plugin;
    private final ParkourManager parkourManager;

    public ParkourAdminCommand(BixisParkourPlugin plugin) {
        this.plugin = plugin;
        this.parkourManager = plugin.getParkourManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bixisparkour.admin")) {
            Msg.prefixed(sender, "&cBu komutu kullanma yetkin yok.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setstart" -> handleSetStart(sender, args);
            case "addcheckpoint" -> handleAddCheckpoint(sender, args, false);
            case "setend" -> handleAddCheckpoint(sender, args, true);
            case "placeleaderboard" -> handlePlaceLeaderboard(sender, args);
            case "removehologram" -> handleRemoveHologram(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "tp" -> handleTp(sender, args);
            case "reset" -> handleReset(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.prefixed(sender, "&cKullanim: /parkour create <id> <isim>");
            return;
        }
        String id = args[1];
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (parkourManager.createParkour(id, name)) {
            Msg.prefixed(sender, "&aParkur olusturuldu: &e" + id + " &7(" + name + ")");
        } else {
            Msg.prefixed(sender, "&cBu id ile bir parkur zaten var: " + id);
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.prefixed(sender, "&cKullanim: /parkour delete <id>");
            return;
        }
        if (parkourManager.deleteParkour(args[1])) {
            Msg.prefixed(sender, "&aParkur silindi: &e" + args[1]);
        } else {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
        }
    }

    private void handleSetStart(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.prefixed(sender, "&cBu komut sadece oyuncular icindir.");
            return;
        }
        if (args.length < 2) {
            Msg.prefixed(sender, "&cKullanim: /parkour setstart <id>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[1]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
            return;
        }
        Block plate = findPressurePlate(player);
        if (plate == null) {
            Msg.prefixed(sender, "&cBir pressure plate uzerinde durmalisin.");
            return;
        }
        Location pl = player.getLocation();
        parkourManager.setStart(parkour, plate.getLocation(), pl.getYaw(), pl.getPitch());
        Msg.prefixed(sender, "&aBaslangic plate'i ayarlandi. &7Oyuncular buraya basinca parkur baslar.");
    }

    private void handleAddCheckpoint(CommandSender sender, String[] args, boolean end) {
        if (!(sender instanceof Player player)) {
            Msg.prefixed(sender, "&cBu komut sadece oyuncular icindir.");
            return;
        }
        if (args.length < 2) {
            Msg.prefixed(sender, "&cKullanim: /parkour " + (end ? "setend" : "addcheckpoint") + " <id>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[1]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
            return;
        }
        Block plate = findPressurePlate(player);
        if (plate == null) {
            Msg.prefixed(sender, "&cBir pressure plate uzerinde durmalisin.");
            return;
        }
        Location loc = plate.getLocation();
        Location pl = player.getLocation();
        if (end) {
            Checkpoint cp = parkourManager.setEnd(parkour, loc, pl.getYaw(), pl.getPitch());
            Msg.prefixed(sender, "&aBitis checkpoint'i eklendi &7(index &e" + cp.getIndex() + "&7).");
        } else {
            Checkpoint cp = parkourManager.addCheckpoint(parkour, loc, pl.getYaw(), pl.getPitch());
            Msg.prefixed(sender, "&aCheckpoint eklendi &7(index &e" + cp.getIndex() + "&7).");
        }
    }

    private void handlePlaceLeaderboard(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.prefixed(sender, "&cBu komut sadece oyuncular icindir.");
            return;
        }
        if (args.length < 2) {
            Msg.prefixed(sender, "&cKullanim: /parkour placeleaderboard <id>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[1]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
            return;
        }
        if (plugin.getLeaderboardHologram() == null || !plugin.getLeaderboardHologram().isEnabled()) {
            Msg.prefixed(sender, "&cDecentHolograms yuklu degil, hologram konulamaz.");
            return;
        }
        plugin.getLeaderboardHologram().placeLeaderboard(parkour, player.getLocation());
        Msg.prefixed(sender, "&aLeaderboard hologrami buraya konuldu: &e" + parkour.getId());
    }

    private void handleRemoveHologram(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.prefixed(sender, "&cKullanim: /parkour removehologram <id>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[1]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
            return;
        }
        boolean removed = plugin.getLeaderboardHologram() != null
                && plugin.getLeaderboardHologram().removeLeaderboard(parkour.getId());
        if (removed) {
            Msg.prefixed(sender, "&aLeaderboard hologrami kaldirildi: &e" + parkour.getId());
        } else {
            Msg.prefixed(sender, "&cBu parkurun leaderboard hologrami yok.");
        }
    }

    private void handleList(CommandSender sender) {
        if (parkourManager.getParkours().isEmpty()) {
            Msg.prefixed(sender, "&7Hic parkur yok.");
            return;
        }
        Msg.prefixed(sender, "&6Parkurlar:");
        for (Parkour p : parkourManager.getParkours()) {
            String startTag = p.getStart() != null ? "&a✔" : "&c✘";
            String activeTag = p.isActive() ? "&aaktif" : "&cpasif";
            Msg.send(sender, "&8- &e" + p.getId() + " &7(" + p.getName() + ") &8| " + activeTag
                    + " &8| baslangic:" + startTag + " &7" + p.getCheckpoints().size()
                    + " checkpoint, " + p.getRecords().size() + " kayit");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Msg.prefixed(sender, "&cKullanim: /parkour info <id>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[1]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
            return;
        }
        Msg.prefixed(sender, "&6" + parkour.getName() + " &7(" + parkour.getId() + ")");
        if (parkour.getStart() != null) {
            Location l = parkour.getStart().getLocation();
            Msg.send(sender, "&7Baslangic: &f" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
        } else {
            Msg.send(sender, "&7Baslangic: &cayarlanmadi");
        }
        Msg.send(sender, "&7Checkpoint'ler:");
        for (Checkpoint cp : parkour.getCheckpoints()) {
            Location l = cp.getLocation();
            String tag = cp.isEnd() ? " &c(bitis)" : "";
            Msg.send(sender, "&8#" + cp.getIndex() + " &f" + l.getBlockX() + ", " + l.getBlockY()
                    + ", " + l.getBlockZ() + tag);
        }
        Msg.send(sender, "&7Rekorlar (en iyi 10):");
        List<RunRecord> best = parkour.getTopRecords(10);
        if (best.isEmpty()) {
            Msg.send(sender, "&8- yok");
        } else {
            int rank = 1;
            for (RunRecord r : best) {
                String n = r.getPlayerName() != null ? r.getPlayerName() : r.getUuid().toString();
                Msg.send(sender, "&8#" + rank + " &f" + n + " &7- &a" + TimeFormatter.format(r.getTimeMillis()));
                rank++;
            }
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.prefixed(sender, "&cBu komut sadece oyuncular icindir.");
            return;
        }
        if (args.length < 3) {
            Msg.prefixed(sender, "&cKullanim: /parkour tp <id> <checkpoint_no|start>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[1]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[1]);
            return;
        }
        Checkpoint target;
        if (args[2].equalsIgnoreCase("start")) {
            target = parkour.getStart();
            if (target == null) {
                Msg.prefixed(sender, "&cBu parkurun baslangici yok.");
                return;
            }
        } else {
            int index;
            try {
                index = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                Msg.prefixed(sender, "&cGecersiz checkpoint numarasi: " + args[2]);
                return;
            }
            target = parkour.getCheckpoint(index);
            if (target == null) {
                Msg.prefixed(sender, "&cBu index'te checkpoint yok: " + index);
                return;
            }
        }
        Location dest = target.getLocation().clone().add(0.5, 1.0, 0.5);
        dest.setYaw(target.getYaw());
        dest.setPitch(target.getPitch());
        player.teleport(dest);
        Msg.prefixed(sender, "&aIsinlandin.");
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.prefixed(sender, "&cKullanim: /parkour reset <oyuncu> <id>");
            return;
        }
        Parkour parkour = parkourManager.getParkour(args[2]);
        if (parkour == null) {
            Msg.prefixed(sender, "&cParkur bulunamadi: " + args[2]);
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (parkourManager.resetRecord(parkour, target.getUniqueId())) {
            Msg.prefixed(sender, "&a" + args[1] + " oyuncusunun &e" + parkour.getId() + " &arekoru sifirlandi.");
        } else {
            Msg.prefixed(sender, "&cBu oyuncunun bu parkurda rekoru yok.");
        }
    }

    private void sendHelp(CommandSender sender) {
        Msg.prefixed(sender, "&6Parkur Admin Komutlari:");
        Msg.send(sender, "&e/parkour create <id> <isim> &7- yeni parkur");
        Msg.send(sender, "&e/parkour delete <id> &7- parkur sil");
        Msg.send(sender, "&e/parkour setstart <id> &7- baslangic plate'i");
        Msg.send(sender, "&e/parkour addcheckpoint <id> &7- checkpoint ekle");
        Msg.send(sender, "&e/parkour setend <id> &7- bitis isaretle");
        Msg.send(sender, "&e/parkour placeleaderboard <id> &7- leaderboard hologrami koy");
        Msg.send(sender, "&e/parkour removehologram <id> &7- leaderboard hologrami kaldir");
        Msg.send(sender, "&e/parkour list &7- parkurlari listele");
        Msg.send(sender, "&e/parkour info <id> &7- detay/rekorlar");
        Msg.send(sender, "&e/parkour tp <id> <no> &7- checkpoint'e isinlan");
        Msg.send(sender, "&e/parkour reset <oyuncu> <id> &7- rekor sifirla");
    }

    /**
     * Oyuncunun üzerinde durduğu pressure plate'i bulur.
     * <p>
     * Plate ya ayak bloğunda ya da 1 altındadır; PlayerInteractEvent'in
     * bildirdiği blok ile birebir eşleşmesi için gerçek plate bloğunu tespit
     * ederiz.
     */
    private Block findPressurePlate(Player player) {
        Block feet = player.getLocation().getBlock();
        if (isPressurePlate(feet)) {
            return feet;
        }
        Block below = feet.getRelative(BlockFace.DOWN);
        if (isPressurePlate(below)) {
            return below;
        }
        return null;
    }

    private boolean isPressurePlate(Block block) {
        return block.getType().name().endsWith("_PRESSURE_PLATE");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("bixisparkour.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && ID_FIRST.contains(args[0].toLowerCase())) {
            return parkourIds(args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
            return parkourIds(args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> parkourIds(String prefix) {
        return parkourManager.getParkours().stream()
                .map(Parkour::getId)
                .filter(id -> id.startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
