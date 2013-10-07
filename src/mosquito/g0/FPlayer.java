package mosquito.g0;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.lang.Number;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;



public class FPlayer extends mosquito.sim.Player {
	
	class Section {
		Line2D l;
		Line2D r;
		Line2D u;
		Line2D d;
		
		boolean visited;
		
		private Section(Line2D l, Line2D r, Line2D u, Line2D d) {
			this.l = l;
			this.r = r;
			this.u = u;
			this.d = d;
			visited = false;
		}
	}

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
	private LinkedList<Section> boardSections = new LinkedList<Section>();
	
	@Override
	public String getName() {
		return "bears";
	}
	
	/*
	 * WIP
	 */
	private LinkedList<Section> createSections(Set<Line2D> input) {
		
		LinkedList<Section> sections = new LinkedList<Section>();
		Iterator<Line2D> walls = input.iterator();
		Line2D wall;
		while(walls.hasNext()) {
			wall = walls.next();
			walls.remove();
		}
		
		return null;
	}
	
	/*
	 * WIP
	 */
	private Point2D.Double intersects(Line2D line, ArrayList<Section> allSections) {
		return null;
	}
	
	private Point2D.Double getMidpoint(Line2D line) {
		return new Point2D.Double((line.getX1() + line.getX2()) / 2, (line.getY1() + line.getY2()) / 2);
	}
	
	private Line2D.Double extendLine(Line2D line) {
		double x1 = line.getX1();
		double x2 = line.getX2();
		double y1 = line.getY1();
		double y2 = line.getY2();
		
		double ydiff = y2 - y1;
		double xdiff = x2 - x1;
		double m = ydiff / xdiff;
		double yIntercept = y1 - m*x1;
		double y100Intercept = yIntercept + m*100;
		double xIntercept = -yIntercept / m;
		double x100Intercept = (100 - yIntercept) / m;
		
		boolean intersectsX0 = yIntercept >= 0 && yIntercept <= 100;
		boolean intersectsX100 = y100Intercept >= 0 && y100Intercept <= 100;
		double newX1 = (intersectsX0) ? 0 : xIntercept;
		double newY1 = (intersectsX0) ? yIntercept : 0;
		double newX2 = (intersectsX100) ? 100 : x100Intercept;
		double newY2 = (intersectsX100) ? y100Intercept : 100;
		
		Point2D newPoint1 = new Point2D.Double(newX1, newY1);
		Point2D newPoint2 = new Point2D.Double(newX2, newY2);
		
		return new Line2D.Double(newPoint1, newPoint2);
		
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
		// vertical lines have undefined slope, so we keep their positions
		ArrayList<Line2D.Double> verticalLines = new ArrayList<Line2D.Double>();
		
		// lines are for sketching
		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		ArrayList<Point2D.Double> midpoints = new ArrayList<Point2D.Double>();
		
		this.numLights = numLights;
		this.walls = walls;
		
		ArrayList<Line2D> extendedLines = new ArrayList<Line2D>();
		
		// creating game borders
		Point2D.Double uR = new Point2D.Double(0, 100.0);
		Point2D.Double uL = new Point2D.Double(0, 0);
		Point2D.Double lL = new Point2D.Double(0, 100.0);
		Point2D.Double lR = new Point2D.Double(100.0, 100.0);
		
		Line2D l = new Line2D.Double(uL, lL);
		Line2D r = new Line2D.Double(uR, lR);
		Line2D u = new Line2D.Double(uL, uR);
		Line2D d = new Line2D.Double(lL, lR);
		
		Section boundary = new Section(l, r, u, d);
		ArrayList<Number.Double> sortedVerticalLines = new ArrayList<Number.Double>();
		boardSections.add(boundary);
		
		for (Line2D w : walls) {
			double x1 = w.getX1();
			double x2 = w.getX2();
			double y1 = w.getY1();
			double y2 = w.getY2();
			
			// dealing with vertical lines first
			if (w.getX1() == w.getX2()) {
				verticalLines.add(new Line2D.Double(new Point2D.Double(x1, 0), new Point2D.Double(x1, 100)));
				sortedVerticalLines.add(w.getX1());
				continue;
			}
			
			Point2D.Double mid = this.getMidpoint(w);
			midpoints.add(mid);
			
			Line2D ext = this.extendLine(w);
			extendedLines.add(ext);
			lines.add(ext);
		}
		
		// subdivide by vertical sections
		for(int i = 0; i < verticalLines.size(); i++) {
			// need to segment by vertical section...
		}
		
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
		// lights.add(new MoveableLight(49,50,true));
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
