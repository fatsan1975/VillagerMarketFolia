package net.bestemor.villagermarket.command.subcommand;

import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.villagermarket.VMPlugin;
import net.bestemor.villagermarket.utils.TaskScheduler;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements ISubCommand {

    private final VMPlugin plugin;

    public ReloadCommand(VMPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        sender.sendMessage(ConfigManager.getMessage("messages.reloaded"));

        TaskScheduler.runAsync(plugin, plugin::saveLog);

        plugin.reloadConfiguration();
        plugin.getShopManager().reloadAll();

    }

    @Override
    public String getDescription() {
        return "Plugini yeniden yükler";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
