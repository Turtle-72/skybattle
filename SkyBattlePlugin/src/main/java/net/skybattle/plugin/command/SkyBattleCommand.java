package net.skybattle.plugin.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.skybattle.plugin.game.GameManager;
import net.skybattle.plugin.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Обработчик команды /skybattle.
 */
public class SkyBattleCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("join", "leave", "start", "stop", "status");

    private final GameManager gameManager;

    public SkyBattleCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "join" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Эту команду может использовать только игрок.", NamedTextColor.RED));
                    return true;
                }
                gameManager.joinGame(player);
            }
            case "leave" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Эту команду может использовать только игрок.", NamedTextColor.RED));
                    return true;
                }
                gameManager.leaveGame(player);
            }
            case "start" -> {
                if (!sender.hasPermission("skybattle.admin")) {
                    sender.sendMessage(Component.text("Недостаточно прав.", NamedTextColor.RED));
                    return true;
                }
                if (gameManager.forceStart()) {
                    sender.sendMessage(Component.text("Запуск Sky Battle...", NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Не удалось запустить игру. Проверьте состояние и наличие игроков.", NamedTextColor.RED));
                }
            }
            case "stop" -> {
                if (!sender.hasPermission("skybattle.admin")) {
                    sender.sendMessage(Component.text("Недостаточно прав.", NamedTextColor.RED));
                    return true;
                }
                gameManager.forceStop();
                sender.sendMessage(Component.text("Игра остановлена.", NamedTextColor.YELLOW));
            }
            case "status" -> {
                GameState state = gameManager.getState();
                sender.sendMessage(Component.text("=== Sky Battle ===", NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Состояние: " + state.name(), NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Участников: " + gameManager.getParticipants().size(), NamedTextColor.AQUA));
                sender.sendMessage(Component.text("Живых: " + gameManager.getAlivePlayers().size(), NamedTextColor.AQUA));
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                  @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase(Locale.ROOT);

            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(input)) {
                    if (sub.equals("start") || sub.equals("stop")) {
                        if (sender.hasPermission("skybattle.admin")) {
                            completions.add(sub);
                        }
                    } else {
                        completions.add(sub);
                    }
                }
            }
            return completions;
        }

        return List.of();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Использование: /skybattle <join|leave|start|stop|status>", NamedTextColor.YELLOW));
    }
}
