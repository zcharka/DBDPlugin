package pl.dbd.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerHealFinishEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Player healer;
    private final Player target;

    public PlayerHealFinishEvent(Player healer, Player target) {
        this.healer = healer;
        this.target = target;
    }

    public Player getHealer() {
        return healer;
    }

    public Player getTarget() {
        return target;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
