package mosquito.g0;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Line2D;
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

import org.jgrapht.alg.*;

public class FPlayer extends mosquito.sim.Player {
	
	CycleDetector a;
	
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
	
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	
	@Override
	public String getName() {
		return "bears";
	}
	
	private Set<Light> lights;
	private Set<MoveableLight> mlights;
	private Set<Line2D> walls;
	private AreaMap map;
	private AStar astar;
	private int move;
	
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
		
		// lines are for sketching
		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		
		this.numLights = numLights;
		this.walls = walls;
		
		return lines;
	}


	/*
	 * This is used to determine the initial placement of the lights.
	 * It is called after startNewGame.
	 * The board tells you where the mosquitoes are: board[x][y] tells you the
	 * number of mosquitoes at coordinate (x, y)
	 */
	public Set<Light> getLights(int[][] board) {
		
		// initializing AStar
		FHeuristic fh = new FHeuristic();
		
		lights = new HashSet<Light>();
		lastLight = new Point2D.Double(10, 10);
		mlights = new HashSet<MoveableLight>();
		for(int a = 0; a<numLights; a++)
		{
			AreaMap cleanMap = new AreaMap(100,100);
		    for(int i = 0; i < board.length; i++) {
		        for(int j = 0; j < board[0].length; j++) {
		            for(Line2D wall: walls) {
		                if(wall.ptSegDist(i, j) < 2.0) {
		                	cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
		                }
		            }
		        }
		    }
			astar = new AStar(cleanMap, fh);
			
			MoveableLight l = new MoveableLight(lastLight.getX()+a*10, lastLight.getY(), true);

			l.turnOff();
			lights.add(l);
			
			log.error("current iteration is " + a);
			
			astar.calcShortestPath((int)l.getX(), (int) l.getY(), 50, 50);
			
			log.error("current iteration is " + a + " and we are done with calculating aStar");
			
			l.shortestPath = astar.shortestPath;
			mlights.add(l);
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
		
//		AStar currAStar;
		
		for (MoveableLight ml : mlights) {
//			MoveableLight ml = (MoveableLight)l;
//			currAStar = ml.astar;
			Path shortest = ml.shortestPath;
			if (move >= shortest.getLength())
				continue;
				
			ml.moveTo(shortest.getX(move), shortest.getY(move));
			
		}
		this.move++;
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
