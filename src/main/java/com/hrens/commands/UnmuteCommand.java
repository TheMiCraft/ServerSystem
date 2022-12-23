package com.hrens.commands;

import com.hrens.ServerSystem;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        UUID senderUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        if(!sender.hasPermission("serversystem.unmute")) {
            sender.sendMessage(ServerSystem.getInstance().getMessage("notallowed"));
            return false;
        }
        if (strings.length >= 1) {
            try {
                int _id = Integer.parseInt(strings[0]);
                String reason = strings.length > 1 ? strings[1] : null;
                if (Objects.nonNull(ServerSystem.getInstance().getBanManager().unMuteIDExist(_id))) {
                    ServerSystem.getInstance().getBanManager().unban(_id, senderUUID, UUID.fromString(ServerSystem.getInstance().getBanManager().unMuteIDExist(_id)), reason);
                    sender.sendMessage(ServerSystem.getInstance().getMessage("unmutesucceeded"));
                } else {
                    sender.sendMessage(ServerSystem.getInstance().getMessage("unmuteidnotexist"));
                }

            } catch (NumberFormatException e) {
                sender.sendMessage(ServerSystem.getInstance().getMessage("idnumber"));
            }

        } else {
            sender.sendMessage(ServerSystem.getInstance().getMessage("giveonearguments"));
        }
        return false;
    }
}
