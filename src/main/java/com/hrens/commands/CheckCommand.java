package com.hrens.commands;

import com.hrens.ServerSystem;
import com.hrens.utils.LogManager;
import com.hrens.utils.UUIDFetcher;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CheckCommand implements CommandExecutor {
    MongoCollection<Document> banned;
    public CheckCommand() {
        banned = ServerSystem.getInstance().getMongoDatabase().getCollection(ServerSystem.getInstance().getConfig().getString("mongodb.bans"));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        UUID senderUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        if(!sender.hasPermission("serversystem.check")) {
            sender.sendMessage(ServerSystem.getInstance().getMessage("notallowed"));
            return false;
        }
        if (args.length != 1) {
            sender.sendMessage(ServerSystem.getInstance().getMessage("checkformat"));
            return false;
        }

        UUID target;
        if (UUIDFetcher.getUUID(args[0]) != null) {
            target = UUIDFetcher.getUUID(args[0]);
        } else if (args[0].matches("/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/")
                && UUIDFetcher.getName(UUID.fromString(args[0])) != null) {
            target = UUID.fromString(args[0]);
        } else if (args[0].matches("-?\\d+") && banned.countDocuments(Filters.eq("_id", args[0])) != 0) {
            target = banned.find(Filters.eq("_id", args[0])).first().get("bannedUUID", UUID.class);
        } else {
            sender.sendMessage(ServerSystem.getInstance().getMessage("checkformat"));
            return false;
        }
        Collection<LogManager.LogEntry> log = ServerSystem.getInstance().getLogManager().getLogByTarget(target);
        List<String> check_log = log.stream().map(logEntry -> ServerSystem.getInstance().getMessage("checklog")
                .replace("{name}", Objects.nonNull(logEntry.getExecutor()) ? UUIDFetcher.getName(UUID.fromString(logEntry.getExecutor())) : "Console")
                .replace("{date}", new SimpleDateFormat(ServerSystem.getInstance().getMessage("dateformat")).format(new Date(logEntry.getTime())))
                .replace("{reason}", Objects.nonNull(logEntry.getReason()) ? logEntry.getReason() : "No Reason")
                .replace("{type}", logEntry.getType().toString())
        ).collect(Collectors.toList());
        sender.sendMessage(ServerSystem.getInstance().getMessage("checkheader") + "\n" + String.join("\n", check_log));
        return false;
    }
}
