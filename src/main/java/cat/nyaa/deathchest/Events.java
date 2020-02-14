package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.Message;
import cat.nyaa.nyaacore.utils.InventoryUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Events implements Listener {

    @EventHandler
    void onChestOpen(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (!e.getClickedBlock().getBlockData().getMaterial().equals(Material.CHEST)) return;
        if (!DeathChestPlugin.plugin.config.enabledInWorld(e.getClickedBlock().getWorld())){
            return;
        }
        Location location = e.getClickedBlock().getLocation();
        if (!ChestManager.hasChestAt(location)) return;
        Player player = e.getPlayer();
        try {
            DeathChest chest = ChestManager.getChest(location);
            if (!player.getUniqueId().equals(chest.deathPlayer.getUniqueId())) {
                if (player.isOp() || ChestManager.isUnlocked(chest.deathPlayer)) {
                    openChestForPlayer(e, chest);
                    return;
                }
                e.setCancelled(true);
                new Message(I18n.format("error.not_owner")).send(player);
            }else {
                openChestForPlayer(e, chest);
            }
        } catch (Exception ex) {
            String message = I18n.format("error.exception");
            player.sendMessage(message);
            DeathChestPlugin.plugin.getLogger().log(Level.SEVERE, message, ex);
        }
    }

    private void openChestForPlayer(PlayerInteractEvent e, DeathChest chest) {
        BlockState blockData = chest.chestBlock.getState();
        Player player = e.getPlayer();
        if (blockData instanceof Chest){
            player.openInventory(((Chest) blockData).getBlockInventory());
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    void onPlayerDeath(PlayerDeathEvent e) {
        Location l = e.getEntity().getLocation();
        Player p = e.getEntity();
        World world = p.getWorld();
        if (!DeathChestPlugin.plugin.config.enabledInWorld(world)){
            return;
        }
        if (DeathChestPlugin.plugin.config.enabled
                && e.getDrops() != null
                && !e.getDrops().isEmpty()
                && p.getLocation().getY() > 1) {
            for (int y = 0; y < 255; y++) {
                Location loc = getChestLoc(p, l);
                if (loc.getBlock().getType() == Material.AIR) {
                    Block block = loc.getBlock();
                    ChestManager.newChest(block, p);
                    block.setType(Material.CHEST);
                    Chest chest = (Chest) block.getState();
                    try {
                        SimpleDateFormat format = new SimpleDateFormat(I18n.format("user.chest.date_format"));
                        String date = format.format(System.currentTimeMillis());
                        String inventoryName = I18n.format("user.chest.name", p.getName(), date);
                        chest.setCustomName(inventoryName);
                        chest.update();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    Inventory inventory = chest.getInventory();

                    if (makeDrop(inventory, e)) break;
                }
            }
        }
    }

    private Location getChestLoc(Player p, Location deathLocation) {
        Location result = null;
        World world = deathLocation.getWorld();
        if (world == null)return deathLocation;

        Location clone = deathLocation.clone();
        if (clone.getBlock().getType().isAir()) {
            for (int i = 0; i > -255; i--) {
                if (clone.getBlockY() < - i) {
                    break;
                }
                Location candidate = clone.clone().add(0, i, 0);
                if (candidate.getBlock().getType().isAir()) {
                    continue;
                }
                clone = checkBoundingBox(p, world, candidate);
            }
        }
        for (int i = 0; i < 255; i++) {
            if (clone.getBlockY() + i > 255) {
                break;
            }
            Location candidate = clone.clone().add(0, i, 0);
            if (!candidate.getBlock().getType().isAir()) {
                continue;
            }
            result = checkBoundingBox(p, world, candidate);
            return result;
        }
        return deathLocation;
    }

    private Location checkBoundingBox(Player p, World world, Location candidate) {
        Location clone = candidate.clone();
        BoundingBox[] blockBoundingBox = {getBlockBoundingBox(clone)};
        while (world.getNearbyEntities(clone, 10, 255, 10).stream().anyMatch(entity -> !(entity instanceof LivingEntity) && entity.getBoundingBox().overlaps(blockBoundingBox[0])) || (!clone.getBlock().getType().isAir() && clone.getY() <= 255)){
            clone.add(0, 1, 0);
            blockBoundingBox[0] = getBlockBoundingBox(clone);
        }
        return clone;
    }

    private BoundingBox getBlockBoundingBox(Location clone) {
        int x = clone.getBlockX();
        int y = clone.getBlockY();
        int z = clone.getBlockZ();
        return new BoundingBox(x, y, z, x+1, y+1, z+1);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent ev){
        Player p = ev.getPlayer();
        Config config = DeathChestPlugin.plugin.config;
        World world = p.getWorld();
        if (!DeathChestPlugin.plugin.config.enabledInWorld(world)){
            return;
        }
        if (!config.respawnBuff) {
            return;
        }
        List<String> respawnBuffList = config.respawnBuffList;
        new BukkitRunnable(){
            @Override
            public void run() {
                if (!respawnBuffList.isEmpty()) {
                    try{
                        respawnBuffList.forEach(s -> {
                            String[] split = s.split(":", 3);
                            if (split.length!=3){
                                ev.getPlayer().getServer().getLogger().log(Level.WARNING, "Bad Config for respawn buff \""+s+"\"");
                                return;
                            }
                            String effect = split[0];
                            int amplifier = Integer.parseInt(split[1]);
                            int duration = Integer.parseInt(split[2]);
                            PotionEffectType potion = PotionEffectType.getByName(effect.toUpperCase());
                            if (potion == null){
                                ev.getPlayer().getServer().getLogger().log(Level.WARNING, "Bad Config for respawn buff \""+s+"\"");
                                return;
                            }
                            PotionEffect potionEffect = new PotionEffect(potion, duration, amplifier);
                            p.addPotionEffect(potionEffect);
                        });
                    }catch (Exception e){
                        ev.getPlayer().getServer().getLogger().log(Level.WARNING, "Bad Config for respawn buff");
                    }
                }
            }
        }.runTaskLater(DeathChestPlugin.plugin, 1);

    }

    Random random = new Random();

    private boolean makeDrop(Inventory deathChestInventory, PlayerDeathEvent e) {
        if (e.getDrops().isEmpty()) {
            return true;
        }
        List<ItemStack> keepItems = this.getKeepItemList(e);
        List<ItemStack> drops = new ArrayList<>(e.getDrops());
        AtomicInteger index = new AtomicInteger();
        if (!keepItems.isEmpty()) {
            keepItems.forEach(itemStack -> drops.remove(itemStack));
        }
        PlayerInventory inventory = e.getEntity().getInventory();
        List<Integer> indexes = drops.stream().mapToInt(itemStack -> index.getAndIncrement()).boxed().collect(Collectors.toList());
        Collections.shuffle(indexes);
        int amount = getDropAmount();

        int dropSize = Math.min(amount, drops.size());
        dropSize = Math.min(dropSize, 27);
        List<Integer> chestDrop = indexes.subList(0, dropSize);
        ItemStack[] itemsToRemove = new ItemStack[dropSize];
        for (int i = 0; i < dropSize; i++) {
            itemsToRemove[i] = drops.get(chestDrop.get(i));
        }
        deathChestInventory.addItem(itemsToRemove);
        List<ItemStack> chestItems = new ArrayList<>(Arrays.asList(itemsToRemove));
        drops.removeAll(chestItems);
        e.setKeepInventory(true);
        List<ItemStack> itemToRemove = keepItems.stream()
                .filter(itemStack -> itemStack!=null && itemStack.getEnchantments().containsKey(Enchantment.VANISHING_CURSE))
                .collect(Collectors.toList());
        InventoryUtils.withdrawInventoryAtomic(inventory, chestItems);
        InventoryUtils.withdrawInventoryAtomic(inventory, drops);
        List<ItemStack> vanishes = Arrays.stream(inventory.getContents())
                .filter(itemStack -> itemStack!=null && itemStack.getEnchantments().containsKey(Enchantment.VANISHING_CURSE))
                .collect(Collectors.toList());
        InventoryUtils.withdrawInventoryAtomic(deathChestInventory, vanishes);
        InventoryUtils.withdrawInventoryAtomic(inventory, vanishes);
        if (!itemToRemove.isEmpty()) {
            itemToRemove.forEach(itemStack -> InventoryUtils.removeItem(e.getEntity(),itemStack, itemStack.getAmount()));
        }
        moveBeltToBackpack(inventory);
        e.getDrops().clear();
        return true;
    }

    private int getDropAmount() {
        DeathChestPlugin plugin = DeathChestPlugin.plugin;
        String dropAmount = plugin.config.getDropAmount().replaceAll(" ", "");
        int amount = 0;
        try {
            if (dropAmount.contains("-")) {
                String[] split = dropAmount.split("-");
                Integer from = Integer.valueOf(split[0]);
                Integer to = Integer.valueOf(split[1]);
                amount = from;
                int range = to - from;
                amount += random.nextInt(range);
            } else {
                amount = Integer.parseInt(dropAmount);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, I18n.format("error.drop_amount_invalid"), ex);
        }
        return amount;
    }

    private void moveBeltToBackpack(PlayerInventory inventory) {
        ItemStack air = new ItemStack(Material.AIR);
        move:
        for (int i = 0; i < 9; i++) {
            ItemStack item = inventory.getItem(i);
            if (item!=null){
                if (item.containsEnchantment(Enchantment.VANISHING_CURSE))break;
                for (int j = 9; j < inventory.getSize(); j++) {
                    ItemStack targetSlot = inventory.getItem(j);
                    if (targetSlot == null || targetSlot.getType().equals(Material.AIR)){
                        inventory.setItem(i, air);
                        inventory.setItem(j, item);
                        continue move;
                    }
                }
                break;
            }
        }
    }

    private List<ItemStack> getKeepItemList(PlayerDeathEvent event) {
        PlayerInventory inventory = event.getEntity().getInventory();
        List<ItemStack> armors = Stream.of(inventory.getArmorContents()).collect(Collectors.toList());
        List<ItemStack> belts = Stream.of(inventory.getContents()).limit(9).collect(Collectors.toList());
        ItemStack offhand = inventory.getItemInOffHand();
        List<ItemStack> results = new ArrayList<>(armors.size() + belts.size() + 1);
        Config config = DeathChestPlugin.plugin.config;
        if (config.keepArmors) {
            results.addAll(armors);
        }
        if (config.keepBelt){
            results.addAll(belts);
        }
        if (config.keepOffhand){
            results.add(offhand);
        }
        return results;
    }
}
