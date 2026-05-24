package net.enelson.sopcrates.crates;

import java.util.List;
import java.util.Map;
import org.bukkit.Location;

interface HologramHandle {
	void refresh(Location location, List<String> lines, Map<String, Object> options);
	void hide();
	void show();
	void remove();
	void ensurePresent();
}
