package com.hrens.commands.tabcompleters;

import com.google.common.base.Functions;
import com.hrens.ServerSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class BanCompleter implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        List<String> banids = ServerSystem.getInstance().getConfig().getIntegerList("banids").stream().map(Functions.toStringFunction()).collect(Collectors.toList());
        if(args.length == 2){
            return banids;
        }
        return null;
    }
}
