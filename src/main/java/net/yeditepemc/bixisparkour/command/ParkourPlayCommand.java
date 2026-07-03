package net.yeditepemc.bixisparkour.command;

import net.yeditepemc.bixisparkour.BixisParkourPlugin;
import net.yeditepemc.bixisparkour.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /p oyuncu komutu: leave / checkpoint.
 * <p>
 * Parkura başlama artık komutla değil, setstart plate'ine basarak yapılır.
 */
public class ParkourPlayCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("leave", "checkpoint");

    private final BixisParkourPlugin plugin;

    public ParkourPlayCommand(BixisParkourPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.prefixed(sender, "&cBu komut sadece oyuncular icindir.");
            return true;
        }
        if (!player.hasPermission("bixisparkour.play")) {
            Msg.prefixed(player, "&cBu komutu kullanma yetkin yok.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "leave" -> plugin.leaveParkour(player);
            case "checkpoint", "cp" -> plugin.teleportToLastCheckpoint(player);
            default -> sendHelp(player);
        }
        return true;
    }

    private void sendHelp(Player player) {
        Msg.prefixed(player, "&6Parkur Komutlari:");
        Msg.send(player, "&e/p leave &7- parkurdan cik");
        Msg.send(player, "&e/p checkpoint &7- son checkpoint'e isinlan");
        Msg.send(player, "&7Parkura baslamak icin baslangic plate'ine bas.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
