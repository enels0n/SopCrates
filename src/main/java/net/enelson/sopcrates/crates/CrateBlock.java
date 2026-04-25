package net.enelson.sopcrates.crates;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import net.enelson.sopcrates.SopCrates;

public class CrateBlock {

	private final Location location;
	private final String crateName;
	private final double direction;
	private final HologramHandle hologram;

	private boolean hologramSuppressed = false;

	CrateBlock(Location location, String crateName, double direction) {
		this.location = location;
		this.crateName = crateName;
		this.direction = direction;
		this.hologram = HologramHandles.create(generateHologramName(location));
	}

	public Location getLocation() {
		return this.location.clone();
	}

	public String getCrateName() {
		return this.crateName;
	}

	public double getDirection() {
		return this.direction;
	}

	public void refreshHologram() {
		if (this.hologramSuppressed) {
			return;
		}

		if (!this.isHologramEnabled()) {
			this.hologram.remove();
			return;
		}

		this.hologram.refresh(this.getHologramLocation(), this.getHologramLines());
	}

	public void hideHologram() {
		this.hologram.hide();
	}

	public void showHologram() {
		if (this.hologramSuppressed) {
			return;
		}
		this.hologram.show();
	}

	public void removeHologram() {
		this.hologram.remove();
	}

	public void suppressHologram() {
		this.hologramSuppressed = true;
		this.hologram.remove();
	}

	public void unsuppressHologram() {
		this.hologramSuppressed = false;
		this.refreshHologram();
	}

	public boolean shouldHideHologramWhileSpinning() {
		return this.getCrateConfigValue("hologram.hideWhileSpinning", true);
	}

	private Location getHologramLocation() {
		Location base = this.location.clone().add(0.5D, 0.5D, 0.5D);
		return base.add(
				this.getCrateConfigDouble("hologram.offset.x", 0.0D),
				this.getCrateConfigDouble("hologram.offset.y", 1.85D),
				this.getCrateConfigDouble("hologram.offset.z", 0.0D)
		);
	}

	private List<String> getHologramLines() {
		YamlConfiguration config = SopCrates.getInstance().getCratesManager().getCrateConfig(this.crateName);
		List<String> lines = config.getStringList("hologram.lines");
		if (lines == null || lines.isEmpty()) {
			String fallback = config.getString("displayName", this.crateName)
					.replace("%page%", "")
					.replace("%pages%", "")
					.replace("()", "")
					.trim();
			return Collections.singletonList(fallback);
		}
		return lines;
	}

	private boolean isHologramEnabled() {
		return this.getCrateConfigValue("hologram.enabled", false);
	}

	private boolean getCrateConfigValue(String path, boolean def) {
		YamlConfiguration config = SopCrates.getInstance().getCratesManager().getCrateConfig(this.crateName);
		return config.getBoolean(path, def);
	}

	private double getCrateConfigDouble(String path, double def) {
		YamlConfiguration config = SopCrates.getInstance().getCratesManager().getCrateConfig(this.crateName);
		return config.getDouble(path, def);
	}

	private String generateHologramName(Location location) {
		return "acrates_" + location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_"
				+ location.getBlockZ();
	}
}