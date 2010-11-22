import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;

public class Towny extends Plugin {
    protected static final Logger log = Logger.getLogger("Minecraft");
    private TownyThread towny;
    private CommandQueue<Object> commandQueue;
    
    public TownyListener listener;
    
    public Towny() {
		listener = new TownyListener();
	}
    
    public void enable() throws NullPointerException {
		commandQueue = new CommandQueue<Object>();
        towny = new TownyThread(commandQueue);
        listener.towny = towny;
		listener.commandQueue = commandQueue;
		
        if (towny.load() && towny.loadData()) {
			towny.updateAllPlayerZones();
            towny.start();
            towny.world.updatePopulationCount();
            log.info("[Towny] Beta 1.9 - Mod Enabled.");
        } else {
            log.info("[Towny] Mod failed to load.");
            //Disable this plugin
        }
    }
    
    public void disable() {
        log.info("[Towny] Mod Disabled.");
		commandQueue.addWork(TownyThread.NO_MORE_WORK);
    }

    public void initialize() {
        etc.getLoader().addListener(PluginLoader.Hook.LOGIN, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.COMMAND, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.PLAYER_MOVE, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_CREATED, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.BLOCK_DESTROYED, listener, this, PluginListener.Priority.MEDIUM);
        etc.getLoader().addListener(PluginLoader.Hook.DISCONNECT, listener, this, PluginListener.Priority.MEDIUM);
		etc.getLoader().addListener(PluginLoader.Hook.ARM_SWING, listener, this, PluginListener.Priority.MEDIUM);
    }
}