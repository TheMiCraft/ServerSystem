package com.hrens.utils;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public abstract class FileConfig {

    // The .yml file itself
    private File file;

    // The data folder directory
    private File dir;

    // The configuration field for 'reading' the file
    private YamlConfiguration yaml;

    FileConfig(JavaPlugin plugin, String name) {

        dir = plugin.getDataFolder();

        // I generally ignore the boolean result
        if(!dir.exists()) {
            dir.mkdirs();
        }

        // Creates a file in the data folder directory called 'name'.
        file = new File(dir, name);

        // If it is not located in here, find the file in the resources
        // directory in the project and save it to the data directory.
        if(!file.exists()) {
            plugin.saveResource(name, false);
        }

        // Making a new configuration
        yaml = new YamlConfiguration();

        // Loading the file's data into the configuration field
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            System.out.println(123);
        }
    }

    // Saves the configuration data
    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Basic getter for retrieving the configuration data
    public YamlConfiguration getYaml() {
        return yaml;
    }
}