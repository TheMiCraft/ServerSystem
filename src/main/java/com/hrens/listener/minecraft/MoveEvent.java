package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.concurrent.TimeUnit;

public class MoveEvent implements Listener {
    ServerSystem serverSystem;

    public MoveEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        long l = System.currentTimeMillis();
        long l1 = serverSystem.getLastMove().get(player);
        if (l - l1 >= serverSystem.getConfig().getInt("modules.playtime.afktime")) {
            player.sendMessage(serverSystem.getMessage("afktime")
                    .replace("{minutes}", String.valueOf(TimeUnit.MILLISECONDS.toMinutes(l - l1)))
                    .replace("{seconds}", String.valueOf((TimeUnit.MILLISECONDS.toSeconds(l - l1) % 60))));
            Long aLong = serverSystem.getAFK().get(player.getUniqueId()) + (l - l1);
            serverSystem.getAFK().replace(player.getUniqueId(), aLong);
        }
        serverSystem.getLastMove().put(player, l);
    }
}
