package com.hrens.utils;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PlaytimeUtils {

    private final ServerSystem.StorageType storageType;
    private final String mysql_dbname;
    private final PlaytimeConfig fileDB;
    private MongoCollection<Document> collection;
    Connection connection;
    private final String servername;
    Map<UUID, Long> jointime;
    Map<UUID, Long> afktime;
    Map<UUID, Long> lastmove;

    public PlaytimeUtils(String servername, ServerSystem.StorageType storageType, @Nullable MongoDatabase mongoDatabase, @Nullable Connection connection, @Nullable PlaytimeConfig fileDB, @Nullable String mysql_dbname) {
        this.storageType = storageType;
        this.servername = servername;
        this.mysql_dbname = mysql_dbname;
        this.fileDB = fileDB;
        jointime = new HashMap<>();
        afktime = new HashMap<>();
        lastmove = new HashMap<>();
        if(Objects.isNull(mongoDatabase) && Objects.isNull(connection) && Objects.isNull(fileDB)) throw new IllegalArgumentException("MongoDB, MySQL, PlaytimeConfig is null");

        switch (storageType) {
            case File:
                break;
            case MySQL:
                assert connection != null;
                try {
                    this.connection = DriverManager.getConnection(ServerSystem.getInstance().getConfig().getString("mysql.DB_URL"), ServerSystem.getInstance().getConfig().getString("mysql.DB_USER"), ServerSystem.getInstance().getConfig().getString("mysql.DB_PASS"));
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                break;
            case MongoDB:
                assert mongoDatabase != null;
                collection = mongoDatabase.getCollection("playtime");
                break;
        }
    }
    public void playerJoin(UUID uuid){
        jointime.put(uuid, System.currentTimeMillis());
        lastmove.put(uuid, System.currentTimeMillis());
        afktime.put(uuid, 0L);
    }
    public void playerLeave(UUID uuid){
        switch (storageType) {
            case File:
                YamlConfiguration yaml = fileDB.getYaml();
                if(yaml.contains(uuid.toString())){
                    long time = yaml.getLong(servername + "." + uuid.toString() + "time");
                    long afk = yaml.getLong(servername + "." + uuid.toString() + "afk");
                    yaml.set(servername + "." + uuid.toString() + ".time", time + (System.currentTimeMillis() - jointime.get(uuid) - this.afktime.get(uuid)));
                    yaml.set(servername + "." + uuid.toString() + ".afk", afk + this.afktime.get(uuid));
                } else {
                    yaml.set(servername + "." + uuid.toString() + ".time", (System.currentTimeMillis() - jointime.get(uuid) - this.afktime.get(uuid)));
                    yaml.set(servername + "." + uuid.toString() + ".afk", this.afktime.get(uuid));
                }
                fileDB.save();
                break;
            case MySQL:
                try {
                    assert connection != null;
                    assert !connection.isClosed();
                    connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + mysql_dbname + ";");
                    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS playtime(uuid VARCHAR(255) PRIMARY KEY, time_" + servername + " BIGINT, afk_" + servername + " BIGINT);");
                    ResultSet rs = connection.createStatement().executeQuery("SELECT EXISTS (SELECT 1 FROM " + mysql_dbname + ".playtime WHERE uuid = uuid)");
                    if (rs.next()) {
                        boolean exists = rs.getBoolean(1);
                        PreparedStatement stmt;
                        if (exists) {
                            stmt = connection.prepareStatement("UPDATE playtime SET time_" + servername + " = ?, afk_" + servername + " = ? WHERE uuid = ?");
                            stmt.setLong(1, (System.currentTimeMillis() - (Objects.nonNull(jointime.get(uuid)) ? jointime.get(uuid) : System.currentTimeMillis()) - (Objects.nonNull(this.afktime.get(uuid)) ? this.afktime.get(uuid) : 0L)));
                            stmt.setLong(2, Objects.nonNull(this.afktime.get(uuid)) ? this.afktime.get(uuid) : 0L);
                            stmt.setString(3, uuid.toString());
                        } else {
                            stmt = connection.prepareStatement("INSERT INTO playtime (uuid, time_" + servername + ", afk_" + servername + ") VALUES (?, ?, ?)");
                            stmt.setString(1, uuid.toString());
                            stmt.setLong(1, (System.currentTimeMillis() - (Objects.nonNull(jointime.get(uuid)) ? jointime.get(uuid) : System.currentTimeMillis()) - (Objects.nonNull(this.afktime.get(uuid)) ? this.afktime.get(uuid) : 0L)));
                            stmt.setLong(2, Objects.nonNull(this.afktime.get(uuid)) ? this.afktime.get(uuid) : 0L);
                        }
                        stmt.executeUpdate();
                    }
                } catch (SQLException e){
                    e.printStackTrace();
                }
                break;
            case MongoDB:
                if (collection.countDocuments(Filters.eq("uuid", uuid.toString())) == 1) {
                    collection.updateOne(
                            Filters.eq("uuid", uuid.toString()),
                            Updates.inc("time-" + servername, (System.currentTimeMillis() - jointime.get(uuid) - this.afktime.get(uuid))));
                    collection.updateOne(
                            Filters.eq("uuid", uuid.toString()),
                            Updates.inc("afk-" + servername, this.afktime.get(uuid)));
                } else {
                    collection.insertOne(new Document()
                            .append("uuid", uuid.toString())
                            .append("time-" + servername, System.currentTimeMillis() - jointime.get(uuid))
                            .append("afk-" + servername, this.afktime.get(uuid)));
                }
                break;
        }
        jointime.remove(uuid);
        afktime.remove(uuid);
        lastmove.remove(uuid);
    }

    public void reload(Collection<? extends Player> players) {
        players.forEach(player -> {
            jointime.put(player.getUniqueId(), System.currentTimeMillis());
            lastmove.put(player.getUniqueId(), System.currentTimeMillis());
            afktime.put(player.getUniqueId(), 0L);
        });
    }

    public void move(Player player) {
        long l = System.currentTimeMillis();
        long l1 = lastmove.get(player.getUniqueId());
        if (l - l1 >= ServerSystem.getInstance().getConfig().getInt("modules.playtime.afktime")) {
            player.sendMessage(ServerSystem.getInstance().getMessage("afktime")
                    .replace("{minutes}", String.valueOf(TimeUnit.MILLISECONDS.toMinutes(l - l1)))
                    .replace("{seconds}", String.valueOf((TimeUnit.MILLISECONDS.toSeconds(l - l1) % 60))));
            Long aLong = afktime.get(player.getUniqueId()) + (l - l1);
            afktime.replace(player.getUniqueId(), aLong);
        }
        lastmove.put(player.getUniqueId(), l);
    }
}
