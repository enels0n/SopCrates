package net.enelson.sopcrates.crates;

import net.enelson.sopcrates.SopCrates;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

final class ModernSlot implements Slot {

    private final Entity entity;
    private final float scale;
    private OpenPrize prize;

    ModernSlot(Location location, float scale) {
        this.scale = scale;
        World world = location.getWorld();
        EntityType entityType = EntityType.valueOf("ITEM_DISPLAY");
        this.entity = world.spawnEntity(location, entityType);
        prepareDisplay();
        applyScale();
    }

    @Override
    public void setPrize(OpenPrize prize) {
        this.prize = prize;
        ItemStack displayItem = prize == null ? null : prize.getDisplayItem();
        invoke(this.entity, "setItemStack", new Class<?>[]{ItemStack.class}, new Object[]{displayItem});
    }

    @Override
    public OpenPrize getPrize() {
        return this.prize;
    }

    @Override
    public void remove() {
        this.entity.remove();
    }

    private void prepareDisplay() {
        this.entity.setInvulnerable(true);
        this.entity.setPersistent(false);
        this.entity.setMetadata("ACrates", new FixedMetadataValue(SopCrates.getInstance(), true));

        try {
            Class<?> transformClass = Class.forName("org.bukkit.entity.ItemDisplay$ItemDisplayTransform");
            Object fixed = Enum.valueOf((Class<Enum>) transformClass.asSubclass(Enum.class), "FIXED");
            invoke(this.entity, "setItemDisplayTransform", new Class<?>[]{transformClass}, new Object[]{fixed});
        } catch (Throwable ignored) {
        }
    }

    private void applyScale() {
        try {
            Class<?> billboardClass = Class.forName("org.bukkit.entity.Display$Billboard");
            Object center = Enum.valueOf((Class<Enum>) billboardClass.asSubclass(Enum.class), "CENTER");
            invoke(this.entity, "setBillboard", new Class<?>[]{billboardClass}, new Object[]{center});
        } catch (Throwable ignored) {
        }

        invoke(this.entity, "setInterpolationDelay", new Class<?>[]{int.class}, new Object[]{0});
        invoke(this.entity, "setInterpolationDuration", new Class<?>[]{int.class}, new Object[]{1});
        invoke(this.entity, "setTeleportDuration", new Class<?>[]{int.class}, new Object[]{1});
        invoke(this.entity, "setDisplayWidth", new Class<?>[]{float.class}, new Object[]{0.7F * this.scale});
        invoke(this.entity, "setDisplayHeight", new Class<?>[]{float.class}, new Object[]{0.7F * this.scale});

        try {
            Class<?> transformationClass = Class.forName("org.bukkit.util.Transformation");
            Class<?> vectorClass = Class.forName("org.joml.Vector3f");
            Class<?> axisAngleClass = Class.forName("org.joml.AxisAngle4f");

            Object translation = vectorClass.getConstructor(float.class, float.class, float.class).newInstance(0F, 0F, 0F);
            Object leftRotation = axisAngleClass.getConstructor().newInstance();
            Object scaleVector = vectorClass.getConstructor(float.class, float.class, float.class).newInstance(this.scale, this.scale, this.scale);
            Object rightRotation = axisAngleClass.getConstructor().newInstance();
            Object transformation = transformationClass
                    .getConstructor(vectorClass, axisAngleClass, vectorClass, axisAngleClass)
                    .newInstance(translation, leftRotation, scaleVector, rightRotation);

            invoke(this.entity, "setTransformation", new Class<?>[]{transformationClass}, new Object[]{transformation});
        } catch (Throwable ignored) {
        }
    }

    private void invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (Throwable ignored) {
        }
    }
}