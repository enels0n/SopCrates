package net.enelson.sopcrates.crates;

import org.bukkit.Location;

final class SlotFactory {

    private static final boolean MODERN_SUPPORTED = isModernSupported();

    private SlotFactory() {
    }

    static Slot create(Location location, float scale) {
        if (MODERN_SUPPORTED) {
            try {
                return new ModernSlot(location, scale);
            } catch (Throwable ignored) {
            }
        }
        return new LegacySlot(location, scale);
    }

    private static boolean isModernSupported() {
        try {
            Class.forName("org.bukkit.entity.ItemDisplay");
            Enum.valueOf(org.bukkit.entity.EntityType.class, "ITEM_DISPLAY");
            Class.forName("org.bukkit.entity.Display$Billboard");
            Class.forName("org.bukkit.util.Transformation");
            Class.forName("org.joml.Vector3f");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}