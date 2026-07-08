package net.skybattle.plugin;

import net.skybattle.plugin.command.SkyBattleCommand;
import net.skybattle.plugin.game.BorderManager;
import net.skybattle.plugin.game.GameManager;
import net.skybattle.plugin.listener.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Главный класс плагина Sky Battle.
 */
public class SkyBattlePlugin extends JavaPlugin {

    private GameManager gameManager;
    private BorderManager borderManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        borderManager = new BorderManager(this);
        gameManager = new GameManager(this, borderManager);

        loadConfiguration();

        registerCommands();
        registerListeners();

        getLogger().info("Sky Battle успешно загружен!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.forceStop();
        }

        if (borderManager != null) {
            borderManager.stopShrinking();
        }

        getLogger().info("Sky Battle выгружен.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    private void loadConfiguration() {
        reloadConfig();

        double centerX = getConfig().getDouble("border.center-x", 0.0);
        double centerZ = getConfig().getDouble("border.center-z", 0.0);
        double initialSize = getConfig().getDouble("border.initial-size", 200.0);
        double finalSize = getConfig().getDouble("border.final-size", 20.0);
        long shrinkDuration = getConfig().getLong("border.shrink-duration-seconds", 300L);
        String worldName = getConfig().getString("game.world", "");

        borderManager.setCenter(centerX, centerZ);
        borderManager.setInitialSize(initialSize);
        borderManager.setFinalSize(finalSize);
        borderManager.setShrinkDurationSeconds(shrinkDuration);

        if (worldName != null && !worldName.isBlank()) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                gameManager.setGameWorld(world);
                getLogger().info("Игровой мир: " + worldName);
            } else {
                getLogger().warning("Мир '" + worldName + "' не найден. Будет использован первый доступный мир.");
            }
        }
    }

    private void registerCommands() {
        SkyBattleCommand commandHandler = new SkyBattleCommand(gameManager);
        var command = getCommand("skybattle");

        if (command == null) {
            getLogger().severe("Команда 'skybattle' не найдена в plugin.yml!");
            return;
        }

        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
    }
}
