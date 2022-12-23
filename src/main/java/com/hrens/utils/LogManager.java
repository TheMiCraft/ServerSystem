package com.hrens.utils;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import sun.rmi.runtime.Log;

import java.sql.*;
import java.util.*;

public class LogManager {

    public static MongoCollection<Document> collection;
    public Connection connection;
    private final ServerSystem.StorageType storageType;

    public LogManager(ServerSystem.StorageType storageType) {
        this.storageType = storageType;
        if(storageType.equals(ServerSystem.StorageType.MongoDB)){
            collection = ServerSystem.getInstance().getMongoClient().getDatabase(ServerSystem.getInstance().getConfig().getString("mongodb.database")).getCollection("Log");
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                this.connection = DriverManager.getConnection(ServerSystem.getInstance().getConfig().getString("mysql.DB_URL"), ServerSystem.getInstance().getConfig().getString("mysql.DB_USER"), ServerSystem.getInstance().getConfig().getString("mysql.DB_PASS"));
                connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS " + ServerSystem.getInstance().getConfig().getString("mysql.DB_NAME") + ";");
                connection.createStatement().execute("CREATE TABLE IF NOT EXISTS Log(type VARCHAR(255), executor VARCHAR(255), target VARCHAR(255), time BIGINT, reason VARCHAR(255));");

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addEntry(LogEntry logEntry) {
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            collection.insertOne(logEntry.toDocument());
        } else if(storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                assert connection != null;
                Document document = logEntry.toDocument();
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO Log (type, executor, target, time, reason) VALUES (?, ?, ?, ?, ?)");
                stmt.setString(1, logEntry.getType().toString());
                stmt.setString(2, logEntry.getExecutor());
                stmt.setString(3, logEntry.getTarget());
                stmt.setLong(4, logEntry.getTime());
                stmt.setString(5, logEntry.getReason());
                stmt.executeUpdate();
            } catch (SQLException e){
                 e.printStackTrace();
            }
        }
    }

//    public Collection<LogEntry> getLogByType(LogEntry.LogType type) {
//        return collection.find(Filters.eq("type", type.name())).map(LogEntry::fromDocument).into(new HashSet<>());
//    }

    public Collection<LogEntry> getLogByTarget(UUID target) {
        if(storageType.equals(ServerSystem.StorageType.MongoDB)) {
            return collection.find(Filters.eq("target", target.toString())).map(LogEntry::fromDocument).into(new HashSet<>());
        } else if (storageType.equals(ServerSystem.StorageType.MySQL)){
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Log WHERE target = ?");
                ArrayList<LogEntry> logEntries = new ArrayList<>();
                stmt.setString(1, target.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()){
                    Document document = new Document()
                            .append("type", rs.getString("type"))
                            .append("executor", rs.getString("executor"))
                            .append("target", rs.getString("target"))
                            .append("time", rs.getLong("time"))
                            .append("reason", rs.getString("reason"));
                    logEntries.add(LogEntry.fromDocument(document));
                }
                return logEntries;
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
        return null;
    }

    @AllArgsConstructor
    @Getter
    public static class LogEntry {
        private final LogType type;
        private final String executor;
        private final String target;
        private final long time;
        private final String reason;

        public Document toDocument() {
            Document document = new Document();
            document.put("type", type.name());
            document.put("executor", executor);
            document.put("target", target);
            document.put("time", time);
            document.put("reason", reason);
            return document;
        }

        public static LogEntry fromDocument(Document document) {
            LogType type = LogType.valueOf(document.getString("type"));
            String executor = document.getString("executor");
            String target = document.getString("target");
            Long time = document.getLong("time");
            String reason = document.getString("reason");
            Objects.requireNonNull(type, "Type is null");
            Objects.requireNonNull(target, "Target is null");
            Objects.requireNonNull(time, "Time is null");
            return new LogEntry(type, executor, target, time, reason);
        }

        public enum LogType {
            BAN,
            UNBAN,
            MUTE,
            UNMUTE,
            KICK
        }
    }
}
