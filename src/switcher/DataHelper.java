package switcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import switcher.Configurations.ConfigEntry;

public class DataHelper
{
    private static final Path CONFIGURATIONS_PATH = Paths
            .get(ClassLoader.getSystemClassLoader().getResource(".").getPath().substring(1).replace("%20", " ") + "configurations.json");
    
    public static File decafConfigFile = null;
    
    public static String readFromFile(Path path) throws IOException
    {
        if (Files.exists(path))
        {
            String result = "";

            for (String string : Files.readAllLines(path))
            {
                result += string + "\n";
            }

            return result;
        }

        Files.createFile(path);
        return "";
    }

    public static String readConfigurationsFile() throws IOException
    {
        return readFromFile(CONFIGURATIONS_PATH);
    }

    public static ConfigEntry mergeConfigEntries(ConfigEntry customConfigEntry, ConfigEntry decafConfigEntry)
    {
        decafConfigEntry.debugger = customConfigEntry.debugger;
        decafConfigEntry.game = customConfigEntry.game;
        decafConfigEntry.gpu = customConfigEntry.gpu;
        decafConfigEntry.jit = customConfigEntry.jit;
        customConfigEntry.log.level = decafConfigEntry.log.level;
        customConfigEntry.log.kernelTraceFilters = decafConfigEntry.log.kernelTraceFilters;
        decafConfigEntry.log = customConfigEntry.log;

        return decafConfigEntry;
    }

    public static void saveStateToFiles()
    {
        // Write to our own configurations file
        String jsonText = Switcher.GSON.toJson(Switcher.configurations);

        try
        {
            Files.write(CONFIGURATIONS_PATH, jsonText.getBytes());
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        // Write to the decaf configuration file for the current entry
        ConfigEntry entry = Switcher.getSelectedConfigEntry();

        if (entry != null)
        {
            String decafConfigText = "";

            try
            {
                decafConfigText = readFromFile(decafConfigFile.toPath());
            } catch (IOException e)
            {
                e.printStackTrace();
            }

            ConfigEntry decafEntry = Switcher.GSON.fromJson(decafConfigText, ConfigEntry.class);
            String newConfigText = Switcher.GSON.toJson(mergeConfigEntries(entry, decafEntry));
            
            try
            {
                Files.write(decafConfigFile.toPath(), newConfigText.getBytes());
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
