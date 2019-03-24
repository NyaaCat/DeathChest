package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Commands extends CommandReceiver {

    public Commands(JavaPlugin plugin, ILocalizer _i18n) {
        super(plugin, _i18n);
    }

    @SubCommand("enable")
    void onEnable(CommandSender sender, Arguments arguments) {
        if (sender.isOp()) DeathChestPlugin.plugin.config.toggleEnabled(sender);
        else sender.sendMessage(I18n.format("error.permission"));
    }

    @SubCommand("reload")
    void onReload(CommandSender sender, Arguments arguments) {
        if (sender.isOp()) {
            DeathChestPlugin.plugin.reload();
        } else {
            sender.sendMessage(I18n.format("error.permission"));
        }
    }

    @SubCommand("unlock")
    void onUnlock(CommandSender sender, Arguments arguments) {
        if (!(sender instanceof Player)) {
            return;
        }
        if (sender.hasPermission("dc.unlock")) {
            Player player = (Player) sender;
            ChestManager.toggleLock(player);
        }else {
            sender.sendMessage(I18n.format("error.permission"));
        }
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }
}
