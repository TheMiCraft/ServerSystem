package com.hrens;

import com.hrens.commands.*;
import com.hrens.commands.tabcompleters.*;
import com.hrens.listener.discord.*;
import com.hrens.listener.minecraft.*;
import com.hrens.utils.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

@Getter
public class ServerSystem extends JavaPlugin {
    JDA jda;
    MongoClient mongoClient;
    Connection connection;
    MongoDatabase mongoDatabase;
    static String prefix;
    public boolean module_mcchat;
    public boolean module_playtime;
    public boolean module_bansystem;
    public boolean luckperms = false;
    public boolean bungeecord;
    MessageConfig messageConfig;
    LuckPerms lpapi;
    PluginManager pluginManager;
    @Getter
    public static ServerSystem instance;
    private LogManager logManager;
    private BanManager banManager;
    PlaytimeUtils playtimeUtils;
    StorageType playtimestorageType;
    StorageType banstorageType;
    private PlaytimeConfig fileDBplaytime;

    @Override
    public void onEnable() {
        getLogger().info("Loading ServerSystem...");
        instance = this;
        getLogger().info("Loading Config...");
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        FileConfiguration cfg = getConfig();
        cfg.options().copyDefaults(true);
        saveConfig();
        getLogger().info("Config loaded");
        playtimestorageType = StorageType.fromString(getConfig().getString("playtimestoragetype"));
        banstorageType = StorageType.fromString(getConfig().getString("banstoragetype"));
        messageConfig = new MessageConfig(this, getConfig().getString("messages"));
        module_mcchat = getConfig().getBoolean("modules.mcchat.enabled");
        module_playtime = getConfig().getBoolean("modules.playtime.enabled");
        module_bansystem = getConfig().getBoolean("modules.bansystem.enabled");
        if(module_playtime && playtimestorageType.equals(StorageType.MongoDB)){
            initMongoDB();
        } else if(module_playtime && playtimestorageType.equals(StorageType.MySQL)){
            this.connection = initMySQL();
        } else if(module_playtime && playtimestorageType.equals(StorageType.File)){
            this.fileDBplaytime = initFileDatabase();
        }
        if(module_bansystem && banstorageType.equals(StorageType.MongoDB)){
            initMongoDB();
        } else if(module_bansystem && banstorageType.equals(StorageType.MySQL) && module_bansystem){
            initMySQL();
        } else if(module_bansystem){
            throw new IllegalArgumentException("StorageType must be MongoDB or MySQL.");
        }
        bungeecord = getConfig().getBoolean("bungeecord");
        prefix = getConfig().getString("prefix");
        pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new JoinEvent(this), this);
        pluginManager.registerEvents(new LeaveEvent(this), this);
        pluginManager.registerEvents(new ChatEvent(this), this);
        PluginCommand healCommand = getCommand("heal");
        assert healCommand != null;
        healCommand.setExecutor(new HealCommand(this));
        PluginCommand feedCommand = getCommand("heal");
        assert feedCommand != null;
        feedCommand.setExecutor(new FeedCommand(this));
        PluginCommand rebootCommand = getCommand("reboot");
        assert rebootCommand != null;
        rebootCommand.setExecutor(new RebootCommand(this));
        if(Objects.nonNull(pluginManager.getPlugin("LuckPerms"))) {
            luckperms = true;
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                lpapi = provider.getProvider();
            }
        }
        if(module_mcchat) initMCChat();
        if(module_playtime) initPlaytime();
        if(module_playtime) playtimeUtils.reload(Bukkit.getOnlinePlayers());
        if(module_bansystem) initBanSystem();
        if(bungeecord) getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("Loaded Serversystem");
    }

    public void initBanSystem(){
        logManager = new LogManager(banstorageType);
        banManager = new BanManager(banstorageType);
        PluginCommand unbanCommand = getCommand("unban");
        assert unbanCommand != null;
        unbanCommand.setExecutor(new UnbanCommand());
        unbanCommand.setTabCompleter(new UnbanCompleter());
        PluginCommand banCommand = getCommand("ban");
        assert banCommand != null;
        banCommand.setExecutor(new BanCommand());
        banCommand.setTabCompleter(new BanCompleter());
        PluginCommand muteCommand = getCommand("mute");
        assert muteCommand != null;
        muteCommand.setExecutor(new MuteCommand());
        muteCommand.setTabCompleter(new MuteCompleter());
        PluginCommand unmuteCommand = getCommand("unmute");
        assert unmuteCommand != null;
        unmuteCommand.setExecutor(new UnmuteCommand());
        unmuteCommand.setTabCompleter(new UnmuteCompleter());
        getCommand("check").setExecutor(new CheckCommand());
    }

    public void initMCChat(){
        pluginManager.registerEvents(new AdvancementEvent(this), this);
        pluginManager.registerEvents(new DeathEvent(this), this);
        getLogger().info("Starting Discord Bot...");
        JDABuilder builder = JDABuilder.createLight(getConfig().getString("modules.mcchat.token"));
        builder.setActivity(Activity.playing(getConfig().getString("activity")));
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setEnabledIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.addEventListeners(new MessageRecieveEvent(this));
        try {
            jda = builder.build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public void initPlaytime(){
        this.playtimeUtils = new PlaytimeUtils(getConfig().getString("server"), playtimestorageType, mongoDatabase, connection, fileDBplaytime, getConfig().getString("mysql.DB_NAME"));
        pluginManager.registerEvents(new MoveEvent(this), this);
    }
    public enum StorageType {
        MongoDB, File, MySQL;

        public static StorageType fromString(String input) {
            for (StorageType type : StorageType.values()) {
                if (type.name().equalsIgnoreCase(input)) {
                    return type;
                }
            }
            return null;
        }
    }
    private Connection initMySQL(){
        getLogger().info("Connecting to Database");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try(Connection conn = DriverManager.getConnection(getConfig().getString("mysql.DB_URL"), getConfig().getString("mysql.DB_USER"), getConfig().getString("mysql.DB_PASS"))){
            getLogger().info("Connected to Database");

            return conn;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }

    private void initMongoDB(){
        getLogger().info("Connecting to Database");
        this.mongoClient = MongoClients.create(Objects.requireNonNull(getConfig().getString("mongodb.mongourl")));
        this.mongoDatabase = mongoClient.getDatabase(Objects.requireNonNull(getConfig().getString("mongodb.database")));
        getLogger().info("Connected to Database");
    }

    private PlaytimeConfig initFileDatabase() {
        getLogger().info("Connecting to File Database");
        PlaytimeConfig playtimeConfig = new PlaytimeConfig(this, "playtime.yml");
        getLogger().info("Connected to File Database");
        return playtimeConfig;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public JDA getJDA() {
        return jda;
    }

    public static String getPrefix() {
        return prefix;
    }

    public LuckPerms getLPAPI() {
        return lpapi;
    }

    public String getMessage(String path) {
        return messageConfig.getYaml().getString(path).replace("{prefix}", getPrefix()).replace("\\n", System.lineSeparator());
    }
}