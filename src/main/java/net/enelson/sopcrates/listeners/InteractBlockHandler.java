package net.enelson.sopcrates.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.tr7zw.nbtapi.NBT;
import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopcrates.SopCrates;
import net.enelson.sopcrates.crates.Crate;
import net.enelson.sopcrates.crates.CrateBlock;
import net.enelson.sopcrates.utils.Utils;

public class InteractBlockHandler implements Listener {
	private List<Player> cooldown;
	
	public InteractBlockHandler() {
		this.cooldown = new ArrayList<>();
	}
	
	@EventHandler
	public void onRightClick(PlayerInteractEvent e) {
		if(!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
			return;
		if(e.getClickedBlock() == null)
			return;

		CrateBlock crateBlock = SopCrates.getInstance().getCratesManager().getCrateBlock(e.getClickedBlock());
		if(crateBlock == null)
			return;
		
		Player player = e.getPlayer();
		
		e.setUseItemInHand(Result.DENY);
		e.setUseInteractedBlock(Result.DENY);
		
		if(this.cooldown.contains(player))
			return;
		 
		this.cooldown.add(player);
		
		Bukkit.getScheduler().runTaskLater(SopCrates.getInstance(), new Runnable() {
			@Override
			public void run() {
				if(e.hasItem() && crateBlock.getCrateName().equals(Utils.getType(e.getItem()))) {
					if(SopCrates.getInstance().getCratesManager().createOpenCrate(crateBlock, player))
						e.getItem().setAmount(e.getItem().getAmount()-1);
				}
				else {
					Crate crate = SopCrates.getInstance().getCratesManager().getCrate(crateBlock.getCrateName());
					if(SopCrates.getInstance().getCratesManager().getCrateKeyType(crate).equals("key")) {
						if(SopCrates.getInstance().getCratesManager().getKeyCount(player.getName(), crateBlock.getCrateName())<1) {
							if(crate.getCrateConfig().getBoolean("denyPush"))
								Utils.pushPlayer(player);
							if(crate.getCrateConfig().getStringList("denyCommands") != null)
								Utils.runCommands(crate.getCrateConfig().getStringList("denyCommands"), player);
							return;
						}
						
						if(SopCrates.getInstance().getCratesManager().createOpenCrate(crateBlock, player)) {
							SopCrates.getInstance().getCratesManager().removeKey(player.getName(), crateBlock.getCrateName(), 1);
						}
					}
					else if(SopCrates.getInstance().getCratesManager().getCrateKeyType(crate).equals("placeholder")) {
						if(Integer.parseInt(PlaceholderAPI.setPlaceholders(player, crate.getCrateConfig().getString("placeholder")))>0) {
							SopCrates.getInstance().getCratesManager().createOpenCrate(crateBlock, player);
						}
					}
				}
			}
		}, 1);

		
		Bukkit.getScheduler().runTaskLater(SopCrates.getInstance(), new Runnable() {
			@Override
			public void run() {
				cooldown.remove(player);
			}
		}, 5);
	}
	
	@EventHandler
	public void onHitCrate(PlayerInteractEvent e) {
		if(!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			return;
		}

		CrateBlock crateBlock = SopCrates.getInstance().getCratesManager().getCrateBlock(e.getClickedBlock());
		if(crateBlock == null)
			return;

		e.setCancelled(true);
		
		Crate crate = SopCrates.getInstance().getCratesManager().getCrate(crateBlock.getCrateName());
		e.getPlayer().openInventory(crate.getMayPrizes());	
	}
	
	@EventHandler
	public void onChangeInventory(InventoryClickEvent e) {
		Crate crate = SopCrates.getInstance().getCratesManager().getCrate(e.getClickedInventory());
		if(crate != null) {
			e.setCancelled(true);
			int slot = e.getSlot();
			if((slot == 48 || slot == 50) && e.getInventory().getItem(slot) != null) {
				((Player)e.getWhoClicked()).openInventory(crate.getMayPizes(e.getInventory().getItem(slot).getAmount()-1));
			}
			else if(e.getWhoClicked().isOp() && e.getInventory().getItem(slot) != null) {
				Player player = (Player)e.getWhoClicked();
				YamlConfiguration crateConfig = SopCrates.getInstance().getCratesManager().getCrateConfig(crate.getCrateName());
				
				String prizeId = NBT.get(e.getInventory().getItem(slot), nbt -> (String) nbt.getString("ACrates-prizeId"));
				
				String prizeLink = "prizes."+prizeId;
				Utils.givePrize(player, crateConfig, prizeLink);
			}
		}
		else if(e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) && SopCrates.getInstance().getCratesManager().getCrate(e.getInventory()) != null)
			e.setCancelled(true);
	}
	
	@EventHandler
	public void onChangeInventory(InventoryDragEvent e) {
		Crate crate = SopCrates.getInstance().getCratesManager().getCrate(e.getInventory());
		if(crate != null)
			e.setCancelled(true);
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		SopCrates.getInstance().getCratesManager().getOpenCrates(e.getPlayer()).forEach(o -> o.forceStop());
	}
}
