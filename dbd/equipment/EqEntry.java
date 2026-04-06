package pl.dbd.equipment;

import org.bukkit.Material;
import java.util.List;

/**
 * Reprezentuje jeden wpis w ekwipunku (przedmiot lub perk) wczytany z
 * konfiguracji.
 */
public record EqEntry(Type type, Role role, String id, String display, Material material, List<String> desc,
        boolean consumable) {

    public enum Type {
        ITEM, PERK
    }

    public enum Role {
        SURVIVOR, KILLER
    }

    public String serialize() {
        return id;
    }

    // Adapter do starych Listenerów (zapobiega błędom kompilacji)
    public Material getMaterial() {
        return material;
    }
}