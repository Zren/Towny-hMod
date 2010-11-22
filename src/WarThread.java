import java.util.*;

public class WarThread extends Thread {
	TownyThread towny;
	ArrayList<PendingWar> pendingWars;
	
	public WarThread(TownyThread towny) {
		this.towny = towny;
		pendingWars = new ArrayList<PendingWar>();
	}
	
	public void run() {
        try {
            while (true) {
				for (PendingWar pWar : pendingWars) {
					if (pWar.nationA == null || pWar.nationB == null) {
						remPendingWar(pWar);
					}
					
					long now = System.currentTimeMillis();
					long timeDiff = pWar.warTime - now;
					
					if (timeDiff < 0) {
						startWar(pWar);
						remPendingWar(pWar);
					} else if (pWar.warnings[0] && timeDiff < 1000*60*60*24) {
						warning(pWar, "begins in 1 day.");
						pWar.warnings[0] = false;
					} else if (pWar.warnings[1] && timeDiff < 1000*60*60) {
						warning(pWar, "begins in 1 hour.");
						pWar.warnings[1] = false;
					} else if (pWar.warnings[2] && timeDiff < 1000*60) {
						warning(pWar, "begins in 1 minute!");
						pWar.warnings[2] = false;
					}
				}
                sleep(1000*60);
            }
        } catch (InterruptedException e) {}
		
    }
	
	public boolean newPendingWar(Nation a, Nation b, long timeTill) {
		try {
			pendingWars.add(new PendingWar(a, b, System.currentTimeMillis()+timeTill));
		} catch(Exception e) { return false; }
		
		return true;
	}
	
	public void remPendingWar(PendingWar pWar) {
		for (int i = 0; i < pendingWars.size(); i++) {
			if (pendingWars.get(i) == pWar) {
				pendingWars.remove(i);
				break;
			}
		}
	}
	
	public void startWar(PendingWar pWar) {
		pWar.nationA.setAliegeance("enemy", pWar.nationB);
		pWar.nationB.setAliegeance("enemy", pWar.nationA);
		towny.updateAllPlayerZones();
		String message = Colors.Red + "WAR! " + Colors.Gold + pWar.nationA +" vs " + pWar.nationB + "!"; 
		towny.sendNationMessage(pWar.nationA, message);
		towny.sendNationMessage(pWar.nationB, message);
	}
	
	public void warning(PendingWar pWar, String warningMsg) {
		towny.sendNationMessage(pWar.nationA, Colors.Red + "War with " + Colors.Gold + pWar.nationB + Colors.Red + ": " + warningMsg);
		towny.sendNationMessage(pWar.nationB, Colors.Red + "War with " + Colors.Gold + pWar.nationA + Colors.Red + ": " + warningMsg);
	}
}

class PendingWar {
	Nation nationA, nationB;
	long warTime;
	boolean[] warnings = {true, true, true}; //Day, Hour, Minute Warnings
	
	public PendingWar(Nation a, Nation b, long t) {
		nationA = a;
		nationB = b;
		warTime = t;
		
		long now = System.currentTimeMillis();
		long timeDiff = warTime - now;
		
		if (timeDiff < 1000*60*60*24)
			warnings[0] = false;
		if (timeDiff < 1000*60*60)
			warnings[1] = false;
		if (timeDiff < 1000*60)
			warnings[2] = false;

	}
}