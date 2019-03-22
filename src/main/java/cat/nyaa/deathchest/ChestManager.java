package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.configuration.FileConfigure;
import cat.nyaa.nyaacore.configuration.ISerializable;
import cat.nyaa.nyaacore.timer.TimerData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

public class ChestManager {
    private static ChestManager instance;
    PersistantChest persistantChest;
    RemoveList removeList;
    Map<Location, DeathChest> chestMap;
    //    TimerManager timer;

    public static DeathChest getChest(Location location) {
        return instance.chestMap.get(location);
    }

    public static void newChest(Block block, Player player) {
        DeathChestPlugin plugin = DeathChestPlugin.plugin;
        DeathChest deathChest = new DeathChest(block, player);
        instance.addChest(block.getLocation(), deathChest);
        instance.removeList.submit(deathChest, new RemoveTask(block, deathChest));
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
        String worldName = location.getWorld() == null ? "null" : location.getWorld().getName();
        return new StringBuilder()
                .append(worldName)
                .append("[")
                .append(location.getBlockX())
                .append(",")
                .append(location.getBlockY())
                .append(",")
                .append(location.getBlockZ())
                .append("]")
                .toString();
    }

    public ChestManager() {
        instance = this;
        persistantChest = new PersistantChest();
        persistantChest.load();
        chestMap = new LinkedHashMap<>();
//        timer = TimerManager.instance();
        removeList = new RemoveList();
        removeList.load();
        this.loadFromPersist();
    }


    public void load() {
        persistantChest.load();
        loadFromPersist();
        removeList.load();
    }

    private void loadFromPersist() {
        if (persistantChest.chests != null) {
            if (!persistantChest.chests.isEmpty()) {
                persistantChest.chests.forEach(((s, chestInfo) -> {
                    try {
                        if (s.contains("[") && s.contains("]")) {
                            String[] split = s.split("\\[");
                            if (split.length != 2) {
                                return;
                            }
                            String worldName = split[0];
                            World world = Bukkit.getServer().getWorld(worldName);
                            if (world == null) throw new Exception();

                            String[] loc = split[1].replace("]", "").split(",");

                            double x = Double.parseDouble(loc[0]);
                            double y = Double.parseDouble(loc[1]);
                            double z = Double.parseDouble(loc[2]);

                            Location location = new Location(world, x, y, z);
                            Block blockAt = world.getBlockAt(location);
                            if (!(blockAt.getState() instanceof Chest)) throw new Exception();
                            OfflinePlayer player = DeathChestPlugin.plugin.getServer().getOfflinePlayer(UUID.fromString(chestInfo.playerUID));
                            if (player == null) throw new Exception();
                            DeathChest deathChest = new DeathChest(blockAt, player);
                            chestMap.put(location, deathChest);
                            removeList.submit(deathChest, new RemoveTask(blockAt, deathChest));
                        }
                    } catch (Exception e) {
                        DeathChestPlugin.plugin.getLogger().log(Level.INFO, "failed to load a death chest, skipping");
                    }
                }));
            }
        }

    }

    public static boolean hasChestAt(Location location) {
        DeathChest deathChest = instance.chestMap.get(location);
        return deathChest != null;
    }

    public static boolean isUnlocked(OfflinePlayer player) {
        return instance.persistantChest.isUnlocked(player);
    }

    public static void toggleLock(Player player) {
        boolean unlocked = instance.persistantChest.isUnlocked(player);
        if (unlocked) {
            instance.persistantChest.lock(player);
            player.sendMessage(I18n.format("info.locked"));
        } else {
            instance.persistantChest.unlock(player);
            player.sendMessage(I18n.format("info.unlocked"));
        }
    }

