package net.bestemor.core;

import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.core.listener.ChatListener;
import net.bestemor.core.menu.MenuListener;
import net.bestemor.core.utils.UpdateChecker;
import net.bestemor.villagermarket.utils.TaskScheduler;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class CorePlugin extends JavaPlugin {

    private static MenuListener menuListener;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        menuListener = new MenuListener();
        getServer().getPluginManager().registerEvents(menuListener, this);

        ConfigManager.loadMappings(getResource("config_mappings.yml"));

        InputStream inputStream = getResource("config_" + VersionUtils.getMCVersion() + ".yml");
        String resourceName = "config_" + VersionUtils.getMCVersion();
        if (inputStream == null && VersionUtils.getMCVersion() < 13) {
            inputStream = getResource("config_legacy.yml");
            resourceName = "config_legacy";
        }
        resourceName = inputStream == null ? "config" : resourceName;

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            if (inputStream == null) {
                saveDefaultConfig();
            } else {
                try {
                    FileUtils.copyInputStreamToFile(Objects.requireNonNull(inputStream), configFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        ConfigManager.setConfig(getConfig());
        getConfig().options().copyDefaults(true);

        if (getLanguageFolder() != null) {
            ConfigManager.setLanguagesFolder(new File(getDataFolder(), getLanguageFolder()));
            ConfigManager.loadLanguages(this, getLanguages());
        }

        boolean autoUpdate = !getConfig().contains("auto_update") || getConfig().getBoolean("auto_update");
        if (enableAutoUpdate() && autoUpdate) {
            ConfigManager.updateConfig(this, resourceName);
        }

        if (getSpigotResourceID() != 0) {
            checkVersion();
        }

        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(chatListener, this);

        onPluginEnable();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (menuListener != null) {
            menuListener.closeAll();
        }
        if (!TaskScheduler.isFolia()) {
            Bukkit.getScheduler().cancelTasks(this);
        }
        onPluginDisable();
    }

    private void checkVersion() {
        final int resourceId = getSpigotResourceID();
        new UpdateChecker(this, resourceId).getVersion(version -> {
            String currentVersion = getDescription().getVersion();
            if (currentVersion.equalsIgnoreCase(version)) {
                return;
            }

            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "A new version of " + getDescription().getName() + " was found!");
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Latest version: " + ChatColor.GREEN + version);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Your version: " + ChatColor.RED + currentVersion);
            Bukkit.getConsoleSender().sendMessage(ChatColor.AQUA + "Get it here for the latest features and bug fixes: "
                    + ChatColor.YELLOW + "https://www.spigotmc.org/resources/" + resourceId + "/");
        });
    }

    protected abstract void onPluginEnable();

    public boolean enableAutoUpdate() {
        return true;
    }

    protected void onPluginDisable() {
    }

    protected String[] getLanguages() {
        return new String[0];
    }

    protected String getLanguageFolder() {
        return null;
    }

    protected int getSpigotResourceID() {
        return 0;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        ConfigManager.setConfig(getConfig());
        if (getLanguageFolder() != null) {
            ConfigManager.setLanguagesFolder(new File(getDataFolder(), getLanguageFolder()));
            ConfigManager.loadLanguages(this, getLanguages());
        }
        ConfigManager.clearCache();
    }

    public static MenuListener getMenuListener() {
        return menuListener;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}
