package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import com.hrens.utils.PlaytimeUtils;
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
        serverSystem.getPlaytimeUtils().move(event.getPlayer());
    }
}