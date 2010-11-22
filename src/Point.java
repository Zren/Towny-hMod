public class Point extends Location {
    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public boolean equals(Object aThat) {
        if (this == aThat) return true;
        if (aThat instanceof Point) {
            Point that = (Point)aThat;
            if (this.x==that.x && this.y == that.y && this.z == that.z)
                return true;
        }
        
        return false;
    }
}