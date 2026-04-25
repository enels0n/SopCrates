package net.enelson.sopcrates.crates;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

final class FancyHologramHandle implements HologramHandle {

    private final String name;
    private Object hologram;
    private boolean hidden;

    FancyHologramHandle(String name) {
        this.name = name;
    }

    private boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("FancyHolograms");
    }

    @Override
    public void refresh(Location location, List<String> lines) {
        if (!isAvailable()) {
            this.hologram = null;
            return;
        }

        try {
            Object manager = getHologramManager();
            Optional<?> existing = getOptionalHologram(manager);

            if (existing.isPresent()) {
                this.hologram = existing.get();

                Object data = this.hologram.getClass().getMethod("getData").invoke(this.hologram);
                updateData(data, location, lines);

                invokeIfExists(this.hologram, "forceUpdate");
                invokeIfExists(this.hologram, "queueUpdate");
            } else {
                Object data = createTextData(location, lines);

                Class<?> hologramDataClass = loadFancyClass("de.oliver.fancyholograms.api.data.HologramData");
                Method createMethod = manager.getClass().getMethod("create", hologramDataClass);
                this.hologram = createMethod.invoke(manager, data);

                Class<?> hologramClass = loadFancyClass("de.oliver.fancyholograms.api.hologram.Hologram");
                Method addMethod = manager.getClass().getMethod("addHologram", hologramClass);
                addMethod.invoke(manager, this.hologram);

                try {
                    addMethod.invoke(manager, this.hologram);
                } catch (Exception ignored) {
                    manager.getClass().getMethod("addHologram", this.hologram.getClass()).invoke(manager, this.hologram);
                }

                invokeIfExists(this.hologram, "forceUpdate");
                invokeIfExists(this.hologram, "queueUpdate");
            }

            if (this.hidden) {
                hide();
            } else {
                show();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void hide() {
        this.hidden = true;
        if (this.hologram == null) {
            return;
        }

        for (Player player : getOnlinePlayers()) {
            invokeViewerMethod("forceHideHologram", player);
        }
    }

    @Override
    public void show() {
        this.hidden = false;
        if (this.hologram == null) {
            return;
        }

        for (Player player : getOnlinePlayers()) {
            invokeViewerMethod("forceShowHologram", player);
        }
    }

    @Override
    public void remove() {
        if (!isAvailable() || this.hologram == null) {
            this.hologram = null;
            return;
        }

        try {
            Object manager = getHologramManager();

            try {
                Method removeMethod = manager.getClass().getMethod("removeHologram", this.hologram.getClass());
                removeMethod.invoke(manager, this.hologram);
            } catch (NoSuchMethodException ignored) {
                Class<?> hologramClass = loadFancyClass("de.oliver.fancyholograms.api.hologram.Hologram");
                manager.getClass().getMethod("removeHologram", hologramClass).invoke(manager, this.hologram);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            this.hologram = null;
        }
    }

    private Object createTextData(Location location, List<String> lines) throws Exception {
        Class<?> textDataClass = loadFancyClass("de.oliver.fancyholograms.api.data.TextHologramData");
        Constructor<?> constructor = textDataClass.getConstructor(String.class, Location.class);
        Object data = constructor.newInstance(this.name, location);

        updateData(data, location, lines);
        return data;
    }

    private void updateData(Object data, Location location, List<String> lines) throws Exception {
        invokeIfExists(data, "setLocation", new Class<?>[]{Location.class}, new Object[]{location});
        invokeIfExists(data, "setText", new Class<?>[]{List.class}, new Object[]{lines});
        invokeIfExists(data, "setPersistent", new Class<?>[]{boolean.class}, new Object[]{false});
        trySetBillboardCenter(data);
    }

    private void trySetBillboardCenter(Object data) {
        try {
            Class<?> billboardClass = Class.forName("org.bukkit.entity.Display$Billboard");
            Object center = Enum.valueOf((Class<Enum>) billboardClass.asSubclass(Enum.class), "CENTER");
            invokeIfExists(data, "setBillboard", new Class<?>[]{billboardClass}, new Object[]{center});
        } catch (Throwable ignored) {
        }
    }

    private Optional<?> getOptionalHologram(Object manager) throws Exception {
        Method getHologramMethod = manager.getClass().getMethod("getHologram", String.class);
        return (Optional<?>) getHologramMethod.invoke(manager, this.name);
    }

    private Object getHologramManager() throws Exception {
        Class<?> pluginApiClass = loadFancyClass("de.oliver.fancyholograms.api.FancyHologramsPlugin");
        Object pluginApi = pluginApiClass.getMethod("get").invoke(null);
        return pluginApiClass.getMethod("getHologramManager").invoke(pluginApi);
    }

    private void invokeViewerMethod(String methodName, Player player) {
        try {
            this.hologram.getClass().getMethod(methodName, Player.class).invoke(this.hologram, player);
        } catch (Exception ignored) {
        }
    }

    private void invokeIfExists(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
        }
    }

    private void invokeIfExists(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
        } catch (Exception ignored) {
        }
    }

    private Collection<? extends Player> getOnlinePlayers() {
        try {
            return Bukkit.getOnlinePlayers();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private ClassLoader getFancyClassLoader() {
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException("FancyHolograms is not enabled");
        }
        return plugin.getClass().getClassLoader();
    }

    private Class<?> loadFancyClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, getFancyClassLoader());
    }
}