import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

public class TownyFlatFileSource extends TownyDataSource {
	private final String newLine = System.getProperty("line.separator");
	
	public boolean loadResidentList() {
		String line;
		String[] tokens;
		try {
			BufferedReader fin = new BufferedReader(new FileReader(TownyProperties.flatFileFolder + "/data/residents.txt"));
			while ( (line = fin.readLine()) != null) {
				tokens = line.split(" ");
				if (tokens.length > 0) {
					world.newResident(tokens[0]);
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean loadTownList() {
		String line;
		String[] tokens;
		try {
			BufferedReader fin = new BufferedReader(new FileReader(TownyProperties.flatFileFolder + "/data/towns.txt"));
			while ( (line = fin.readLine()) != null) {
				if (!line.equals("")) {
					world.newTown(line);  
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean loadNationList() {
		String line;
		String[] tokens;
		try {
			BufferedReader fin = new BufferedReader(new FileReader(TownyProperties.flatFileFolder + "/data/nations.txt"));
			while ( (line = fin.readLine()) != null) {
				if (!line.equals("")) {
					world.newNation(line);  
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	//
	
	public boolean loadResidents() {
		for (Resident resident : world.residents.values()) {
			loadResident(resident);
		}
		return true;
	}
	
	public boolean loadTowns() {
		for (Town town : world.towns.values()) {
			loadTown(town);
		}
		return true;
	}
	
	public boolean loadNations() {
		for (Nation nation : world.nations.values()) {
			loadNation(nation);
		}
		return true;
	}
	
	//
	
	public boolean loadResident(Resident resident) {
		String path = TownyProperties.flatFileFolder+"/data/residents/"+resident.name+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				resident.lastLogin = Long.parseLong(kvFile.get("lastLogin"));
				resident.town = world.towns.get(kvFile.get("town"));
				resident.isMayor = (resident.town != null && (resident == resident.town.mayor || resident.town.assistants.contains(resident)));
				resident.isKing = (resident.town != null && resident.town.nation != null && resident.town.nation.capital != null && (resident == resident.town.nation.capital.mayor || resident.town.nation.assistants.contains(resident)));
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading resident file "+resident.name, e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	public boolean loadTown(Town town) {
		String line;
		String[] tokens;
		String path = TownyProperties.flatFileFolder+"/data/towns/"+town.name+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				
				line = kvFile.get("residents");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident resident = world.residents.get(token);
						if (resident != null) {
							town.residents.add(resident);
						}
					}
				}
				
				line = kvFile.get("mayor");
				town.mayor = world.residents.get(line);
				
				/*line = kvFile.get("nation");
				town.nation = world.nations.get(line);*/
				town.isCapital = (town.nation != null && town == town.nation.capital);
				
				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = TownyWorld.getInstance().residents.get(token);
						if (assistant != null) {
							town.assistants.add(assistant);
							assistant.isMayor = true;
						}
					}
				}
				
				town.townBoard = kvFile.get("townBoard");
				
				line = kvFile.get("protectionStatus");
				if (line != null) {
					try {
						town.protectionStatus = Integer.parseInt(line);
						if (town.protectionStatus < 3 || town.protectionStatus > 5)
							town.protectionStatus = 3;
					} catch (Exception e) {
						town.protectionStatus = 3;
					}
				}
				
				line = kvFile.get("bonusBlocks");
				if (line != null) {
					try {
						town.bonusBlocks = Integer.parseInt(line);
					} catch (Exception e) {
						town.bonusBlocks = 0;
					}
				}
				
				line = kvFile.get("wall");
				if (line != null) {
					try {
						tokens = line.split(":");
						if (tokens.length == 2) {
							town.wall.blockType = Integer.parseInt(tokens[0]);
							town.wall.height = Integer.parseInt(tokens[1]);
						}
					} catch (NumberFormatException nfe) {} catch (Exception e) {}
				}
				
				line = kvFile.get("wallBlocks");
				if (line != null) {
					try {
						for (String wb : line.split(",")) {
							tokens = wb.split(":");
							if (tokens.length == 5)
								town.wall.sections.add(new WallBlock(new Point(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2])), Integer.parseInt(tokens[3]), Integer.parseInt(tokens[4])));
						}
					} catch (NumberFormatException nfe) {} catch (Exception e) {}
				}
				
				line = kvFile.get("homeBlock");
				if (line != null) {
					try {
						tokens = line.split(":");
						if (tokens.length == 2) {
							town.homeBlock = world.townblocks.get(tokens[0]+","+tokens[1]);
						}
					} catch (NumberFormatException nfe) {} catch (Exception e) {}
				}
				
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading town file "+town.name, e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	public boolean loadNation(Nation nation) {
		String line = "";
		String[] tokens;
		String path = TownyProperties.flatFileFolder+"/data/nations/"+nation.name+".txt";
		File fileResident = new File(path);
		if ((fileResident.exists() && fileResident.isFile())) {
			try {
				KeyValueFile kvFile = new KeyValueFile(path);
				
				line = kvFile.get("towns");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Town town = world.towns.get(token);
						if (town != null)
							nation.addTown(town);
					}
				}
				
				line = kvFile.get("capital");
				nation.capital = world.towns.get(line);
				
				line = kvFile.get("assistants");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Resident assistant = world.residents.get(token);
						if (assistant != null) {
							nation.assistants.add(assistant);
						}
					}
				}
				
				line = kvFile.get("friends");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation friend = world.nations.get(token);
						if (friend != null) {
							nation.setAliegeance("friend", friend);
						}
					}
				}
				
				line = kvFile.get("enemies");
				if (line != null) {
					tokens = line.split(",");
					for (String token : tokens) {
						Nation enemy = world.nations.get(token);
						if (enemy != null) {
							nation.setAliegeance("enemy", enemy);
						}
					}
				}
				
			} catch (Exception e) {
				log.log(Level.SEVERE, "Exception while reading nation file "+nation.name, e);
				return false;
			}
			
			return true;
		} else {
			return false;
		} 
	}
	
