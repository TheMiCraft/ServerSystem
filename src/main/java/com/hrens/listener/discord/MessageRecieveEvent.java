package com.hrens.listener.discord;

import com.hrens.ServerSystem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class MessageRecieveEvent extends ListenerAdapter {
    ServerSystem serverSystem;

    public MessageRecieveEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        String id = e.getChannel().getId();
        if(id.equals(serverSystem.getConfig().getString("modules.mcchat.chatid"))){
            Bukkit.broadcastMessage(serverSystem.getMessage("dctomcmsg").replace("{author}", e.getAuthor().getName()).replace("{message}", e.getMessage().getContentRaw()));
        }
    }
}
