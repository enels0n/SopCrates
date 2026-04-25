package net.enelson.sopcrates.api;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.enelson.sopcrates.SopCrates;

public class Placeholder extends PlaceholderExpansion {

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public String getAuthor() {
		return "E.NeLsOn";
	}

	@Override
	public String getIdentifier() {
		return "sopcrates";
	}

	@Override
	public String getPlugin() {
		return null;
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		String[] st = identifier.split("_");
		// %sopcrates_count_case%
		if(st.length == 2 && st[0].equals("count")) {
			return SopCrates.getInstance().getCratesManager().getKeyCount(player.getName(), st[1])+"";
		}
		return "none";
	}
}