	//
	
	public boolean loadTownBlocks() {
		String line;
		String[] tokens;
		
		try {
			BufferedReader fin = new BufferedReader(new FileReader(TownyProperties.flatFileFolder + "/data/townblocks.csv"));
			while ( (line = fin.readLine()) != null) {
				tokens = line.split(",");
				if (tokens.length == 4) {
					world.newTownBlock(tokens[0], tokens[1]);
					TownBlock townblock = world.townblocks.get(tokens[0] + "," + tokens[1]);
					Town town = world.towns.get(tokens[2]);
					Resident resident = world.residents.get(tokens[3]);
					if (townblock != null) {
						townblock.town = town;
						townblock.resident = resident;
					}
				}
			}
			fin.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	//
	
	public boolean saveResidents() {
		for (Resident resident : world.residents.values()) {
			saveResident(resident);
		}
		return true;
	}
	
	public boolean saveTowns() {
		for (Town town : world.towns.values()) {
			saveTown(town);
		}
		return true;
	}
	
	public boolean saveNations() {
		for (Nation nation : world.nations.values()) {
			saveNation(nation);
		}
		return true;
	}
	
	//
	
	public boolean saveResident(Resident resdient) {
		try {
			String path = TownyProperties.flatFileFolder+"/data/residents/"+resdient.name+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("lastLogin=" + Long.toString(resdient.lastLogin) + newLine);
			if (resdient.town != null)
				fout.write("town=" + resdient.town.name);
			fout.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean saveTown(Town town) {
		try {
			String path = TownyProperties.flatFileFolder+"/data/towns/"+town.name+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			// Residents
			fout.write("residents=");
			for (Resident resident : town.residents)
				fout.write(resident.name + ",");
			fout.write(newLine);
			// Mayor
			if (town.mayor != null)
				fout.write("mayor=" + town.mayor.name + newLine);
			// Nation
			if (town.nation != null)
				fout.write("nation=" + town.nation.name + newLine);
			// Assistants
			fout.write("assistants=");
			for(Resident assistant : town.assistants)
				fout.write(assistant.name + ",");
			fout.write(newLine);
			// Town Board
			fout.write("townBoard=" + town.townBoard + newLine);
			// Town Protection
			fout.write("protectionStatus=" + Integer.toString(town.protectionStatus) + newLine);
			// Bonus Blocks
			fout.write("bonusBlocks=" + Integer.toString(town.bonusBlocks) + newLine);
			// Wall
			fout.write("wall=" + Integer.toString(town.wall.blockType) + ":" + Integer.toString(town.wall.height) + newLine);
			// Wall Blocks
			fout.write("wallBlocks=");
			for(WallBlock wb : town.wall.sections)
				fout.write(Integer.toString((int)wb.p.x)+":"+Integer.toString((int)wb.p.y)+":"+Integer.toString((int)wb.p.z)+":"+Integer.toString(wb.r)+":"+Integer.toString(wb.t) + ",");
			fout.write(newLine);
			// Home Block
			fout.write("homeBlock=" + Long.toString(town.homeBlock.x) + ":" + Long.toString(town.homeBlock.z) + newLine);
			
			fout.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public boolean saveNation(Nation nation) {
		try {
			String path = TownyProperties.flatFileFolder+"/data/nations/"+nation.name+".txt";
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			fout.write("towns=");
			for (Town town : nation.towns)
				fout.write(town.name + ",");
			fout.write(newLine);
			if (nation.capital != null)
				fout.write("capital=" + nation.capital.name);
			fout.write(newLine);
			fout.write("assistants=");
			for(Resident assistant : nation.assistants)
				fout.write(assistant.name + ",");
			fout.write(newLine);
			fout.write("friends=");
			for(Nation friendlyNation : nation.friends)
				fout.write(friendlyNation.name + ",");
			fout.write(newLine);
			fout.write("enemies=");
			for(Nation enemyNation : nation.enemies)
				fout.write(enemyNation.name + ",");
			fout.write(newLine);
			
			fout.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	//
	
    public boolean saveResidentList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(TownyProperties.flatFileFolder + "/data/residents.txt"));
            for (Resident resident : world.residents.values()) {
                fout.write(resident.name + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving residents list file", e);
			return false;
        }
    }
	
	public boolean saveTownList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(TownyProperties.flatFileFolder + "/data/towns.txt"));
            for (Town town : world.towns.values()) {
                fout.write(town.name + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving town list file", e);
			return false;
        }
    }
	
	public boolean saveNationList() {
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(TownyProperties.flatFileFolder + "/data/nations.txt"));
            for (Nation nation : world.nations.values()) {
                fout.write(nation.name + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving town list file", e);
			return false;
        }
    }
	
	//
	
	public boolean saveTownBlocks() {
		try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(TownyProperties.flatFileFolder + "/data/townblocks.csv"));
            for (TownBlock townblock : world.townblocks.values()) {
                String town = townblock.town == null ? "" : townblock.town.name;
                String resident = townblock.resident == null ? "" : townblock.resident.name;
                fout.write(Long.toString(townblock.x) + "," + Long.toString(townblock.z) + "," + town + "," + resident + newLine);
            }    
            fout.close();
			return true;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception while saving town blocks list file", e);
			return false;
        }
    }
}