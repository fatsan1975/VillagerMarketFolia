package net.bestemor.core.listener;

import net.bestemor.core.CorePlugin;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.utils.Utils;
import net.bestemor.villagermarket.utils.TaskScheduler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatListener implements Listener {

    private final Map<UUID, Consumer<String>> stringListeners = new ConcurrentHashMap<>();
    private final Map<UUID, Consumer<BigDecimal>> decimalListeners = new ConcurrentHashMap<>();

    private String cancelInput;
    private final CorePlugin plugin;

    public ChatListener(CorePlugin plugin) {
        this.plugin = plugin;
        this.cancelInput = ConfigManager.getString("cancel");
    }

    public void setCancelInput(String cancelInput) {
        this.cancelInput = cancelInput;
    }

    public void addStringListener(Player player, Consumer<String> consumer) {
        player.sendMessage(ConfigManager.getMessage("messages.type_cancel").replace("%cancel%", cancelInput));
        stringListeners.put(player.getUniqueId(), consumer);
    }

    public void addDecimalListener(Player player, Consumer<BigDecimal> consumer) {
        player.sendMessage(ConfigManager.getMessage("messages.type_cancel").replace("%cancel%", cancelInput));
        decimalListeners.put(player.getUniqueId(), consumer);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!stringListeners.containsKey(uuid) && !decimalListeners.containsKey(uuid)) {
            return;
        }

        event.setCancelled(true);

        String cleanMessage = ChatColor.stripColor(event.getMessage())
                .replace("§", "")
                .replace("&", "");

        if (cleanMessage.equalsIgnoreCase(cancelInput)) {
            stringListeners.remove(uuid);
            decimalListeners.remove(uuid);
            runForPlayer(player, () -> player.sendMessage(ConfigManager.getMessage("messages.cancelled")));
            return;
        }

        Consumer<String> stringConsumer = stringListeners.remove(uuid);
        if (stringConsumer != null) {
            runForPlayer(player, () -> stringConsumer.accept(event.getMessage()));
            return;
        }

        Consumer<BigDecimal> decimalConsumer = decimalListeners.get(uuid);
        if (decimalConsumer == null) {
            return;
        }

        if (!Utils.isNumeric(cleanMessage)) {
            String message = Utils.hasComma(cleanMessage)
                    ? ConfigManager.getMessage("messages.use_dot")
                    : ConfigManager.getMessage("messages.not_number");
            runForPlayer(player, () -> player.sendMessage(message));
            return;
        }

        if (Double.parseDouble(cleanMessage) < 0) {
            runForPlayer(player, () -> player.sendMessage(ConfigManager.getMessage("messages.negative_price")));
            return;
        }

        decimalListeners.remove(uuid);
        BigDecimal value = new BigDecimal(cleanMessage);
        runForPlayer(player, () -> decimalConsumer.accept(value));
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        stringListeners.remove(uuid);
        decimalListeners.remove(uuid);
    }

    private void runForPlayer(Player player, Runnable task) {
        TaskScheduler.runAtEntity(plugin, player, task, () -> {
        });
    }
}
