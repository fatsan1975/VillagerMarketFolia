package net.bestemor.core.utils;

import net.bestemor.villagermarket.utils.TaskScheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class UpdateChecker {

    private final JavaPlugin plugin;
    private final int resourceId;

    public UpdateChecker(JavaPlugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void getVersion(Consumer<String> consumer) {
        TaskScheduler.runAsync(plugin, () -> {
            try (Scanner scanner = new Scanner(new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openStream())) {
                if (scanner.hasNext()) {
                    consumer.accept(scanner.next());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[BestemorCore] Cannot look for updates:");
                e.printStackTrace();
            }
        });
    }
}
