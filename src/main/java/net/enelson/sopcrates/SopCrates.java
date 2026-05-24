package net.enelson.sopcrates;

import net.enelson.sopcrates.api.Placeholder;
import net.enelson.sopcrates.commands.CommandManager;
import net.enelson.sopcrates.crates.CratesManager;
import net.enelson.sopcrates.listeners.ArmorStandInteract;
import net.enelson.sopcrates.listeners.FireworkHandler;
import net.enelson.sopcrates.listeners.InteractBlockHandler;
import net.enelson.sopcrates.listeners.SopDisplaysLifecycleListener;
import net.enelson.sopli.lib.SopLib;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SopCrates extends JavaPlugin {

    private static SopCrates plugin;

    private CratesManager manager;
    private SopLib soplib;

    @Override
    public void onEnable() {
        this.soplib = SopLib.getInstance();
        if (this.soplib == null) {
            Bukkit.getLogger().warning("SopLib is not installed!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        plugin = this;
        ensureDefaultCrateFiles();

        CommandManager commandManager = new CommandManager();
        PluginCommand command = getCommand("sopcrates");
        if (command != null) {
            command.setExecutor(commandManager);
            command.setTabCompleter(commandManager);
        }

        this.manager = new CratesManager();
        this.manager.reload();

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ArmorStandInteract(), this);
        pluginManager.registerEvents(new FireworkHandler(), this);
        pluginManager.registerEvents(new InteractBlockHandler(), this);
        pluginManager.registerEvents(new SopDisplaysLifecycleListener(), this);

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            new Placeholder().register();
        }

        getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                if (manager != null && pluginManager.isPluginEnabled("SopDisplays")) {
                    manager.ensureExternalHologramsPresent();
                }
            }
        }, 100L, 100L);
    }

    @Override
    public void onDisable() {
        if (this.manager != null) {
            this.manager.onDisable();
        }
    }

    private void ensureDefaultCrateFiles() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File cratesDir = new File(getDataFolder(), "crates");
        if (!cratesDir.exists()) {
            cratesDir.mkdirs();
        }

        File exampleFile = new File(cratesDir, "example.yml");
        if (!exampleFile.exists()) {
            saveResource("crates/example.yml", false);
        }
    }

    public static SopCrates getInstance() {
        return plugin;
    }

    public CratesManager getCratesManager() {
        return this.manager;
    }

    public SopLib getSoplib() {
        return this.soplib;
    }
}
