package mosquito.g0;

import java.awt.geom.Line2D;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;

import java.util.Set;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;

public class ImpossibleGirl extends mosquito.sim.Player {
	
	@Override
	public String getName() {
		return "I section things";
	}
	
	// general instance variables relevant to our problem
	private Set<Light> lights;
	private Set<Line2D> walls;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	
	private int collectorX, collectorY, numLights;
	
	// related to lights
	private ArrayList<MoveableLight> mlights;
	
	// related to astar
	private AStar astar;
	
	// related to sections
	private int pointLineRelationships[][][];
	private ArrayList<Section> sections = new ArrayList<Section>();
	int numberOfSections;
//	ArrayList<Section> prunedSections;
	
	// related to prims
	private int[] orderedSections;
    private HashMap<MoveableLight, Boolean> movementMap = new HashMap<MoveableLight, Boolean>();
    private HashMap<MoveableLight, Integer> lightsToMovesMap = new HashMap<MoveableLight, Integer>();
	
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
		/********* TEST SUITE*************/
		/*
		WeightedGraph testGraph = new WeightedGraph(6); 
		int[] testResult = new int[numberOfSections];
		
//		testGraph.addEdge(0, 1, 34);
//		testGraph.addEdge(1, 0, 34);
//		testGraph.addEdge(0, 2, 1);
//		testGraph.addEdge(2, 0, 1);
//		testGraph.addEdge(0, 3, 5);
//		testGraph.addEdge(3, 0, 5);
//		testGraph.addEdge(1, 2, 4);
//		testGraph.addEdge(2, 1, 4);
//		testGraph.addEdge(1, 3, 66);
//		testGraph.addEdge(3, 1, 66);
//		testGraph.addEdge(2, 3, 34);
//		testGraph.addEdge(3, 2, 34);
//		testGraph.addEdge(0, 0, 99);
//		testGraph.addEdge(1, 1, 99);
//		testGraph.addEdge(2, 2, 99);
//		testGraph.addEdge(3, 3, 99);
//		testGraph.print();

        testGraph.addEdge (0,1,2);
        testGraph.addEdge (0,5,9);
        testGraph.addEdge (1,2,8);
        testGraph.addEdge (1,3,15);
        testGraph.addEdge (1,5,6);
        testGraph.addEdge (2,3,1);
        testGraph.addEdge (4,3,3);
        testGraph.addEdge (4,2,7);
        testGraph.addEdge (5,4,3);
		log.error("Neighbor of 0: "+Arrays.toString(testGraph.neighbors(0)));
		log.error("Neighbor of 1: "+Arrays.toString(testGraph.neighbors(1)));
		log.error("Neighbor of 2: "+Arrays.toString(testGraph.neighbors(2)));
		log.error("Neighbor of 3: "+Arrays.toString(testGraph.neighbors(3)));
		log.error("Neighbor of 4: "+Arrays.toString(testGraph.neighbors(4)));
		log.error("Neighbor of 5: "+Arrays.toString(testGraph.neighbors(5)));
		testResult = TPrim.Prim(testGraph);
//		testResult = Prims.prim(testGraph, 0);
		//testResult = Dijkstra.dijkstra(testGraph, 0);
//		testResult = PrimMinimumSpanningTree.mininumSpanningTree(testGraph.edges);
//		
	    for (int i=0; i<6; i++) {
	    	log.error("The " + -i + "th point to visit is: " + testResult[i]);
	    }*/
		/**************END TEST***********/
		
		
		
		
		
		this.numLights = numLights;
		this.walls = walls;
		pointLineRelationships = new int[100][100][walls.size()];
		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		sectioningAlgorithm();
		identifySections(pointLineRelationships);
		
		// set the midpoints for each of the sections
		for(int i = 0; i < sections.size(); i++) {
			sections.get(i).setMidpoints();
		}
				
		sections = this.pruneSections(sections, walls);
		numberOfSections = sections.size();
		
