package me.kwilson272.electrobola;

import com.projectkorra.projectkorra.BendingPlayer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class ElectroBolaListener implements Listener {

    @EventHandler
    private void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null) {
            return;
        }

        if (bPlayer.getBoundAbilityName().equals("ElectroBola") && bPlayer.canCurrentlyBendWithWeapons()) {
            new ElectroBola(player);
        }
    }
}
