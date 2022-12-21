package com.hrens.utils;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

public class BanManager {
    private static final String ID_CHARS = "123456789";
    private static final Random random = new SecureRandom();
    private final MongoCollection<Document> banned;

    public BanManager() {
        banned = ServerSystem.getInstance().getMongoDatabase().getCollection(ServerSystem.getInstance().getConfig().getString("mongodb.bans"));
    }

    public void mute(UUID uuid, int reason, UUID moderator) {
        int _id = generateId();
        String reasonAsString = ServerSystem.getInstance().getConfig().getString("mute." + reason + ".reason");
        Player p = Bukkit.getPlayer(uuid);
        Document document = new Document()
                .append("_id", _id)
                .append("bannedUUID", uuid.toString())
                .append("moderatorUUID", Objects.nonNull(moderator) ? moderator.toString() : null)
                .append("reason", reason)
                .append("timestamp", System.currentTimeMillis())
                .append("end", (System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("mute." + reason + ".time")))
                .append("type", "mute");
        banned.insertOne(document);
        Player player = Bukkit.getServer().getPlayer(uuid);
        if(player != null){
            Duration diff = Duration.ofMillis((System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("ban." + reason + ".time"))-System.currentTimeMillis());
            String s = ServerSystem.getInstance().getMessage("bandateformat")
                    .replace("{d}", String.valueOf(diff.toDays()))
                    .replace("{h}", String.valueOf(diff.toHours() % 24))
                    .replace("{m}", String.valueOf(diff.toMinutes() % 60))
                    .replace("{s}", String.valueOf((diff.toMillis() / 1000) % 60));
            player.sendMessage(ServerSystem.getInstance().getMessage("mutemessage")
                    .replace("{id}", String.valueOf(_id))
                    .replace("{reason}", reasonAsString)
                    .replace("{duration}", s.toLowerCase()));
        }
        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.MUTE, Objects.nonNull(moderator) ? moderator.toString() : null, uuid.toString(), System.currentTimeMillis(), reasonAsString);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }

    public void ban(UUID uuid, int reason, UUID moderator) {
        int _id = generateId();
        String reasonAsString = ServerSystem.getInstance().getConfig().getString("ban." + reason + ".reason");
        Player player = Bukkit.getServer().getPlayer(uuid);
        if(player != null){
            Duration diff = Duration.ofMillis((System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("ban." + reason + ".time"))-System.currentTimeMillis());
            String s = ServerSystem.getInstance().getMessage("bandateformat")
                    .replace("{d}", String.valueOf(diff.toDays()))
                    .replace("{h}", String.valueOf(diff.toHours() % 24))
                    .replace("{m}", String.valueOf(diff.toMinutes() % 60))
                    .replace("{s}", String.valueOf((diff.toMillis() / 1000) % 60));
            player.kickPlayer(ServerSystem.getInstance().getMessage("banmessage")
                    .replace("{id}", String.valueOf(_id))
                    .replace("{reason}", reasonAsString)
                    .replace("{duration}", s.toLowerCase()));
        }
        Document document = new Document()
                .append("_id", _id)
                .append("bannedUUID", uuid.toString())
                .append("moderatorUUID", Objects.nonNull(moderator) ? moderator.toString() : null)
                .append("reason", reason)
                .append("timestamp", System.currentTimeMillis())
                .append("end", (System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("ban." + reason + ".time")))
                .append("type", "ban");
        banned.insertOne(document);

        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.BAN, Objects.nonNull(moderator) ? moderator.toString() : null, uuid.toString(), System.currentTimeMillis(), reasonAsString);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }

    public boolean isMuted(UUID uuid) {
        Bson b = Filters.and(Filters.eq("bannedUUID", uuid.toString()), Filters.eq("type", "mute"), Filters.not(Filters.lt("end", System.currentTimeMillis())));
        if(Objects.nonNull(banned.find(b).first())) return true;
        return false;
    }

    public boolean isBanned(UUID uuid) {
        Bson b = Filters.and(Filters.eq("bannedUUID", uuid.toString()), Filters.eq("type", "ban"), Filters.not(Filters.lt("end", System.currentTimeMillis())));
        if(Objects.nonNull(banned.find(b).first())) return true;
        return false;
    }

    public List<Integer> getBanIds() {
        return ServerSystem.getInstance().getConfig().getIntegerList("banids");
    }

    public List<Integer> getMuteIds() {
        return ServerSystem.getInstance().getConfig().getIntegerList("muteids");
    }

    public boolean canBan(CommandSender commandSender, int id) {
        String permisson = ServerSystem.getInstance().getConfig().getString("ban." + id + ".permission");
        if (permisson == null) return true;
        return commandSender.hasPermission(permisson);
    }

    public boolean canMute(CommandSender commandSender, int id) {
        String permisson = ServerSystem.getInstance().getConfig().getString("mute." + id + ".permission");
        if (permisson == null) return true;
        return commandSender.hasPermission(permisson);

    }

    public void unban(int _id, UUID moderator, UUID target, String reason) {
        if (banned.find(Filters.eq("_id", _id)).first() == null || banned.find(Filters.eq("_id", _id)).first().isEmpty())
            return;
        if (banned.find(Filters.eq("_id", _id)).first().getString("type").equals("ban"))
            banned.deleteOne(banned.find(Filters.eq("_id", _id)).first());

        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.UNBAN, Objects.nonNull(moderator) ? moderator.toString() : null, target.toString(), System.currentTimeMillis(), reason);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }

    public void unmute(int _id, UUID moderator, UUID target, String reason) {
        if (banned.find(Filters.eq("_id", _id)).first() == null || banned.find(Filters.eq("_id", _id)).first().isEmpty())
            return;
        if (banned.find(Filters.eq("_id", _id)).first().getString("type").equals("mute"))
            banned.deleteOne(banned.find(Filters.eq("_id", _id)).first());
        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.UNMUTE, Objects.nonNull(moderator) ? moderator.toString() : null, target.toString(), System.currentTimeMillis(), reason);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }


    public int generateId() {
        Random random = new Random();
        int i = random.nextInt(1000000);
        if (banned.countDocuments(Filters.eq("_id", i)) != 0) {
            while (banned.find().into(new ArrayList<>()).contains(i)) {
                i = random.nextInt(1000000);
            }
        }
        return i;
    }

    public String getDateFromMillis(long millis) {
        return formatDuration(Duration.ofMillis(millis));
    }

    private String formatDuration(Duration d) {
        long days = d.toDays();
        d = d.minusDays(days);
        long hours = d.toHours();
        d = d.minusHours(hours);
        long minutes = d.toMinutes();
        d = d.minusMinutes(minutes);
        long seconds = d.getSeconds();
        String string =
                (days == 0 ? "" : (days == 1) ? days + " Tag, " : days + " Tage, ") +
                        (hours == 0 ? "" : (hours == 1) ? hours + " Stunde, " : hours + " Stunden, ") +
                        (minutes == 0 ? "" : (minutes == 1) ? minutes + " Minute, " : minutes + " Minuten, ") +
                        (seconds == 0 ? "" : (seconds == 1) ? seconds + " Sekunde, " : seconds + " Sekunden, ");
        if (string.length() > 2)
            string = string.substring(0, string.length() - 2);
        else
            string = "0 Sekunden";
        return string;
    }

}
