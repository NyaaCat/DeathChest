package cat.nyaa.deathchest;

import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class DeathChest {
    public boolean removed = false;
    Block chestBlock;
    OfflinePlayer deathPlayer;

    public DeathChest(Block chestBlock, OfflinePlayer deathPlayer){
        this.chestBlock = chestBlock;
        this.deathPlayer = deathPlayer;
    }

    public boolean isDeathPlayer(Player player){
        return deathPlayer.equals(player);
    }
}
