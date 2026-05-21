package pl.dbd.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

public class EffectSetState extends Effect {

    static {
        Skript.registerEffect(EffectSetState.class, "dbd set state of %player% to %string%");
    }

    private Expression<Player> player;
    private Expression<String> state;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        this.player = (Expression<Player>) expressions[0];
        this.state = (Expression<String>) expressions[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Player p = player.getSingle(event);
        String s = state.getSingle(event);

        if (p == null || s == null) return;

        DBDPlugin plugin = DBDPlugin.getInstance();
        if (plugin == null) return;

        PlayerStateManager psm = plugin.getStateManager();
        String stateName = s.toLowerCase();

        switch (stateName) {
            case "downed":
            case "down":
            case "powalony":
                psm.setDowned(p.getUniqueId());
                break;
            case "injured":
            case "ranny":
                psm.setState(p, PlayerStateManager.PlayerState.INJURED);
                break;
            case "healthy":
            case "heal":
            case "zdrowy":
                psm.heal(p);
                break;
            case "dead":
            case "martwy":
                psm.setState(p, PlayerStateManager.PlayerState.DEAD);
                break;
            case "hooked":
            case "hak":
                psm.setState(p, PlayerStateManager.PlayerState.HOOKED);
                break;
            case "carried":
            case "niesiony":
                psm.setState(p, PlayerStateManager.PlayerState.CARRIED);
                break;
            case "in_locker":
            case "locker":
            case "szafa":
                psm.setState(p, PlayerStateManager.PlayerState.IN_LOCKER);
                break;
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "dbd set state of " + player.toString(event, debug) + " to " + state.toString(event, debug);
    }
}
