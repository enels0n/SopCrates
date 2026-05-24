package net.enelson.sopcrates.crates;

import java.util.List;
import java.util.Map;
import org.bukkit.Location;

final class NoopHologramHandle implements HologramHandle {

	@Override
	public void refresh(Location location, List<String> lines, Map<String, Object> options) {
	}

	@Override
	public void hide() {
	}

	@Override
	public void show() {
	}

	@Override
	public void remove() {
	}

	@Override
	public void ensurePresent() {
	}
}
