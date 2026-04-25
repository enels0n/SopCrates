package net.enelson.sopcrates.crates;

import org.bukkit.Bukkit;

final class HologramHandles {

	private HologramHandles() {
	}

	static HologramHandle create(String name) {
		if (Bukkit.getPluginManager().isPluginEnabled("FancyHolograms")) {
			return new FancyHologramHandle(name);
		}
		return new NoopHologramHandle();
	}
}