    public void addChest(Location location, DeathChest deathChest) {
        Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, () -> {
            chestMap.put(location, deathChest);
            persistantChest.addChest(getLoc(location), deathChest);
            String loc = String.format("%s [%d, %d, %d]", location.getWorld().getName()
                    , location.getBlockX(), location.getBlockY(), location.getBlockZ());
            int removetime = DeathChestPlugin.plugin.config.getRemoveTime();
            Message message = new Message(I18n.format("info.created", loc, removetime));
            message.send(deathChest.deathPlayer);
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
                    persistantChest.remove(getLoc(location), deathChest);

                    String loc = String.format("%s [%d, %d, %d]", location.getWorld().getName()
                            , location.getBlockX(), location.getBlockY(), location.getBlockZ());
                    Message message = new Message(I18n.format("info.removed", loc));
                    message.send(deathChest.deathPlayer);
                }
            }
        });
    }

    public static class RemoveList extends FileConfigure {
        @Serializable
        Map<String, Long> scheduledMap = new LinkedHashMap<>();
        Map<String, Runnable> runnableMap = new HashMap<>();

        RemoveList() {
            Bukkit.getScheduler().runTaskTimer(DeathChestPlugin.plugin, () -> {
                List<String> removeTasks = removeLoop();
                if (!removeTasks.isEmpty()) {
                    removeTasks.forEach(s -> scheduledMap.remove(s));
                }
                this.save();
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

        public List<String> removeLoop() {
            List<String> removeTasks = new ArrayList<>();
            if ((!scheduledMap.isEmpty())) {
                Set<String> strings = scheduledMap.keySet();
                for (String s : strings) {
                    Long aLong = scheduledMap.get(s);
                    if (System.currentTimeMillis() >= aLong) {
                        removeTasks.add(s);
                        Runnable runnable = runnableMap.get(s);
                        if (runnable != null) {
                            runnable.run();
                            runnableMap.remove(s);
                        }
                    }
                }
            }
            return removeTasks;
        }

        public void submit(DeathChest deathChest, Runnable runnable) {
            int removeTime = DeathChestPlugin.plugin.config.removeTime;
            long removeInstant = System.currentTimeMillis() + removeTime * 1000;
            String loc = getLoc(deathChest.chestBlock.getLocation());
            final Map finalScheduledMap = scheduledMap;
            scheduledMap.put(loc, removeInstant);
            runnableMap.put(loc, runnable);
            Bukkit.getScheduler().runTaskLater(DeathChestPlugin.plugin, () -> {
                synchronized (finalScheduledMap) {
                    scheduledMap.remove(loc);
                    runnable.run();
                }
            }, removeTime * 20);
            Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, () -> this.save());
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

        void addChest(String loc, DeathChest deathChest) {
            ChestInfo value = new ChestInfo();
            value.playerUID = deathChest.deathPlayer.getUniqueId().toString();
            value.loc = loc;
            chests.put(loc, value);
            lock(deathChest.deathPlayer);
        }

        public void unlock(OfflinePlayer player) {
            unlockedMap.put(player.getUniqueId().toString(), true);
            Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, this::save);
        }

        public boolean isUnlocked(OfflinePlayer player) {
            Boolean aBoolean = unlockedMap.get(player.getUniqueId().toString());
            return aBoolean != null && aBoolean;
        }

        public void lock(OfflinePlayer player) {
            unlockedMap.put(player.getUniqueId().toString(), false);
            Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, this::save);
        }

        public void removeLock(OfflinePlayer deathPlayer) {
            unlockedMap.remove(deathPlayer.getUniqueId().toString());
            Bukkit.getScheduler().runTask(DeathChestPlugin.plugin, this::save);
        }

        public void remove(String loc, DeathChest deathChest) {
            chests.remove(loc);
            removeLock(deathChest.deathPlayer);
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

    private static class RemoveTask implements Runnable {

        private final Block block;
        private final DeathChest deathChest;

        RemoveTask(Block block, DeathChest deathChest) {
            this.block = block;
            this.deathChest = deathChest;
        }

        @Override
        public void run() {
            instance.removeChest(block.getLocation(), deathChest);
        }
    }
}
