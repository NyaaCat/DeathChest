package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class Commands extends CommandReceiver {

    public Commands(JavaPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @SubCommand("enable")
    void onEnable(CommandSender sender, Arguments arguments){
        DeathChestPlugin.plugin.config.toggleEnabled(sender);
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }
}
