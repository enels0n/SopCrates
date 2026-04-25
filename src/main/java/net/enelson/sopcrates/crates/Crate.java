package net.enelson.sopcrates.crates;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.enelson.sopcrates.SopCrates;

//import com.iridium.iridiumcolorapi.IridiumColorAPI;

import net.enelson.sopcrates.utils.Utils;
import net.enelson.sopli.lib.SopLib;

public class Crate {
	private YamlConfiguration config;
	private String crateName;
	private Inventory[] inventory;
	private ItemStack[] items;
	
	Crate(YamlConfiguration config, String crateName, ItemStack[] items) {
		this.config = config;
		this.crateName = crateName;
		this.items = items;
		this.inventory = new Inventory[this.getPagesCount()];
		for(int x=0; x<this.getPagesCount(); x++) {
			String name = config.getString("displayName") != null ? config.getString("displayName") : "Case prizes (%page%/%pages%)";
			this.inventory[x] = Bukkit.createInventory(null, 54, Utils.coloring(name.replaceAll("%page%", x+1+"").replaceAll("%pages%", this.getPagesCount()+"")));
			//this.inventory[x] = Bukkit.createInventory(null, 54, IridiumColorAPI.process(name.replaceAll("%page%", x+1+"").replaceAll("%pages%", this.getPagesCount()+"")));
			ItemStack[] pageItems = new ItemStack[45];
			for(int y=0; y<45 && y+(x*45)<items.length; y++) {
				pageItems[y] = items[y+(x*45)];
			}
			this.inventory[x].setContents(pageItems);
			if(x!=0) {
				inventory[x].setItem(48, this.getPreviousPageItem(x));
			}
			if(x+1<this.getPagesCount()) {
				inventory[x].setItem(50, this.getNextPageItem(x+2));
			}
		}
	}
	
	public YamlConfiguration getCrateConfig() {
		return this.config;
	}
	
	public String getCrateName() {
		return this.crateName;
	}
	
	public Inventory getMayPrizes() {
		return this.inventory[0];
	}
	
	public Inventory getMayPizes(int page) {
		return this.inventory[page];
	}
	
	public int getPagesCount() {
		return (int)Math.ceil(this.items.length/45)+1;
	}
	
	private ItemStack getPreviousPageItem(int previuosPage) {
		ItemStack item = SopCrates.getInstance().getSoplib().getItemUtils().getHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2EyYzEyY2IyMjkxODM4NGUwYTgxYzgyYTFlZDk5YWViZGNlOTRiMmVjMjc1NDgwMDk3MjMxOWI1NzkwMGFmYiJ9fX0=", "Previous page");
		item.setAmount(previuosPage);
		SopCrates.getInstance().getSoplib().getItemUtils().setCustomItemKey(item, "item.acrates.previous", "в†ђ Previous");
		return item;
	}
	
	private ItemStack getNextPageItem(int nextPage) {
		ItemStack item = SopCrates.getInstance().getSoplib().getItemUtils().getHead("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjkxYWM0MzJhYTQwZDdlN2E2ODdhYTg1MDQxZGU2MzY3MTJkNGYwMjI2MzJkZDUzNTZjODgwNTIxYWYyNzIzYSJ9fX0=", "Next Page");
		item.setAmount(nextPage);
		SopCrates.getInstance().getSoplib().getItemUtils().setCustomItemKey(item, "item.acrates.next", "Next в†’");
		return item;
		
	}
	
	public boolean checkInventory(Inventory inventory) {
		for(Inventory i : this.inventory) {
			if(i.equals(inventory))
				return true;
		}
		return false;
	}
}
