package net.skybattle.plugin.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.skybattle.plugin.SkyBattlePlugin;
import net.skybattle.plugin.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Центральный менеджер игрового цикла Sky Battle.
 */
public class GameManager {

    private static final int MIN_PLAYERS_TO_START = 1;
    private static final int START_COUNTDOWN_SECONDS = 10;
    private static final int END_DELAY_SECONDS = 8;

    private final SkyBattlePlugin plugin;
    private final BorderManager borderManager;

    private GameState state = GameState.WAITING;
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> alivePlayers = new HashSet<>();

    private BukkitTask countdownTask;
    private BukkitTask endTask;
    private World gameWorld;

    public GameManager(SkyBattlePlugin plugin, BorderManager borderManager) {
        this.plugin = plugin;
        this.borderManager = borderManager;
    }

    public GameState getState() {
        return state;
    }

    public Set<UUID> getParticipants() {
        return Collections.unmodifiableSet(participants);
    }

    public Set<UUID> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    public boolean isInLobby(Player player) {
        return state == GameState.WAITING || state == GameState.STARTING;
    }

    public boolean isInGame(Player player) {
        return state == GameState.IN_GAME && alivePlayers.contains(player.getUniqueId());
    }

    public boolean isParticipant(Player player) {
        return participants.contains(player.getUniqueId());
    }

    /**
     * Добавляет игрока в очередь на участие в следующем раунде.
     */
    public boolean joinGame(Player player) {
        if (state == GameState.IN_GAME) {
            player.sendMessage(Component.text("Игра уже идёт. Дождитесь окончания раунда.", NamedTextColor.RED));
            return false;
        }

        if (participants.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Вы уже в очереди.", NamedTextColor.YELLOW));
            return false;
        }

        participants.add(player.getUniqueId());
        prepareLobbyPlayer(player);
        broadcast(Component.text(player.getName() + " присоединился к игре! (" + participants.size() + " игроков)", NamedTextColor.GREEN));

        if (state == GameState.WAITING && participants.size() >= MIN_PLAYERS_TO_START) {
            beginStartingPhase();
        }

        return true;
    }

    /**
     * Убирает игрока из очереди до начала боя.
     */
    public boolean leaveGame(Player player) {
        if (!participants.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Вы не участвуете в игре.", NamedTextColor.RED));
            return false;
        }

        if (state == GameState.IN_GAME) {
            player.sendMessage(Component.text("Нельзя покинуть игру во время боя.", NamedTextColor.RED));
            return false;
        }

        participants.remove(player.getUniqueId());
        player.sendMessage(Component.text("Вы покинули очередь Sky Battle.", NamedTextColor.YELLOW));
        broadcast(Component.text(player.getName() + " покинул очередь.", NamedTextColor.GRAY));

        if (state == GameState.STARTING && participants.size() < MIN_PLAYERS_TO_START) {
            cancelStartingPhase();
        }

        return true;
    }

    /**
     * Принудительный старт игры (админ-команда).
     */
    public boolean forceStart() {
        if (state == GameState.IN_GAME) {
            return false;
        }

        if (participants.isEmpty()) {
            return false;
        }

        if (state == GameState.STARTING) {
            return false;
        }

        beginStartingPhase();
        return true;
    }

