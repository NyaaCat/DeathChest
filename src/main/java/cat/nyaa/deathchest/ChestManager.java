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
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        instance.persistantChest.lock(player);
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

    public static boolean isUnlocked(Player player) {
        return instance.persistantChest.isUnlocked(player);
    }

    public static void toggleLock(Player player) {
        boolean unlocked = instance.persistantChest.isUnlocked(player);
        if (unlocked){
            instance.persistantChest.lock(player);
            player.sendMessage(I18n.format("info.locked"));
        }else {
            instance.persistantChest.unlock(player);
            player.sendMessage(I18n.format("info.unlocked"));
        }
    }

    public void addChest(Location location, DeathChest deathChest) {
        Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, () -> {
            chestMap.put(location, deathChest);
            ChestInfo chestInfo = new ChestInfo();
            chestInfo.loc = getLoc(location);
            chestInfo.playerUID = deathChest.deathPlayer.getUniqueId().toString();
            persistantChest.chests.put(chestInfo.loc, chestInfo);
            persistantChest.save();
            String loc = String.format("%s [%d, %d, %d]" , location.getWorld().getName()
                    , location.getBlockX(), location.getBlockY(), location.getBlockZ());
            int removetime = DeathChestPlugin.plugin.config.getRemoveTime();
            deathChest.deathPlayer.sendMessage(I18n.format("info.created", loc, removetime));
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
                    String loc = String.format("%s [%d, %d, %d]" , location.getWorld().getName()
                            , location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    deathChest.deathPlayer.sendMessage(I18n.format("info.removed", loc));
                    instance.persistantChest.removeLock(deathChest.deathPlayer);
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
                Set<String> strings = scheduledMap.keySet();
                for (String s : strings) {
                    Long aLong = scheduledMap.get(s);
                    if (System.currentTimeMillis() >= aLong) {
                        scheduledMap.remove(s);
                        runnableMap.get(s).run();
                        runnableMap.remove(s);
                    }
                }
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
        @Serializable
        Map<String, Boolean> unlockedMap = new LinkedHashMap<>();

        @Override
        protected String getFileName() {
            return "chests.yml";
        }

        @Override
        protected JavaPlugin getPlugin() {
            return DeathChestPlugin.plugin;
        }

        public void unlock(Player player){ unlockedMap.put(player.getUniqueId().toString(), true); }

        public boolean isUnlocked(Player player) {
            Boolean aBoolean = unlockedMap.get(player.getUniqueId().toString());
            return aBoolean != null && aBoolean;
        }

        public void lock(Player player) {
            unlockedMap.put(player.getUniqueId().toString(), false);
        }

        public void removeLock(Player deathPlayer) {
            unlockedMap.remove(deathPlayer.getUniqueId().toString());
        }
    }

    public static class ChestInfo implements ISerializable {
        @Serializable
        public String loc;
        @Serializable
        public String playerUID;

        public ChestInfo() {
        }
    }
}
