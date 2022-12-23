package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.*;

public class LeaveEvent implements Listener {
    ServerSystem serverSystem;

    public LeaveEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(serverSystem.getMessage("quitmessage").replace("{player}", player.getName()));
        if(serverSystem.module_mcchat) {
            serverSystem.getJDA().getTextChannelById(serverSystem.getConfig().getString("modules.mcchat.chatid")).sendMessageEmbeds(
                    new EmbedBuilder().setTitle(serverSystem.getMessage("playerleftserver").replace("{player}", player.getName())).setColor(Color.RED).build()).queue();
        }
        if(serverSystem.module_playtime) {
            serverSystem.getPlaytimeUtils().playerLeave(player.getUniqueId());
        }
    }
}
