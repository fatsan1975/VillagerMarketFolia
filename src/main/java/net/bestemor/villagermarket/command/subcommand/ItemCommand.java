package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemCommand implements ISubCommand {

    private final VMPlugin plugin;

    public ItemCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return switch (args.length) {
            case 2 -> Collections.singletonList("give");
            case 3 -> null;
            case 4, 5 -> Arrays.asList("infinite", "1", "2", "3", "4", "5", "6");
            default -> new ArrayList<>();
        };
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (args.length != 5 && args.length != 6) {
            sender.sendMessage(ChatColor.RED + "Hatalı argüman sayısı!");
            sender.sendMessage(ChatColor.RED + "/vm item give <player> <shopsize> <storagesize> [amount]");
            return;
        }
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Oyuncu bulunamadı: " + args[2]);
            return;
        }
        if ((!args[3].equals("infinite") && !args[4].equals("infinite")) && (!canConvert(args[3]) || !canConvert(args[4]))) {
            sender.sendMessage(ChatColor.RED + "Geçersiz boyut: " + args[3] + " veya " + args[4]);
            return;
        }
        int amount = 1;
        int shopSize = (args[3].equals("infinite") ? 0 : Integer.parseInt(args[3]));
        int storageSize = (args[4].equals("infinite") ? 0 : Integer.parseInt(args[4]));

        if (storageSize < 0 || storageSize > 6 || shopSize < 0 || shopSize > 6) {
            sender.sendMessage(ChatColor.RED + "Geçersiz dükkan/depo boyutu!");
            return;
        }
        if (args.length == 6) {
            if (!canConvert(args[5])) {
                sender.sendMessage(ChatColor.RED + "Geçersiz miktar: " + args[5]);
                return;
            }
            amount = Integer.parseInt(args[5]);
        }
        target.getInventory().addItem(plugin.getShopManager().getShopItem(plugin, shopSize, storageSize, amount));
        target.playSound(target.getLocation(), ConfigManager.getSound("sounds.give_shop_item"), 1, 1);
    }

    private boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getDescription() {
        return "Dükkan yumurtası verir";
    }

    @Override
    public String getUsage() {
        return "<oyuncu> <dukkan_boyutu> <depo_boyutu> [miktar]";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
