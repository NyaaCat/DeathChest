package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.LanguageRepository;
import org.bukkit.plugin.java.JavaPlugin;

public class I18n extends LanguageRepository {
    private static I18n instance;

    private String language;

    I18n(String language){
        this.language = language;
        instance = this;
    }

    public static String format(String format, Object ... args){
        return instance.getFormatted(format, args);
    }
    @Override
    protected JavaPlugin getPlugin() {
        return DeathChestPlugin.plugin;
    }

    @Override
    protected String getLanguage() {
        return language;
    }
}
