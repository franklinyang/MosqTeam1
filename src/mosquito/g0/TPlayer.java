package mosquito.g0;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;



public class TPlayer extends mosquito.sim.Player {

	private int numLights;
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	
	@Override
	public String getName() {
		return "bears";
	}
	
	private Set<Light> lights;
	private Set<Line2D> walls;
	private int phase = 0;
	
	/*
	 * This is called when a new game starts. It is passed the set
	 * of lines that comprise the different walls, as well as the 
	 * maximum number of lights you are allowed to use.
	 * 
	 * The return value is a set of lines that you would like to have drawn on the screen.
	 * These lines don't actually affect gameplay, they're just there so you can have some
	 * visual clue as to what's happening in the simulation.
	 */
	@Override
	public ArrayList<Line2D> startNewGame(Set<Line2D> walls, int numLights) {
		this.numLights = numLights;
		this.walls = walls;

		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		return lines;
	}


	/*
	 * This is used to determine the initial placement of the lights.
	 * It is called after startNewGame.
	 * The board tells you where the mosquitoes are: board[x][y] tells you the
	 * number of mosquitoes at coordinate (x, y)
	 */
	public Set<Light> getLights(int[][] board) {
		// Initially place the lights randomly, and put the collector next to the last light

		lights = new HashSet<Light>();
		Random r = new Random();
		for(int i = 0; i<numLights;i++)
		{
			// this player just picks random points for the Light
			lastLight = new Point2D.Double(20, i*100/numLights+50/(numLights));
			
			/*
			 * The arguments to the Light constructor are: 
			 * - X coordinate
			 * - Y coordinate
			 * - whether or not the light is on
			 */
			
			
			MoveableLight l = new MoveableLight(lastLight.getX(),lastLight.getY(), true);

			log.trace("Positioned a light at (" + lastLight.getX() + ", " + lastLight.getY() + ")");
			lights.add(l);
		}
		
		return lights;
	}
	
	/*
	 * This is called at the beginning of each step (before the mosquitoes have moved)
	 * If your Set contains additional lights, an error will occur. 
	 * Also, if a light moves more than one space in any direction, an error will occur.
	 * The board tells you where the mosquitoes are: board[x][y] tells you the
	 * number of mosquitoes at coordinate (x, y)
	 */
	public Set<Light> updateLights(int[][] board) {

		for (Light l : lights) {
			MoveableLight ml = (MoveableLight)l;

			// randomly move it in one direction
			// these methods return true if the move is allowed, false otherwise
			// a move is not allowed if it would go beyond the world boundaries
			// you can get the light's position with getX() and getY()
			Point2D location = l.getLocation();
			
			switch (phase) {
			case 0: 
				if (location.getX() > 80) phase++;
				ml.moveRight();
				break;
			case 1: 
				if (location.getX() < 20) phase++;
				ml.moveLeft();
				break;
			case 2:
				if (location.getX() < 50) ml.moveRight(); 
				else if (location.getY() > 50) ml.moveUp(); 
				else if (location.getY() < 50) ml.moveDown();
				break;
			case 3:
				Random r = new Random();
				switch (r.nextInt(4)) {
				case 0: ml.moveUp(); break;
				case 1: ml.moveDown(); break;
				case 2: ml.moveLeft(); break;
				case 3: ml.moveRight(); break;
				}
				break;
			default: ml.moveLeft();
			}
	
		}
		
		return lights;
	}

	/*
	 * Currently this is only called once (after getLights), so you cannot
	 * move the Collector.
	 */
	@Override
	public Collector getCollector() {
		// this one just places a collector next to the last light that was added
		Collector c = new Collector(50,50);
		return c;
	}

}
