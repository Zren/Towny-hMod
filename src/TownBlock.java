import java.util.*;

public class TownBlock {
    public long x, z;
    public Town town;
    public Resident resident;
    
    public TownBlock(long x, long z) {
        this.x = x;
        this.z = z;
    }
	
	public boolean isEdgeBlock() {
		boolean isEdgeBlock = false;
		int[][] offset = {{-1,0},{1,0},{0,-1},{0,1}};
		for (int i = 0; i < 4; i++) {
			String edgeKey = Long.toString(x+offset[i][0])+","+Long.toString(z+offset[i][1]);
			//log.info(edgeKey + "=" + isEdgeBlock);
			TownBlock edgeTownBlock = TownyWorld.getInstance().townblocks.get(edgeKey);
			if (edgeTownBlock == null)
				continue;
			else if (edgeTownBlock.town != town)
				continue;
			
			return true;
		}
		
		return false;
	}
	
	public String toString() {
		return "TownBlock["+x+","+z+"]";
	}
}