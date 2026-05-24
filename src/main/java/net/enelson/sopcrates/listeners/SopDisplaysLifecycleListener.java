package net.enelson.sopcrates.listeners;

import net.enelson.sopcrates.SopCrates;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public final class SopDisplaysLifecycleListener implements Listener {

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!"SopDisplays".equalsIgnoreCase(event.getPlugin().getName())) {
            return;
        }
        if (SopCrates.getInstance().getCratesManager() != null) {
            SopCrates.getInstance().getCratesManager().ensureExternalHologramsPresent();
        }
    }
}
