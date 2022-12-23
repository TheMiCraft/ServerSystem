package com.hrens.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PlaytimeConfig {

    private File file;

    private File dir;

    private YamlConfiguration yaml;

    public PlaytimeConfig(JavaPlugin plugin, String name) {
        dir = plugin.getDataFolder();

        if(!dir.exists()) {
            dir.mkdirs();
        }

        file = new File(dir, name);

        if (!file.exists()) {
            plugin.saveResource(file.getName(), false);
        }

        yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public YamlConfiguration getYaml() {
        return yaml;
    }
}