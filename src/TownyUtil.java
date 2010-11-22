import java.util.*;

public class TownyUtil {
    public static long[] getTownBlock(long x, long y) {
        long[] out = {0, 0};
        out[0] = x / TownyProperties.blockSize - (x < 0 ? 1 : 0);
        out[1] = y / TownyProperties.blockSize - (y < 0 ? 1 : 0);
        return out;
    }
	// Townblock -> lower left point of townblock.
	/*public static long[] getPoint(long x, long y) {
		TownyProperties.blockSize;
		
	}*/
}

class TownSortBySize implements Comparator<Town> {
    public int compare(Town o1, Town o2) {
        return o1.size() - o2.size();
    }
}

class NationSortBySize implements Comparator<Nation> {
    public int compare(Nation o1, Nation o2) {
        return o1.size() - o2.size();
    }
}