		for (int i=0; i<numberOfSections; i++) {
			
			Point2D ul = new Point2D.Double(sections.get(i).midX-1, sections.get(i).midY-1);
			Point2D ll = new Point2D.Double(sections.get(i).midX-1, sections.get(i).midY+1);
			Point2D ur = new Point2D.Double(sections.get(i).midX+1, sections.get(i).midY-1);
			Point2D lr = new Point2D.Double(sections.get(i).midX+1, sections.get(i).midY+1);
			
			Line2D c1 = new Line2D.Double(ul, lr);
			Line2D c2 = new Line2D.Double(ll, ur);
			
			lines.add(c1);
			lines.add(c2);
			
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

		// initializing AStar
		FHeuristic fh = new FHeuristic();
		
		lights = new HashSet<Light>();
		mlights = new ArrayList<MoveableLight>();
		
		this.orderedSections = findOptimalRoute(board, numberOfSections, sections);
	    
	    for (int i=0; i<numberOfSections; i++) {
	    	log.error("The " + i + "th point to visit is: (" + sections.get(orderedSections[i]).midX + " , " +
	    			sections.get(orderedSections[i]).midY + ").");
	    }

		
		// initially position each of the nights
		for (int i = 0; i < numLights; i++) {
			MoveableLight l;
			if(i > numberOfSections-1) {
				l = new MoveableLight(50, 50, true);
                l.hasFinishedPhaseOne = true;
                movementMap.put(l, false); //This is a hashmap that tells us whether each light is currently on an A* path
                lightsToMovesMap.put(l, 0); //This hashmap tells us what move number is the current light in, in its A* path
			}
			else {
				int sectionIndex = orderedSections[i];
				int midX = sections.get(sectionIndex).midX;
				int midY = sections.get(sectionIndex).midY;
				// need to place lights on a point where the collector IS NOT
				l = new MoveableLight(sections.get(sectionIndex).midX, sections.get(sectionIndex).midY, true);
			}
			mlights.add(l);
			lights.add(l);
			lightsToMovesMap.put(l, 0);
	        movementMap.put(l, false);
		}
	    
		// add a list of waypoints to each light
		int index;
		Point2D waypoint;
		AreaMap correctMap = generateAreaMap(board, walls);
		for(int i = 0; i < numberOfSections; i++) {
			index = i % numLights;
			waypoint = new Point2D.Double(sections.get(orderedSections[i]).midX, sections.get(orderedSections[i]).midY);
			if (!correctMap.getNodes().get(sections.get(orderedSections[i]).midX).get(sections.get(orderedSections[i]).midY).isObstacle) {
				mlights.get(index).waypoints.add(waypoint);
				this.collectorX = sections.get(orderedSections[i]).midX;
				this.collectorY = sections.get(orderedSections[i]).midY;
			}
		}
		
		this.collectorX = (int)getCollector().getX();
		this.collectorY = (int)getCollector().getY();
		
		// for each light, make the last light the collector
		for (int i = 0; i < numLights; i++) {
			//assume that collector is at 50,50
			mlights.get(i).waypoints.add(new Point2D.Double(getCollector().getX(), getCollector().getY()));
		}
		
		int len;
		Point2D currPoint;
		Point2D nextPoint;
		// generate paths between waypoints
		for (MoveableLight currLight : mlights) {
			len = currLight.waypoints.size();
			// set all paths in moving light
			for (int j = 0; j < len-1; j++) {
				currPoint = currLight.waypoints.get(j);
				nextPoint = currLight.waypoints.get(j+1);
				AreaMap cleanMap = generateAreaMap(board, walls);
				astar = new AStar(cleanMap, fh);
				astar.calcShortestPath((int)currPoint.getX(), (int)currPoint.getY(), (int)nextPoint.getX(), (int)nextPoint.getY());
				currLight.shortestPaths.add(astar.shortestPath);
			}
			// initialize paths to first path
			if(currLight.shortestPaths.size() == 0)
				continue;
			currLight.currPath = currLight.shortestPaths.get(0);
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
		
		for (MoveableLight ml : mlights) {
			//TODO: VISHWA, this movementMap.get(ml) isn't  being set correctly. we're only returning to the collector once now
            if(ml.getX() == getCollector().getX() && ml.getY() == getCollector().getY() /*&& !movementMap.get(ml)*/) { //If you've reached the collector, stay there for 15 moves
                if(ml.numMovesAtCollector >= 15) { //If you've stayed at the collector for 15 moves then time to move on
                    ml.hasFinishedPhaseOne = true;
                    ml.numMovesAtCollector = 0;
                    movementMap.put(ml, false); //This is a hashmap that tells us whether each light is currently on an A* path
                    lightsToMovesMap.put(ml, 0); //This hashmap tells us what move number is the current light in, in its A* path
                }
                else {
                    ml.numMovesAtCollector++; //If you haven't stayed for 15 moves yet, stay put and increment your movesAtCollector
                    continue;
                }
            }
            
            if(ml.hasFinishedPhaseOne) {
				if(!movementMap.get(ml)) { // If we aren't on an A* path
	                List<Point2D.Double> mosquitoLocations = getMosquitoLocationsByDistance(board, ml); //Get locations of all the mosquitos ordered in descending order by distance
	                if(!mosquitoLocations.isEmpty()) {
	                    AreaMap cleanMap = generateAreaMap(board, walls);
	                    FHeuristic fh = new FHeuristic();
	                    astar = new AStar(cleanMap, fh);
	                    astar.calcShortestPath((int)ml.getX(),    //Calculate a* path to the farthest mosquito
	                            (int) ml.getY(), 
	                            (int)mosquitoLocations.get(0).getX(), 
	                            (int)mosquitoLocations.get(0).getY());
	                    ml.currDestinationX = mosquitoLocations.get(0).getX(); 
	                    ml.currDestinationY = mosquitoLocations.get(0).getY();
	                    ml.shortestPath = astar.shortestPath;
	                    movementMap.put(ml, true);
	                    log.error("Current x: "+ml.getX());
	                    log.error("Current y: "+ml.getY());
	                    if(ml.shortestPath != null) {
	                        log.error("Moving to x = "+ml.shortestPath.getX(0)); //Start moving towards farthest mosquito
	                        log.error("Moving to y = "+ml.shortestPath.getY(0));
	                        ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
	                        lightsToMovesMap.put(ml, 1);
	                    }
	                }
	            }
	            else if(ml.getX() == ml.currDestinationX && ml.getY() == ml.currDestinationY) { //Once we've reached the farthest mosquito we now go back to the collector
	            	movementMap.put(ml, true);
	                ml.currDestinationX = 0;
	                ml.currDestinationY = 0;
	                lightsToMovesMap.put(ml, 0);
	                AreaMap cleanMap = generateAreaMap(board, walls);
	                FHeuristic fh = new FHeuristic();
	                astar = new AStar(cleanMap, fh);
	                astar.calcShortestPath((int)ml.getX(), (int) ml.getY(), (int)getCollector().getX(), (int)getCollector().getY());
	                ml.currDestinationX = getCollector().getX();
	                ml.currDestinationY = getCollector().getY();
	                ml.shortestPath = astar.shortestPath;
	                movementMap.put(ml, true);
	                log.error("Current x: "+ml.getX());
	                log.error("Current y: "+ml.getY());
	                if(ml.shortestPath != null) {
    	                if(ml.shortestPath.getLength() > 0) {
    	                    log.error("Moving to x = "+ml.shortestPath.getX(0));
    	                    log.error("Moving to y = "+ml.shortestPath.getY(0));
    	                    ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
    	                    lightsToMovesMap.put(ml, 1);
    	                }
	                }
	                continue;
	            }
	            else if(ml.shortestPath != null){ //If we haven't reached the furthest mosquito then continue moving towards it using the A* path
	                int moveNum = lightsToMovesMap.get(ml);
	                log.error("CURR: x = "+ml.getX()+ " y = "+ml.getY());
	                log.error("PATH: x = "+ml.shortestPath.getX(moveNum)+" y = "+ml.shortestPath.getY(moveNum));//Log the place we are moving to
	                ml.moveTo(ml.shortestPath.getX(moveNum), ml.shortestPath.getY(moveNum));
	                log.error(ml +"   distance: x = "+(ml.shortestPath.getX(moveNum) - ml.getX())+" y = "+(ml.shortestPath.getY(moveNum) - ml.getY())); //Log the distance between the current position and the next position.
	                moveNum++;
	                lightsToMovesMap.put(ml, moveNum);
	            }
            }
            else { 
    			Path currPath = ml.currPath; // get the current path we're working through
    			if (currPath == null) {
    				ml.move = 0;
    				ml.indexOfPath++;
    				if(ml.indexOfPath >= ml.shortestPaths.size())
    					continue;
    				ml.currPath = ml.shortestPaths.get(ml.indexOfPath);
    				continue;
    			}
    						
    			// check to see if we're done moving
    			if (ml.move >= currPath.getLength()) {
    				ml.indexOfPath++;
    				if (ml.indexOfPath >= (ml.shortestPaths.size())) {
    					continue;
    				}
    				
    				ml.currPath = ml.shortestPaths.get(ml.indexOfPath);
    				ml.move = 0;
    				continue;
    			}
    			
    			ml.moveTo(currPath.getX(ml.move), currPath.getY(ml.move));
    			ml.move++;
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
		Collector c = new Collector(this.collectorX-1, this.collectorY-1);
		return c;
	}
	
	private ArrayList<Section> pruneSections(ArrayList<Section> sections, Set<Line2D> walls) {
		int x1, x2, y1, y2;
		ArrayList<Section> prunedSections = sections;
		for(int i = 0; i < sections.size(); i++) {
			Section s = sections.get(i);
			x1 = s.midX;
			y1 = s.midY;
			for(int j = i+1; j < sections.size(); j++) {
				Section st = sections.get(j);
				x2 = st.midX;
				y2 = st.midY;
				if(ptDist(x1, y1, x2, y2) < 15 && !intersectsWall(x1, y1, x2, y2, walls)) {
					prunedSections.remove(s);
					break;
				}
			}
		}
		log.error("Pruned Section size is: " + prunedSections.size());
		return prunedSections;
	}
	
	private int ptDist(int x1, int y1, int x2, int y2) {
		return (int)Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
	}
	
	private boolean intersectsWall(int x1, int y1, int x2, int y2, Set<Line2D> walls) {
		Point2D p1 = new Point2D.Double(x1, y1);
		Point2D p2 = new Point2D.Double(x2, y2);
		Line2D l = new Line2D.Double(p1, p2);
		for (Line2D w : walls) {
			if(w.intersectsLine(l))
				return true;
		}
		return false;
	}


	public AreaMap generateAreaMap(int[][] board, Set<Line2D> walls) {
		AreaMap cleanMap = new AreaMap(101,101);
		for(int i = 0; i <= board.length; i++) {
		    for(int j = 0; j <= board[0].length; j++) {
		        for(Line2D wall: walls) {
		        	int dist = (int)wall.ptSegDist(i,j);
		        	if(dist == 0)
		        		cleanMap.getNodes().get(i).get(j).isObstacle = true;
		        	if(dist < 0.05)
		        		cleanMap.getNodes().get(i).get(j).heuristicDistanceFromGoal = Integer.MAX_VALUE; // nay on the current node
		        	else if(dist < 1) {
		            	cleanMap.getNodes().get(i).get(j).heuristicDistanceFromGoal = 1000;
		            }
		        	else if(dist < 2) {
		        		cleanMap.getNodes().get(i).get(j).heuristicDistanceFromGoal = 10000; // nay on the current node
		        	}
		        }
		    }
		}
		return cleanMap;
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
				if (x < line.getX1()) return 0;
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
	
	public void identifySections(int[][][] pointLineRelationships) {
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
		        if (x > sections.get(i).maxX)
		        	sections.get(i).maxX = x;
		        else if (x < sections.get(i).minX)
		        	sections.get(i).minX = x;
		        if (y > sections.get(i).maxY)
		        	sections.get(i).maxY = y;
		        else if (y < sections.get(i).minY)
		        	sections.get(i).minY = y;
		        break;
		      } 
		    } 
		    if (!hasComboBeenSeen) {
		        Section newSection = new Section(walls.size());
		        newSection.boolCombo=pointLineRelationships[x][y];
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
	
	int[] findOptimalRoute(int[][] board, int numberOfSections, ArrayList<Section> sections) {
		/* initialize a weighted graph, where each node 
		is a midpoint and the weights represent the actual distances between them, taking obstacles into account */
		WeightedGraph midpointGraph = new WeightedGraph(numberOfSections); 
		orderedSections = new int[numberOfSections];

		// initializing AStar
		FHeuristic fh = new FHeuristic();
		AreaMap cleanMap = new AreaMap(101,101); //101 because we don't want to miss out the last row and column of the grid
	    for(int i = 0; i <= board.length; i++) {
	        for(int j = 0; j <= board[0].length; j++) {
	            for(Line2D wall: walls) {
	                if(wall.ptSegDist(i, j) < 2.0) {
	                	cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
	                }
	            }
	        }
	    }
		astar = new AStar(cleanMap, fh);
		

		//For each section, 
		for (int i=0; i<numberOfSections; i++) {
			//log.error("Segment "+i+" is at: ("+sections.get(i).midX+" , "+sections.get(i).midY+" ).");
			cleanMap = new AreaMap(100,100);
		    for(int k = 0; k < board.length; k++) {
		        for(int l = 0; l < board[0].length; l++) {
		            for(Line2D wall: walls) {
		                if(wall.ptSegDist(k, l) < 2.0) cleanMap.getNodes().get(k).get(l).isObstacle = true; // nay on the current nodes
		            }
		        }
		    }
		    
			astar = new AStar(cleanMap, fh);
			for (int j=i+1; j<numberOfSections; j++) {
				//build an adjacency matrix with the distance between each midpoint
				astar.calcShortestPath(sections.get(i).midX, sections.get(i).midY, sections.get(j).midX, sections.get(j).midY);
				//log.error("line 311.  iX: " + sections.get(i).midX + " iY: " + sections.get(j).midY);
				if (astar.shortestPath == null) {
					//TODO: This is hacky
					midpointGraph.addEdge(i,  j, 10000);
				} else midpointGraph.addEdge(i,j,astar.shortestPath.getLength());
			} 
		}
		//midpointGraph.print();
		orderedSections = TPrim.Prim(midpointGraph);
		return orderedSections;
	}
	
	public List<Point2D.Double> getMosquitoLocationsByDistance(int[][] board, MoveableLight ml) {
        List<Point2D.Double> result = new ArrayList<Point2D.Double>();
        for(int i = 0; i < 100; i++) {
            for(int j = 0; j < 100; j++) {
                if(i < 95 && (j < 48 || j > 52) && !isNearAnotherLight(i,j,ml)) { //If the mosquito isn't near the collector and it isn't near another light
                    if(board[i][j] != 0) {
                        result.add(new Point2D.Double(i, j));
                    }
                }
            }
        }
        Collections.sort(result, new Comparator<Point2D.Double>() {

            @Override
            public int compare(Point2D.Double o1, Point2D.Double o2) { //Sort the mosquito's in descending order by distance.
                Point2D.Double origin = new Point2D.Double(collectorX, collectorY);
                double o1Distance = origin.distance(o1);
                double o2Distance = origin.distance(o2);
                if(o1Distance > o2Distance) {
                    return -1;
                }
                else if(o2Distance > o1Distance) {
                    return 1;
                }
                return 0;
            }
        });
        
        return result;
    }
    
    public boolean isNearAnotherLight(int i, int j, MoveableLight ml) { //Returns if the the mosquitos at this point are near another light (to avoid random bugs like one light moving to another light
       for(MoveableLight other: mlights) {                              //unnecessarily because the other light would have already caught the mosquitos).
            if(other.getLocation() != ml.getLocation()) {
                Point2D.Double locationOfOther = (Point2D.Double) other.getLocation();
                Point2D.Double locationOfMosquito = new Point2D.Double(i,j);
                if (locationOfOther.distance(locationOfMosquito) < 8) {
                    return true;
                }
            }
        }
        return false;
    }

}

