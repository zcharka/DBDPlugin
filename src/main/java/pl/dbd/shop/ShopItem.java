package pl.dbd.shop;

import org.bukkit.Material;

public class ShopItem {
    private final String id;
    private final String name;
    private final Material material;
    private final int price;

    public ShopItem(String id, String name, Material material, int price) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.price = price;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public int getPrice() { return price; }
}