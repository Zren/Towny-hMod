import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Town implements Comparable<Town> {
    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
    public String name;
    public Resident mayor;
	public ArrayList<Resident> assistants;
    public ArrayList<Resident> residents;
	public Wall wall;
    public String townBoard;
    public Nation nation;
    public int activeResidents;
    public boolean isCapital;
    public Region region;
	public int bonusBlocks;
	public TownBlock homeBlock;
	
	public int protectionStatus;
	// 3 = Protect
	// 4 = Don't protect (Build+Destroy)
	// 5 = Build-Only
	
    public Town(String name) {
        this.name = name;
		assistants = new ArrayList<Resident>();
        residents = new ArrayList<Resident>();
        activeResidents = 0;
		wall = new Wall();
        isCapital = false;
		townBoard = "To change me, use: /town setboard [msg]";
		protectionStatus = 3;
		bonusBlocks = 0;
    }
	
	public ArrayList<Player> getOnlinePlayers() {
		ArrayList<Player> out = new ArrayList<Player>();
		for (Player player : etc.getServer().getPlayerList()) {
			if (belongsToTown(player.getName()))
				out.add(player);
		}
		return out;
	}
	
	public boolean belongsToTown(String name) {
		for (Resident resident : residents) {
			if (resident.name.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
    
    public boolean addResident(Resident resident) {
        if (!residents.contains(resident)) {
            residents.add(resident);
            resident.town = this;
            return true;
        } else {
            return false;
        }
    }
    
    public boolean remResident(Resident resident) {
        if (residents.contains(resident)) {
            if (resident == mayor)
                mayor = null;
            if (resident.isMayor)
				resident.isMayor = false;
            resident.town = null;
            residents.remove(resident);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean remAll() {
        mayor = null;
        for (Resident resident : residents) {
            resident.isMayor = false;
            resident.town = null;
        }
        residents.clear(); 
        if (size() == 0)
            return true;
        else
            return false;
    }
    
    public boolean setMayor(Resident resident) {
        if (residents.contains(resident)) {
            if (mayor != null)
                mayor.isMayor = false;
			clearAssistants();
            mayor = resident;
            resident.isMayor = true;
            return true;
        }
        return false;
    }
	
	public void clearAssistants() {
		for (Resident assistant : assistants)
			assistant.isMayor = false;
		assistants.clear();
	}
    
    public void countActiveResidents() {
        activeResidents = 0;
        long now = System.currentTimeMillis();
        for (Resident resident : residents) {
            if (now - resident.lastLogin < TownyProperties.activePeriod)
                activeResidents++;
        }   
    }
    
    public int size() {
        return residents.size();
    }
	
	public int getMaxTownBlocks() {
		return TownyProperties.getTownBlockLimit(size()) + bonusBlocks;
	}
    
    public ArrayList<String> getStatus() {
        ArrayList<String> out = new ArrayList<String>();
        
        // ___[ Racoon City ]___
        out.add(ChatTools.formatTitle(toString()));
        
        // Lord: Mayor Quimby
        // Board: Get your fried chicken
        if (townBoard != null)
            out.add(Colors.Green + "Board: " + Colors.LightGreen + townBoard);
        
		// Town Size: 0 / 16 [Bonus: 0]
		out.add(Colors.Green + "Town Size: " + Colors.LightGreen + TownyWorld.getInstance().countTownBlocks(this) + " / " + getMaxTownBlocks() + Colors.LightBlue + " [Bonus: "+bonusBlocks+"]");
		
		//if (mayor != null)
            out.add(Colors.Green + "Lord: " + Colors.LightGreen + mayor);
        // Assistants:
		// Sammy, Ginger
        if (assistants.size() > 0) {
			out.add(Colors.Green + "Assistants:");
			out.addAll(ChatTools.list(assistants.toArray()));
		}
        // Nation: Azur Empire
        if (nation != null)
            out.add(Colors.Green + "Nation: " + Colors.LightGreen + nation);
        
        // Residents [12]:
        // James, Carry, Mason
        out.add(Colors.Green + "Residents " + Colors.LightGreen + "[" + size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(residents.toArray()));
        
        return out;
    }
    
    public String toString() {
        if (isCapital)
            return name + TownyProperties.getCapitalPrefix(activeResidents);
        else
            return name + TownyProperties.getTownPrefix(activeResidents);
    }
    
    public int compareTo(Town o) {
        return this.size() - o.size() ;
    }
}