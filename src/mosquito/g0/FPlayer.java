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



public class FPlayer extends mosquito.sim.Player {

	private int numLights;
	private int currLight;
	private int spirals;
	private int currSpiral;
	private int currSide;
	private int stayCount;
	
	private final static int GAPSIZE = 13;
	
	private Light[] allLights;
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
		
		this.spirals = 3;
		this.currLight = 0;
		this.currSpiral = 0;
		this.currSide = 0;

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

		lights = new HashSet<Light>();
		for(int i = 0; i<numLights;i++)
		{
			lastLight = new Point2D.Double(10, 10);
			
			/*
			 * The arguments to the Light constructor are: 
			 * - X coordinate
			 * - Y coordinate
			 * - whether or not the light is on
			 */
			
			MoveableLight l = new MoveableLight(lastLight.getX(),lastLight.getY(), true);

			log.trace("Positioned a light at (" + lastLight.getX() + ", " + lastLight.getY() + ")");
			l.turnOff();
			lights.add(l);
		}
		allLights = lights.toArray(new Light[numLights]);
		return lights;
	}
	
	/*
	 * This is called at the beginning of each step (before the mosquitoes have moved)
	 * If your Set contains additional lights, an error will occur. 
	 * Also, if a light moves more than one space in any direction, an error will occur.
	 * The board tells you where the mosquitoes are: board[x][y] tells you the
	 * number of mosquitoes at coordinate (x, y)
	 */
	
	/*
	 * 	private int spirals = 8;
	private int currSpiral = 0;
	private int currSide = 0;
	private int sidesOfSpiral = 3;
	private int currLight = 0
	private Light[] allLights = array of lights;
	 */
	public Set<Light> updateLights(int[][] board) {
		
		MoveableLight l = (MoveableLight)allLights[currLight];
		Point2D location = l.getLocation();
		l.turnOn();

		if (currSpiral == spirals) {
			currSide = 4;
		}
		
		if (stayCount == 50) {
			log.error("Switching to next light at: " + currSpiral);
			stayCount = 0;
			currSide = 0;
			currLight++;
			currSpiral = 0;
			l.turnOff();
			return lights;	
		}
		
		switch (currSide) {
		case 0:
			if (location.getX() < 90-GAPSIZE*currSpiral) {
				l.moveRight();
				break;
			}
			currSide = 1;
		case 1:
			if (location.getY() < 90-GAPSIZE*currSpiral) {
				l.moveDown();
				break;
			}
			currSide = 2;
		case 2:
			if (location.getX() > 10+GAPSIZE*currSpiral) {
				l.moveLeft();
				break;
			}
			currSpiral++;
			currSide = 3;
		case 3:
			if (location.getY() > 10+GAPSIZE*currSpiral) {
				l.moveUp();
				break;
			}
			currSide = 0;
		case 4:
			log.error("here4");
			if (location.getX() < 50)  {
				log.error("1");
				l.moveRight(); 
				break;
			}
			else if (location.getY() > 50) { 
				log.error("2");
				l.moveUp(); 
				break;
			}
			else if (location.getY() < 50) { 
				log.error("3");
				l.moveDown();
				break;
			}
			else if (location.getX() > 50) {
				log.error("4");
				l.moveLeft();
				break;
			}
			else
				stayCount++;
		default:
			l.moveLeft();
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
