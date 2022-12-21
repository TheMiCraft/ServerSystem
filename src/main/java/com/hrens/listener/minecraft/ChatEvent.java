package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.TimeZone;

public class ChatEvent implements Listener {
    ServerSystem serverSystem;
    MongoCollection<Document> mutes;
    public ChatEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
        mutes = serverSystem.getMongoDatabase().getCollection(serverSystem.getConfig().getString("mongodb.bans"));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        Player p = e.getPlayer();
        if(serverSystem.getBanManager().isMuted(p.getUniqueId())){
            e.setCancelled(true);
            Document document = mutes.find(Filters.and(Filters.eq("type", "mute"), Filters.not(Filters.lt("end", System.currentTimeMillis())), Filters.eq("bannedUUID", p.getUniqueId().toString()))).first();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String s = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(document.getLong("end")), TimeZone.getDefault().toZoneId()));
            p.sendMessage(serverSystem.getMessage("youaremuted")
                    .replace("{id}", String.valueOf(document.getInteger("_id")))
                    .replace("{reason}", serverSystem.getConfig().getString("mute." + document.getInteger("reason") + ".reason"))
                    .replace("{date}", s));
        } else {
            String prefix = serverSystem.getLPAPI().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData().getPrefix();
            String suffix = serverSystem.getLPAPI().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData().getSuffix();
            e.setFormat(prefix + p.getName() + suffix + ": " + e.getMessage());
            e.setFormat(serverSystem.getMessage("chatformat")
                    .replace("{pf}", Objects.nonNull(prefix) ? prefix : "")
                    .replace("{player}", p.getName())
                    .replace("{suffix}", Objects.nonNull(suffix) ? suffix : "")
                    .replace("{message}", e.getMessage()));
            if (serverSystem.module_mcchat) {
                serverSystem.getJDA().getTextChannelById(serverSystem.getConfig().getString("modules.mcchat.chatid"))
                        .sendMessage(serverSystem.getMessage("mctodcmsg")
                                .replace("{player}", p.getName())
                                .replace("{message}", e.getMessage())).queue();
            }
        }
    }
}
