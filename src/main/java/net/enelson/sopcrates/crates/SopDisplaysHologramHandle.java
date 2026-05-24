package net.enelson.sopcrates.crates;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import net.enelson.sopcrates.SopCrates;

import java.util.Collections;
import java.util.HashMap;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

final class SopDisplaysHologramHandle implements HologramHandle {

    private final String name;
    private boolean hidden;
    private Location lastLocation;
    private List<String> lastLines;
    private Map<String, Object> lastOptions = Collections.emptyMap();

    SopDisplaysHologramHandle(String name) {
        this.name = name;
    }

    @Override
    public void refresh(Location location, List<String> lines, Map<String, Object> options) {
        this.lastLocation = location == null ? null : location.clone();
        this.lastLines = lines;
        this.lastOptions = options == null ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(options);
        if (this.hidden || location == null) {
            return;
        }

        Object manager = resolveManager();
        if (manager == null) {
            SopCrates.getInstance().getLogger().warning("SopDisplays manager is unavailable for crate hologram '" + this.name + "'.");
            return;
        }

        String text = lines == null ? "" : String.join("\n", lines);
        boolean upserted = invokeBoolean(
                manager,
                "upsertExternalDisplay",
                new Class<?>[]{String.class, Location.class, String.class, Map.class},
                this.name, location, text, this.lastOptions
        );
        if (!upserted) {
            SopCrates.getInstance().getLogger().warning("Failed to upsert crate hologram '" + this.name + "' via SopDisplays.");
        }
    }

    @Override
    public void hide() {
        this.hidden = true;
        remove();
    }

    @Override
    public void show() {
        this.hidden = false;
        if (this.lastLocation != null) {
            refresh(this.lastLocation, this.lastLines, this.lastOptions);
        }
    }

    @Override
    public void remove() {
        Object manager = resolveManager();
        if (manager == null) {
            return;
        }
        boolean removed = invokeBoolean(manager, "removeExternalDisplay", new Class<?>[]{String.class}, this.name);
        if (!removed) {
            invokeBoolean(manager, "remove", new Class<?>[]{String.class}, this.name);
        }
    }

    @Override
    public void ensurePresent() {
        if (this.hidden || this.lastLocation == null) {
            return;
        }

        Object manager = resolveManager();
        if (manager == null) {
            return;
        }

        Object ids = invoke(manager, "getIds", new Class<?>[0]);
        if (!(ids instanceof Collection<?>)) {
            refresh(this.lastLocation, this.lastLines, this.lastOptions);
            return;
        }

        for (Object id : (Collection<?>) ids) {
            if (id != null && this.name.equalsIgnoreCase(String.valueOf(id))) {
                return;
            }
        }

        refresh(this.lastLocation, this.lastLines, this.lastOptions);
    }

    private Object resolveManager() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("SopDisplays");
            if (plugin == null || !plugin.isEnabled()) {
                return null;
            }
            Method getter = plugin.getClass().getMethod("getFocusDisplayManager");
            return getter.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean invokeBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(target, args);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            return method.invoke(target, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
