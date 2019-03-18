package cat.nyaa.deathchest;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

public class DeathChest {
    public boolean removed = false;
    Block chestBlock;
    Player deathPlayer;

    public DeathChest(Block chestBlock, Player deathPlayer){
        this.chestBlock = chestBlock;
        this.deathPlayer = deathPlayer;
    }

    public boolean isDeathPlayer(Player player){
        return deathPlayer.equals(player);
    }
}
