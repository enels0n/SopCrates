package net.enelson.sopcrates.crates;

import java.util.List;
import org.bukkit.Location;

interface HologramHandle {
	void refresh(Location location, List<String> lines);
	void hide();
	void show();
	void remove();
}