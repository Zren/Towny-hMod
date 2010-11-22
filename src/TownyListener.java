import java.util.*;

public class TownyListener extends PluginListener {
    public TownyThread towny;
    public CommandQueue<Object> commandQueue;
    private ArrayList<String> commands;

    public TownyListener() {
        commands = new ArrayList<String>();
        commands.add("/resident");commands.add("/player");
        commands.add("/town");
        commands.add("/nation");
		commands.add("/towny");
        commands.add("/claim");
        commands.add("/ally");
		commands.add("/townyadmin");
		
        //Debug Commands
        commands.add("/wait");
		commands.add("/loc");
    }

    public void onLogin(Player player) {
        towny.onLogin(player);
    }
    
    public void onDisconnect(Player player) {
        towny.playerZone.remove(player.getName());
		
		// End claim mode for player
		if (towny.playerClaim.contains(player.getName())) {
			for (int i = 0; i < towny.playerClaim.size(); i++) {
				if (towny.playerClaim.get(i) == player.getName()) {
					towny.playerClaim.remove(i);
					break;
				}
			}
		}
		// End claim mode for player
		if (towny.playerMap.contains(player.getName())) {
			for (int i = 0; i < towny.playerMap.size(); i++) {
				if (towny.playerMap.get(i) == player.getName()) {
					towny.playerMap.remove(i);
					break;
				}
			}
		}
    }

    public boolean onCommand(Player player, String[] split) {
        if (!player.canUseCommand(split[0]))
            return false;
        if (commands.contains(split[0])) {
            Object[] objs = {player, split};
            commandQueue.addWork(objs);
            return true;
        }
		
		return false;
    }
    
    public void onPlayerMove(Player player, Location from, Location to) {
        Object[] objs = {player, from, to};
		commandQueue.addWork(objs);
    }
    
    public boolean onBlockCreate(Player player, Block blockPlaced, Block blockClicked, int itemInHand) {
		int zone;
		if (towny == null || towny.playerZone == null)
			zone = -1;
		else
			zone = (towny.playerZone.get(player.getName()) == null) ? -1 : towny.playerZone.get(player.getName());
			
		if (zone == -1) {
			player.sendMessage(Colors.Rose + "Error: You don't have a playerZone.");
			player.sendMessage(Colors.Rose + "Notify admin. Shade (coder) needs to know what");
			player.sendMessage(Colors.Rose + "happened. Tell admin what you interactions with");
			player.sendMessage(Colors.Rose + "Towny this login. Thank you. -Shade");
            return true;
		}	
		// Stop creation if player doesn't belong to town or if he's an enemy
        // ToDo: Let enemies place TNT and/or fire.
        if (zone == 3 || zone == 2) {
			player.sendMessage(Colors.Rose + "The nearby town's barrier prevents you from creation.");
            return true;
		}
		
		// Stop creation outside towns if configured so.
		if (zone == 0 && !TownyProperties.unclaimedZoneBuildRights) {
			player.sendMessage(Colors.Rose + "You should start a town first before building.");
			return true;
		}
        
        // Otherwise let the block be placed.
        return false;
    }
    
    public boolean onBlockDestroy(Player player, Block block) {
        int zone = (towny.playerZone.get(player.getName()) == null) ? -1 : towny.playerZone.get(player.getName());
		if (zone == -1) {
			player.sendMessage(Colors.Rose + "Error: You don't have a playerZone.");
			player.sendMessage(Colors.Rose + "Notify admin. Shade (coder) needs to know what");
			player.sendMessage(Colors.Rose + "happened. Tell admin what you interactions with");
			player.sendMessage(Colors.Rose + "Towny this login. Thank you. -Shade");
            return true;
		}	
        
		// Stop destruction only if player doesn't belong to town.
        if (zone == 3 || zone == 5) {
			if (block.getStatus() == 3)
				player.sendMessage(Colors.Rose + "The nearby town's barrier prevents you from destruction.");
			return true;
		}
        
        // Otherwise, destroy it.
        return false;
    }
	
	public void onArmSwing(Player player) {
		if (towny.playerClaim.contains(player.getName())) {
			towny.claimSingleTownBlock(player);
		}
	}
	
	public boolean onTeleport(Player player, Location from, Location to) { 
		Object[] objs = {player, from, to};
		commandQueue.addWork(objs);
		return false;
	}
}