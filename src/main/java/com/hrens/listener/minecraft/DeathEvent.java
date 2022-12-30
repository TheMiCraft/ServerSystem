package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.awt.*;

public class DeathEvent implements Listener {
    ServerSystem serverSystem;

    public DeathEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @EventHandler
    public void onJoin(PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (serverSystem.module_mcchat) {
                serverSystem.getJDA().getTextChannelById(serverSystem.getConfig().getString("modules.mcchat.chatid")).sendMessageEmbeds(
                        new EmbedBuilder()
                                .setTitle(event.getDeathMessage())
                                .setColor(Color.BLACK)
                                .build()).queue();
        }
        if (serverSystem.module_playtime) serverSystem.getPlaytimeUtils().playerJoin(p.getUniqueId());
    }
}
