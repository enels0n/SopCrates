package net.enelson.sopcrates.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import de.tr7zw.nbtapi.NBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopcrates.SopCrates;

public class Utils {
	public static String getType(ItemStack item) {
		String s = SopCrates.getInstance().getSoplib().getItemUtils().getNBT(item, "ACrates", String.class);
		return s;
	}

	public static void runCommands(List<String> commands, Player player) {
		for (String command : commands) {
			if (command.startsWith("[console] ")) {
				String prepared = prepareCommand(command.replaceFirst("\\[console\\] ", ""), player);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), prepared);

			} else if (command.startsWith("[player] ")) {
				String prepared = prepareCommand(command.replaceFirst("\\[player\\] ", ""), player);
				Bukkit.dispatchCommand(player, prepared);
			}
		}
	}

	private static String prepareCommand(String command, Player player) {
		String result = command.replaceAll("%%([^%]+)%%", "\u0007$1\u0007");
		result = PlaceholderAPI.setPlaceholders(player, result);
		result = result.replaceAll("\u0007([^\\u0007]+)\u0007", "%$1%");
		return result;
	}

	public static ItemStack setTags(ItemStack item, List<String> tags) {
		NBT.modify(item, nbt -> {
			for (String t : tags) {
				nbt.setString(t.split("::")[0], t.split("::")[1]);
			}
		});
		return item;
	}

	@SuppressWarnings("unchecked")
	public static ItemStack getItem(Map<?, ?> itemMap) {
		if (itemMap.get("material") == null)
			return new ItemStack(Material.AIR);

		String material = (String) itemMap.get("material");
		int amount = itemMap.get("amount") == null ? 1 : (int) itemMap.get("amount");
		String name = itemMap.get("name") == null ? null : (String) itemMap.get("name");
		List<String> lore = itemMap.get("lore") == null ? null : (List<String>) itemMap.get("lore");
		List<String> enchantments = itemMap.get("enchantments") == null ? null
				: (List<String>) itemMap.get("enchantments");
		boolean hideEnchantments = itemMap.get("hideEnchantments") == null ? false
				: (boolean) itemMap.get("hideEnchantments");
		List<String> nbts = (List<String>) itemMap.get("nbts") == null ? null : (List<String>) itemMap.get("nbts");
		int model = itemMap.get("model") == null ? 0 : (int) itemMap.get("model");
		return getItem(material, amount, name, lore, enchantments, hideEnchantments, nbts, model, false, "prize", "0");
	}

	public static ItemStack getItem(YamlConfiguration crateConfig, String id) {
		return getItem(crateConfig, id, getPrizeChance(crateConfig, id));
	}

	public static ItemStack getItem(YamlConfiguration crateConfig, String id, double chance) {
		String material = crateConfig.getString("prizes." + id + ".display.material");
		int amount = crateConfig.getInt("prizes." + id + ".display.amount");
		String name = applyChance(crateConfig.getString("prizes." + id + ".display.name"), chance);
		List<String> lore = applyChance(crateConfig.getStringList("prizes." + id + ".display.lore"), chance);
		List<String> enchantments = crateConfig.getStringList("prizes." + id + ".display.enchantments");
		boolean hideEnchantments = crateConfig.getBoolean("prizes." + id + ".display.hideEnchantments");
		int model = crateConfig.getInt("prizes." + id + ".display.model");
		boolean glowing = crateConfig.getBoolean("prizes." + id + ".display.glowing");

		return getItem(material, amount, name, lore, enchantments, hideEnchantments, null, model, glowing, "display",
				id);
	}

	public static ItemStack getItem(String material, int amount, String name, List<String> lore,
			List<String> enchantments, boolean hideEnchantments, List<String> nbts, int model, boolean glowing,
			String type, String id) {
		
		if (material.startsWith("PLAYER_HEAD:")) {
			String value = material.replaceFirst("PLAYER_HEAD:", "");
			return SopCrates.getInstance().getSoplib().getItemUtils().getHead(value, "Prize");
		} else if (material.startsWith("PLAYER_HEAD_URL:")) {
			String value = material.replaceFirst("PLAYER_HEAD_URL:", "");
			return SopCrates.getInstance().getSoplib().getItemUtils().getHeadURL(value, "Prize");
		} else {
			return SopCrates.getInstance().getSoplib().getItemUtils().createItem(material, amount, model, name, enchantments, lore, nbts);
		}
	}

	public static void pushPlayer(Player player) {
		Vector direction = player.getLocation().getDirection().normalize();
		Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector pushDirection = direction.multiply(-0.5).add(right.multiply(0.15));
		player.setVelocity(pushDirection);
	}

	@SuppressWarnings("unchecked")
	public static void givePrize(Player player, YamlConfiguration crateConfig, String prizeLink) {
		if (crateConfig.get(prizeLink + ".checkAlternatives") != null) {
			List<Map<?, ?>> permissionsList = crateConfig.getMapList(prizeLink + ".checkAlternatives");

			checkPremissions:
			for (Map<?, ?> permissionMap : permissionsList) {
				boolean checkPermission = false;
				String type = (String) permissionMap.get("type");
				
				List<Map<?, ?>> checks = (List<Map<?, ?>>) permissionMap.get("checks");
				for (Map<?, ?> check : checks) {
					boolean equals = checkInput(player, check);
					if (type.equals("all")) {
						if (equals)
							continue;
						continue checkPremissions;
					}

					if (type.equals("any")) {
						if (equals) {
							checkPermission = true;
							break;
						}
						continue;
					}
				}

				if (type.equals("any") && !checkPermission)
					continue;

				if (permissionMap.containsKey("alternative")) {
					Map<?, ?> alternative = (Map<?, ?>) permissionMap.get("alternative");
					if (alternative.get("commands") != null) {
						List<String> commands = (List<String>) alternative.get("commands");
						Utils.runCommands(commands, player);
					}
					if (alternative.get("items") != null) {
						List<Map<?, ?>> itemDetails = (List<Map<?, ?>>) alternative.get("items");
						for (Map<?, ?> itemDetail : itemDetails) {
							ItemStack item = Utils.getItem(itemDetail);
							if (player.getInventory().addItem(item).size() != 0) {
								player.getWorld().dropItem(player.getLocation(), item);
							}
						}
					}
				}
				return;
			}
		}

		if (crateConfig.get(prizeLink + ".prize.commands") != null) {
			Utils.runCommands(crateConfig.getStringList(prizeLink + ".prize.commands"), player);
		}
		if (crateConfig.get(prizeLink + ".prize.items") != null) {
			for (Map<?, ?> itemMap : crateConfig.getMapList(prizeLink + ".prize.items")) {
				ItemStack item = Utils.getItem(itemMap);
				if (player.getInventory().addItem(item).size() != 0) {
					player.getWorld().dropItem(player.getLocation(), item);
				}
			}
		}
	}
	
	public static boolean checkInput(Player player, Map<?, ?> check) {
		CheckType type = CheckType.getType((String)check.get("type"));
		String input = (String)check.get("input");
		String output = check.get("output") != null ? (String)check.get("output") : null;
		
		switch (type) {
			case HAS_PERM:
				return player.isPermissionSet(input);
			case HAS_NO_PERM:
				return !player.isPermissionSet(input);
			case STRING_EQUALS:
				return PlaceholderAPI.setPlaceholders(player, input).equalsIgnoreCase(PlaceholderAPI.setPlaceholders(player, output));
			case STRING_NOT_EQUALS:
				return !PlaceholderAPI.setPlaceholders(player, input).equalsIgnoreCase(PlaceholderAPI.setPlaceholders(player, output));
		}
		return false;
	}
	
	public static String coloring(String string) {
		return PlaceholderAPI.setPlaceholders(null, "%acolor_"+string+"%");
	}

	public static double getPrizeChance(YamlConfiguration crateConfig, String id) {
		if (crateConfig == null || crateConfig.getConfigurationSection("prizes") == null) {
			return 0.0D;
		}

		int totalWeight = 0;
		for (String prizeId : crateConfig.getConfigurationSection("prizes").getKeys(false)) {
			totalWeight += Math.max(1, crateConfig.getInt("prizes." + prizeId + ".weight", 1));
		}

		if (totalWeight <= 0) {
			return 0.0D;
		}

		int weight = Math.max(1, crateConfig.getInt("prizes." + id + ".weight", 1));
		double chance = ((double) weight / (double) totalWeight) * 100.0D;
		return roundChance(chance);
	}

	private static String applyChance(String input, double chance) {
		if (input == null) {
			return null;
		}
		return input.replace("{chance}", formatChance(chance));
	}

	private static List<String> applyChance(List<String> input, double chance) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		List<String> result = new ArrayList<String>(input.size());
		for (String line : input) {
			result.add(applyChance(line, chance));
		}
		return result;
	}

	private static double roundChance(double chance) {
		return Math.round(chance * 100.0D) / 100.0D;
	}

	private static String formatChance(double chance) {
		BigDecimal value = BigDecimal.valueOf(roundChance(chance)).stripTrailingZeros();
		String text = value.toPlainString();
		return text.replace(',', '.');
	}
}