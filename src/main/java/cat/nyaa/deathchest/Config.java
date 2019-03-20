package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Config extends FileConfigure {
    @Serializable
    String language = "en_US";
    @Serializable
    boolean enabled = true;
    @Serializable
    Object dropAmount = 27;
    @Serializable
    int removeTime = 600;
    @Serializable
    boolean openSelfChestOnly = true;
    @Serializable
    List<String> enabledWorld = new ArrayList<>();

    @Override
    protected String getFileName() {
        return "config.yml";
    }

    @Override
    protected JavaPlugin getPlugin() {
        return DeathChestPlugin.plugin;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleEnabled(CommandSender sender){
        this.enabled = !enabled;
        String message;
        if (enabled){
            message = "info.enabled";
        }else {
            message = "info.disabled";
        }
        sender.sendMessage(I18n.format(message));
    }

    public String getDropAmount() {
        return String.valueOf(dropAmount);
    }

    public int getRemoveTime() {
        return removeTime;
    }
}
