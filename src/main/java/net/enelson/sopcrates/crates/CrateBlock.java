package net.enelson.sopcrates.crates;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import net.enelson.sopcrates.SopCrates;

public class CrateBlock {

	private final Location location;
	private final String crateName;
	private final double direction;
	private final List<CrateHologramEntry> holograms;

	CrateBlock(Location location, String crateName, double direction) {
		this.location = location;
		this.crateName = crateName;
		this.direction = direction;
		this.holograms = loadHolograms();
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
		for (CrateHologramEntry entry : this.holograms) {
			entry.refresh();
		}
	}

	public void hideHologram() {
		for (CrateHologramEntry entry : this.holograms) {
			entry.handle.hide();
		}
	}

	public void showHologram() {
		for (CrateHologramEntry entry : this.holograms) {
			if (!entry.suppressed) {
				entry.handle.show();
			}
		}
	}

	public void removeHologram() {
		for (CrateHologramEntry entry : this.holograms) {
			entry.handle.remove();
		}
	}

	public void ensureHologramPresent() {
		for (CrateHologramEntry entry : this.holograms) {
			entry.ensurePresent();
		}
	}

	public void suppressHologram() {
		for (CrateHologramEntry entry : this.holograms) {
			if (!entry.hideWhileSpinning) {
				continue;
			}
			entry.suppressed = true;
			entry.handle.remove();
		}
	}

	public void unsuppressHologram() {
		for (CrateHologramEntry entry : this.holograms) {
			if (!entry.hideWhileSpinning) {
				continue;
			}
			entry.suppressed = false;
		}
		refreshHologram();
	}

	public boolean shouldHideHologramWhileSpinning() {
		for (CrateHologramEntry entry : this.holograms) {
			if (entry.hideWhileSpinning) {
				return true;
			}
		}
		return false;
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

	private List<CrateHologramEntry> loadHolograms() {
		YamlConfiguration config = SopCrates.getInstance().getCratesManager().getCrateConfig(this.crateName);
		List<CrateHologramEntry> entries = new ArrayList<CrateHologramEntry>();

		ConfigurationSection multi = config.getConfigurationSection("holograms");
		if (multi != null && !multi.getKeys(false).isEmpty()) {
			for (String key : multi.getKeys(false)) {
				ConfigurationSection section = multi.getConfigurationSection(key);
				if (section == null) {
					continue;
				}
				entries.add(buildEntry(section, key, config));
			}
		} else {
			ConfigurationSection legacy = config.getConfigurationSection("hologram");
			if (legacy != null) {
				entries.add(buildEntry(legacy, "main", config));
			}
		}
		return entries;
	}

	private CrateHologramEntry buildEntry(ConfigurationSection section, String id, YamlConfiguration crateConfig) {
		String name = generateHologramName(this.location) + "_" + id.toLowerCase();
		HologramHandle handle = HologramHandles.create(name);

		boolean enabled = section.getBoolean("enabled", false);
		boolean hideWhileSpinning = section.getBoolean("hideWhileSpinning", true);
		double x = section.getDouble("offset.x", 0.0D);
		double y = section.getDouble("offset.y", 1.85D);
		double z = section.getDouble("offset.z", 0.0D);
		float yaw = (float) section.getDouble("yaw", 0.0D);
		float pitch = (float) section.getDouble("pitch", 0.0D);

		List<String> lines = section.getStringList("lines");
		if (lines == null || lines.isEmpty()) {
			String fallback = crateConfig.getString("displayName", this.crateName)
					.replace("%page%", "")
					.replace("%pages%", "")
					.replace("()", "")
					.trim();
			lines = Collections.singletonList(fallback);
		}

		Map<String, Object> options = readOptions(section.getConfigurationSection("sopdisplays"));
		return new CrateHologramEntry(handle, enabled, hideWhileSpinning, x, y, z, yaw, pitch, lines, options);
	}

	private Map<String, Object> readOptions(ConfigurationSection section) {
		if (section == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		for (String key : section.getKeys(false)) {
			Object value = section.get(key);
			if (value instanceof ConfigurationSection) {
				map.put(key, readOptions((ConfigurationSection) value));
			} else {
				map.put(key, value);
			}
		}
		return map;
	}

	private final class CrateHologramEntry {
		private final HologramHandle handle;
		private final boolean enabled;
		private final boolean hideWhileSpinning;
		private final double offsetX;
		private final double offsetY;
		private final double offsetZ;
		private final float yaw;
		private final float pitch;
		private final List<String> lines;
		private final Map<String, Object> options;
		private boolean suppressed;

		private CrateHologramEntry(HologramHandle handle, boolean enabled, boolean hideWhileSpinning, double offsetX, double offsetY, double offsetZ, float yaw, float pitch, List<String> lines, Map<String, Object> options) {
			this.handle = handle;
			this.enabled = enabled;
			this.hideWhileSpinning = hideWhileSpinning;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.yaw = yaw;
			this.pitch = pitch;
			this.lines = lines;
			this.options = options;
			this.suppressed = false;
		}

		private void refresh() {
			if (!this.enabled || this.suppressed) {
				this.handle.remove();
				return;
			}
			Location base = location.clone().add(0.5D, 0.5D, 0.5D);
			Location hologramLocation = base.add(this.offsetX, this.offsetY, this.offsetZ);
			hologramLocation.setYaw(this.yaw);
			hologramLocation.setPitch(this.pitch);
			this.handle.refresh(hologramLocation, this.lines, this.options);
		}

		private void ensurePresent() {
			if (!this.enabled || this.suppressed) {
				return;
			}
			this.handle.ensurePresent();
		}
	}
}