    /**
     * Принудительная остановка текущего раунда.
     */
    public void forceStop() {
        cancelCountdownTask();
        cancelEndTask();
        borderManager.stopShrinking();

        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                resetPlayer(player);
            }
        }

        participants.clear();
        alivePlayers.clear();
        setState(GameState.WAITING);
        broadcast(Component.text("Игра была остановлена администратором.", NamedTextColor.RED));
    }

    /**
     * Обрабатывает смерть игрока во время активной фазы.
     */
    public void handlePlayerDeath(Player player) {
        if (state != GameState.IN_GAME) {
            return;
        }

        if (!alivePlayers.remove(player.getUniqueId())) {
            return;
        }

        player.setGameMode(GameMode.SPECTATOR);
        broadcast(Component.text(player.getName() + " выбыл! Осталось: " + alivePlayers.size(), NamedTextColor.GOLD));

        checkWinCondition();
    }

    /**
     * Обрабатывает выход игрока с сервера во время игры.
     */
    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();

        if (state == GameState.IN_GAME && alivePlayers.contains(uuid)) {
            alivePlayers.remove(uuid);
            broadcast(Component.text(player.getName() + " покинул сервер и выбыл из игры.", NamedTextColor.GRAY));
            checkWinCondition();
        }

        participants.remove(uuid);
        alivePlayers.remove(uuid);

        if ((state == GameState.WAITING || state == GameState.STARTING) && participants.size() < MIN_PLAYERS_TO_START) {
            cancelStartingPhase();
        }
    }

    public void setGameWorld(World gameWorld) {
        this.gameWorld = gameWorld;
    }

    public World getGameWorld() {
        return gameWorld;
    }

    private void beginStartingPhase() {
        setState(GameState.STARTING);
        cancelCountdownTask();

        final int[] secondsLeft = {START_COUNTDOWN_SECONDS};

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (secondsLeft[0] <= 0) {
                cancelCountdownTask();
                beginInGamePhase();
                return;
            }

            Title title = Title.title(
                    Component.text(String.valueOf(secondsLeft[0]), NamedTextColor.GOLD),
                    Component.text("До начала Sky Battle", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(250), Duration.ofMillis(1000), Duration.ofMillis(250))
            );

            for (UUID uuid : participants) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.showTitle(title);
                }
            }

            if (secondsLeft[0] <= 3 || secondsLeft[0] == START_COUNTDOWN_SECONDS) {
                broadcast(Component.text("Старт через " + secondsLeft[0] + " сек.", NamedTextColor.AQUA));
            }

            secondsLeft[0]--;
        }, 0L, 20L);
    }

    private void cancelStartingPhase() {
        cancelCountdownTask();
        setState(GameState.WAITING);
        broadcast(Component.text("Недостаточно игроков. Ожидание...", NamedTextColor.YELLOW));
    }

    private void beginInGamePhase() {
        setState(GameState.IN_GAME);
        alivePlayers.clear();
        alivePlayers.addAll(participants);

        World world = resolveGameWorld();
        if (world == null) {
            plugin.getLogger().severe("Игровой мир не найден. Раунд отменён.");
            forceStop();
            return;
        }

        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                prepareCombatPlayer(player, world);
            } else {
                alivePlayers.remove(uuid);
            }
        }

        if (alivePlayers.size() < 2) {
            broadcast(Component.text("Недостаточно игроков для начала боя.", NamedTextColor.RED));
            beginEndPhase(null);
            return;
        }

        borderManager.startShrinking(world);
        broadcast(Component.text("Sky Battle началась! Сражайтесь!", NamedTextColor.GREEN));
    }

    private void checkWinCondition() {
        if (state != GameState.IN_GAME) {
            return;
        }

        if (alivePlayers.size() <= 1) {
            Player winner = null;
            if (alivePlayers.size() == 1) {
                UUID winnerId = alivePlayers.iterator().next();
                winner = Bukkit.getPlayer(winnerId);
            }
            beginEndPhase(winner);
        }
    }

    private void beginEndPhase(Player winner) {
        setState(GameState.ENDED);
        borderManager.stopShrinking();
        cancelCountdownTask();
        cancelEndTask();

        if (winner != null) {
            broadcast(Component.text("Победитель: " + winner.getName() + "!", NamedTextColor.GOLD));
            Title winTitle = Title.title(
                    Component.text("ПОБЕДА!", NamedTextColor.GOLD),
                    Component.text(winner.getName(), NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            winner.showTitle(winTitle);
        } else {
            broadcast(Component.text("Раунд завершён без победителя.", NamedTextColor.GRAY));
        }

        endTask = Bukkit.getScheduler().runTaskLater(plugin, this::resetToWaiting, END_DELAY_SECONDS * 20L);
    }

    private void resetToWaiting() {
        for (UUID uuid : new HashSet<>(participants)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                resetPlayer(player);
            }
        }

        participants.clear();
        alivePlayers.clear();
        setState(GameState.WAITING);
        broadcast(Component.text("Новый раунд Sky Battle скоро начнётся. Используйте /skybattle join", NamedTextColor.AQUA));
    }

    private void prepareLobbyPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        PlayerUtil.resetPlayerState(player);
        player.sendMessage(Component.text("Вы в лобби Sky Battle. Ожидайте начала игры.", NamedTextColor.AQUA));
    }

    private void prepareCombatPlayer(Player player, World world) {
        player.setGameMode(GameMode.SURVIVAL);
        PlayerUtil.resetPlayerState(player);
        player.teleportAsync(world.getSpawnLocation());
    }

    private void resetPlayer(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        PlayerUtil.resetPlayerState(player);
    }

    private World resolveGameWorld() {
        if (gameWorld != null) {
            return gameWorld;
        }
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private void setState(GameState newState) {
        GameState previous = this.state;
        this.state = newState;
        plugin.getLogger().info("Состояние игры: " + previous + " -> " + newState);
    }

    private void broadcast(Component message) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(message);
        }
    }

    private void cancelCountdownTask() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void cancelEndTask() {
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
    }
}
