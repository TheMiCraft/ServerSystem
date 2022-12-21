package com.hrens.commands.tabcompleters;

import com.hrens.ServerSystem;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class UnbanCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> list = new ArrayList<>();
        if(args.length == 1){
            Bson bson = Filters.and(Filters.eq("type", "ban"), Filters.not(Filters.lt("end", System.currentTimeMillis())));
            ServerSystem.getInstance().getMongoDatabase().getCollection("bans").find(bson).forEach(document -> {
                list.add(String.valueOf(document.getInteger("_id")));
            });
        }
        return null;
    }
}
