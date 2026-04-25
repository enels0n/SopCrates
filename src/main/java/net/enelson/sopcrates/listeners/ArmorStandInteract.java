package net.enelson.sopcrates.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.enelson.sopcrates.utils.Utils;

public class ArmorStandInteract implements Listener {

	@EventHandler
	public void onClick(PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked().getMetadata("ACrates").size() > 0) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPrepareItemCraft(PrepareItemCraftEvent e) {
		for (ItemStack item : e.getInventory()) {
			if (Utils.getType(item) != null) {
				e.getInventory().setResult(null);
			}
		}
	}

	@EventHandler
	public void onPrepareItemEnchant(PrepareItemEnchantEvent e) {
		if (Utils.getType(e.getItem()) != null) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlace(BlockPlaceEvent e) {
		String type = Utils.getType(e.getItemInHand());
		if (type != null && !type.equals("")) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onPrepareItemAnvil(InventoryClickEvent e) {
		if (e.getInventory().getType() == InventoryType.ANVIL && (Utils.getType(e.getInventory().getItem(0)) != null
				|| Utils.getType(e.getInventory().getItem(2)) != null)) {
			ItemMeta oldMeta = e.getInventory().getItem(0).getItemMeta();
			ItemMeta meta = e.getCurrentItem().getItemMeta();
			if (meta.hasDisplayName()) {
				if (!meta.getDisplayName().equals(oldMeta.getDisplayName())) {
					this.dontRename((Player) e.getWhoClicked(), e.getCurrentItem(), meta, oldMeta);
				}
			} else if (meta.hasDisplayName() && !oldMeta.hasDisplayName()) {
				this.dontRename((Player) e.getWhoClicked(), e.getCurrentItem(), meta, oldMeta);
			}
		}
	}

	private void dontRename(Player p, ItemStack item, ItemMeta meta, ItemMeta oldMeta) {
		meta.setDisplayName(oldMeta.getDisplayName());
		item.setItemMeta(meta);
	}
}
