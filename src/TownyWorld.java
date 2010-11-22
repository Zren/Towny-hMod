import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TownyWorld {
    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
    private static volatile TownyWorld instance;
    public HashMap<String,Nation> nations;
    public HashMap<String,Town> towns;
    public HashMap<String,Resident> residents;
    public HashMap<String,TownBlock> townblocks;
    public int activeResidents;
	public TownyDataSource database;
    
    public TownyWorld() {
        nations = new HashMap<String,Nation>();
        towns = new HashMap<String,Town>();
        residents = new HashMap<String,Resident>();
        townblocks = new HashMap<String,TownBlock>();
        activeResidents = 0;
        instance = this;
    }
    
    public static TownyWorld getInstance() {
        return instance;
    }
    
    public boolean newNation(String name) {
        Nation testNation = nations.get(name);
        if (testNation == null) {
            nations.put(name, new Nation(name));
            return true;
        } else {
            return false;
        }
    }
    
    public boolean newTown(String name) {
        Town testTown = towns.get(name);
        if (testTown == null) {
            towns.put(name, new Town(name));
            return true;
        } else {
            return false;
        }
    }
    
    public boolean newResident(String name) {
        Resident testResident = residents.get(name);
        if (testResident == null) {
            residents.put(name, new Resident(name));
            return true;
        } else {
            return false;
        }
    }
    
    public boolean newTownBlock(long x, long z) {
        return newTownBlock(Long.toString(x), Long.toString(z));
    }
    
    public boolean newTownBlock(String x, String z) {
        String key = x + "," + z;
        TownBlock testTownBlock = townblocks.get(key);
        if (testTownBlock == null) {
            townblocks.put(key, new TownBlock(Long.parseLong(x), Long.parseLong(z)));
            return true;
        } else {
            return false;
        }
    }
    
    public void delTownBlocks(Town town) {
		ArrayList<String> keysToRemove = new ArrayList<String>();
        for (String key : townblocks.keySet()) {
            TownBlock townblock = townblocks.get(key);
			if (townblock == null)
				continue;
            if (townblock.town == town)
                keysToRemove.add(key);
        }
		for (String key : keysToRemove)
			townblocks.remove(key);
		database.saveTown(town);
		database.saveTownBlocks();
    }
    
    public boolean delNation(String name) {
        if (nations.containsKey(name)) {
            Nation nation = nations.get(name);
            nation.remAll();
            nations.remove(name);
            return true;
        }
        return false;
    }
    
    public boolean delTown(String name) {
        if (towns.containsKey(name)) {
            Town town = towns.get(name);
			WallGen.deleteTownWall(this, town);
            delTownBlocks(town);
            if (town.nation != null)
                town.nation.remTown(town);
            town.remAll();
            towns.remove(name);
            return true;
        }
        return false;
    }
    
    public boolean delResident(String name) {
        if (residents.containsKey(name)) {
            Resident resident = residents.get(name);
            if (resident.town != null)
                resident.town.remResident(resident);
            residents.remove(name);
            return true;
        }
        return false;
    }
    
    public boolean delTownBlock(String key) {
        if (townblocks.containsKey(key)) {
            townblocks.remove(key);
            return true;
        }
        return false;
    }
	
	public int countTownBlocks(Town town) {
		int n = 0;
		for (TownBlock tb : townblocks.values()) {
			if (tb.town == town)
				n++;
		}
		
		return n;
	}
	
	public ArrayList<TownBlock> getTownBlocks(Town town) {
		ArrayList<TownBlock> out = new ArrayList<TownBlock>();
		for (TownBlock tb : townblocks.values()) {
			if (tb.town == town)
				out.add(tb);
		}
		return out;
	}
    
    public void updatePopulationCount() {
        long now = System.currentTimeMillis();
    
        // Update Server Count
        activeResidents = 0;
        for (Resident resident : residents.values()) {
            if (now - resident.lastLogin < TownyProperties.activePeriod) {
                activeResidents++;
				resident.isActive = true;
			} else {
				resident.isActive = false;
			}
        } 
        
        // Update Town & Nation Count
        for (Town town : towns.values())
            town.countActiveResidents(); 
        for (Nation nation : nations.values())
            nation.countActiveResidents();
    }
    
    
    public ArrayList<String> getStatus() {
        ArrayList<String> out = new ArrayList<String>();
        
        int inTown = 0;
        int notInTown = 0;
        for (Town town : towns.values())
            inTown += town.size();
        notInTown = activeResidents-inTown;
        
        out.add("World Population: " + Integer.toString(activeResidents));
        out.add("Hobos: " + Integer.toString(notInTown));
        out.add("Nations: " + Integer.toString(nations.size()));
        for (Nation nation : nations.values()) {
            out.add(nation + " [" + Integer.toString(nation.towns.size()) + "][" + nation.activeResidents + "]");
            for (Town town : nation.towns) {
                out.add("   " + town + " [" + Integer.toString(town.residents.size()) + "] " + town.townBoard);
                for (Resident resident : town.residents) {
                    out.add("       "+resident.name);
                }
            }
        }
        
        return out;
    }
}