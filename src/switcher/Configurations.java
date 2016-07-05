package switcher;

import java.util.HashMap;
import java.util.Map;

public class Configurations
{
    public Map<String, ConfigEntry> configEntries;
    public Map<String, GameEntry> gameEntries;
    public String decafConfigPath;

    public Configurations()
    {
        this.configEntries = new HashMap<String, ConfigEntry>();
        this.gameEntries = new HashMap<String, GameEntry>();
        this.decafConfigPath = "";
    }

    public ConfigEntry getOrCreateConfigEntry(String name)
    {
        if (!configEntries.containsKey(name))
            configEntries.put(name, new ConfigEntry());

        return configEntries.get(name);
    }

    public static class ConfigEntry
    {
        public DebuggerOptions debugger;
        public GameOptions game;
        public GPUOptions gpu;
        public GX2Options gx2;
        public Map<String, Object> input; //Untouched by us
        public JITOptions jit;
        public LogOptions log;
        public Object system; //Untouched by us

        public ConfigEntry()
        {
            this.debugger = new DebuggerOptions();
            this.game = new GameOptions();
            this.gpu = new GPUOptions();
            this.gx2 = new GX2Options();
            this.input = new HashMap();
            this.jit = new JITOptions();
            this.log = new LogOptions();
        }
    }

    public static class DebuggerOptions
    {
        public boolean enabled;
        public boolean breakOnEntry;
    }

    public static class GameOptions
    {
        public String path;
    }
    
    public static class GPUOptions
    {
        public boolean forceSync;
    }

    public static class GX2Options
    {
        public boolean dumpTextures;
        public boolean dumpShaders;
    }

    public static class JITOptions
    {
        public boolean enabled;
        public boolean debug;
    }

    public static class LogOptions
    {
        public boolean async;
        public boolean toFile;
        public boolean toStdout;
        public boolean kernelTrace;
        public boolean branchTrace;
        public String level;
        public String[] kernelTraceFilters;
    }
    
    public static class GameEntry
    {
        public String path;
        
        public GameEntry(String path)
        {
            this.path = path;
        }
    }
}
