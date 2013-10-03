package mosquito.sim;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;

public class Mosquito {
	public Point2D location;
	public boolean caught;
	public Mosquito(Point2D p)
	{
		this.location = p;
		this.caught=  false;
	}
	public void moveInDirection(double d, HashSet<Line2D> walls) {
		d = d -30 + GameConfig.random.nextInt(60);

		double newX = location.getX() + Math.cos(d*Math.PI/180);
		double newY = location.getY() - Math.sin(d*Math.PI/180);
		
		// if the path takes them outside the limits of the world, have them move in the other direction
		if (newX < 0 || newX > 100) {
			newX = location.getX() - Math.cos(d*Math.PI/180);
		}
		if (newY < 0 || newY > 100) {
			newY = location.getY() + Math.sin(d*Math.PI/180);
		}

		// TODO: if their path would have them cross a wall, have them move in another direction
		Line2D.Double pathLine = new Line2D.Double(location.getX(),location.getY(),newX, newY);
		for(Line2D l : walls)
		{
			if(l.intersectsLine(pathLine))
				return;
		}
		location.setLocation(newX, newY);
	}
}
