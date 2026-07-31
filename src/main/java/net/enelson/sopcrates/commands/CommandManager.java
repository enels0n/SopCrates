package net.enelson.sopcrates.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.enelson.sopcrates.SopCrates;
import net.enelson.sopcrates.crates.Crate;
import net.enelson.sopcrates.crates.CrateBlock;
import net.md_5.bungee.api.ChatColor;

public class CommandManager implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            return false;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("chances")) {
            YamlConfiguration crateConfig = SopCrates.getInstance().getCratesManager().getCrateConfig(args[1]);
            if (crateConfig == null) {
                sender.sendMessage("РќРµРёР·РІРµСЃС‚РЅС‹Р№ С‚РёРї РєРµР№СЃРѕРІ.");
                return false;
            }

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "РЁР°РЅСЃС‹ РїСЂРёР·РѕРІ РєРµР№СЃР° &a&n" + crateConfig.getString("displayName")));

            int totalWeight = 0;
            for (String id : crateConfig.getConfigurationSection("prizes").getKeys(false)) {
                totalWeight = totalWeight + crateConfig.getInt("prizes." + id + ".weight");
            }

            for (String id : crateConfig.getConfigurationSection("prizes").getKeys(false)) {
                Double chance = crateConfig.getDouble("prizes." + id + ".weight") / totalWeight;
                chance = (double) Math.round(chance * 10000.0) / 100;
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        id + ": " + crateConfig.getString("prizes." + id + ".display.name") + " &a" + chance + "&f%"));
            }
            return false;
        }

        if ((args.length == 4 || args.length == 5) && args[0].equalsIgnoreCase("give")) {
            Player player = Bukkit.getPlayerExact(args[1]);
            if (player == null) {
                sender.sendMessage("РРіСЂРѕРє РЅРµ РЅР°Р№РґРµРЅ");
                return false;
            }

            if (args.length == 5 && args[4].equalsIgnoreCase("virtual")) {
                SopCrates.getInstance().getCratesManager().addKey(player.getName(), args[2], Integer.parseInt(args[3]));
                return false;
            }

            ItemStack item = SopCrates.getInstance().getCratesManager().generateKey(args[2], Integer.parseInt(args[3]));
            if (player.getInventory().addItem(item).size() != 0) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            return false;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only by player");
                return false;
            }

            Player player = (Player) sender;
            double direction = player.getLocation().getYaw();
            Block block = player.getTargetBlockExact(5);
            if (block != null && !block.getType().equals(Material.AIR)) {
                if (!SopCrates.getInstance().getCratesManager().createCrateBlock(block, direction, args[1])) {
                    sender.sendMessage("Р§С‚Рѕ-С‚Рѕ РїРѕС€Р»Рѕ РЅРµ С‚Р°Рє. РЈР±РµРґРёС‚РµСЃСЊ РІ РїСЂР°РІРёР»СЊРЅРѕСЃС‚Рё РЅР°РїРёСЃР°РЅРёСЏ РЅР°Р·РІР°РЅРёСЏ РєРµР№СЃР°.");
                }
            }
            return false;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only by player");
                return false;
            }

            Player player = (Player) sender;
            Block block = player.getTargetBlockExact(5);
            CrateBlock crateBlock = SopCrates.getInstance().getCratesManager().getCrateBlock(block);
            if (crateBlock == null) {
                sender.sendMessage("Targeted block is not a crate");
                return false;
            }

            SopCrates.getInstance().getCratesManager().removeCrateBlock(crateBlock);
            return false;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            SopCrates.getInstance().getCratesManager().reload();
            sender.sendMessage("The plugin has been reloaded.");
            return false;
        }

        sender.sendMessage("/sopcrates chances <crateName>");
        sender.sendMessage("/sopcrates give <player> <crateName> <amount> [virtual]");
        sender.sendMessage("/sopcrates create <crateName>");
        sender.sendMessage("/sopcrates remove");
        sender.sendMessage("/sopcrates reload");

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filterStartsWith(Arrays.asList("chances", "give", "create", "remove", "reload"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                List<String> players = new ArrayList<String>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    players.add(player.getName());
                }
                return filterStartsWith(players, args[1]);
            }
            if (args[0].equalsIgnoreCase("chances") || args[0].equalsIgnoreCase("create")) {
                return filterStartsWith(getCrateNames(), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filterStartsWith(getCrateNames(), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return filterStartsWith(Arrays.asList("1", "2", "5", "10", "16", "32", "64"), args[3]);
        }

        if (args.length == 5 && args[0].equalsIgnoreCase("give")) {
            return filterStartsWith(Collections.singletonList("virtual"), args[4]);
        }

        return Collections.emptyList();
    }

    private List<String> getCrateNames() {
        List<String> names = new ArrayList<String>();
        if (SopCrates.getInstance() == null || SopCrates.getInstance().getCratesManager() == null
                || SopCrates.getInstance().getCratesManager().crates == null) {
            return names;
        }

        for (Crate crate : SopCrates.getInstance().getCratesManager().crates) {
            names.add(crate.getCrateName());
        }
        return names;
    }

    private List<String> filterStartsWith(List<String> values, String input) {
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        Collections.sort(result);
        return result;
    }
}
