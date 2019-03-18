package cat.nyaa.deathchest;

import org.bukkit.plugin.java.JavaPlugin;

public class DeathChestPlugin extends JavaPlugin {
    public static DeathChestPlugin plugin;
    Config config;
    Events events;
    Commands commands;
    I18n i18n;
    ChestManager manager;


    @Override
    public void onLoad() {
        super.onLoad();
        plugin = this;
        config = new Config();
        config.load();
        i18n = new I18n(config.language);
        i18n.load();
        events = new Events();
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(events, plugin);
        commands = new Commands(this, i18n);
        getCommand("deathchest").setExecutor(commands);
        manager = new ChestManager();
    }
}
