package net.enelson.sopcrates.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called when a player opens a crate/case and the reward roll begins.
 */
public class CrateOpenEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String crateId;

    public CrateOpenEvent(Player player, String crateId) {
        this.player = player;
        this.crateId = crateId;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getCrateId() {
        return this.crateId;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
