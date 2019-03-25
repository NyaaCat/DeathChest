package cat.nyaa.deathchest;

import cat.nyaa.nyaacore.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class Events implements Listener {

    @EventHandler
    void onChestOpen(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (!e.getClickedBlock().getBlockData().getMaterial().equals(Material.CHEST)) return;
        Location location = e.getClickedBlock().getLocation();
        if (!ChestManager.hasChestAt(location)) return;
        Player player = e.getPlayer();
        try {
            DeathChest chest = ChestManager.getChest(location);
            if (!player.getUniqueId().equals(chest.deathPlayer.getUniqueId())) {
                if (player.isOp() || ChestManager.isUnlocked(chest.deathPlayer)) {
                    return;
                }
                e.setCancelled(true);
                new Message(I18n.format("error.not_owner")).send(player);
            }
        } catch (Exception ex) {
            String message = I18n.format("error.exception");
            player.sendMessage(message);
            DeathChestPlugin.plugin.getLogger().log(Level.SEVERE, message, ex);
        }
    }

    @EventHandler
    void onPlayerDeath(PlayerDeathEvent e) {
        Location l = e.getEntity().getLocation();
        Player p = e.getEntity();
        if (DeathChestPlugin.plugin.config.enabled
                && e.getDrops() != null
                && !e.getDrops().isEmpty()
                && p.getLocation().getY() > 1) {
            for (int y = 0; y < 255; y++) {
                Location loc = p.getLocation().clone();
                if (loc.getY() + y >= loc.getWorld().getMaxHeight()) {
                    break;
                }
                loc.setY(loc.getY() + y);
                if (loc.getBlock().getType() == Material.AIR) {
                    Block block = loc.getBlock();
                    ChestManager.newChest(block, p);
                    block.setType(Material.CHEST);
                    Chest chest = (Chest) block.getState();
                    try {
                        SimpleDateFormat format = new SimpleDateFormat(I18n.format("user.chest.date_format"));
                        String date = format.format(System.currentTimeMillis());
                        chest.setCustomName(I18n.format("user.chest.name", p.getName(), date));
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    Inventory inventory = chest.getInventory();

                    if (makeDrop(inventory, e)) break;
                }
            }
        }
    }

    private boolean makeDrop(Inventory inv, PlayerDeathEvent e) {
        if (e.getDrops().isEmpty()) {
            return true;
        }
        List<ItemStack> drops = new ArrayList<>(e.getDrops());
        List<ItemStack> finalDrops = drops;
        drops.stream().forEach(itemStack -> {
            if (itemStack.getEnchantments().containsKey(Enchantment.VANISHING_CURSE))
                finalDrops.remove(itemStack);
            e.getEntity().getInventory().removeItem(itemStack);
        });
        Collections.shuffle(drops);
        Random random = new Random();
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
        int dropSize = Math.min(amount, drops.size());
        dropSize = Math.min(dropSize, 27);
        List<ItemStack> chestDrop = drops.subList(0, dropSize);
        List<ItemStack> drop = drops.subList(dropSize, drops.size());
        ItemStack[] itemsToRemove = chestDrop.toArray(new ItemStack[0]);
        inv.addItem(itemsToRemove);
        e.getDrops().clear();
        e.getDrops().addAll(drop);
        e.setKeepInventory(false);
        e.getEntity().getInventory().removeItem(itemsToRemove);
        return true;
    }
}
