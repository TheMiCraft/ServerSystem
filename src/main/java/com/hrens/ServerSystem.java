package com.hrens;

import com.hrens.commands.*;
import com.hrens.commands.tabcompleters.BanCompleter;
import com.hrens.commands.tabcompleters.MuteCompleter;
import com.hrens.commands.tabcompleters.UnbanCompleter;
import com.hrens.commands.tabcompleters.UnmuteCompleter;
import com.hrens.listener.discord.MessageRecieveEvent;
import com.hrens.listener.minecraft.*;
import com.hrens.utils.BanManager;
import com.hrens.utils.LogManager;
import com.hrens.utils.MessageConfig;
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
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
@Getter
public class ServerSystem extends JavaPlugin {
    JDA jda;
    MongoClient mongoClient;
    MongoDatabase mongoDatabase;
    static String prefix;
    Map<UUID, Long> map;
    Map<UUID, Long> afk;
    Map<Player, Long> lastmove;
    public boolean module_mcchat;
    public boolean module_playtime;
    public boolean module_bansystem;
    public boolean bungeecord;
    MessageConfig messageConfig;
    LuckPerms lpapi;
    PluginManager pluginManager;
    @Getter
    public static ServerSystem instance;
    private LogManager logManager;
    private BanManager banManager;
    @Override
    public void onEnable() {
        getLogger().info("Loading ServerSystem...");
        instance = this;
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            lpapi = provider.getProvider();
        }
        messageConfig = new MessageConfig(this, "messages.yml");
        map = new HashMap<>();
        afk = new HashMap<>();
        lastmove = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            map.put(player.getUniqueId(), System.currentTimeMillis());
            lastmove.put(player, System.currentTimeMillis());
            afk.put(player.getUniqueId(), 0L);
        }
        getLogger().info("Loading Config...");
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        FileConfiguration cfg = getConfig();
        cfg.options().copyDefaults(true);
        saveConfig();
        getLogger().info("Config loaded");
        module_mcchat = getConfig().getBoolean("modules.mcchat.enabled");
        module_playtime = getConfig().getBoolean("modules.playtime.enabled");
        module_bansystem = getConfig().getBoolean("modules.bansystem.enabled");
        bungeecord = getConfig().getBoolean("bungeecord");
        prefix = getConfig().getString("prefix");
        getLogger().info("Connecting to Database");
        mongoClient = MongoClients.create(Objects.requireNonNull(getConfig().getString("mongodb.mongourl")));
        mongoDatabase = mongoClient.getDatabase(Objects.requireNonNull(getConfig().getString("mongodb.database")));
        getLogger().info("Connected to Database");
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new JoinEvent(this), this);
        pluginManager.registerEvents(new LeaveEvent(this), this);
        pluginManager.registerEvents(new ChatEvent(this), this);
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("reboot").setExecutor(new RebootCommand(this));
        if(module_mcchat) initMCChat();
        if(module_playtime) initPlaytime();
        if(module_bansystem) initBanSystem();
        if(bungeecord) getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("Loaded Serversystem");
    }


    public Map<UUID, Long> getMap() {
        return map;
    }

    public Map<UUID, Long> getAFK() {
        return afk;
    }

    public MongoDatabase getMongoDatabase() {
        return mongoDatabase;
    }

    public JDA getJDA() {
        return jda;
    }

    public Map<Player, Long> getLastMove() {
        return lastmove;
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

    public void initBanSystem(){
        logManager = new LogManager();
        banManager = new BanManager();

        PluginCommand unbanCommand = getCommand("unban");
        unbanCommand.setExecutor(new UnbanCommand());
        unbanCommand.setTabCompleter(new UnbanCompleter());
        PluginCommand banCommand = getCommand("ban");
        banCommand.setExecutor(new BanCommand());
        banCommand.setTabCompleter(new BanCompleter());
        PluginCommand muteCommand = getCommand("mute");
        muteCommand.setExecutor(new MuteCommand());
        muteCommand.setTabCompleter(new MuteCompleter());
        PluginCommand unmuteCommand = getCommand("unmute");
        unmuteCommand.setExecutor(new UnmuteCommand());
        unmuteCommand.setTabCompleter(new UnmuteCompleter());
        getCommand("check").setExecutor(new CheckCommand());
    }

    public void initMCChat(){
        pluginManager.registerEvents(new AdvancementEvent(this), this);
        getLogger().info("Starting Discord Bot...");
        JDABuilder builder = JDABuilder.createLight(getConfig().getString("modules.mcchat.token"));
        builder.setActivity(Activity.playing(getConfig().getString("activity")));
        builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
        builder.setEnabledIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGES);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.addEventListeners(new MessageRecieveEvent(this));
        try {
            jda = builder.build();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public void initPlaytime(){
        pluginManager.registerEvents(new MoveEvent(this), this);
    }
}
