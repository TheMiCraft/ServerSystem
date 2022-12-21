package com.hrens.commands;

import com.hrens.ServerSystem;
import com.hrens.utils.UUIDFetcher;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        UUID senderUUID = commandSender instanceof Player ? ((Player) commandSender).getUniqueId() : null;
        if (args.length == 2) {
            if (UUIDFetcher.getUUID(args[0]) != null) {
                UUID targetUUID = UUIDFetcher.getUUID(args[0]);
                try {
                    int id = Integer.parseInt(args[1]);
                    List<Integer> muteIds = ServerSystem.getInstance().getBanManager().getMuteIds();
                    if (!muteIds.contains(id)) {
                        commandSender.sendMessage(ServerSystem.instance.getMessage("idnotexist").replace("{ids}", String.join(", ", muteIds.stream().map(Object::toString).collect(Collectors.toList()))));
                        return false;
                    }
                    if (ServerSystem.getInstance().getBanManager().canMute(commandSender, id)) {
                        ServerSystem.getInstance().getBanManager().mute(targetUUID, id, senderUUID);
                        commandSender.sendMessage(ServerSystem.getInstance().getMessage("playermuted"));
                    } else {
                        commandSender.sendMessage(ServerSystem.getInstance().getMessage("notallowed"));
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                commandSender.sendMessage(ServerSystem.getInstance().getMessage("playernotfound"));
            }
        } else {
            commandSender.sendMessage(ServerSystem.getInstance().getMessage("muteformat"));
        }
        return false;
    }
}
