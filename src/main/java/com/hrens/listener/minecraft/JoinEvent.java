package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.awt.*;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class JoinEvent implements Listener {
    ServerSystem serverSystem;

    public JoinEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        if (serverSystem.module_bansystem && !serverSystem.getConfig().getBoolean("bungeecord") && serverSystem.getBanManager().isBanned(p.getUniqueId())){
            serverSystem.getBanManager().onJoin(p);
        } else {
            event.setJoinMessage(serverSystem.getMessage("joinmessage").replace("{player}", p.getName()));
            event.setJoinMessage("§a» §7" + p.getName());
            if (serverSystem.module_mcchat) {
                serverSystem.getJDA().getTextChannelById(serverSystem.getConfig().getString("modules.mcchat.chatid")).sendMessageEmbeds(
                        new EmbedBuilder()
                                .setTitle(serverSystem.getMessage("playerjoinserver").replace("{player}", p.getName()))
                                .setColor(Color.GREEN)
                                .build()).queue();
            }
            if (serverSystem.module_playtime) serverSystem.getPlaytimeUtils().playerJoin(p.getUniqueId());
        }
    }
}
