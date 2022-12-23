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
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BanManager {
    private static final String ID_CHARS = "123456789";
    private static final Random random = new SecureRandom();
    private Connection connection;
    private MongoCollection<Document> banned;
    private final ServerSystem.StorageType storageType;

    public BanManager(ServerSystem.StorageType storageType) {
        this.storageType = storageType;
        switch (storageType) {
            case MongoDB:
                banned = ServerSystem.getInstance().getMongoDatabase().getCollection(ServerSystem.getInstance().getConfig().getString("mongodb.bans"));
            case MySQL:
                try {
                    this.connection = DriverManager.getConnection(ServerSystem.getInstance().getConfig().getString("mysql.DB_URL"), ServerSystem.getInstance().getConfig().getString("mysql.DB_USER"), ServerSystem.getInstance().getConfig().getString("mysql.DB_PASS"));
                    connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + ServerSystem.getInstance().getConfig().getString("mysql.DB_NAME") + ";");
                    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS bans(_id INT PRIMARY KEY, bannedUUID VARCHAR(255), moderatorUUID VARCHAR(255), reason INT, timestamp BIGINT, end BIGINT, type VARCHAR(255));");
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                break;
        }
    }

    public void mute(UUID uuid, int reason, UUID moderator) {
        int _id = generateId();
        String reasonAsString = ServerSystem.getInstance().getConfig().getString("mute." + reason + ".reason");
        Player p = Bukkit.getPlayer(uuid);
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            Document document = new Document()
                    .append("_id", _id)
                    .append("bannedUUID", uuid.toString())
                    .append("moderatorUUID", Objects.nonNull(moderator) ? moderator.toString() : null)
                    .append("reason", reason)
                    .append("timestamp", System.currentTimeMillis())
                    .append("end", (System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("mute." + reason + ".time")))
                    .append("type", "mute");
            banned.insertOne(document);
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO bans (_id, bannedUUID, moderatorUUID, reason, timestamp, end, type) VALUES (?, ?, ?, ?, ?, ?, ?)");

                stmt.setInt(1, _id);
                stmt.setString(2, uuid.toString());
                stmt.setString(3, Objects.nonNull(moderator) ? moderator.toString() : null);
                stmt.setInt(4, reason);
                stmt.setLong(5, System.currentTimeMillis());
                stmt.setLong(6, (System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("mute." + reason + ".time")));
                stmt.setString(7, "mute");
                stmt.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
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
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            Document document = new Document()
                    .append("_id", _id)
                    .append("bannedUUID", uuid.toString())
                    .append("moderatorUUID", Objects.nonNull(moderator) ? moderator.toString() : null)
                    .append("reason", reason)
                    .append("timestamp", System.currentTimeMillis())
                    .append("end", (System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("ban." + reason + ".time")))
                    .append("type", "ban");
            banned.insertOne(document);
        } else if (storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO bans (_id, bannedUUID, moderatorUUID, reason, timestamp, end, type) VALUES (?, ?, ?, ?, ?, ?, ?)");

                stmt.setInt(1, _id);
                stmt.setString(2, uuid.toString());
                stmt.setString(3, Objects.nonNull(moderator) ? moderator.toString() : null);
                stmt.setInt(4, reason);
                stmt.setLong(5, System.currentTimeMillis());
                stmt.setLong(6, (System.currentTimeMillis() + ServerSystem.getInstance().getConfig().getLong("mute." + reason + ".time")));
                stmt.setString(7, "ban");
                stmt.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.BAN, Objects.nonNull(moderator) ? moderator.toString() : null, uuid.toString(), System.currentTimeMillis(), reasonAsString);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }

    public boolean isMuted(UUID uuid) {
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            Bson b = Filters.and(Filters.eq("bannedUUID", uuid.toString()), Filters.eq("type", "mute"), Filters.not(Filters.lt("end", System.currentTimeMillis())));
            if(Objects.nonNull(banned.find(b).first())) return true;
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE bannedUUID = ? AND end > ? AND type = ?");
                stmt.setString(1, uuid.toString());
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(2, "mute");
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return false;
    }

    public boolean isBanned(UUID uuid) {
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            Bson b = Filters.and(Filters.eq("bannedUUID", uuid.toString()), Filters.eq("type", "ban"), Filters.not(Filters.lt("end", System.currentTimeMillis())));
            if(Objects.nonNull(banned.find(b).first())) return true;
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE bannedUUID = ? AND end > ? AND type = ?");
                stmt.setString(1, uuid.toString());
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setString(3, "ban");
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
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
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            if (banned.find(Filters.eq("_id", _id)).first() == null || banned.find(Filters.eq("_id", _id)).first().isEmpty())
                return;
            if (banned.find(Filters.eq("_id", _id)).first().getString("type").equals("ban"))
                banned.deleteOne(banned.find(Filters.eq("_id", _id)).first());
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE _id = ? AND type = ? LIMIT 1");
                stmt.setInt(1, _id);
                stmt.setString(2, "ban");
                if (!stmt.executeQuery().next()) return;
                PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM bans WHERE _id = ? LIMIT 1");
                stmt2.setInt(1, _id);
                stmt2.executeUpdate();
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.UNBAN, Objects.nonNull(moderator) ? moderator.toString() : null, target.toString(), System.currentTimeMillis(), reason);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }

    public void unmute(int _id, UUID moderator, UUID target, String reason) {
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            if (banned.find(Filters.eq("_id", _id)).first() == null || banned.find(Filters.eq("_id", _id)).first().isEmpty())
                return;
            if (banned.find(Filters.eq("_id", _id)).first().getString("type").equals("mute"))
                banned.deleteOne(banned.find(Filters.eq("_id", _id)).first());
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE _id = ? AND type = ? LIMIT 1");
                stmt.setInt(1, _id);
                stmt.setString(2, "mute");
                if (!stmt.executeQuery().next()) return;
                PreparedStatement stmt2 = connection.prepareStatement("DELETE FROM bans WHERE _id = ? LIMIT 1");
                stmt2.setInt(1, _id);
                stmt2.executeUpdate();
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        LogManager.LogEntry logEntry = new LogManager.LogEntry(LogManager.LogEntry.LogType.UNBAN, Objects.nonNull(moderator) ? moderator.toString() : null, target.toString(), System.currentTimeMillis(), reason);
        ServerSystem.getInstance().getLogManager().addEntry(logEntry);
    }

    public void onJoin(Player p){
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            Document document = banned.find(Filters.and(Filters.eq("type", "ban"), Filters.not(Filters.lt("end", System.currentTimeMillis())), Filters.eq("bannedUUID", p.getUniqueId().toString()))).first();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String s = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(document.getLong("end")), TimeZone.getDefault().toZoneId()));
            p.kickPlayer(ServerSystem.getInstance().getMessage("youarebanned")
                    .replace("{id}", String.valueOf(document.getInteger("_id")))
                    .replace("{reason}", ServerSystem.getInstance().getConfig().getString("mute." + document.getInteger("reason") + ".reason"))
                    .replace("{date}", s));
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE type = 'ban' AND end > ? AND bannedUUID = ? LIMIT 1");
                stmt.setLong(1, System.currentTimeMillis());
                stmt.setString(2, p.getUniqueId().toString());
                ResultSet rs = stmt.executeQuery();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                if (rs.next()) {
                    String s = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(rs.getLong("end")), TimeZone.getDefault().toZoneId()));
                    String id = rs.getString("_id");
                    int reasonCode = rs.getInt("reason");
                    String reason = ServerSystem.getInstance().getConfig().getString("mute." + reasonCode + ".reason");
                    p.kickPlayer(ServerSystem.getInstance().getMessage("youarebanned")
                            .replace("{id}", id)
                            .replace("{reason}", reason)
                            .replace("{date}", s));
                }


            } catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public String unBanIDExist(int id){
        if(storageType.equals(ServerSystem.StorageType.MongoDB)){
            return ServerSystem.getInstance().getMongoDatabase().getCollection(ServerSystem.getInstance().getConfig().getString("mongodb.bans"))
                    .find(Filters.and(Filters.eq("_id", id), Filters.eq("type", "ban"))).first().getString("bannedUUID");
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE type = 'ban' AND _id = ? LIMIT 1");

                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if(rs.next()){
                    return rs.getString("bannedUUID");
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public String unMuteIDExist(int id) {
        if(storageType.equals(ServerSystem.StorageType.MongoDB)){
            return ServerSystem.getInstance().getMongoDatabase().getCollection(ServerSystem.getInstance().getConfig().getString("mongodb.bans"))
                    .find(Filters.and(Filters.eq("_id", id), Filters.eq("type", "mute"))).first().getString("bannedUUID");
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE type = 'mute' AND _id = ? LIMIT 1");

                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if(rs.next()){
                    return rs.getString("bannedUUID");
                }
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        return null;
    }
    public int generateId() {
        Random random = new Random();
        int i = random.nextInt(1000000);
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            if (banned.countDocuments(Filters.eq("_id", i)) != 0) {
                while (banned.find().into(new ArrayList<>()).contains(i)) {
                    i = random.nextInt(1000000);
                }
            }
            return i;
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM bans WHERE _id = ?");
                stmt.setInt(1, i);
                ResultSet rs = stmt.executeQuery();
                while(rs.next() && rs.getInt(1) == 1){
                    i = random.nextInt();
                    stmt.setInt(1, i);
                    rs = stmt.executeQuery();
                }
                return i;
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        return 0;
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
