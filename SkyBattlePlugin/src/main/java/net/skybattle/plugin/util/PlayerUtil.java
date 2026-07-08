package net.skybattle.plugin.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * Вспомогательные методы для работы с игроками (Paper/Purpur 1.21.11).
 */
public final class PlayerUtil {

    private PlayerUtil() {
    }

    /**
     * Восстанавливает здоровье игрока до максимума через Attribute API.
     */
    public static void restoreFullHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            player.setHealth(maxHealth.getValue());
        }
    }

    /**
     * Сбрасывает сытость и инвентарь, восстанавливает здоровье.
     */
    public static void resetPlayerState(Player player) {
        restoreFullHealth(player);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.getInventory().clear();
    }
}
