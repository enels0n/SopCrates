package net.enelson.sopcrates.crates;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import net.enelson.sopcrates.SopCrates;

final class LegacySlot implements Slot {

    private final ArmorStand armorStand;
    private OpenPrize prize;

    LegacySlot(Location location, float scale) {
        this.armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        this.armorStand.setVisible(false);
        this.armorStand.setGravity(false);
        this.armorStand.setBasePlate(false);
        this.armorStand.setArms(false);
        this.armorStand.setInvulnerable(true);
        this.armorStand.setMarker(true);
        this.armorStand.setSmall(scale < 1.0F);
        this.armorStand.setMetadata("ACrates", new FixedMetadataValue(SopCrates.getInstance(), true));
    }

    @Override
    public void setPrize(OpenPrize prize) {
        this.prize = prize;
        ItemStack displayItem = prize == null ? null : prize.getDisplayItem().clone();
        if (displayItem != null) {
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
                meta.setLore(null);
                displayItem.setItemMeta(meta);
            }
        }
        this.armorStand.getEquipment().setHelmet(displayItem);
    }

    @Override
    public OpenPrize getPrize() {
        return this.prize;
    }

    @Override
    public void remove() {
        this.armorStand.remove();
    }
}