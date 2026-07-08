package net.skybattle.plugin.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.skybattle.plugin.game.GameManager;
import net.skybattle.plugin.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Базовая обработка игровых событий Sky Battle.
 */
public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        if (!gameManager.isInGame(player)) {
            return;
        }

        event.deathMessage(null);
        event.deathScreenMessageOverride(null);
        event.setShowDeathMessages(false);
        event.setKeepInventory(false);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setShouldDropExperience(false);

        gameManager.handlePlayerDeath(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (gameManager.isInLobby(player) && gameManager.isParticipant(player)) {
            event.setCancelled(true);
            return;
        }

        if (gameManager.getState() == GameState.ENDED && gameManager.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (gameManager.isInLobby(player) && gameManager.isParticipant(player)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Строительство запрещено в лобби.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (gameManager.isInLobby(player) && gameManager.isParticipant(player)) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Разрушение блоков запрещено в лобби.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (gameManager.isInLobby(player) && gameManager.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (gameManager.isInLobby(player) && gameManager.isParticipant(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        gameManager.handlePlayerQuit(event.getPlayer());
    }
}
