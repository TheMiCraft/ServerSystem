package com.hrens.commands;

import com.hrens.ServerSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FeedCommand implements CommandExecutor {
    ServerSystem serverSystem;

    public FeedCommand(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(commandSender instanceof Player){
            Player p = (Player) commandSender;
            if(p.hasPermission("serversystem.feed")){
                if(strings.length == 1){
                    Player target = Bukkit.getPlayer(strings[0]);
                    if(target != null){
                        target.setFoodLevel(20);
                        p.sendMessage(serverSystem.getMessage("youhavefeeded"));
                    } else {
                        p.sendMessage(serverSystem.getMessage("playernotfound"));
                    }
                } else {
                    p.setFoodLevel(20);
                    p.sendMessage(serverSystem.getMessage("youfeeded"));
                }
            } else {
                p.sendMessage(serverSystem.getMessage("notallowed"));
            }
        } else {
            commandSender.sendMessage(serverSystem.getMessage("havetobeplayer"));
        }
        return false;
    }
}
