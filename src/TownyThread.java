import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Array;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;

public class TownyThread extends Thread {
    protected static final Logger log = Logger.getLogger("Minecraft");
    private final String newLine = System.getProperty("line.separator");
    
    private PropertiesFile properties;
    private String townsLocation;
    private String residentsLocation;
    private String townsDir;
    private String residentsDir;
    private int activePeriod;
	
	
	private Timer timer;
    
	public String[] zoneDeffinitions = {"Unclaimed", "Friendly", "Enemy", "Neutral", "Neutral-Public", "Neutral-Build Only"};
	public HashMap<String, Integer> playerZone;
    // 0 = Unclaimed Zone Restrictions in effect.
    // 1 = Belong to town / Is an ally
    // 2 = Enemy town
	// 3 = Don't belong to town -> Protect town
	// 4 = Don't belong to town -> Town is public (Build+Destroy)
	// 5 = Don't belong to town -> Town is build-only (Build)
	
    public static final Object NO_MORE_WORK = new Object();
    private CommandQueue<Object> commandQueue;
	
	public ArrayList<String> playerClaim;
	public ArrayList<String> playerMap;
    
    public TownyWorld world;
	private WarThread warThread;
    
    public TownyThread(CommandQueue<Object> cQ) {
        this.commandQueue = cQ;
        world = new TownyWorld();
		
		warThread = new WarThread(this);
		warThread.start();
		
		timer = new Timer();
		
		playerZone = new HashMap<String, Integer>();
		playerMap = new ArrayList<String>();
		playerClaim = new ArrayList<String>();
    }
	
    public void run() {
        try {
            while (true) {
                Object obj = commandQueue.getWork();
                
                if (obj == NO_MORE_WORK)
                    break;
                
                Class classType = obj.getClass().getComponentType();
                
                if (classType != null) { // Is this an Array of objects?
                    Object[] objs = new Object[Array.getLength(obj)];
                    for (int i = 0; i < Array.getLength(obj); i++)
                        objs[i] = Array.get(obj, i);
                    
                    if (objs.length == 2) {
                        if (objs[0] instanceof Player && objs[1] instanceof String[]) {
							try {
								onCommand((Player)objs[0], (String[])objs[1]);
								world.database.saveAll();
							} catch (Exception e) {
								log.info("[Towny] Command Error: " + ((Player)objs[0]).getName() + " trying command " + Arrays.toString((String[])objs[1]));
								e.printStackTrace();
							}
                        }
                    } else if (objs.length == 3) {
						if (objs[0] instanceof Player && objs[1] instanceof Location && objs[2] instanceof Location) {
							onPlayerMove((Player)objs[0], (Location)objs[1], (Location)objs[2]);
						}
					}
                }
            }
        } catch (InterruptedException e) {}
		
		world.database.saveAll();
		log.info("[Towny] CommandQueue Thread stoped.");
		world = null;
		commandQueue = null;
    }
	
    public void onLogin(Player player) {
        if (!world.residents.containsKey(player.getName())) {
            player.sendMessage(Colors.Green + "This is your first login to this server!");
            world.newResident(player.getName());
            Resident resident = world.residents.get(player.getName());
            if (resident != null) {
                resident.lastLogin = System.currentTimeMillis();
                player.sendMessage(Colors.Green + "You are now a registered citizen.");
            } else {
                player.sendMessage(Colors.Rose + "Error occured when logging in,");
                player.sendMessage(Colors.Rose + "you are not a registered citizen.");
                log.info("[Towny] Could not create the player "+player.getName());
            }
			world.database.saveResident(resident);
        } else {
            Resident resident = world.residents.get(player.getName());
            resident.lastLogin = System.currentTimeMillis();
            if (resident.town != null)
                player.sendMessage(Colors.Gold + "[" + resident.town.name + "] " + Colors.Yellow + resident.town.townBoard);
			world.database.saveResident(resident);
        }
		
		updatePlayerZone(player);
    }
	
	public void onPlayerMove(Player player, Location from, Location to) {
	
		updatePlayerZone(player);
        long[] fromTownBlock = TownyUtil.getTownBlock((long)from.x, (long)from.z);
        long[] toTownBlock = TownyUtil.getTownBlock((long)to.x, (long)to.z);
        
        if (fromTownBlock[0] != toTownBlock[0] || fromTownBlock[1] != toTownBlock[1]) {
            String key = toTownBlock[0]+","+toTownBlock[1];
            TownBlock townblock = world.townblocks.get(key);
            
			
            setPlayerZone(player, townblock);
			if (playerMap.contains(player.getName())) {
				sendMap(player);
				player.sendMessage("TownBlock: " + Long.toString(toTownBlock[0]) + ", " + Long.toString(toTownBlock[1]) + " [Zone: " + zoneDeffinitions[playerZone.get(player.getName())] + "]");
				if (townblock != null && townblock.town != null) {
					String line = "Town: " + townblock.town;
					if (townblock.resident != null)
						line += " [" + townblock.resident +"]";
					player.sendMessage(line);
				}
			}
        }
    }
	
	public void updateAllPlayerZones() {
		for (Player p : etc.getServer().getPlayerList())
			updatePlayerZone(p);
	}
	
	public void updatePlayerZone(Player player) {
		if (player == null) return;
		
		long[] posTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
        
		String key = posTownBlock[0]+","+posTownBlock[1];
		TownBlock townblock = world.townblocks.get(key);
		
		setPlayerZone(player, townblock);
		if (playerMap.contains(player.getName())) {
			if (townblock != null && townblock.town != null) {
				sendMap(player);
				player.sendMessage("TownBlock: " + Long.toString(posTownBlock[0]) + ", " + Long.toString(posTownBlock[1]) + " [Zone: " + zoneDeffinitions[playerZone.get(player.getName())] + "]");
				String line = "Town: " + townblock.town;
				if (townblock.resident != null)
					line += " [" + townblock.resident +"]";
				player.sendMessage(line);
			}
		}
	}
	
	public void setPlayerZone(Player player, TownBlock townblock) {
		if (townblock == null || townblock.town == null) { playerZone.put(player.getName(), 0); return; }
		Resident resident = world.residents.get(player.getName());
		if (resident == null) { playerZone.put(player.getName(), townblock.town.protectionStatus); return; }
		if (resident.town == null) { playerZone.put(player.getName(), townblock.town.protectionStatus); return; }
		if (resident.town == townblock.town) { playerZone.put(player.getName(), 1); return; }
		if (resident.town.nation == null || townblock.town.nation == null) { playerZone.put(player.getName(), townblock.town.protectionStatus); return; }
		else if (resident.town.nation.friends.contains(townblock.town.nation)) { playerZone.put(player.getName(), 1); return; }
		else if (resident.town.nation.enemies.contains(townblock.town.nation)) { playerZone.put(player.getName(), 2); return; }
		// Error, it shouldn't get this far.
    }
	
