package pl.dbd.equipment;

import org.bukkit.Material;

public class EqEntry {
    public enum Role { SURVIVOR, KILLER }
    public enum Type { PERK, ITEM }
    
    private final String id;
    private final Role role;
    private final Type type;
    private final Material material; // Zmiana ze String na Material
    private final String displayName;
    private final String description;
    private final int cost; // Dodano koszt

    public EqEntry(String id, Role role, Type type, Material material, String displayName, String description, int cost) {
        this.id = id;
        this.role = role;
        this.type = type;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
    }

    public String getId() { return id; }
    public Role getRole() { return role; }
    public Type getType() { return type; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getCost() { return cost; }
}