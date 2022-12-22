package com.hrens.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public abstract class FileConfig {

    private File file;

    private File dir;

    ArrayList<String> files;

    private YamlConfiguration yaml;

    FileConfig(JavaPlugin plugin, String name) {
        files = new ArrayList<>();
        files.add("de-DE-messages.yml");
        files.add("en-EN-messages.yml");
        dir = plugin.getDataFolder();


        if(!dir.exists()) {
            dir.mkdirs();
        }

        for (String sf : files) {
            if (!new File(dir, name).exists()) {
                plugin.saveResource(sf, false);
            }
        }

        yaml = new YamlConfiguration();
        file = new File(dir, name);
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