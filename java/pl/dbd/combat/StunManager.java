package pl.dbd.combat;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pl.dbd.DBDPlugin;
import pl.dbd.state.PlayerStateManager;

/**
 * <h1>API DLA INNYCH AI ORAZ SKRYPTÓW</h1>
 * Ta klasa zarządza systemem ogłuszeń i oślepień (Stun & Blind) Mordercy.
 * Stworzona w celu ułatwienia programowania interakcji Ocalałych z Mordercą
 * (np. rzucanie paletami, użycie latarki).
 * 
 * <h2>Jak używać StunManager?</h2>
 * 
 * <pre>{@code
 * DBDPlugin plugin = DBDPlugin.getInstance(); // Pamiętaj o zdobyciu instancji głównego pluginu
 * StunManager sm = plugin.getStunManager();
 * 
 * // 1. Samo oślepienie (użycie latarki do oczu) na 3 sekundy (60 ticków):
 * sm.stunKiller(killer, survivor, true, false, 60);
 * 
 * // 2. Twardy Stun ze spowolnieniem (np. zrzucenie palety albo uderzenie w
 * // głowę) na 4 sekundy (80 ticków):
 * sm.stunKiller(killer, survivor, false, true, 80);
 * 
 * // 3. Ultymatywne uderzenie (Stun + Blindness) w jednej metodzie:
 * sm.stunKiller(killer, survivor, true, true, 100);
 * }</pre>
 *
 * <h3>Funkcja Ratowania (Flashlight / Pallet Save)</h3>
 * Metoda `stunKiller()` <b>automatycznie</b> weryfikuje, czy uderzony Morderca
 * podczas otrzymywania efeku niósł Ocalałego.
 * Jeśli tak było, mechanika naturalnie symuluje "Pallet Save" - wymusza
 * zrzucenie niesionego gracza a tamten gracz ożywa w stanie INJURED (zamiast
 * DOWNED) aby móc natychmiast uciec (znane z DBD). Nie musisz wcale
 * implementować oddzielnie zrzucania gracza!
 */
public class StunManager {
    private final DBDPlugin plugin;

    public StunManager(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Główna i jedyna metoda służąca do nakładania efektów negatywnych na Mordercę
     * wywołanych przez Ocalałego (stuny, debuffy).
     * 
     * @param killer        Gracz będący mordercą, który ma zostać trafiony czarem
     *                      lub paletą.
     * @param survivor      Gracz (Ocalały), który zadaje stuna (może być null,
     *                      parametr używany jest do budowania komunikatów na
     *                      czacie).
     * @param applyBlind    Jeśli true, nadaje Zabójcy efekt Całkowitej Ślepoty
     *                      (Blindness) - przydatne do latarek (Flashlights).
     * @param applySlow     Jeśli true, nakłada na Zabójcę miażdżące Spowolnienie
     *                      (Slowness VI) drastycznie ucinające mobilność jako
     *                      odpowiednik bycia Ogłuszonym (Stun).
     * @param durationTicks Czas trwania wszystkich nałożonych powyżej efektów
     *                      liczony w Tickach (20 ticków = 1 sekunda).
     */
    public void stunKiller(Player killer, Player survivor, boolean applyBlind, boolean applySlow, int durationTicks) {
        if (killer == null || !killer.isOnline())
            return;

        // --- MECHANIKA ZBAWIENIA (Flashlight / Pallet Save) ---
        // Jeśli killer niósł jakiegoś ocalałego w momencie bycia stunowanym - ratujemy
        // ofiarę!
        if (plugin.getCarrySystem() != null && plugin.getCarrySystem().isCarrying(killer)) {
            Player carried = plugin.getCarrySystem().getCarriedSurvivor(killer);
            if (carried != null) {
                plugin.getCarrySystem().stopCarrying(killer);
                plugin.getStateManager().setState(carried, PlayerStateManager.PlayerState.INJURED);
                carried.sendMessage("§aZostałeś uratowany przed hakiem przez świetne uderzenie!");
            }
            killer.getWorld().playSound(killer.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 0.8f);
            killer.sendMessage("§cZostałeś ogłuszony! Twoja ofiara natychmiastowo ucieka z uścisku!");
        }

        // --- EFEKTY WIZUALNE I GAMEPLAYOWE ---
        if (applyBlind) {
            killer.removePotionEffect(PotionEffectType.BLINDNESS);
            killer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 1, false, false));
            killer.playSound(killer.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.2f);
            if (survivor != null) {
                survivor.sendMessage("§aOślepiłeś mordercę!");
                killer.sendMessage("§cZostałeś oślepiony przez " + survivor.getName() + "!");
            }
        }

        if (applySlow) {
            killer.removePotionEffect(PotionEffectType.SLOWNESS);
            killer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, 6, false, false));
            killer.playSound(killer.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.8f);
            if (survivor != null) {
                survivor.sendMessage("§aOgłuszyłeś mordercę!");
                if (!applyBlind) { // Nie dubluj spamu, jeśli obie opcje nakładane są jednocześnie
                    killer.sendMessage("§cZostałeś ogłuszony przez " + survivor.getName() + "!");
                }
            }
        }
    }
}
