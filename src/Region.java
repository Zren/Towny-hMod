public class Region {
    double x1, x2, y1, y2, z1, z2;
    String name;
    
    public Region() {
        x1 = 0;
        x2 = 0;
        y1 = 0;
        y2 = 0;
        z1 = 0;
        z2 = 0;
    }
    
    public Region(Point a, Point b) {
        this(a, b, "");
    }
    
    public Region(Point a, Point b, String name) {
        this.name = name;
        
        if (a.x < b.x) {
            x1 = a.x;
            x2 = b.x;
        } else {
            x1 = b.x;
            x2 = a.x;
        }
        if (a.y < b.y) {
            y1 = a.y;
            y2 = b.y;
        } else {
            y1 = b.y;
            y2 = a.y;
        }
        if (a.z < b.z) {
            z1 = a.x;
            z2 = b.x;
        } else {
            z1 = b.x;
            z2 = a.x;
        }
    }
    
    public String toString() {
        return name+"["+x1+","+y1+","+z1+"]["+x2+","+y2+","+z2+"]";
    }
    
    public boolean inside(Location p) {
        if (x1 < p.x && p.x < x2 && y1 < p.y && p.y < y2 && z1 < p.z && p.z < z2)
            return true;
        return false;
    }
}