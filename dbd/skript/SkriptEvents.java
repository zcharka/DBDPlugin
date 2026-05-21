package pl.dbd.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SkriptEvents {

    public static void register() {
        // Zdarzenie: on player unhook
        Skript.registerEvent("Player Unhook", SimpleEvent.class, PlayerUnhookEvent.class, "player unhook");
        EventValues.registerEventValue(PlayerUnhookEvent.class, Player.class, new Getter<Player, PlayerUnhookEvent>() {
            @Override
            public Player get(PlayerUnhookEvent e) {
                return e.getRescuer();
            }
        }, 0);

        // Wyrażenie: unhooked player
        Skript.registerExpression(ExprUnhookedPlayer.class, Player.class, ExpressionType.SIMPLE, "[the] unhooked player");

        // Zdarzenie: on generator complete
        Skript.registerEvent("Generator Complete", SimpleEvent.class, GeneratorCompleteEvent.class, "generator complete");
        EventValues.registerEventValue(GeneratorCompleteEvent.class, Player.class, new Getter<Player, GeneratorCompleteEvent>() {
            @Override
            public Player get(GeneratorCompleteEvent e) {
                return e.getPlayer();
            }
        }, 0);
        // Zdarzenie: on player injured
        Skript.registerEvent("Player Injured", SimpleEvent.class, PlayerInjuredEvent.class, "player injur(ed|e)");
        EventValues.registerEventValue(PlayerInjuredEvent.class, Player.class, new Getter<Player, PlayerInjuredEvent>() {
            @Override
            public Player get(PlayerInjuredEvent e) { return e.getVictim(); }
        }, 0);

        // Zdarzenie: on player downed
        Skript.registerEvent("Player Downed", SimpleEvent.class, PlayerDownedEvent.class, "player downed");
        EventValues.registerEventValue(PlayerDownedEvent.class, Player.class, new Getter<Player, PlayerDownedEvent>() {
            @Override
            public Player get(PlayerDownedEvent e) { return e.getVictim(); }
        }, 0);

        // Zdarzenie: on player death
        Skript.registerEvent("Player Death", SimpleEvent.class, PlayerDeathEvent.class, "player death");
        EventValues.registerEventValue(PlayerDeathEvent.class, Player.class, new Getter<Player, PlayerDeathEvent>() {
            @Override
            public Player get(PlayerDeathEvent e) { return e.getVictim(); }
        }, 0);

        // Zdarzenie: on killer stun
        Skript.registerEvent("Killer Stun", SimpleEvent.class, KillerStunEvent.class, "killer stun");
        EventValues.registerEventValue(KillerStunEvent.class, Player.class, new Getter<Player, KillerStunEvent>() {
            @Override
            public Player get(KillerStunEvent e) { return e.getKiller(); }
        }, 0);

        // Wyrażenia dodatkowe
        Skript.registerExpression(ExprEventKiller.class, Player.class, ExpressionType.SIMPLE, "[the] event-killer", "[the] killer");
        Skript.registerExpression(ExprEventSurvivor.class, Player.class, ExpressionType.SIMPLE, "[the] event-survivor", "[the] survivor");

        // Efekty Stun
        Skript.registerEffect(EffectStunKiller.class, 
            "dbd stun %player% [by %-player%] for %timespan%",
            "dbd blind %player% [by %-player%] for %timespan%",
            "dbd stun and blind %player% [by %-player%] for %timespan%"
        );
    }

    // --- EVENTY BUKKITOWE ---

    public static class PlayerUnhookEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player rescuer;
        private final Player target;

        public PlayerUnhookEvent(Player rescuer, Player target) {
            this.rescuer = rescuer;
            this.target = target;
        }

        public Player getRescuer() { return rescuer; }
        public Player getTarget() { return target; }

        @Override
        public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    public static class GeneratorCompleteEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player player;

        public GeneratorCompleteEvent(Player player) {
            this.player = player;
        }

        public Player getPlayer() { return player; }

        @Override
        public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    public static class PlayerInjuredEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player victim;
        private final Player killer;
        public PlayerInjuredEvent(Player victim, Player killer) { this.victim = victim; this.killer = killer; }
        public Player getVictim() { return victim; }
        public Player getKiller() { return killer; }
        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    public static class PlayerDownedEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player victim;
        private final Player killer;
        public PlayerDownedEvent(Player victim, Player killer) { this.victim = victim; this.killer = killer; }
        public Player getVictim() { return victim; }
        public Player getKiller() { return killer; }
        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    public static class PlayerDeathEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player victim;
        public PlayerDeathEvent(Player victim) { this.victim = victim; }
        public Player getVictim() { return victim; }
        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    public static class KillerStunEvent extends Event {
        private static final HandlerList handlers = new HandlerList();
        private final Player killer;
        private final Player survivor;
        private final boolean blinded;
        private final boolean slowed;
        public KillerStunEvent(Player killer, Player survivor, boolean blinded, boolean slowed) {
            this.killer = killer; this.survivor = survivor; this.blinded = blinded; this.slowed = slowed;
        }
        public Player getKiller() { return killer; }
        public Player getSurvivor() { return survivor; }
        public boolean isBlinded() { return blinded; }
        public boolean isSlowed() { return slowed; }
        @Override public HandlerList getHandlers() { return handlers; }
        public static HandlerList getHandlerList() { return handlers; }
    }

    // --- WYRAŻENIA SKRIPTA ---

    public static class ExprUnhookedPlayer extends SimpleExpression<Player> {
        @Override
        protected Player[] get(Event e) {
            if (e instanceof PlayerUnhookEvent) {
                return new Player[] { ((PlayerUnhookEvent) e).getTarget() };
            }
            return null;
        }

        @Override
        public boolean isSingle() { return true; }

        @Override
        public Class<? extends Player> getReturnType() { return Player.class; }

        @Override
        public String toString(Event e, boolean debug) { return "unhooked player"; }

        @Override
        public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
            if (!getParser().isCurrentEvent(PlayerUnhookEvent.class)) {
                Skript.error("The expression 'unhooked player' can only be used in 'on player unhook' event!");
                return false;
            }
            return true;
        }
    }

    public static class ExprEventKiller extends SimpleExpression<Player> {
        @Override
        protected Player[] get(Event e) {
            if (e instanceof PlayerInjuredEvent) return new Player[] { ((PlayerInjuredEvent) e).getKiller() };
            if (e instanceof PlayerDownedEvent) return new Player[] { ((PlayerDownedEvent) e).getKiller() };
            return null;
        }

        @Override
        public boolean isSingle() { return true; }

        @Override
        public Class<? extends Player> getReturnType() { return Player.class; }

        @Override
        public String toString(Event e, boolean debug) { return "event killer"; }

        @Override
        public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
            if (!getParser().isCurrentEvent(PlayerInjuredEvent.class, PlayerDownedEvent.class)) {
                Skript.error("The expression 'event killer' can only be used in injured/downed events!");
                return false;
            }
            return true;
        }
    }

    public static class ExprEventSurvivor extends SimpleExpression<Player> {
        @Override
        protected Player[] get(Event e) {
            if (e instanceof KillerStunEvent) return new Player[] { ((KillerStunEvent) e).getSurvivor() };
            return null;
        }

        @Override
        public boolean isSingle() { return true; }

        @Override
        public Class<? extends Player> getReturnType() { return Player.class; }

        @Override
        public String toString(Event e, boolean debug) { return "event survivor"; }

        @Override
        public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
            if (!getParser().isCurrentEvent(KillerStunEvent.class)) {
                Skript.error("The expression 'event survivor' can only be used in 'on killer stun' event!");
                return false;
            }
            return true;
        }
    }

    public static class EffectStunKiller extends ch.njol.skript.lang.Effect {
        private Expression<Player> killerExpr;
        private Expression<Player> survivorExpr;
        private Expression<ch.njol.skript.util.Timespan> timeExpr;
        private int pattern;

        @Override
        @SuppressWarnings("unchecked")
        public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
            killerExpr = (Expression<Player>) exprs[0];
            survivorExpr = (Expression<Player>) exprs[1];
            timeExpr = (Expression<ch.njol.skript.util.Timespan>) exprs[2];
            pattern = matchedPattern;
            return true;
        }

        @Override
        protected void execute(Event event) {
            Player killer = killerExpr != null ? killerExpr.getSingle(event) : null;
            Player survivor = survivorExpr != null ? survivorExpr.getSingle(event) : null;
            ch.njol.skript.util.Timespan timespan = timeExpr != null ? timeExpr.getSingle(event) : null;

            if (killer == null || timespan == null) return;
            long ticks = timespan.getTicks_i();

            boolean blind = (pattern == 1 || pattern == 2);
            boolean slow = (pattern == 0 || pattern == 2);

            pl.dbd.DBDPlugin.getInstance().getStunManager().stunKiller(killer, survivor, blind, slow, (int) ticks);
        }

        @Override
        public String toString(Event event, boolean debug) {
            return "stun/blind killer";
        }
    }
}
