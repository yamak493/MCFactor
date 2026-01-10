package net.enabify.mcfactor.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Utility class to handle scheduling for both Spigot/Paper and Folia servers.
 */
public class FoliaScheduler {
    private static final boolean IS_FOLIA = tryClass("io.papermc.paper.threadedregions.scheduler.RegionScheduler");

    private static boolean tryClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    public static void runTask(Plugin plugin, Player player, Runnable task) {
        if (IS_FOLIA && player != null) {
            try {
                // player.getScheduler().run(plugin, task -> task.run(), null)
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null);
                return;
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runTaskLater(Plugin plugin, Player player, Runnable task, long ticks) {
        if (IS_FOLIA && player != null) {
            try {
                // player.getScheduler().runDelayed(plugin, task -> task.run(), null, ticks)
                Object scheduler = player.getClass().getMethod("getScheduler").invoke(player);
                Method runMethod = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), null, ticks);
                return;
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    public static void runGlobalTask(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            try {
                // Bukkit.getGlobalRegionScheduler().run(plugin, task -> task.run())
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, Consumer.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run());
                return;
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runGlobalTaskLater(Plugin plugin, Runnable task, long ticks) {
        if (IS_FOLIA) {
            try {
                // Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> task.run(), ticks)
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), ticks);
                return;
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
    }

    public static void runGlobalTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
        if (IS_FOLIA) {
            try {
                // Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> task.run(), delay, period)
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                runMethod.invoke(scheduler, plugin, (Consumer<Object>) t -> task.run(), delay, period);
                return;
            } catch (Exception ignored) {}
        }
        Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
    }

    public static void teleport(Player player, org.bukkit.Location location) {
        if (IS_FOLIA) {
            try {
                // player.teleportAsync(location)
                Method teleportAsyncMethod = player.getClass().getMethod("teleportAsync", org.bukkit.Location.class);
                teleportAsyncMethod.invoke(player, location);
                return;
            } catch (Exception ignored) {}
        }
        player.teleport(location);
    }
}
