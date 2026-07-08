package net.skybattle.plugin.game;

import net.skybattle.plugin.SkyBattlePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitTask;

/**
 * Управляет динамическим сужением границы мира во время активной игры.
 */
public class BorderManager {

    private final SkyBattlePlugin plugin;

    private double centerX = 0.0;
    private double centerZ = 0.0;
    private double initialSize = 200.0;
    private double finalSize = 20.0;
    private long shrinkDurationSeconds = 300L;

    private BukkitTask monitorTask;
    private World activeWorld;

    public BorderManager(SkyBattlePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Запускает постепенное сужение границы к центру карты.
     *
     * @param world мир, в котором проходит игра
     */
    public void startShrinking(World world) {
        stopShrinking();

        this.activeWorld = world;
        WorldBorder border = world.getWorldBorder();

        border.setCenter(centerX, centerZ);
        border.setSize(initialSize);
        border.setDamageAmount(2.0);
        border.setDamageBuffer(0.0);
        border.setWarningDistance(10);
        border.setWarningTime(15);

        border.setSize(finalSize, shrinkDurationSeconds);

        plugin.getLogger().info(String.format(
                "Граница мира запущена: %.0f -> %.0f за %d сек. (центр: %.1f, %.1f)",
                initialSize, finalSize, shrinkDurationSeconds, centerX, centerZ
        ));

        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeWorld == null) {
                return;
            }
            WorldBorder currentBorder = activeWorld.getWorldBorder();
            if (currentBorder.getSize() <= finalSize + 0.5) {
                plugin.getLogger().info("Граница мира достигла минимального размера.");
                stopMonitorTask();
            }
        }, 20L, 20L);
    }

    /**
     * Останавливает сужение и сбрасывает границу к начальным параметрам.
     */
    public void stopShrinking() {
        stopMonitorTask();

        if (activeWorld != null) {
            WorldBorder border = activeWorld.getWorldBorder();
            border.setSize(initialSize);
            border.setCenter(centerX, centerZ);
            border.setDamageAmount(0.0);
        }

        activeWorld = null;
    }

    /**
     * Проверяет, находится ли локация за пределами текущей границы.
     */
    public boolean isOutsideBorder(Location location) {
        if (location.getWorld() == null || activeWorld == null) {
            return false;
        }
        if (!location.getWorld().equals(activeWorld)) {
            return false;
        }
        return !activeWorld.getWorldBorder().isInside(location);
    }

    public void setCenter(double centerX, double centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public void setInitialSize(double initialSize) {
        this.initialSize = initialSize;
    }

    public void setFinalSize(double finalSize) {
        this.finalSize = finalSize;
    }

    public void setShrinkDurationSeconds(long shrinkDurationSeconds) {
        this.shrinkDurationSeconds = shrinkDurationSeconds;
    }

    public double getInitialSize() {
        return initialSize;
    }

    public double getFinalSize() {
        return finalSize;
    }

    public long getShrinkDurationSeconds() {
        return shrinkDurationSeconds;
    }

    private void stopMonitorTask() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
    }
}
