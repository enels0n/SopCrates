package net.enelson.sopcrates.crates;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.enelson.sopcrates.SopCrates;
import net.enelson.sopcrates.utils.Utils;
import net.enelson.sopli.lib.SopLib;

public class CratesManager {

	public List<Crate> crates;
	public List<CrateBlock> blocks;
	public List<OpenCrate> openCrates;

	private File fileBlocks;
	private YamlConfiguration configBlocks;
	private File fileKeys;
	private YamlConfiguration configKeys;

	public CrateBlock getCrateBlock(Block block) {
		return this.blocks.stream().filter(b -> b.getLocation().equals(block.getLocation())).findFirst().orElse(null);
	}

	public Crate getCrate(String crateName) {
		return this.crates.stream().filter(b -> b.getCrateName().equals(crateName)).findFirst().orElse(null);
	}

	public boolean createOpenCrate(CrateBlock crateBlock, Player player) {
		if (this.openCrates.stream().filter(o -> o.getCrateBlock().getLocation().equals(crateBlock.getLocation()))
				.findFirst().orElse(null) != null) {
			Crate crate = SopCrates.getInstance().getCratesManager().getCrate(crateBlock.getCrateName());
			if(crate.getCrateConfig().getBoolean("denyOpeningPush"))
				Utils.pushPlayer(player);
			if(crate.getCrateConfig().getStringList("denyOpeningCommands") != null)
				Utils.runCommands(crate.getCrateConfig().getStringList("denyOpeningCommands"), player);
			return false;
		}
		YamlConfiguration configCrate = this.getCrateConfig(crateBlock.getCrateName());
		Utils.runCommands(configCrate.getStringList("open_commands"), player);
		OpenCrate openCrate = new OpenCrate(crateBlock, player);
		this.openCrates.add(openCrate);
		openCrate.open();
		return true;
	}

	public OpenPrize[] generatePrizes(Crate crate) {
		OpenPrize[] openPrizes = new OpenPrize[100];

		for (int x = 0; x < 100; x++) {
			int totalWeight = 0;
			YamlConfiguration crateConfig = this.getCrateConfig(crate.getCrateName());

			for (String id : crateConfig.getConfigurationSection("prizes").getKeys(false)) {
				totalWeight = totalWeight + crateConfig.getInt("prizes." + id + ".weight");
			}

			int random = ThreadLocalRandom.current().nextInt(100) + 1;
			totalWeight = 0;
			OpenPrize op;

			for (String id : crateConfig.getConfigurationSection("prizes").getKeys(false)) {
				totalWeight = totalWeight + crateConfig.getInt("prizes." + id + ".weight");
				if (totalWeight >= random) {
					double chance = Utils.getPrizeChance(crateConfig, id);
					ItemStack item = Utils.getItem(crateConfig, id, chance);
					op = new OpenPrize(item, crate.getCrateName(), id, chance);
					openPrizes[x] = op;
					break;
				}
			}

		}

		return openPrizes;
	}

	public void removeOpenCrate(OpenCrate openCrate) {
		this.openCrates.remove(openCrate);
	}

	public YamlConfiguration getCrateConfig(String crate) {
		return YamlConfiguration
				.loadConfiguration(new File(SopCrates.getInstance().getDataFolder(), "crates/" + crate + ".yml"));
	}

	public ItemStack generateKey(String crateName, int amount) {
		
		String material = this.getCrateConfig(crateName).getString("key.item");
		int model = this.getCrateConfig(crateName).getInt("key.model");
		
		List<String> nbts = Arrays.asList("ACrates::" + crateName);
		
		String name = this.getCrateConfig(crateName).getString("key.name");
		List<String> lore = this.getCrateConfig(crateName).getStringList("key.lore");
		
		return SopLib.getInstance().getItemUtils().createItem(material, amount, model, name, null, lore, nbts);
	}

	public int getKeyCount(String playerName, String crateName) {
		return this.configKeys.getInt("keys." + playerName + "." + crateName);
	}

