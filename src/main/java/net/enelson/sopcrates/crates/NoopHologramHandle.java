package net.enelson.sopcrates.crates;

import java.util.List;
import org.bukkit.Location;

final class NoopHologramHandle implements HologramHandle {

	@Override
	public void refresh(Location location, List<String> lines) {
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
}