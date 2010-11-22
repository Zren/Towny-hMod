import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TownyProperties {
    protected static final Logger log = Logger.getLogger("Minecraft");
    public static long activePeriod;
    public static int timeTillWar, blockSize;
    public static String source, flatFileFolder, settingsFolder;
    public static HashMap<Integer, HashMap<String, String>> townLevel = new HashMap<Integer, HashMap<String,String>>();
    public static HashMap<Integer, HashMap<String, String>> nationLevel = new HashMap<Integer, HashMap<String,String>>();
    public static boolean townCreationAdminOnly, unclaimedZoneBuildRights, wallGenOn;
	public static int claimRatio;
	
	@SuppressWarnings ("unchecked")
    public static void load() {
        //Default values
        HashMap<String, String> temp = new HashMap<String, String>();
        temp.put("description", "Country");
        temp.put("capital", "Capital");
        temp.put("monarch", "King");
        nationLevel.put(0, (HashMap<String, String>)temp.clone());
        temp.clear();
        temp.put("description", "Town");
        temp.put("mayor", "Mayor");
        temp.put("blocklimit", "0");
        townLevel.put(0, (HashMap<String, String>)temp.clone());
        
        BufferedReader fin;
        String line;
        String[] tokens;
        
        if (source.equalsIgnoreCase("flatfile")) {
            // Load Nation Levels
            try {
                fin = new BufferedReader(new FileReader(flatFileFolder + "/settings/nationLevels.txt"));
                while ((line = fin.readLine()) != null) {
                    if (!line.startsWith("#")) { //Ignore comment lines
                        tokens = line.split(":");
                        if (tokens.length == 4) {
                            try {
                                int size = Integer.parseInt(tokens[0]);
                                temp = new HashMap<String, String>();
                                temp.put("description", tokens[1]);
                                temp.put("capital", tokens[2]);
                                temp.put("monarch", tokens[3]);
                                nationLevel.put(size, temp);
                            } catch (Exception e) { log.info("[Towny] Input Error: Nation level ignored: " + line); }
                        }
                    }
                }
                fin.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Load Town Levels
            try {
                fin = new BufferedReader(new FileReader(flatFileFolder + "/settings/townLevels.txt"));
                while ((line = fin.readLine()) != null) {
                    if (!line.startsWith("#")) { //Ignore comment lines
                        tokens = line.split(":");
                        if (tokens.length == 3 || tokens.length == 4) {
                            try {
                                int size = Integer.parseInt(tokens[0]);
                                temp = new HashMap<String, String>();
                                temp.put("description", tokens[1]);
                                temp.put("mayor", tokens[2]);
								if (claimRatio == 0 && tokens.length == 4)
									temp.put("blocklimit", tokens[3]);
                                townLevel.put(size, temp);
								//log.info("Added town level: "+size+" "+Arrays.toString(temp.values().toArray()));
                            } catch (Exception e) { log.info("[Towny] Input Error: Town level ignored: " + line); }
                        }
                    }
                }
                fin.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
			log.info("[Towny] Input Error: Unsupported database type.");
		}
    }
    
    public static HashMap<String, String> getNationProperties(int size) {
        HashMap<String, String> nationProperties = new HashMap<String, String>();;
        for (int n=size; n >= 0; n--) {
            nationProperties = nationLevel.get(n);
            if (nationProperties != null)
                break;
        }
        return nationProperties;
    }
    
    public static HashMap<String, String> getTownProperties(int size) {
        HashMap<String, String> townProperties = new HashMap<String, String>();;
        for (int n=size; n >= 0; n--) {
            townProperties = townLevel.get(n);
            if (townProperties != null)
                n = -1;
        }
        return townProperties;
    }
    
    public static String getMayorPrefix(int townSize) {
        HashMap<String, String> townProperties = getTownProperties(townSize);
        if (townProperties == null) {
            return "";
        } else {
            String prefix = townProperties.get("mayor");
            if (prefix == null)
                return "";
            else
                return prefix + " ";
        }
    }
    
    public static String getTownPrefix (int townSize) {
        HashMap<String, String> townProperties = getTownProperties(townSize);
        if (townProperties == null) {
            return "";
        } else {
            String prefix = townProperties.get("description");
            if (prefix == null)
                return "";
            else
                return " " + prefix;
        }
    }
    
    public static String getNationPrefix(int nationSize) {
        HashMap<String, String> nationProperties = getNationProperties(nationSize);
        if (nationProperties == null) {
            return "";
        } else {
            String prefix = nationProperties.get("description");
            if (prefix == null)
                return "";
            else
                return " " + prefix;
        }
    }
    
    public static String getCapitalPrefix (int nationSize) {
        HashMap<String, String> nationProperties = getNationProperties(nationSize);
        if (nationProperties == null) {
            return "";
        } else {
            String prefix = nationProperties.get("capital");
            if (prefix == null)
                return "";
            else
                return " " + prefix;
        }
    }
    
    public static String getKingPrefix (int nationSize) {
        HashMap<String, String> nationProperties = getNationProperties(nationSize);
        if (nationProperties == null) {
            return "";
        } else {
            String prefix = nationProperties.get("king");
            if (prefix == null)
                return "";
            else
                return prefix + " ";
        }
    }
	
	public static int getTownBlockLimit(int townSize) {
		
		if (claimRatio > 0) {
			return townSize*claimRatio;
		} else {
			HashMap<String, String> townProperties = getTownProperties(townSize);
			if (townProperties == null) {
				return 0;
			} else {
				//log.info("BlockLimit="+townProperties.get("blocklimit"));
				return Integer.parseInt(townProperties.get("blocklimit"));
			}
		}
	}
}