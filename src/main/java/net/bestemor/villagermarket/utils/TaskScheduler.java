package net.bestemor.villagermarket.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class TaskScheduler {

    private static final boolean FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");

    private TaskScheduler() {
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runSync(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, task);
            return;
        }
        runGlobal(plugin, task);
    }

    public static void runSyncLater(Plugin plugin, Runnable task, long delayTicks) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return;
        }
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method runDelayed = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            runDelayed.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), delayTicks);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            return;
        }
        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method runNow = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
            runNow.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runAsyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
            return;
        }

        try {
            Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
            Method fixedRate = scheduler.getClass().getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    Consumer.class,
                    long.class,
                    long.class,
                    TimeUnit.class
            );
            fixedRate.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) t -> task.run(),
                    Math.max(0L, delayTicks) * 50L,
                    Math.max(1L, periodTicks) * 50L,
                    TimeUnit.MILLISECONDS
            );
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runSyncTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        if (!FOLIA) {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
            return;
        }

        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method fixedRate = scheduler.getClass().getMethod(
                    "runAtFixedRate",
                    Plugin.class,
                    Consumer.class,
                    long.class,
                    long.class
            );
            fixedRate.invoke(
                    scheduler,
                    plugin,
                    (Consumer<Object>) t -> task.run(),
                    Math.max(1L, delayTicks),
                    Math.max(1L, periodTicks)
            );
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    public static void runAtLocation(Plugin plugin, Location location, Runnable task) {
        if (!FOLIA || location == null || location.getWorld() == null) {
            runSync(plugin, task);
            return;
        }

        try {
            Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
            Method execute = scheduler.getClass().getMethod("execute", Plugin.class, org.bukkit.World.class, int.class, int.class, Runnable.class);
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;
            execute.invoke(scheduler, plugin, location.getWorld(), chunkX, chunkZ, task);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            runSync(plugin, task);
        }
    }

    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        runAtEntity(plugin, entity, task, task);
    }

    public static void runAtEntity(Plugin plugin, Entity entity, Runnable task, Runnable retiredTask) {
        if (entity == null) {
            if (retiredTask != null) {
                runSync(plugin, retiredTask);
            }
            return;
        }

        if (!FOLIA) {
            Bukkit.getScheduler().runTask(plugin, task);
            return;
        }

        try {
            Object scheduler = entity.getClass().getMethod("getScheduler").invoke(entity);
            Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
            execute.invoke(scheduler, plugin, task, retiredTask, 1L);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public static void teleportEntity(Plugin plugin, Entity entity, Location target, Runnable successTask) {
        if (entity == null || target == null) {
            return;
        }

        if (!FOLIA) {
            if (entity.teleport(target) && successTask != null) {
                successTask.run();
            }
            return;
        }

        runAtEntity(plugin, entity, () -> {
            try {
                Method teleportAsync = entity.getClass().getMethod("teleportAsync", Location.class);
                Object result = teleportAsync.invoke(entity, target);
                if (result instanceof CompletableFuture<?> future) {
                    future.whenComplete((success, throwable) -> {
                        if (throwable != null) {
                            plugin.getLogger().warning("Failed to teleport entity asynchronously: " + throwable.getMessage());
                            return;
                        }
                        if (Boolean.TRUE.equals(success) && successTask != null) {
                            successTask.run();
                        }
                    });
                    return;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
                plugin.getLogger().warning("Failed to access teleportAsync, entity move was skipped on Folia.");
            }
        }, () -> {
        });
    }

    private static void runGlobal(Plugin plugin, Runnable task) {
        try {
            Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
            Method execute = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);
            execute.invoke(scheduler, plugin, task);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
