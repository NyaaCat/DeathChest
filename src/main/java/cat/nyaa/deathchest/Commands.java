package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.CommandReceiver;
import cat.nyaa.nyaacore.ILocalizer;
import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.OfflinePlayerUtils;
import org.bukkit.OfflinePlayer;
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
        OfflinePlayer player = (Player) sender;
        String top = arguments.top();
        if (top != null) {
            if (sender.isOp()) {
                String playerName = arguments.nextString();
                player = OfflinePlayerUtils.lookupPlayer(playerName);
            } else {
                new Message(I18n.format("error.permission")).send(sender);
                return;
            }
        }
        if (sender.isOp() || sender.hasPermission("dc.unlock")) {
            boolean unlocked = ChestManager.toggleLock(player);
            if (!player.getUniqueId().equals(((Player) sender).getUniqueId())){
                String message;
                if (unlocked){
                    message = "info.unlocked_player";
                }else{
                    message = "info.locked_player";
                }
                new Message(I18n.format(message, player.getName())).send(sender);
            }
        } else {
            sender.sendMessage(I18n.format("error.permission"));
        }
    }

    @Override
    public String getHelpPrefix() {
        return "";
    }
}