    public boolean onCommand(Player player, String[] split) {        
        if (player == null) {
            log.info("[Towny] Command skipped, player doesn't exist.");
            return false;
        }
        
        if (split[0].equalsIgnoreCase("/resident") || split[0].equalsIgnoreCase("/player")) {
			/*
				/resident
				/resident ?
				/resdient [resident]
				/resdient list
				/resdient delete [resident] *Admin
			*/
			if (split.length == 1) {
				// /resident
				Resident resident = world.residents.get(player.getName());
				if (resident == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
				
				for (String line : resident.getStatus())
					player.sendMessage(line);
				return true;
			} else if (split.length == 2) {
				// /resident ?
				if (split[1].equalsIgnoreCase("?")) { 
					// Skip to end and display commands.
				}
				// /resident list
				else if (split[1].equalsIgnoreCase("list")) { 
					player.sendMessage(ChatTools.formatTitle("Residents"));
					String colour;
					ArrayList<String> formatedList = new ArrayList<String>();
					for (Resident resident : world.residents.values()) {
						colour = Colors.White;
						if (resident.isActive) {
							if (resident.isMayor)
								colour = Colors.LightBlue;
							if (resident.isKing)
								colour = Colors.Gold;
							formatedList.add(colour + resident.name + Colors.White);
						}
					}
					for (String line : ChatTools.list(formatedList.toArray()))
						player.sendMessage(line);
					return true;
				}
				// /resident [resident]
				else {
					Resident resident = world.residents.get(split[1]);
					if (resident == null) { player.sendMessage(Colors.Rose + "Target is not registered."); return true; }
					for (String line : resident.getStatus())
						player.sendMessage(line);
					return true;
				}
			} else if (split.length == 3) {
				// /resident delete [resident]
				if (split[1].equalsIgnoreCase("delete")) {
					if (player.canUseCommand("/townyadmin"))
						delResident(player, split[2]);
					world.database.saveResidentList();
					return true;
				}
			}
			
			player.sendMessage(ChatTools.formatTitle("/resident"));
			player.sendMessage("  §3/resident");
			player.sendMessage("  §3/resident §b?");
			player.sendMessage("  §3/resdient §b[resident]");
			player.sendMessage("  §3/resdient §blist");
			player.sendMessage("  §cAdmin: §3/resdient §bdelete [resident]");
        }  
        else if (split[0].equalsIgnoreCase("/town")) {
            /*
				/town
				/town ?
				/town list
				/town leave
				/town new [town] [mayor] *Admin
				/town givebonus [town] [bonus] *Admin
				/town delete [town] *Admin
				/town add [resident] *Mayor
				/town kick [resident] *Mayor
				/town wall
				/town setboard [message]
				/town setlord [town] [lord]
				/town sethome
				/town protect [on/off/buildonly]
			*/
            if (split.length == 1) {
				// /town
				Resident resident = world.residents.get(player.getName());
				if (resident == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
				if (resident.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
				
				for (String line : resident.town.getStatus())
					player.sendMessage(line);
				return true;
			} else if (split.length == 2) {
				// /town ?
				if (split[1].equalsIgnoreCase("?")) {
					// Skip to end.
				}
				// /town list
				else if (split[1].equalsIgnoreCase("list")) {
					player.sendMessage(ChatTools.formatTitle("Towns"));
					ArrayList<String> formatedList = new ArrayList<String>();
					for (Town town : world.towns.values())
						formatedList.add(Colors.LightBlue + town.name + Colors.Blue + " [" + town.size() + "]" + Colors.White);
					for (String line : ChatTools.list(formatedList.toArray()))
						player.sendMessage(line);
					return true;
				}
				// /town leave
				else if (split[1].equalsIgnoreCase("leave")) {
					Resident resident = world.residents.get(player.getName());
					if (resident == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (resident.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					
					Town town = resident.town;
					sendTownMessage(town, Colors.Gold + resident + " just left town.");
					town.remResident(resident);
					updatePlayerZone(player);
					
					if (resident.town.nation != null && resident.town.nation.size() == 1)
						delNation(player, resident.town.nation.name);
					if (resident.town.size() == 1)
						delTown(player, resident.town.name);
					return true;
				}
				// /town sethome
				else if (split[1].equalsIgnoreCase("sethome")) {
					Resident resident = world.residents.get(player.getName());
					if (resident == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (resident.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					
					long[] playerTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
					String key = playerTownBlock[0] + "," + playerTownBlock[1];	
					TownBlock townblock = world.townblocks.get(key);
					
					if (townblock != null) { player.sendMessage(Colors.Rose + "Town hasn't set."); return true; }
					
					sendTownMessage(resident.town, Colors.Gold + resident + " moved the town center.");
					
					return true;
				}
				// /town [town]
				else {
					Town town = world.towns.get(split[1]);
					if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
					
					for (String line : town.getStatus())
						player.sendMessage(line);
					return true;
				}
			} else if (split.length >= 3 && (split[1].equalsIgnoreCase("setboard") || split[1].equalsIgnoreCase("add"))) {
				// /town add [resident] .. [resident]
				if (split[1].equalsIgnoreCase("add")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					for (int i = 2; i < split.length; i++) {
						Resident resident = world.residents.get(split[i]);
						if (resident == null) { player.sendMessage(Colors.Rose + "That player is not registered."); return true; }
						if (resident.town != null) { player.sendMessage(Colors.Rose + "That player already belongs to a town."); return true; }
						
						if (mayor.town.addResident(resident)) {
							sendTownMessage(mayor.town, Colors.Green + resident + " joined town!");
							updatePlayerZone(etc.getServer().matchPlayer(resident.name));
						} else {
							player.sendMessage(Colors.Rose + split[i] + " is already a resident.");
						}
					}
					return true;
				}
				// /town setboard [message]
				else if (split[1].equalsIgnoreCase("setboard")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					String line = "";
					for (int i=2; i < split.length; i++) {
						line += split[i];
						if (i != split.length-1)
							line += " ";
					}
					mayor.town.townBoard = line;
					sendTownMessage(mayor.town, Colors.Gold + "[" + mayor.town.name + "] " +Colors.Yellow + mayor.town.townBoard);
					return true;
				}
			} else if (split.length == 3) {
				// /town delete [town]
				if (split[1].equalsIgnoreCase("delete")) {
					if (!player.canUseCommand("/townyadmin")) { player.sendMessage(Colors.Rose + "This command is admin only."); return true; }
					Town town = world.towns.get(split[2]);
					if (town != null && town.nation != null) {
						Nation nation = town.nation;
						delTown(player, split[2]);
						if (nation.size() == 0)
							delNation(player, nation.name);
					} else {
						delTown(player, split[2]);
					}
					return true;
				}
				// /town kick [resident]
				else if (split[1].equalsIgnoreCase("kick")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					Resident resident = world.residents.get(split[2]);
					if (resident == null) { player.sendMessage(Colors.Rose + "That player is not registered."); return true; }
					if (resident.town == null) { player.sendMessage(Colors.Rose + "That player doesn't belong to any town."); return true; }
					if (resident == mayor) { player.sendMessage(Colors.Rose + "You cannot kick yourself."); return true; }
					if (resident == mayor.town.mayor) { player.sendMessage(Colors.Rose + "You cannot kick the mayor."); return true; }
					
					if (mayor.town.remResident(resident)) {
						sendTownMessage(mayor.town, Colors.Green + resident + " was kicked from town!");
						updatePlayerZone(etc.getServer().matchPlayer(resident.name));
					} else {
						player.sendMessage(Colors.Rose + "That resident doesn't belong to that town.");
					}
					return true;
				}
				// /town protect [on/off/buildonly]
				else if (split[1].equalsIgnoreCase("protect")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					if (split[2].equalsIgnoreCase("on")) {
						mayor.town.protectionStatus = 3;
						player.sendMessage(Colors.Green + "The town is now protected.");
						updateAllPlayerZones();
						return true;
					} else if (split[2].equalsIgnoreCase("off")) {
						mayor.town.protectionStatus = 4;
						player.sendMessage(Colors.Rose + "The town has lowered it's barriers.");
						updateAllPlayerZones();
						return true;
					} else if (split[2].equalsIgnoreCase("buildonly")) {
						mayor.town.protectionStatus = 5;
						player.sendMessage(Colors.Rose + "The town has a strict build only policy from outsiders.");
						updateAllPlayerZones();
						return true;
					} 
				}
				// /town wall remove
				else if (split[1].equalsIgnoreCase("wall")) {
					if (TownyProperties.wallGenOn) {					
						Resident mayor = world.residents.get(player.getName());
						if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
						if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
						if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
						
						if (split[2].equalsIgnoreCase("remove")) {
							WallGen.deleteTownWall(world, mayor.town);
							player.sendMessage("WallGen - Completed");
						}
						return true;
					} else {
						player.sendMessage("WallGen is not on. Ask an admin to turn it on.");
						return true;
					}
				}
			} else if (split.length == 4) {
				// /town setlord [town] [lord]
				if (split[1].equalsIgnoreCase("setlord")) {
					if (player.canUseCommand("/townyadmin")) {
						Town town = world.towns.get(split[2]);
						if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
						Resident mayor = world.residents.get(split[3]);
						if (mayor == null) { player.sendMessage(Colors.Rose + "Mayor is not registered."); return true; }
						if (mayor.town != null) { player.sendMessage(Colors.Rose + "Selected mayor doesn't belong to any town."); return true; }
						if (mayor.town != town) { player.sendMessage(Colors.Rose + "Selected mayor doesn't belong to target town."); return true; }
						
						if (town.setMayor(mayor)) {
							updateAllPlayerZones();
							sendTownMessage(town, Colors.Green + split[2] + " is now the lord of " + town.name + "!");
						} else {
							player.sendMessage(Colors.Rose + "Resident doesn't belong to that town.");
						}
						return true;
					}
				}
				// /town new [town] [mayor]
				else if (split[1].equalsIgnoreCase("new")) {
					if (!TownyProperties.townCreationAdminOnly || player.canUseCommand("/townyadmin")) {
						if (world.towns.containsKey(split[2])) { player.sendMessage(Colors.Rose + "That town name already exists!"); return true; }
						Resident mayor = world.residents.get(split[3]);
						if (mayor == null) { player.sendMessage(Colors.Rose + "Selected mayor is not registered."); return true; }
						if (mayor.town != null) { player.sendMessage(Colors.Rose + "Selected mayor is currently in a town."); return true; }
						long[] curTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
						if (!world.newTownBlock(curTownBlock[0], curTownBlock[1])) { player.sendMessage(Colors.Rose + "A town has already claimed this area."); return true; }
						
						world.newTown(split[2]);
						Town town = world.towns.get(split[2]);
						town.addResident(mayor);
						town.setMayor(mayor);
						TownBlock townblock = world.townblocks.get(Long.toString(curTownBlock[0])+","+Long.toString(curTownBlock[1]));
						townblock.town = town;
						townblock.resident = mayor;
						town.homeBlock = townblock;
						updateAllPlayerZones();
						globalMessage(Colors.Green + split[3] +" just started a new town called " + split[2] + "!");
						log.info("[Towny] " + split[3] +" just started a new town called " + split[2] + "!");
						return true;
					}
				}
				// /town givebonus [town] [bonus]
				else if (split[1].equalsIgnoreCase("givebonus")) {
					if (!player.canUseCommand("/townyadmin")) { player.sendMessage(Colors.Rose + "This command is admin only."); return true; }
					Town town = world.towns.get(split[2]);
					if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
					
					try { town.bonusBlocks += Integer.parseInt(split[3]); }
					catch (Exception e) { player.sendMessage(Colors.Rose + "Bonus must be an integer."); return true; }
					
					sendTownMessage(town, Colors.Green + player.getName() +" just gave your town " + split[3] + " bonus town blocks!");
					player.sendMessage(Colors.Green + "You just gave "+split[2]+" " + split[3] + " bonus town blocks!");
					log.info("[Towny] " + player.getName() +" just gave " + split[2] + " " + split[3] + " bonus blocks!");
					return true;
				}
				// /town assistant [+/-] [player]
				else if (split[1].equalsIgnoreCase("assistant")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					if (mayor.town.mayor != mayor) { player.sendMessage(Colors.Rose + "Assistants don't have access to this command."); return true; }
					Resident resident = world.residents.get(split[3]);
					if (resident == null) { player.sendMessage(Colors.Rose + "That player is not registered."); return true; }
					if (resident.town == null) { player.sendMessage(Colors.Rose + "That player doesn't belong to any town."); return true; }
					if (resident == mayor) { player.sendMessage(Colors.Rose + "You cannot target yourself."); return true; }
					if (resident.town != mayor.town) { player.sendMessage(Colors.Rose + "Target player doesn't belong to this town."); return true; }
					
					// /town assistant + [player]
					if (split[2].equals("+")) {
						if (resident.town.assistants.contains(resident)) { player.sendMessage(Colors.Rose + "That player is already an assistant."); return true; }
						resident.town.assistants.add(resident);
						resident.isMayor = true;
						sendTownMessage(mayor.town, Colors.Gold + split[3] + " is now the mayor's assistant!");
						return true;
					}
					// /town assistant - [player]
					else if (split[2].equals("-")) {
						if (!resident.town.assistants.contains(resident)) { player.sendMessage(Colors.Rose + "That player is not an assistant."); return true; }
						for (int i = 0; i < resident.town.assistants.size(); i++) {
							if (resident.town.assistants.get(i) == resident) {
								resident.town.assistants.remove(i);
								break;
							}
						}
						resident.isMayor = false;
						sendTownMessage(mayor.town, Colors.Gold + split[3] + " was removed from the mayor's assistants!");
						return true;
					}
				}
				// /town wall [type] [height]
				else if (split[1].equalsIgnoreCase("wall")) {
					if (TownyProperties.wallGenOn) {					
						Resident mayor = world.residents.get(player.getName());
						if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
						if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
						if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
						
						int newHeight = 2;
						try {
							newHeight = Integer.parseInt(split[3]);
						} catch (Exception e) {
							player.sendMessage(Colors.Rose + "Invalid height");
							return true;
						}
						if (newHeight < 2) {
							player.sendMessage(Colors.Rose + "Invalid height. Walls ought to be bigger.");
							return true;
						}
						
						WallGen.deleteTownWall(world, mayor.town);
						if (split[2].equalsIgnoreCase("wood")) {
							mayor.town.wall.blockType = 17;
						} else if (split[2].equalsIgnoreCase("cobble")) {
							mayor.town.wall.blockType = 4;
						} else if (split[2].equalsIgnoreCase("obsidian")) {
							mayor.town.wall.blockType = 49;
						} else if (split[2].equalsIgnoreCase("smooth")) {
							mayor.town.wall.blockType = 1;
						}
						mayor.town.wall.height = newHeight;
						WallGen.townGen(world, mayor.town);
						player.sendMessage("WallGen - Completed");
						return true;
					} else {
						player.sendMessage("WallGen is not on. Ask an admin to turn it on.");
					}
				}
			}
			
			player.sendMessage(ChatTools.formatTitle("/town"));
			player.sendMessage("  §3/town §b: Your town's status");
			player.sendMessage("  §3/town §blist");
			player.sendMessage("  §3/town §bleave");
			player.sendMessage("  §cMayor: §3/town §badd [resident]");
			player.sendMessage("  §cMayor: §3/town §bkick [resident]");
			player.sendMessage("  §cMayor: §3/town §bsetboard [message]");
			player.sendMessage("  §cMayor: §3/town §bprotect [on/off/buildonly]");
			player.sendMessage("  §cMayor: §3/town §bwall [type] [height]");
			player.sendMessage("  §cMayor: §3/town §bassistant [+/-] [player]");
			player.sendMessage("  §cMayor: §3/town §bwall remove");
			player.sendMessage("  §cAdmin: §3/town §bsetlord [town] [lord]");
			player.sendMessage("  §cAdmin: §3/town §bnew [town] [mayor]");
			player.sendMessage("  §cAdmin: §3/town §bgivebonus [town] [bonus]");
			player.sendMessage("  §cAdmin: §3/town §bdelete [town]");
        }
        else if (split[0].equalsIgnoreCase("/nation")) {
            /*
				/nation
				/nation list
				/nation leave *Mayor
				/nation new [nation] [capital] *Admin
				/nation delete [nation] *Admin
				/nation add [town] *King
				/nation kick [town] *King
			*/
            if (split.length == 1) {
				// /nation
				Resident resident = world.residents.get(player.getName());
				if (resident == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
				if (resident.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
				if (resident.town.nation == null) { player.sendMessage(Colors.Rose + "Your town doesn't belong to a nation."); return true; }
				
				for (String line : resident.town.nation.getStatus())
					player.sendMessage(line);
				return true;	
			} else if (split.length == 2) {
				// /nation ?
				if (split[1].equalsIgnoreCase("?")) {
					
				}
				// /nation list
				else if (split[1].equalsIgnoreCase("list")) {
					player.sendMessage(ChatTools.formatTitle("Nations"));
					for (String line : ChatTools.list(world.nations.keySet().toArray()))
						player.sendMessage(line);
					return true;
				}
				// /nation leave
				else if (split[1].equalsIgnoreCase("leave")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (mayor.town.nation == null) { player.sendMessage(Colors.Rose + "Your town doesn't belong to a nation."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					
					Town town = mayor.town;
					Nation nation = mayor.town.nation;
					sendNationMessage(nation, Colors.Gold + town + " left the nation.");
					nation.remTown(town);
					updateAllPlayerZones();
					if (nation.size() == 0)
						delNation(player, nation.name);
					return true;
				}
				// /nation [nation]
				else {
					Nation nation = world.nations.get(split[1]);
					if (nation == null) { player.sendMessage(Colors.Rose + "That nation doesn't exist."); return true; }
					
					for (String line : nation.getStatus())
						player.sendMessage(line);
					return true;
				}
			} else if (split.length >= 3 && (split[1].equalsIgnoreCase("add"))) {
				// /nation add [town] .. [town]
				log.info("[Towny] reached");
				if (split[1].equalsIgnoreCase("add")) {
					Resident king = world.residents.get(player.getName());
					if (king == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (king.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (king.town.nation == null) { player.sendMessage(Colors.Rose + "Your town doesn't belong to a nation."); return true; }
					if (!king.isKing) { player.sendMessage(Colors.Rose + "You're not the king."); return true; }
					
					
					for (int i = 2; i < split.length; i++) {
						log.info("[Towny] " + i);
						Town town = world.towns.get(split[i]);
						if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
						
						if (king.town.nation.addTown(town)) {
							sendNationMessage(king.town.nation, Colors.Green + town + " joined the nation!");
							updateAllPlayerZones();
						} else {
							player.sendMessage(Colors.Rose + split[i] + " is already a part of this nation.");
						}
					}
					return true;
				}
			} else if (split.length == 3) {
				// /nation delete [nation]
				if (split[1].equalsIgnoreCase("delete")) {
					if (!player.canUseCommand("/townyadmin")) { player.sendMessage(Colors.Rose + "This command is admin only."); return true; }
					delNation(player, split[2]);
					updateAllPlayerZones();
					return true;
				}				
				// /nation kick [town]
				else if (split[1].equalsIgnoreCase("kick")) {
					Resident king = world.residents.get(player.getName());
					if (king == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (king.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (king.town.nation == null) { player.sendMessage(Colors.Rose + "Your town doesn't belong to a nation."); return true; }
					if (!king.isKing) { player.sendMessage(Colors.Rose + "You're not the king."); return true; }
					Town town = world.towns.get(split[2]);
					if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
					if (town.nation == null) { player.sendMessage(Colors.Rose + "That town doesn't belong to any nation."); return true; }
					if (town == king.town) { player.sendMessage(Colors.Rose + "You cannot kick your own town."); return true; }
					
					if (king.town.nation.remTown(town)) {
						world.database.saveTown(town);
						world.database.saveNation(king.town.nation);
						sendNationMessage(king.town.nation, Colors.Green + town + " was kicked from town!");
						updateAllPlayerZones();
					} else {
						player.sendMessage(Colors.Rose + "That town doesn't belong to your nation.");
					}
					return true;
				}
				// /nation setcapital [town]
				else if (split[1].equalsIgnoreCase("setcapital")) {
					Resident king = world.residents.get(player.getName());
					if (king == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (king.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (king.town.nation == null) { player.sendMessage(Colors.Rose + "Your town doesn't belong to a nation."); return true; }
					if (!king.isKing) { player.sendMessage(Colors.Rose + "You're not the king."); return true; }
					Town town = world.towns.get(split[2]);
					if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
					if (town.nation == null) { player.sendMessage(Colors.Rose + "That town doesn't belong to any nation."); return true; }
					if (town == king.town) { player.sendMessage(Colors.Rose + "That's already the capital."); return true; }
					
					for (Resident assistant : king.town.nation.assistants) {
						assistant.isKing = false;
						world.database.saveResident(assistant);
					}
					king.town.nation.assistants.clear();
					
					king.isKing = false;
					world.database.saveResident(king);
					king.town.isCapital = false;
					world.database.saveTown(king.town);
					king.town.nation.capital = null;
					
					town.mayor.isKing = true;
					world.database.saveResident(town.mayor);
					town.isCapital = true;
					world.database.saveTown(town);
					town.nation.capital = town;
					world.database.saveNation(king.town.nation);
					sendNationMessage(king.town.nation, Colors.Green + town + " is now the capital of " + town.nation + "!");
					
					return true;
				}
			} else if (split.length == 4) {
				// /nation new [nation] [capital]
				if (!TownyProperties.townCreationAdminOnly || player.canUseCommand("/townyadmin")) {
					if (world.nations.containsKey(split[2])) { player.sendMessage(Colors.Rose + "That nation name already exists!"); return true; }
					Town capital = world.towns.get(split[3]);
					if (capital == null) { player.sendMessage(Colors.Rose + "Selected capital is not registered."); return true; }
					if (capital.mayor == null) { player.sendMessage(Colors.Rose + "Selected capital has no mayor."); return true; }
					if (capital.nation != null) { player.sendMessage(Colors.Rose + "Selected capital is currently in another nation."); return true; }
					
					world.newNation(split[2]);
					Nation nation = world.nations.get(split[2]);
					Town town = world.towns.get(split[3]);
					nation.addTown(town);
					nation.dertermineCapital();
					world.database.saveResident(town.mayor);
					world.database.saveTown(town);
					world.database.saveNation(nation);
					globalMessage(Colors.Green + split[3] +" just started a new nation called " + split[2] + "!");
					log.info("[Towny] " + split[3] +" just started a new nation called " + split[2] + "!");
					return true;
				}
				// /nation assistant [+/-] [player]
				else if (split[1].equalsIgnoreCase("assistant")) {
					Resident king = world.residents.get(player.getName());
					if (king == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (king.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (king.town.nation == null) { player.sendMessage(Colors.Rose + "Your town doesn't belong to a nation."); return true; }
					if (!king.isKing) { player.sendMessage(Colors.Rose + "You're not the king."); return true; }
					if (king.town.nation.capital != king.town) { player.sendMessage(Colors.Rose + "Assistants don't have access to this command."); return true; }
					Resident resident = world.residents.get(split[3]);
					if (resident == null) { player.sendMessage(Colors.Rose + "That player is not registered."); return true; }
					if (resident.town == null) { player.sendMessage(Colors.Rose + "That player doesn't belong to any town."); return true; }
					if (resident.town.nation == null) { player.sendMessage(Colors.Rose + "That player's town doesn't belong to any nation."); return true; }
					if (resident == king) { player.sendMessage(Colors.Rose + "You cannot target yourself."); return true; }
					if (resident.town.nation != king.town.nation) { player.sendMessage(Colors.Rose + "Target player doesn't belong to this nation."); return true; }
					
					// /town assistant + [player]
					if (split[2].equals("+")) {
						if (resident.town.nation.assistants.contains(resident)) { player.sendMessage(Colors.Rose + "That player is already an assistant."); return true; }
						resident.town.nation.assistants.add(resident);
						resident.isKing = true;
						world.database.saveResident(resident);
						world.database.saveNation(king.town.nation);
						sendNationMessage(king.town.nation, Colors.Gold + split[3] + " is now the king's assistant!");
						return true;
					}
					// /town assistant - [player]
					else if (split[2].equals("-")) {
						if (!resident.town.nation.assistants.contains(resident)) { player.sendMessage(Colors.Rose + "That player is not an assistant."); return true; }
						for (int i = 0; i < resident.town.nation.assistants.size(); i++) {
							if (resident.town.nation.assistants.get(i) == resident) {
								resident.town.nation.assistants.remove(i);
								break;
							}
						}
						resident.isKing = false;
						world.database.saveResident(resident);
						world.database.saveNation(king.town.nation);
						sendNationMessage(king.town.nation, Colors.Gold + split[3] + " was removed from the king's assistants!");
						return true;
					}
				}
				// /nation setcapital [nation] [town]
				else if (split[1].equalsIgnoreCase("setcapital")) {
					if (player.canUseCommand("/townyadmin")) {
						Nation nation = world.nations.get(split[2]);
						if (nation == null) { player.sendMessage(Colors.Rose + "Nation not registered."); return true; }
						Town town = world.towns.get(split[3]);
						if (town == null) { player.sendMessage(Colors.Rose + "That town doesn't exist."); return true; }
						if (town.nation == null) { player.sendMessage(Colors.Rose + "That town doesn't belong to any nation."); return true; }
						if (town.nation == nation) { player.sendMessage(Colors.Rose + "That's already the capital."); return true; }
						
						for (Resident assistant : nation.assistants) {
							assistant.isKing = false;
							world.database.saveResident(assistant);
						}
						nation.assistants.clear();
						if (nation.capital != null) {
							if (nation.capital.mayor != null) {
								nation.capital.mayor.isKing = false;
								world.database.saveResident(nation.capital.mayor);
							}
							nation.capital.isCapital = false;
							world.database.saveTown(nation.capital);
							nation.capital = null;
						}
						
						town.mayor.isKing = true;
						world.database.saveResident(town.mayor);
						town.isCapital = true;
						world.database.saveTown(town);
						town.nation.capital = town;
						world.database.saveNation(town.nation);
						sendNationMessage(nation, Colors.Green + town + " is now the capital of " + town.nation + "!");
						
						return true;
					}
				}
			}
			
			player.sendMessage(ChatTools.formatTitle("/nation"));
			player.sendMessage("  §3/nation §b: Your nation's status");
			player.sendMessage("  §3/nation §blist");
			player.sendMessage("  §cMayor: §3/nation §bleave");
			player.sendMessage("  §cKing: §3/nation §badd [town]");
			player.sendMessage("  §cKing: §3/nation §bkick [town]");
			player.sendMessage("  §cKing: §3/nation §bassistant [+/-] [player]");
			player.sendMessage("  §cAdmin: §3/nation §bdelete [nation]");
			player.sendMessage("  §cAdmin: §3/nation §bnew [nation] [capital]");
        }
		else if (split[0].equalsIgnoreCase("/townyadmin")) {
			/*
				/townyadmin
				/townyadmin
				/townyadmin toggle [option]

			*/
			if (split.length != 1) {
				if (split.length == 2) {
					// /townyadmin ?
					if (split[1].equalsIgnoreCase("?")) {
						player.sendMessage("[Towny] CommandQueue size: " + commandQueue.queue.size());
						player.sendMessage("[Towny] Wall Generator " + ((TownyProperties.wallGenOn) ? "§aON" : "§cOFF"));
						player.sendMessage("[Towny] Town/Nation Creating Admin Only " + ((TownyProperties.townCreationAdminOnly) ? "§aON" : "§cOFF"));
						player.sendMessage("[Towny] Can Build In Unclaimed Zone " + ((TownyProperties.unclaimedZoneBuildRights) ? "§aON" : "§cOFF"));
						return true;
					}
				} else if (split.length == 3) {
					// /townyadmin toggle [option]
					if (split[1].equalsIgnoreCase("toggle")) {
						if (split[2].equalsIgnoreCase("wallgen")) {
							TownyProperties.wallGenOn = !TownyProperties.wallGenOn;
							player.sendMessage("[Towny] Wall Generator " + ((TownyProperties.wallGenOn) ? "§aON" : "§cOFF"));
							return true;
						} else if (split[2].equalsIgnoreCase("admintowncreate")) {
							TownyProperties.townCreationAdminOnly = !TownyProperties.townCreationAdminOnly;
							player.sendMessage("[Towny] Town/Nation Creating Admin Only " + ((TownyProperties.townCreationAdminOnly) ? "§aON" : "§cOFF"));
							return true;
						} else if (split[2].equalsIgnoreCase("buildonunclaimed")) {
							TownyProperties.unclaimedZoneBuildRights = !TownyProperties.unclaimedZoneBuildRights;
							player.sendMessage("[Towny] Can Build In Unclaimed Zone " + ((TownyProperties.unclaimedZoneBuildRights) ? "§aON" : "§cOFF"));
							return true;
						}
						
						player.sendMessage("Toggle options:");
						player.sendMessage("wallgen, admintowncreate, buildonunclaimed");
						return true;
					}
				}
			}
			
			
			player.sendMessage(ChatTools.formatTitle("/towny"));
			player.sendMessage("/townyadmin ? : Shows admin stats");
			player.sendMessage("/townyadmin toggle [option]");
			player.sendMessage("    Options: wallgen, admintowncreate, buildonunclaimed");
			return true;
		}
        else if (split[0].equalsIgnoreCase("/towny")) {
			/*
				/towny
				/towny map
				/towny map toggle
				/towny war [nation] [nation]
				/towny capturetheflag [nation] [nation]
			*/
			if (split.length != 1) {
				if (split.length == 2) {
					// /towny ?
					if (split[1].equalsIgnoreCase("?")) {
						player.sendMessage("§0-§4###§0---§4###§0-");
						player.sendMessage("§4#§c###§4#§0-§4#§c###§4#§0   §6[§eTowny Beta 1.9§6]");
						player.sendMessage("§4#§c####§4#§c####§4#   §3By: §bChris H (Shade)");
						player.sendMessage("§0-§4#§c#######§4#§0-");
						player.sendMessage("§0--§4##§c###§4##§0-- §3Residents: §b" + Integer.toString(world.residents.size()));
						player.sendMessage("§0----§4#§c#§4#§0---- §3Towns: §b" + Integer.toString(world.towns.size()));
						player.sendMessage("§0-----§4#§0----- §3Nations: §b" + Integer.toString(world.nations.size()));
						return true;
					}
					// /towny map
					else if (split[1].equalsIgnoreCase("map")) {
						sendMap(player);
						return true;
					}
				} else if (split.length == 3) {
					// /towny map toggle
					if (split[1].equalsIgnoreCase("map") && split[2].equalsIgnoreCase("toggle")) {
						
						// End claim mode for player
						if (playerMap.contains(player.getName())) {
							for (int i = 0; i < playerMap.size(); i++) {
								if (playerMap.get(i) == player.getName()) {
									playerMap.remove(i);
									break;
								}
							}
							player.sendMessage(Colors.Rose + "Auto TownyMap is turned off.");
						}
						// Start claim mode for player
						else {
							playerMap.add(player.getName());
							player.sendMessage(Colors.Green + "TownyMap will auto update.");
						}
						return true;
					}
				} else if (split.length == 4) {
					// /towny war [nation] [nation]
					if (split[1].equalsIgnoreCase("war")) {
						Nation nationA = world.nations.get(split[2]);
						if (nationA == null) { player.sendMessage(Colors.Rose + split[2] + " doesn't exist."); return true; }
						Nation nationB = world.nations.get(split[3]);
						if (nationB == null) { player.sendMessage(Colors.Rose + split[3] + " doesn't exist."); return true; }
						if (nationA == nationB) { player.sendMessage(Colors.Rose + "A nation can't go to war against itself."); return true; }
						
						nationA.setAliegeance("enemy", nationB);
						nationB.setAliegeance("enemy", nationA);
						updateAllPlayerZones();
						world.database.saveNation(nationB);
						world.database.saveNation(nationA);
						globalMessage(Colors.Red + "WAR! " + Colors.Gold + nationA+" vs. "+nationB+"!");
						return true;
					}
					// /towny capturetheflag [nation] [nation]
					else if (split[1].equalsIgnoreCase("capturetheflag")) {
						
					}
				}
			}
			
			player.sendMessage(ChatTools.formatTitle("/towny"));
			player.sendMessage("/towny ? : Shows some stats");
			player.sendMessage("/towny map");
			player.sendMessage("/towny map toggle");
			player.sendMessage("Admin: /towny war [nation] [nation]");
        }
        else if (split[0].equalsIgnoreCase("/claim")) {
			/*
				/claim
				/claim remove
				/claim toggle
				/claim rect [town resident]
			*/
			if (split.length == 1) {
				// /claim
				claimSingleTownBlock(player);
				return true;
			} else if (split.length == 2) {
				// /claim remove
				if (split[1].equalsIgnoreCase("remove")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					long[] curTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
					String key = Long.toString(curTownBlock[0])+","+Long.toString(curTownBlock[1]);
					TownBlock townblock = world.townblocks.get(key);
					if (townblock == null || townblock.town == null) { player.sendMessage(Colors.Rose + "This block hasn't been claimed yet."); return true; }
					if (townblock.town != mayor.town) { player.sendMessage(Colors.Rose + "This block doesn't belong to your town."); return true; }
					
					world.townblocks.remove(key);
					updateAllPlayerZones();
					world.database.saveTown(mayor.town);
					world.database.saveTownBlocks();
					player.sendMessage(Colors.Green + "You abandon this sector.");
					log.info("[Towny] " + mayor.town.name + " left block [" +key+"]");
					return true;
				}
				// /claim toggle
				else if (split[1].equalsIgnoreCase("toggle")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					// End claim mode for player
					if (playerClaim.contains(player.getName())) {
						for (int i = 0; i < playerClaim.size(); i++) {
							if (playerClaim.get(i) == player.getName()) {
								playerClaim.remove(i);
								break;
							}
						}
						player.sendMessage(Colors.Rose + "Simple town block claiming turned off.");
					}
					// Start claim mode for player
					else {
						playerClaim.add(player.getName());
						player.sendMessage(Colors.Green + "Simple town block claiming turned on.");
					}
					
					return true;
				}
				/*
				//
				else if (split[1].equalsIgnorecase("auto")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					
					ArrayList<long[]> newTownBlocks = new ArrayList<long[]>();
					while (world.countTownBlocks(mayor.town) < mayor.town.getMaxTownBlocks()) {
						newTownBlocks.clear();
						for (Townblock tb : world.getTownBlocks(mayor.town)) {
							if (tb.isEdgeBlock()) {
								int[][] offset = {{-1,0},{1,0},{0,-1},{0,1}};
								for (int i = 0; i < 4; i++) {
									String edgeKey = Long.toString(tb.x+offset[i][0])+","+Long.toString(tb.z+offset[i][1]);
									if (world.townblocks.get(edgeKey) == null) {
										long[] temp = {tb.x+offset[i][0], tb.z+offset[i][1]};
										newTownBlocks.add(temp);
									}
								}
							}
						}
						for (long[] tb : newTownBlocks) {
							claimTownBlock(mayor, tb);
						}
					}
					
					return true;
				}
				*/
			} else if (split.length == 3) {
				// /claim rect [town resident]
				if (split[1].equalsIgnoreCase("rect")) {
					Resident mayor = world.residents.get(player.getName());
					if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return true; }
					if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return true; }
					if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return true; }
					
					Player target = etc.getServer().matchPlayer(split[2]);
					if (target == null) { player.sendMessage(Colors.Rose + "Player isn't online / Misstype."); return true; }
					Resident resident = world.residents.get(target.getName());
					if (resident == null) { player.sendMessage(Colors.Rose + "That player is not registered."); return true; }
					if (resident.town == null) { player.sendMessage(Colors.Rose + "That player does not belong to a town."); return true; }
					if (mayor.town != resident.town) { player.sendMessage(Colors.Rose + "That player does not belong to your town."); return true; }
					
					long[] targetTownBlock = TownyUtil.getTownBlock((long)target.getX(), (long)target.getZ());
					long[] playerTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
					
					
					
					long rectArea = Math.abs(targetTownBlock[0]-playerTownBlock[0])*Math.abs(targetTownBlock[1]-playerTownBlock[1]);
					int townBlockLimit = mayor.town.getMaxTownBlocks();
					int usedTownBlocks = world.countTownBlocks(mayor.town);
					if (usedTownBlocks+rectArea >= townBlockLimit) {
						player.sendMessage(Colors.Rose + "Selected area("+rectArea+") is larger than your remaining townblocks ("+Integer.toString(townBlockLimit-usedTownBlocks)+").");
						return true;
					}
					
					boolean edgeTest = false;
					for (long z = Math.min(playerTownBlock[1], targetTownBlock[1]); z <= Math.max(playerTownBlock[1], targetTownBlock[1]); z++) {
						for (long x = Math.min(playerTownBlock[0], targetTownBlock[0]); x <= Math.max(playerTownBlock[0], targetTownBlock[0]); x++) {
							long[] curTownBlock = {x, z};
							if (isTownEdge(mayor.town, curTownBlock)) {
								edgeTest = true;
								break;
							}
						}
					}
					
					if (!edgeTest) {
						player.sendMessage(Colors.Rose + "This selected area is not connected to an edge of your town.");
						return true;
					}
					
					for (long z = Math.min(playerTownBlock[1], targetTownBlock[1]); z <= Math.max(playerTownBlock[1], targetTownBlock[1]); z++) {
						for (long x = Math.min(playerTownBlock[0], targetTownBlock[0]); x <= Math.max(playerTownBlock[0], targetTownBlock[0]); x++) {
							long[] curTownBlock = {x, z};
							claimTownBlock(mayor, curTownBlock);
						}
					}

					updateAllPlayerZones();
					world.database.saveTown(mayor.town);
					world.database.saveTownBlocks();
					player.sendMessage(Colors.Green + "You annex your new territory.");
					return true;
				}
			}			
            
			player.sendMessage(ChatTools.formatTitle("/claim"));
			player.sendMessage("Mayor: /claim");
			player.sendMessage("Mayor: /claim remove");
			player.sendMessage("Mayor: /claim toggle");
			player.sendMessage("Mayor: /claim rect [town resident]");
        }
        else if (split[0].equalsIgnoreCase("/ally")) {
            if (split.length != 3 && (split[1].equals("+") || split[1].equals("-") || split[1].equalsIgnoreCase("n"))) {
                player.sendMessage(Colors.Rose + "Correct usage is: /ally [+/n/-][nation]");
                return true;
            }
            
			Nation targetNation = world.nations.get(split[2]);
			if (targetNation == null) {
				player.sendMessage(Colors.Rose + "Target nation doesn't exist.");
				return true;
			}
			
            if (world.residents.containsKey(player.getName())) {
                Resident king = world.residents.get(player.getName());
                if (king.town != null) {
                    if (king.town.isCapital == true && king.town.nation != null && king.town.nation.capital == king.town) {
						if (king.town.nation != targetNation) {
							if (king.isMayor) {
								if (split[1].equals("+")) {
									targetNation.setAliegeance("friend", king.town.nation);
									king.town.nation.setAliegeance("friend", targetNation);
									updateAllPlayerZones();
									world.database.saveNation(king.town.nation);
									world.database.saveNation(targetNation);
									player.sendMessage(Colors.Green + "Your nations are now at peace, and become friendly.");
								} else if (split[1].equals("-")) {
									warThread.newPendingWar(king.town.nation, targetNation, TownyProperties.timeTillWar);
									player.sendMessage(Colors.Green + "Your nations have developed a hatred for each other.");
								} else if (split[1].equalsIgnoreCase("n")) {
									targetNation.setAliegeance("neutral", king.town.nation);
									king.town.nation.setAliegeance("neutral", targetNation);
									updateAllPlayerZones();
									world.database.saveNation(king.town.nation);
									world.database.saveNation(targetNation);
									player.sendMessage(Colors.Green + "Your nations have ignore each other.");
								} 
							} else {
								player.sendMessage(Colors.Rose + "You are not the king.");
							}
						} else {
							player.sendMessage(Colors.Rose + "You cannot target your own nation.");
						}
                    } else {
                        player.sendMessage(Colors.Rose + "The town your in isn't the capital.");
                    }
                } else {
                    player.sendMessage(Colors.Rose + "You don't belong to any town.");
                }
            } else {
                player.sendMessage(Colors.Rose + "You are not registered.");
			}
        }
        
        
        //Test Commands
        else if (split[0].equalsIgnoreCase("/wait")) {
            log.info("Thread test [20s] - Try doing stuff.");
            try {
                this.sleep(20000);
            } catch (InterruptedException e) {}
            log.info("Thread test finished.");
			return true;			
        }
		//
		else if (split[0].equalsIgnoreCase("/loc")) {
            long[] cTB =  TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
			player.sendMessage("TB: " + cTB[0] + ", "+cTB[1]);
			player.sendMessage("XYZ: "+player.getX()+", "+player.getY()+", "+player.getZ());
			return true;
        }
        else
            return false;
            
        return false;
    }
    
    public boolean load() {
        if (properties == null) { properties = new PropertiesFile("towny.properties"); }
        else { properties.load(); }
        
        TownyProperties.activePeriod = 1000*60*60*24*properties.getInt("activeperiod", 7);
        TownyProperties.timeTillWar = 1000*60*properties.getInt("timetillwar", 1440);
        TownyProperties.blockSize = properties.getInt("blocksize", 16);
		if (TownyProperties.blockSize <= 0)
			TownyProperties.blockSize = 16;
        TownyProperties.source = properties.getString("source", "flatfile");
		TownyProperties.claimRatio = properties.getInt("claimratio", 0);
		if (TownyProperties.claimRatio < 0)
			TownyProperties.claimRatio = 0;
		TownyProperties.unclaimedZoneBuildRights = properties.getBoolean("unclaimedzone-buildrights", true);
		TownyProperties.townCreationAdminOnly = properties.getBoolean("towncreation-adminonly", true);
		TownyProperties.wallGenOn = properties.getBoolean("wallgenon", false);
        if (TownyProperties.source.equalsIgnoreCase("flatfile")) {
            TownyProperties.flatFileFolder = properties.getString("flatfilefolder", "towny");
            
			//Create files and folders if non-existant
            try {
                String[] foldersToCreate = {"","/settings","/data","/data/residents","/data/towns","/data/nations"};
                String[] filesToCreate = {"/data/residents.txt","/data/towns.txt","/data/nations.txt","/data/townblocks.csv","/settings/townLevels.txt","/settings/nationLevels.txt","/settings/wallConfig.txt"};
                for (String folder : foldersToCreate) {
                    File f = new File(TownyProperties.flatFileFolder + folder);
                    if (!(f.exists() && f.isDirectory()))
                        f.mkdir();
                }
                for (String file : filesToCreate) {
                    File f = new File(TownyProperties.flatFileFolder + file);
                    if (!(f.exists() && f.isFile()))
                        f.createNewFile();
                }
            } catch (IOException e) {
                log.info("[Towny] Error creating flatfile default files and folders.");
				return false;
            }
			
			// Init Database
			world.database = new TownyFlatFileSource();
			world.database.initialize(world);
        } else {
			log.info("[Towny] Database input not recognized.");
            return false;
        }
        
        TownyProperties.load();

        return true;
    }
    
    public boolean loadData() {
        return world.database.loadAll();
    }

    public void delNation(Player player, String nationName) {
        if (world.delNation(nationName)) {
            globalMessage(Colors.Rose + "The nation of "+nationName+" just crumbled out of existence.");
        } else {
            player.sendMessage(Colors.Rose + "That nation already doesn't exist.");
        }
    }
    
    public void delTown(Player player, String name) {
        if (world.delTown(name)) {
            globalMessage(Colors.Rose + "The town of "+name+" just crumbled out of existence.");
        } else {
            player.sendMessage(Colors.Rose + "That town already doesn't exist.");
        }
    }
    
    public void delResident(Player player, String residentName) {
        //Kick player if he's online.
        Player target = etc.getServer().matchPlayer(residentName);
        if (target != null)
            target.kick("Reseting your Towny Data.");
            
        if (world.delResident(residentName)) {
            player.sendMessage(Colors.Green + "Player data reset.");
        } else {
            player.sendMessage(Colors.Rose + "Player "+residentName+" doesn't exist.");
        }
    }
    
    public void sendTownMessage(Town town, String msg) {
        for (Player p : etc.getServer().getPlayerList()) {
            if (town.residents.contains(world.residents.get(p.getName())))
                p.sendMessage(msg);
        }
    }
    
    public void sendNationMessage(Nation nation, String msg) {
        for (Player p : etc.getServer().getPlayerList()) {
            Resident resident = world.residents.get(p.getName());
            if (resident != null && resident.town != null && resident.town.nation == nation)
                p.sendMessage(msg);
        }
    }
    
    public void globalMessage(String msg) {
        for (Player p : etc.getServer().getPlayerList()) {
            p.sendMessage(msg);
        }
    }
	
	public void sendMap(Player player) {
		long[] curTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
            
		boolean hasTown = false;
		Resident resident = world.residents.get(player.getName());
		if (resident != null && resident.town != null)
			hasTown = true;
			
		player.sendMessage(ChatTools.formatTitle("Towny Map"));
		int lineCount = 0;
		
		String[][] townyMap = new String[31][7];
		int x, y = 0;
		for (int tby = (int)curTownBlock[1]-15; tby <= curTownBlock[1]+15; tby++) {
			x = 0;
			for (int tbx = (int)curTownBlock[0]-3; tbx <= curTownBlock[0]+3; tbx++) {
				String key = Long.toString(tbx)+","+Long.toString(tby);
				TownBlock townblock = world.townblocks.get(key);
				if (townblock != null && townblock.town != null) {
					if (x == 3 && y == 15) {
						townyMap[y][x] = Colors.Gold;
					} else if (hasTown) {
						if (resident.town == townblock.town) {
							townyMap[y][x] = Colors.LightGreen;
						} else {
							if (resident.town.nation != null) {
								if (resident.town.nation.towns.contains(townblock.town)) {
									townyMap[y][x] = Colors.Green;
								} else {
									if (townblock.town.nation != null) {
										if (resident.town.nation.friends.contains(townblock.town.nation)) {
											townyMap[y][x] = Colors.Green;
										} else if (resident.town.nation.enemies.contains(townblock.town.nation)) {
											townyMap[y][x] = Colors.Red;
										} else {
											townyMap[y][x] = Colors.White;
										}
									} else {
										townyMap[y][x] = Colors.White;
									}
								}
							} else {
								townyMap[y][x] = Colors.White;
							}
						}
					} else {
						townyMap[y][x] = Colors.White;
					}
					townyMap[y][x] += "+";
				} else {
					if (x == 3 && y == 15) {
						townyMap[y][x] = Colors.Gold;
					} else {
						townyMap[y][x] = Colors.Gray;
					}
					townyMap[y][x] += "-";
				}
				
				x++;
			}
			y++;
		}
		
		String[] compass = {
			Colors.Black + "  -----  ",
			Colors.Black + "  --" + Colors.White + "N" + Colors.Black + "--  ",
			Colors.Black + "  -" + Colors.White + "W+E" + Colors.Black + "-  ",
			Colors.Black + "  --" + Colors.White + "S" + Colors.Black + "--  "
		};
		
		String line;
		//Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < 7; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];
		
				
			for (int mx = 30; mx >= 0; mx--) {
				line += townyMap[mx][my];
			}
			player.sendMessage(line);
			lineCount++;
		}
	}
	
	public boolean isTownEdge(Town town, long[] curTownBlock) {
		boolean isEdgeBlock = false;
		int[][] offset = {{-1,0},{1,0},{0,-1},{0,1}};
		for (int i = 0; i < 4; i++) {
			String edgeKey = Long.toString(curTownBlock[0]+offset[i][0])+","+Long.toString(curTownBlock[1]+offset[i][1]);
			//log.info(edgeKey + "=" + isEdgeBlock);
			TownBlock edgeTownBlock = world.townblocks.get(edgeKey);
			if (edgeTownBlock == null)
				continue;
			else if (edgeTownBlock.town != town)
				continue;
			
			return true;
		}
		
		return false;
	}
	
	public boolean claimTownBlock(Resident mayor, long[] curTownBlock) {
		if (!world.newTownBlock(curTownBlock[0], curTownBlock[1]))
			return false;
			
		String key = Long.toString(curTownBlock[0])+","+Long.toString(curTownBlock[1]);
		TownBlock townblock = world.townblocks.get(key);
		townblock.town = mayor.town;
		townblock.resident = mayor;

		log.info("[Towny] " + mayor.name + " from " + mayor.town.name + " annexed block [" +key+"]");
		return true;
	}
	
	public void claimSingleTownBlock(Player player) {
		Resident mayor = world.residents.get(player.getName());
		if (mayor == null) { player.sendMessage(Colors.Rose + "You are not registered."); return; }
		if (mayor.town == null) { player.sendMessage(Colors.Rose + "You don't belong to any town."); return; }
		if (!mayor.isMayor) { player.sendMessage(Colors.Rose + "You're not the mayor."); return; }
		
		int townBlockLimit = mayor.town.getMaxTownBlocks();
		if (world.countTownBlocks(mayor.town)+1 > townBlockLimit) {
			player.sendMessage(Colors.Rose + "You've already used up all "+Integer.toString(townBlockLimit)+" town blocks.");
			return;
		}
		
		long[] curTownBlock = TownyUtil.getTownBlock((long)player.getX(), (long)player.getZ());
		
		if (!isTownEdge(mayor.town, curTownBlock)) {
			player.sendMessage(Colors.Rose + "This block is not connected to an edge of your town.");
			return;
		}
			
		if (claimTownBlock(mayor, curTownBlock)) {
			updateAllPlayerZones();
			world.database.saveTown(mayor.town);
			world.database.saveTownBlocks();
			player.sendMessage(Colors.Green + "You annex your new territory.");
			return;
		} else {
			player.sendMessage(Colors.Rose + "This block belongs to another town.");
			return;
		}
	}
}