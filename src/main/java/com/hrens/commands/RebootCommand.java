package com.hrens.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.hrens.ServerSystem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class RebootCommand implements CommandExecutor {
    ServerSystem serverSystem;

    public RebootCommand(ServerSystem serverSystem) {
        this.serverSystem = serverSystem;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(!commandSender.hasPermission("serversystem.restart")) return false;
        try {
            int n = 10;
            int b = 10;
            for (int i = 1; i <= n; ++i) {
                Bukkit.broadcastMessage(serverSystem.getMessage("serverrestartseconds").replace("{seconds}", String.valueOf(b)));
                b--;
                TimeUnit.SECONDS.sleep(1);
            }

            for (Player player : Bukkit.getOnlinePlayers()) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF("lobby");
                player.sendPluginMessage(serverSystem, "BungeeCord", out.toByteArray());
            }
            TimeUnit.SECONDS.sleep(1);
            Bukkit.getServer().shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
}
