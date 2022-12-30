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
    public ChatEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        Player p = e.getPlayer();
        if(serverSystem.getBanManager().isMuted(p.getUniqueId())){
            e.setCancelled(true);
            serverSystem.getBanManager().onChat(e);
        } else {
            String prefix = "";
            String suffix = "";
            if(serverSystem.luckperms) {
                prefix = serverSystem.getLPAPI().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData().getPrefix();
                suffix = serverSystem.getLPAPI().getUserManager().getUser(p.getUniqueId()).getCachedData().getMetaData().getSuffix();
            }
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
