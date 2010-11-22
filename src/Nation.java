import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Nation implements Comparable<Nation> {
    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
    public String name;
    public Town capital;
	public ArrayList<Resident> assistants;
    public ArrayList<Town> towns;
    public ArrayList<Nation> friends;
    public ArrayList<Nation> enemies;
    public int activeResidents;
    
    public Nation(String name) {
        this.name = name;
		assistants = new ArrayList<Resident>();
        towns = new ArrayList<Town>();
        friends = new ArrayList<Nation>();
        enemies = new ArrayList<Nation>();
        activeResidents = 0;
    }
    
    public boolean addTown(Town town) {
        if (!towns.contains(town)) {
            towns.add(town);
            town.nation = this;
            return true;
        } else {
            return false;
        }
    }
    
    public boolean remTown(Town town) {
        int index = towns.indexOf(town);
        if (index != -1) {
            towns.remove(index);
            town.nation = null;
            return true;
        } else {
            return false;
        }
    }
	
	public boolean addFriend(Nation nation) {
        if (!friends.contains(nation)) {
            friends.add(nation);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean remFriend(Nation nation) {
        int index = friends.indexOf(nation);
        if (index != -1) {
            friends.remove(index);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean addEnemy(Nation nation) {
        if (!enemies.contains(nation)) {
            enemies.add(nation);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean remEnemy(Nation nation) {
        int index = enemies.indexOf(nation);
        if (index != -1) {
            enemies.remove(index);
            return true;
        } else {
            return false;
        }
    }
	
	public ArrayList<Player> getOnlinePlayers() {
		ArrayList<Player> out = new ArrayList<Player>();
		for (Town town : towns) {
			out.addAll(town.getOnlinePlayers());
		}
		return out;
	}
	
	public boolean setAliegeance(String type, Nation nation) {
        if (type.equalsIgnoreCase("friend")) {
            remEnemy(nation);
            addFriend(nation);
            if (!enemies.contains(nation) && friends.contains(nation))
                return true;
        } else if (type.equalsIgnoreCase("neutral")) {
            remEnemy(nation);
            remFriend(nation);
            if (!enemies.contains(nation) && !friends.contains(nation))
                return true;
        } else if (type.equalsIgnoreCase("enemy")) {
            remFriend(nation);
            addEnemy(nation);
            if (enemies.contains(nation) && !friends.contains(nation))
                return true;
        }
        
        return false;
    }
    
    public boolean remAll() {
        for (Town town : towns) {
            town.nation = null;
        }
        towns.clear();
        if (size() == 0)
            return true;
        else
            return false;
    }
    
    public void countActiveResidents() {
        activeResidents = 0;
        for (Town town : towns) {
            activeResidents += town.activeResidents;
        }   
    }
    
    public void dertermineCapital() {
        if (size() > 0) {
            sortTownsBySize();
            if (capital != null)
                capital.isCapital = false;
            capital = null;
            capital = towns.get(0);
            capital.isCapital = true;
			if (capital.mayor != null)
				capital.mayor.isKing = true;
        }
    }
    
    public void sortTownsBySize() {       
        Collections.sort(towns, new TownSortBySize());
        Collections.reverse(towns);
    }
    
    public int size() {
        return towns.size();
    }
    
    public ArrayList<String> getStatus() {
        ArrayList<String> out = new ArrayList<String>();
        
        // ___[ Azur Empire ]___
        out.add(ChatTools.formatTitle(toString()));
        
        // King: King Harlus
        if (size() > 0 && capital != null && capital.mayor != null)
            out.add(Colors.Green + "King: " + Colors.LightGreen + capital.mayor);
		// Assistants:
		// Mayor Rockefel, Sammy, Ginger
        if (assistants.size() > 0) {
			out.add(Colors.Green + "Assistants:");
			out.addAll(ChatTools.list(assistants.toArray()));
		}
        // Towns [44]:
        // James City, Carry Grove, Mason Town
        out.add(Colors.Green + "Towns " + Colors.LightGreen + "[" + size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(towns.toArray()));
		// Friendly towards [4]:
        // James Nation, Carry Territory, Mason Country
        out.add(Colors.Green + "Friendly towards " + Colors.LightGreen + "[" + friends.size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(friends.toArray()));
		// Enemies [4]:
        // James Nation, Carry Territory, Mason Country
        out.add(Colors.Green + "Enemies " + Colors.LightGreen + "[" + enemies.size() + "]" + Colors.Green + ":");
        out.addAll(ChatTools.list(enemies.toArray()));
		
        
        return out;
    }
    
    public String toString() {
        return name + TownyProperties.getNationPrefix(activeResidents);
    }
    
    public int compareTo(Nation o) {
        return this.size() - o.size() ;
    }
}