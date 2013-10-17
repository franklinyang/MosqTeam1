package mosquito.g0;

import java.awt.geom.Point2D;

public class Waypoint implements Comparable<Waypoint> {

	Point2D p;
	public Waypoint(Point2D p) {
		this.p = p;
	}
	

	@Override
	public int compareTo(Waypoint w) {
		Point2D o = w.p;
		boolean smallerX = p.getX() < o.getX();
		boolean smallerY = p.getY() < o.getY();
		if (smallerX) {
			if(smallerY)
				return -1;
			return 1;
		}
		else 
			return 1; 
		
		// TODO Auto-generated method stub
	}

}
