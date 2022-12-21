package com.hrens.listener.minecraft;

import com.hrens.ServerSystem;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.awt.*;
import java.util.Objects;

public class AdvancementEvent implements Listener {
    ServerSystem serverSystem;

    public AdvancementEvent(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event){
        try {
            if(Objects.isNull(event.getAdvancement().getDisplay())) return;
            serverSystem.getJDA().getTextChannelById(serverSystem.getConfig().getString("modules.mcchat.chatid")).sendMessageEmbeds(
                    new EmbedBuilder().setTitle(serverSystem.getMessage("playeradvancement")
                            .replace("{player}", event.getPlayer().getName())
                            .replace("{advancement}", event.getAdvancement().getDisplay().getTitle())).setColor(Color.GREEN).build()).queue();
        } catch (NullPointerException | NoSuchMethodError e){}
    }
}
