package com.hrens.utils;

import com.hrens.ServerSystem;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

public class LogManager {

    public static MongoCollection<Document> collection = ServerSystem.getInstance().getMongoClient().getDatabase(ServerSystem.getInstance().getConfig().getString("mongodb.database")).getCollection("Log");

    public void addEntry(LogEntry logEntry) {
        collection.insertOne(logEntry.toDocument());
    }

    public Collection<LogEntry> getLogByType(LogEntry.LogType type) {
        return collection.find(Filters.eq("type", type.name())).map(LogEntry::fromDocument).into(new HashSet<>());
    }

    public Collection<LogEntry> getLogByTarget(UUID target) {
        return collection.find(Filters.eq("target", target.toString())).map(LogEntry::fromDocument).into(new HashSet<>());
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
