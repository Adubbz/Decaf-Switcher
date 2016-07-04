package switcher;

import java.util.HashMap;
import java.util.Map;

public class Configurations
{
    public Map<String, ConfigEntry> configEntries;
    public String decafConfigPath;

    public Configurations()
    {
        this.configEntries = new HashMap<String, ConfigEntry>();
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
        public GPUOptions gpu;
        public GX2Options gx2;
        public Map<String, PadOptions> input;
        public JITOptions jit;
        public LogOptions log;
        public SystemOptions system;

        public ConfigEntry()
        {
            this.debugger = new DebuggerOptions();
            this.gpu = new GPUOptions();
            this.gx2 = new GX2Options();
            this.input = new HashMap();
            this.jit = new JITOptions();
            this.log = new LogOptions();
            this.system = new SystemOptions();
        }
    }

    public static class DebuggerOptions
    {
        public boolean enabled;
        public boolean breakOnEntry;
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

    public static class PadOptions
    {
        public String name;
        public int buttonUp;
        public int buttonDown;
        public int buttonLeft;
        public int buttonRight;
        public int buttonA;
        public int buttonB;
        public int buttonX;
        public int buttonY;
        public int buttonTriggerR;
        public int buttonTriggerL;
        public int buttonTriggerZr;
        public int buttonTriggerZl;
        public int buttonStickL;
        public int buttonStickR;
        public int buttonPlus;
        public int buttonMinus;
        public int buttonHome;
        public int buttonSync;
        public int leftStickX;
        public int leftStickY;
        public int rightStickX;
        public int rightStickY;
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
    }

    public static class SystemOptions
    {
        public String systemPath;
    }
}
