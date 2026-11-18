package com.hookshot.commands;

import com.hookshot.HookshotPlugin;
import com.hookshot.harness.HarnessItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /hookshot [help|give [player] [amount]|reload] */
public final class HookshotCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_GIVE = 64;

    private final HookshotPlugin plugin;
    private final HarnessItem harness;

    public HookshotCommand(HookshotPlugin plugin, HarnessItem harness) {
        this.plugin = plugin;
        this.harness = harness;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> sendHelp(sender, label);
            case "give" -> give(sender, label, args);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(error("Unknown subcommand. Try /" + label + " help"));
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("=== Grapple Harness ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Wear the harness as leggings, then:", NamedTextColor.GRAY));
        line(sender, "Right-click with a sword", "fire a hook where you aim (up to "
                + plugin.settings().maxHooks() + " at once)");
        line(sender, "Hold Sneak", "reel in toward your hooks (uses gas)");
        if (plugin.settings().releaseOnSneakRelease()) {
            line(sender, "Release Sneak", "let go and keep your momentum");
            line(sender, "Jump", "let go without reeling first");
        } else {
            line(sender, "Jump", "let go and keep your momentum");
        }
        line(sender, "Jump in mid-air, no hooks", "gas burst where you look");
        line(sender, "W A S D in mid-air", "steer your swing or flight");
        sender.sendMessage(Component.text("Gas refills on the ground. Fast melee hits deal bonus damage.",
                NamedTextColor.GRAY));
        if (sender.hasPermission("hookshot.give")) {
            line(sender, "/" + label + " give [player] [amount]", "give harnesses");
        }
        if (sender.hasPermission("hookshot.reload")) {
            line(sender, "/" + label + " reload", "reload config.yml");
        }
    }

    private void give(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission("hookshot.give")) {
            sender.sendMessage(error("You don't have permission to do that."));
            return;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(error("Player not found: " + args[1]));
                return;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            sender.sendMessage(error("Usage: /" + label + " give <player> [amount]"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(error("Amount must be a number."));
                return;
            }
            if (amount < 1 || amount > MAX_GIVE) {
                sender.sendMessage(error("Amount must be between 1 and " + MAX_GIVE + "."));
                return;
            }
        }
        for (int i = 0; i < amount; i++) {
            ItemStack item = harness.create();
            target.getInventory().addItem(item).values()
                    .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
        }
        String what = amount == 1 ? "a Grapple Harness" : amount + " Grapple Harnesses";
        sender.sendMessage(Component.text("Gave " + what + " to " + target.getName() + ".", NamedTextColor.GREEN));
        if (!target.equals(sender)) {
            target.sendMessage(Component.text("You received " + what + ".", NamedTextColor.GREEN));
        }
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("hookshot.reload")) {
            sender.sendMessage(error("You don't have permission to do that."));
            return;
        }
        plugin.reload();
        sender.sendMessage(Component.text("Hookshot configuration reloaded.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("help");
            if (sender.hasPermission("hookshot.give")) {
                options.add("give");
            }
            if (sender.hasPermission("hookshot.reload")) {
                options.add("reload");
            }
        } else if (args[0].equalsIgnoreCase("give") && sender.hasPermission("hookshot.give")) {
            if (args.length == 2) {
                Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            } else if (args.length == 3) {
                options.add("1");
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }

    private static void line(CommandSender sender, String key, String text) {
        sender.sendMessage(Component.text(key, NamedTextColor.YELLOW)
                .append(Component.text(" - " + text, NamedTextColor.GRAY)));
    }

    private static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }
}
