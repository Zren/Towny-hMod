import java.util.*;

public class WarChunkThread extends Thread {
	TownyThread towny;
	ArrayList<PendingWarChunk> pendingWarChunks;
	
	public WarChunkThread(TownyThread towny) {
		this.towny = towny;
		pendingWarChunks = new ArrayList<PendingWarChunk>();
	}
	
	public void run() {
        try {
            while (true) {
				for (PendingWarChunk pWarChunk : pendingWarChunks) {
					if (pWarChunk.nationA == null || pWarChunk.nationB == null) {
						remPendingWarChunk(pWarChunk);
					}
					
					long now = System.currentTimeMillis();
					long timeDiff = pWarChunk.warTime - now;
					
					if (timeDiff < 0) {
						startWar(pWarChunk);
						remPendingWar(pWarChunk);
					} else if (pWarChunk.warnings[0] && timeDiff < 1000*60*60*24) {
						warning(pWarChunk, "begins in 1 day.");
						pWarChunk.warnings[0] = false;
					} else if (pWarChunk.warnings[1] && timeDiff < 1000*60*60) {
						warning(pWarChunk, "begins in 1 hour.");
						pWarChunk.warnings[1] = false;
					} else if (pWarChunk.warnings[2] && timeDiff < 1000*60) {
						warning(pWarChunk, "begins in 1 minute!");
						pWarChunk.warnings[2] = false;
					}
				}
                sleep(1000*60);
            }
        } catch (InterruptedException e) {}
		
    }
	
	public boolean newPendingWarChunk(Nation a, Nation b, long timeTill) {
		try {
			pendingWarChunks.add(new PendingWar(a, b, System.currentTimeMillis()+timeTill));
		} catch(Exception e) { return false; }
		
		return true;
	}
	
	public void remPendingWarChunk(PendingWarChunk pWarChunk) {
		for (int i = 0; i < pendingWars.size(); i++) {
			if (pendingWars.get(i) == pWarChunk) {
				pendingWars.remove(i);
				break;
			}
		}
	}
	
	public void startWar(PendingWarChunk pWarChunk) {
		String message = Colors.Red + "[WAR] " + Colors.Gold + pWarChunk.player.getName() + " is attacking " + pWarChunk + "!"; 
		towny.sendNationMessage(pWarChunk.player.town.nation, message);
		towny.sendNationMessage(pWarChunk.townblock.town.nation, message);
	}
	
	public void warning(PendingWarChunk pWarChunk, String warningMsg) {
		pWarChunk.player.sendMessage(warningMsg);
		towny.sendNationMessage(pWarChunk.nationB, warningMsg);
	}
}

class PendingWarChunk {
	TownBlock townBlock;
	Player player;
	long enterTime;
	boolean[] warnings = {true, true, true}; //Day, Hour, Minute Warnings
	
	public PendingWarChunk(Player p, TownBlock tb, long t) {
		player = p;
		townBlock = tb;
		enterTime = t;
		
		long now = System.currentTimeMillis();
		long timeDiff = enterTime - now;
		
		if (timeDiff < 1000*60*60*24)
			warnings[0] = false;
		if (timeDiff < 1000*60*60)
			warnings[1] = false;
		if (timeDiff < 1000*60)
			warnings[2] = false;

	}
}