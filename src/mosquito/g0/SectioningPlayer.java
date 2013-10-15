package mosquito.g0;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;



public class SectioningPlayer extends mosquito.sim.Player {
	class Section {
		  int[] boolCombo;
		  ArrayList<Integer> xPoints;
		  ArrayList<Integer> yPoints;
		  int maxX;
		  int minX;
		  int maxY;
		  int minY;
		  public Section() {
			  boolCombo = new int[walls.size()];
			  xPoints = new ArrayList<Integer>();
			  yPoints = new ArrayList<Integer>();
		  }
		  //boolean through; //describes if there are multiple entrances into the section;
		  void printDetails() {
			  log.debug("boolCombo: " + Arrays.toString(boolCombo));
			  //  log.trace("Area: " + this.area + ", endpoints: " + Arrays.toString(this.endpoints) + ", through? " + through);
		  }
	}

	private int numLights;
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	
	@Override
	public String getName() {
		return "I section things";
	}
	
	private Set<Light> lights;
	private Set<Line2D> walls;
	
	private int pointLineRelationships[][][];
	private int numberOfSections;
	private ArrayList<Section> sections = new ArrayList<Section>();
	
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
		pointLineRelationships = new int[100][100][walls.size()];
		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		sectioningAlgorithm();
		identifySections(pointLineRelationships);
		for (int i=0; i<numberOfSections; i++) {
			System.err.println("Section " + i + " midpoint: (" + (sections.get(i).maxX+sections.get(i).minX)/2 +
				" , " + (sections.get(i).maxY+sections.get(i).minY)/2 + ")");
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
		// Initially place the lights randomly, and put the collector next to the last light

		lights = new HashSet<Light>();
		Random r = new Random();
		for(int i = 0; i<numLights;i++)
		{
			// this player just picks random points for the Light
			lastLight = new Point2D.Double(r.nextInt(100), r.nextInt(100));
			
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
		int counter = 0;

		for (Light l : lights) {
			MoveableLight ml = (MoveableLight)l;

			// randomly move it in one direction
			// these methods return true if the move is allowed, false otherwise
			// a move is not allowed if it would go beyond the world boundaries
			// you can get the light's position with getX() and getY()
			switch (counter) {
			case 0: ml.goTo((sections.get(0).maxX+sections.get(0).minX)/2, (sections.get(0).maxY+sections.get(0).minY)/2);
				break;
			case 1: ml.goTo((sections.get(1).maxX+sections.get(1).minX)/2, (sections.get(1).maxY+sections.get(1).minY)/2);
				break;
			case 2: ml.goTo((sections.get(2).maxX+sections.get(2).minX)/2, (sections.get(2).maxY+sections.get(2).minY)/2);
				break;
			case 3: ml.goTo((sections.get(3).maxX+sections.get(3).minX)/2, (sections.get(3).maxY+sections.get(3).minY)/2);
				break;
			case 4: ml.goTo((sections.get(4).maxX+sections.get(4).minX)/2, (sections.get(4).maxY+sections.get(4).minY)/2);
				break;
			default: ml.goTo(50, 50);
			}

			// randomly turn the light off or on
			// you don't have to call these each time, of course: a light that's on stays on
			// you can query the state of the light with the isOn() method
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
		Collector c = new Collector(lastLight.getX()+1,lastLight.getY() +1);
		System.err.println("Positioned a Collector at (" + (lastLight.getX()+1) + ", " + (lastLight.getY()+1) + ")");
		return c;
	}
	
	public void sectioningAlgorithm() {
		Line2D[] wallArray;
		  for (int x=0; x<100; x++) {	//could probably also be x+=2, depending on how well we want our sections to be defined
		  	for (int y=0; y<100; y++) {
		  		for (int i = 0; i < walls.size(); i++) {
		  			wallArray = walls.toArray(new Line2D[walls.size()]);
		  			pointLineRelationships[x][y][i] = comparePointToLine(x, y, wallArray[i]);	
		  		}
		  	}
		  }
	}
	
	public int comparePointToLine(int x, int y, Line2D line) {
		boolean lineIsVertical = (line.getX1() == line.getX2());
		
	  if (lineIsVertical) {
	    //for vertical lines, assume that points to the left are lesser and points to the right are greater
			if (y < line.getY1()) return 0;
	    else return 1;
		} else {
	    //find the slope and intercept of the line given
	    double slope = (line.getY2()-line.getY1()) / (line.getX2()-line.getX1());
	    double yIntercept = line.getY1() - (slope*line.getX1());
	    //identify if point is above or below line
	    if (y > slope * x + yIntercept) return 1;
	    else return 0;
		}
	}
	
	void identifySections(int[][][] pointLineRelationships) {
		numberOfSections=0;
		for (int x=0; x<100; x++) {
			for (int y=0; y<100; y++) {
				int[] boolCombo = pointLineRelationships[x][y];
		    boolean hasComboBeenSeen = false;
		    for (int i=0; i<numberOfSections; i++) {
		      if (Arrays.equals(boolCombo,sections.get(i).boolCombo)) {
		        hasComboBeenSeen = true;
		        sections.get(i).xPoints.add(x);
		        sections.get(i).yPoints.add(y);
		        if (x > sections.get(numberOfSections-1).maxX) sections.get(i).maxX = x;
		        else if (x < sections.get(numberOfSections-1).minX) sections.get(i).minX = x;
		        if (y > sections.get(numberOfSections-1).maxY) sections.get(i).maxY = y;
		        else if (y < sections.get(numberOfSections-1).minY) sections.get(i).minY = y;
		        break;
		      } 
		    } 
		    if (!hasComboBeenSeen) {
		        Section newSection = new Section();
		        newSection.xPoints.add(x);
		        newSection.yPoints.add(y);
		        newSection.maxX = x;
		        newSection.minX = x;
		        newSection.maxY = y;
		        newSection.minY = y;
		        sections.add(newSection);
		        numberOfSections++;
		    } 
		  }
		}
	}

}
