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
        event.setQuitMessage(serverSystem.getMessage("quitmessage").replace("{player}", event.getPlayer().getName()));
        if(serverSystem.module_mcchat) {
            serverSystem.getJDA().getTextChannelById(serverSystem.getConfig().getString("modules.mcchat.chatid")).sendMessageEmbeds(
                    new EmbedBuilder().setTitle(serverSystem.getMessage("playerleftserver").replace("{player}", event.getPlayer().getName())).setColor(Color.RED).build()).queue();
        }
        if(serverSystem.module_playtime) {
            Player player = event.getPlayer();
            Long joint = serverSystem.getMap().get(player.getUniqueId());
            MongoCollection<Document> playtime = serverSystem.getMongoDatabase().getCollection(serverSystem.getConfig().getString("mongodb.playtime"));
            if (playtime.countDocuments(Filters.eq("uuid", player.getUniqueId().toString())) == 1) {
                playtime.updateOne(
                        Filters.eq("uuid", player.getUniqueId().toString()),
                        Updates.inc("time-" + serverSystem.getConfig().getString("server"), System.currentTimeMillis() - joint - serverSystem.getAFK().get(player.getUniqueId())));
                playtime.updateOne(
                        Filters.eq("uuid", player.getUniqueId().toString()),
                        Updates.inc("afk-" + serverSystem.getConfig().getString("server"), serverSystem.getAFK().get(player.getUniqueId())));
            } else {
                playtime.insertOne(new Document()
                        .append("uuid", player.getUniqueId().toString())
                        .append("time-" + serverSystem.getConfig().getString("server"), System.currentTimeMillis() - joint)
                        .append("afk-" + serverSystem.getConfig().getString("server"), serverSystem.getAFK().get(player.getUniqueId())));
            }
            serverSystem.getMap().remove(player.getUniqueId());
            serverSystem.getAFK().remove(player.getUniqueId());
            serverSystem.getLastMove().remove(player);
        }
    }
}