	public void addKey(String playerName, String crateName, int amount) {
		if (amount < 1)
			return;
		this.configKeys.set("keys." + playerName + "." + crateName,
				this.configKeys.getInt("keys." + playerName + "." + crateName) + amount);
		try {
			this.configKeys.save(fileKeys);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void removeKey(String playerName, String crateName, int amount) {
		if (amount < 1)
			return;
		this.configKeys.set("keys." + playerName + "." + crateName,
				this.configKeys.getInt("keys." + playerName + "." + crateName) - amount);
		try {
			this.configKeys.save(fileKeys);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean createCrateBlock(Block block, double direction, String crateName) {
		Crate create = this.getCrate(crateName);
		if (create == null)
			return false;
		String id = this.generateId(block.getLocation());
		this.configBlocks.set(id + ".location", SopCrates.getInstance().getSoplib().getUtil().getSerializedLocation(block.getLocation()));
		this.configBlocks.set(id + ".direction", direction);
		this.configBlocks.set(id + ".type", crateName);
		try {
			this.configBlocks.save(fileBlocks);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.blocks.add(new CrateBlock(block.getLocation(), crateName, direction));
		this.refreshCrateBlocks();
		return true;
	}

	public void removeCrateBlock(CrateBlock crate) {
		String id = this.generateId(crate.getLocation());
		this.configBlocks.set(id, null);
		try {
			this.configBlocks.save(fileBlocks);
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.blocks.remove(crate);
		crate.removeHologram();
		this.refreshCrateBlocks();
	}

	public String getCrateKeyType(Crate crate) {
		YamlConfiguration crateConfig = this.getCrateConfig(crate.getCrateName());
		return crateConfig.getString("keyType");
	}

	private void refreshCrateBlocks() {
		this.clearCrateBlocks();
		this.blocks = new ArrayList<CrateBlock>();
		if (this.configBlocks.getConfigurationSection("blocks") == null) {
			return;
		}
		for (String id : this.configBlocks.getConfigurationSection("blocks").getKeys(false)) {
			String path = "blocks." + id;
			CrateBlock crateBlock = new CrateBlock(
					SopCrates.getInstance().getSoplib().getUtil().getDeserializedLocation(this.configBlocks.getString(path + ".location")),
					this.configBlocks.getString(path + ".type"),
					this.readDirection(path + ".direction"));
			crateBlock.refreshHologram();
			blocks.add(crateBlock);
		}
	}

	private void clearCrateBlocks() {
		if (this.blocks == null) {
			return;
		}
		this.blocks.forEach(CrateBlock::removeHologram);
	}

	private double readDirection(String path) {
		Object raw = this.configBlocks.get(path);
		if (raw instanceof Number) {
			return ((Number) raw).doubleValue();
		}
		if (raw instanceof String) {
			switch (((String) raw).toUpperCase()) {
			case "N":
			case "NORTH":
				return 180.0D;
			case "S":
			case "SOUTH":
				return 0.0D;
			case "E":
			case "EAST":
				return -90.0D;
			case "W":
			case "WEST":
				return 90.0D;
			default:
				try {
					return Double.parseDouble((String) raw);
				} catch (NumberFormatException ignored) {
				}
			}
		}
		return 180.0D;
	}

	private void refreshCrates() {
        this.crates = new ArrayList<Crate>();
        File cratesFolder = new File(SopCrates.getInstance().getDataFolder(), "crates");
        File[] crateFiles = cratesFolder.listFiles();
        if (crateFiles == null) {
            return;
        }
        for (File crateFileName : crateFiles) {
            if (!crateFileName.getName().endsWith(".yml"))
                continue;

            String crateName = org.apache.commons.io.FilenameUtils.getBaseName(crateFileName.getName());
            Crate crate = new Crate(YamlConfiguration.loadConfiguration(crateFileName), crateName, this.getMayPrizes(crateName));
            this.crates.add(crate);
        }
    }
	
	private void createFiles() {
        File cratesFolder = new File(SopCrates.getInstance().getDataFolder(), "crates");
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
        }
        File exampleFile = new File(cratesFolder, "example.yml");
        if (!exampleFile.exists()) {
            SopCrates.getInstance().saveResource("crates/example.yml", false);
        }

        this.fileBlocks = new File(SopCrates.getInstance().getDataFolder(), "blocks.yml");
        if (!this.fileBlocks.exists())
            SopCrates.getInstance().saveResource("blocks.yml", true);
        this.configBlocks = YamlConfiguration.loadConfiguration(this.fileBlocks);

        this.fileKeys = new File(SopCrates.getInstance().getDataFolder(), "keys.yml");
        if (!this.fileKeys.exists())
            SopCrates.getInstance().saveResource("keys.yml", true);
        this.configKeys = YamlConfiguration.loadConfiguration(this.fileKeys);
    }

	private String generateId(Location location) {
		String s = "blocks." + location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY()
				+ "_" + location.getBlockZ();
		return s;
	}

	public ItemStack[] getMayPrizes(String crateName) {
		YamlConfiguration crateConfig = this.getCrateConfig(crateName);
		ItemStack[] items = new ItemStack[crateConfig.getConfigurationSection("prizes").getKeys(false).size()];

		int x = 0;
		for (String id : crateConfig.getConfigurationSection("prizes").getKeys(false)) {
			double chance = Utils.getPrizeChance(crateConfig, id);
					ItemStack item = Utils.getItem(crateConfig, id, chance);
			items[x] = item;
			x++;
		}
		
		return items;
	}
	
	public Crate getCrate(Inventory inventory) {
		return this.crates.stream().filter(b -> b.checkInventory(inventory)).findFirst().orElse(null);
	}
	
	public List<OpenCrate> getOpenCrates(Player player) {
		return this.openCrates.stream().filter(o -> o.getPlayer().equals(player)).collect(Collectors.toList());
	}
	
	public void reload() {
		this.openCrates = new ArrayList<OpenCrate>();
		
		this.createFiles();
		this.refreshCrates();
		this.refreshCrateBlocks();
	}

	public void ensureExternalHologramsPresent() {
		if (this.blocks == null || this.blocks.isEmpty()) {
			return;
		}
		this.blocks.forEach(CrateBlock::ensureHologramPresent);
	}
	
	public void onDisable() {
		this.crates.stream().forEach(c -> c.getMayPrizes().getViewers().forEach(v -> ((Player)v).closeInventory()));
		this.openCrates.stream().forEach(o -> o.forceStop());
		this.blocks.stream().forEach(CrateBlock::removeHologram);
	}
}
