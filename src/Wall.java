import java.util.*;

public class Wall {
	ArrayList<WallBlock> sections;
	int blockType, height, walkwayHeight;
	public Wall() {
		sections = new ArrayList<WallBlock>();
		blockType = 17;
		height = 2;
		walkwayHeight = 0;
	}
}