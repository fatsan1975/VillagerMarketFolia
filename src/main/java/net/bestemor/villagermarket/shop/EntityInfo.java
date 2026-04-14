package net.bestemor.villagermarket.shop;

import net.bestemor.villagermarket.VMPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

public class EntityInfo {

    private final VMPlugin plugin;
    private final FileConfiguration config;
    private final VillagerShop shop;

    private String name = "Villager Shop";
    private Location location = null;

    public EntityInfo(VMPlugin plugin, FileConfiguration config, VillagerShop shop) {
        this.plugin = plugin;
        this.config = config;
        this.shop = shop;

        if (config.getString("entity.name") != null) {
            this.name = config.getString("entity.name");

            double x = config.getDouble("entity.location.x");
            double y = config.getDouble("entity.location.y");
            double z = config.getDouble("entity.location.z");

            org.bukkit.World world = org.bukkit.Bukkit.getWorld(config.getString("entity.location.world"));
            if (world != null) {
                this.location = new Location(world, x, y, z);
            }
        }
    }

    public void save() {
        config.set("entity.name", name);
        if (location == null || location.getWorld() == null) {
            return;
        }
        config.set("entity.location.x", location.getX());
        config.set("entity.location.y", location.getY());
        config.set("entity.location.z", location.getZ());
        config.set("entity.location.world", location.getWorld().getName());
    }

    private boolean isProfession(String s) {
        if (s == null) {
            return false;
        }
        for (Villager.Profession profession : Villager.Profession.values()) {
            if (s.equals(profession.name())) {
                return true;
            }
        }
        return false;
    }

    public void capture(Entity entity) {
        if (entity == null) {
            return;
        }
        if (entity instanceof Villager) {
            config.set("entity.profession", ((Villager) entity).getProfession().name());
        }
        this.name = entity.getName();
        this.location = entity.getLocation().clone();
    }

    public void setName(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public void setLocation(Location location) {
        if (location != null) {
            this.location = location.clone();
        }
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }
}
