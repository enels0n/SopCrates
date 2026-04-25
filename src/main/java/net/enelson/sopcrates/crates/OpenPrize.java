package net.enelson.sopcrates.crates;

import org.bukkit.inventory.ItemStack;

public class OpenPrize {

	private ItemStack displayItem;
	private String crateName;
	private String id;
	private double chance;
	
	OpenPrize(ItemStack displayItem, String crateType, String id, double chance) {
		this.displayItem = displayItem;
		this.crateName = crateType;
		this.id = id;
		this.chance = chance;
	}
	
	public ItemStack getDisplayItem() {
		return this.displayItem;
	}

	public String getCrateType() {
		return this.crateName;
	}
	
	public String getId() {
		return this.id;
	}

	public double getChance() {
		return this.chance;
	}
}