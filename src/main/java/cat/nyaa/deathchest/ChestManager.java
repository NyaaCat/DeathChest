package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.timer.TimerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.IsoFields;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChestManager {
    private static ChestManager instance;
    PersistantChest persistantChest;
    Map<Location, DeathChest> chestMap;
    RemoveList removeList;
//    TimerManager timer;

    public static DeathChest getChest(Location location) {
        return instance.chestMap.get(location);
    }

    public static void newChest(Block block, Player player) {
        DeathChestPlugin plugin = DeathChestPlugin.plugin;
        DeathChest deathChest = new DeathChest(block, player);
        instance.addChest(block.getLocation(), deathChest);
        instance.removeList.submit(deathChest, () -> {
            instance.removeChest(block.getLocation(), deathChest);
        });
//        TimerData timerData = createTimerData();
//        instance.timer.registerTimer(plugin, getLoc(block.getLocation()),
//                timerData,
//                (timerName, handler, designatedTime, isResetCallback) ->
//                        instance.removeChest(block.getLocation(),deathChest), null
//        );
    }

    private static TimerData createTimerData() {
        DeathChestPlugin plugin = DeathChestPlugin.plugin;
        TimerData timerData = new TimerData();
        timerData.autoReset = false;
        int removeTime = plugin.config.getRemoveTime();
        timerData.duration = Duration.ofSeconds(removeTime);
        return timerData;
    }

    static String getLoc(Location location) {
        return new StringBuilder()
                .append(location.getWorld())
                .append(location.getBlockX())
                .append(location.getBlockY())
                .append(location.getBlockZ())
                .toString();
    }

    public ChestManager() {
        persistantChest = new PersistantChest();
        persistantChest.load();
        chestMap = new LinkedHashMap<>();
//        timer = TimerManager.instance();
        instance = this;
        removeList = new RemoveList();
    }

    public static boolean hasChestAt(Location location) {
        DeathChest deathChest = instance.chestMap.get(location);
        return deathChest != null;
    }

    public void addChest(Location location, DeathChest deathChest) {
        Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, () -> {
            chestMap.put(location, deathChest);
            ChestInfo chestInfo = new ChestInfo();
            chestInfo.loc = getLoc(location);
            chestInfo.playerUID = deathChest.deathPlayer.getUniqueId().toString();
            persistantChest.chests.put(chestInfo.loc, chestInfo);
            persistantChest.save();
        });
    }

    public void removeChest(Location location, DeathChest deathChest) {
        Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, () -> {
            if (!deathChest.removed) {
                if (deathChest.chestBlock.getType().equals(Material.CHEST)) {
                    Chest chestBlock = (Chest) deathChest.chestBlock.getState();
                    chestBlock.getBlockInventory().clear();
                    deathChest.chestBlock.setType(Material.AIR);
                    deathChest.removed = true;
                    chestMap.remove(location);
                    persistantChest.chests.remove(getLoc(location));
                    persistantChest.save();
                }

            }
        });
    }

    public static class RemoveList extends FileConfigure {
        @Serializable
        Map<String, Long> scheduledMap = new LinkedHashMap<>();
        Map<String, Runnable> runnableMap = new HashMap<>();

        RemoveList(){
            Bukkit.getScheduler().runTaskTimer(DeathChestPlugin.plugin, ()->{
                removeLoop();
            }, 0, 200);
        }

        @Override
        protected String getFileName() {
            return "remove_cache.yml";
        }

        @Override
        protected JavaPlugin getPlugin() {
            return DeathChestPlugin.plugin;
        }

        public void removeLoop(){
            if ((!scheduledMap.isEmpty())) {
                scheduledMap.forEach((s, aLong) -> {
                    if (System.currentTimeMillis() >= aLong){
                        scheduledMap.remove(s);
                        runnableMap.get(s).run();
                        runnableMap.remove(s);
                    }
                });
                this.save();
            }
        }

        public void submit(DeathChest deathChest, Runnable runnable) {
            int removeTime = DeathChestPlugin.plugin.config.removeTime;
            long removeInstant = System.currentTimeMillis() + removeTime * 1000;
            String loc = getLoc(deathChest.chestBlock.getLocation());
            final Map finalScheduledMap = scheduledMap;
            scheduledMap.put(loc, removeInstant);
            runnableMap.put(loc,runnable);
            Bukkit.getScheduler().runTaskLater(DeathChestPlugin.plugin, () -> {
                synchronized (finalScheduledMap) {
                    scheduledMap.remove(loc);
                    runnable.run();
                }
            }, removeTime * 20);
            Bukkit.getScheduler().runTask(DeathChestPlugin.plugin,()-> this.save());
        }
    }

    public static class PersistantChest extends FileConfigure {
        @Serializable
        Map<String, ChestInfo> chests = new LinkedHashMap<>();

        @Override
        protected String getFileName() {
            return "chests.yml";
        }

        @Override
        protected JavaPlugin getPlugin() {
            return DeathChestPlugin.plugin;
        }
    }

    private static class ChestInfo implements ISerializable {
        @Serializable
        public String loc;
        @Serializable
        public String playerUID;

        public ChestInfo() {
        }
    }
}
