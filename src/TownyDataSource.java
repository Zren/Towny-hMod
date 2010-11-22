import java.util.logging.Level;
import java.util.logging.Logger;
//import java.io.*;

public abstract class TownyDataSource {
	protected static final Logger log = Logger.getLogger("Minecraft");
	//private final String newLine = System.getProperty("line.separator");
	public TownyWorld world;
	
	public void initialize(TownyWorld world) {
		this.world = world;
	}
	
	public boolean loadAll() {
		return (
			loadNationList() &&
			loadTownList() &&
			loadResidentList() &&
			loadTownBlocks() &&
			loadNations() &&
			loadTowns() &&
			loadResidents()
		);
	}
	
	public boolean saveAll() {
		//saveXml();
		return (
			saveNationList() &&
			saveTownList() &&
			saveResidentList() &&
			saveTownBlocks() &&
			saveNations() &&
			saveTowns() &&
			saveResidents()
		);
	}
	
	abstract public boolean loadResidentList();
	abstract public boolean loadTownList();
	abstract public boolean loadNationList();
	abstract public boolean saveResidentList();
	abstract public boolean saveTownList();
	abstract public boolean saveNationList();
	
	abstract public boolean loadResidents();
	abstract public boolean loadTowns();
	abstract public boolean loadNations();
	abstract public boolean saveResidents();
	abstract public boolean saveTowns();
	abstract public boolean saveNations();
	
	abstract public boolean loadResident(Resident resdient);
	abstract public boolean loadTown(Town town);
	abstract public boolean loadNation(Nation nation);
	abstract public boolean saveResident(Resident resdient);
	abstract public boolean saveTown(Town town);
	abstract public boolean saveNation(Nation nation);
	
	abstract public boolean loadTownBlocks();
	abstract public boolean saveTownBlocks();
	
	/*public void saveXml() {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(TownyProperties.flatFileFolder + "/data/world.xml"));
			fout.write("<?xml version=\"1.0\" ?>" + newLine);
			fout.write("<world>" + newLine);
			for (Nation nation : world.nations.values()) {
				fout.write(" <nation name=\""+nation.name+"\">" + newLine);
				for (Town town : nation.towns) {
					fout.write("  <town name=\""+town.name+"\" mayor=\""+town.mayor.name+"\">" + newLine);
					fout.write("   <residents>" + newLine);
					for (Resident resident: town.residents)
						fout.write("    <resident name=\""+resident.name+"\" />" + newLine);
					fout.write("   </residents>" + newLine);
					fout.write("   <townblocks>" + newLine);
					for (TownBlock townblock : world.getTownBlocks(town))
						fout.write("    <townblock x=\""+Long.toString(townblock.x)+"\" z=\""+Long.toString(townblock.z)+"\" owner=\""+(townblock.resident == null ? "" : townblock.resident.name) + "\" />" + newLine);
					fout.write("   </townblocks>" + newLine);
					fout.write("  </town>" + newLine);
				}
				fout.write(" </nation>" + newLine);
			}
			fout.write("</world>" + newLine);
			fout.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while saving town blocks xml file", e);
		}
	}*/
}