package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Config extends FileConfigure {
    @Serializable
    String language;
    @Serializable
    boolean enabled;
    @Serializable
    Object dropAmount;
    @Serializable
    int removeTime;
    @Serializable
    boolean openSelfChestOnly;
    @Serializable
    List<String> enabledWorld;

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
