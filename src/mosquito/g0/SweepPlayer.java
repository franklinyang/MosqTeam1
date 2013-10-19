package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;

import org.apache.log4j.Logger;
import org.jgrapht.alg.CycleDetector;

public class SweepPlayer extends mosquito.sim.Player {
    
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
    private Set<MoveableLight> mlights;
    private Set<Line2D> walls;
    private int phase = 0;
    private AStar astar;
    private int move = 0;
    
    private HashMap<Integer, Integer> numLightsToSpacingMap = new HashMap<Integer, Integer>();
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
        // vertical lines have undefined slope, so we keep their positions
        ArrayList<Line2D.Double> verticalLines = new ArrayList<Line2D.Double>();
        
        // lines are for sketching
        ArrayList<Line2D> lines = new ArrayList<Line2D>();
        ArrayList<Point2D.Double> midpoints = new ArrayList<Point2D.Double>();
        
        this.numLights = numLights;
        this.walls = walls;
        
        ArrayList<Line2D> extendedLines = new ArrayList<Line2D>();
        
        numLightsToSpacingMap.put(10, 9);
        numLightsToSpacingMap.put(9, 7);
        numLightsToSpacingMap.put(8, 8);
        numLightsToSpacingMap.put(7, 10);
        numLightsToSpacingMap.put(6, 13);
        numLightsToSpacingMap.put(5, 18);
        numLightsToSpacingMap.put(4, 22);
        numLightsToSpacingMap.put(3, 33);
        numLightsToSpacingMap.put(1, 22);
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
        ArrayList<Double> sortedVerticalLines = new ArrayList<Double>();
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
        // initializing AStar
        FHeuristic fh = new FHeuristic();
        
        int x = 0; 
        int y = numLightsToSpacingMap.get(numLights);
        
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
            lastLight = new Point2D.Double(x, y);
            y += numLightsToSpacingMap.get(numLights);
            MoveableLight l = new MoveableLight(lastLight.getX(), lastLight.getY(), true);

//          MoveableLight l = new MoveableLight(lastLight.getX()+a*10, lastLight.getY(), true);

//          l.turnOff();
            lights.add(l);
            
            log.error("current iteration is " + a);
            
//          astar.calcShortestPath((int)l.getX(), (int) l.getY(), 99, (int) lastLight.getY());
            
            log.error("current iteration is " + a + " and we are done with calculating aStar");
            
//          l.shortestPath = astar.shortestPath;
            lightsToMovesMap.put(l, 0);
            mlights.add(l);
            movementMap.put(l, false);
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
     *  private int spirals = 8;
    private int currSpiral = 0;
    private int currSide = 0;
    private int sidesOfSpiral = 3;
    private int currLight = 0
    private Light[] allLights = array of lights;
     */
    public Set<Light> updateLights(int[][] board) {
        int timesMoved = 0;
        for(MoveableLight ml: mlights) {
            if(ml.getX() == 97 && ml.getY() == 50) {
                if(ml.numMovesAtCollector >= 15) {
                    ml.hasReachedRightSide = true;
                    movementMap.put(ml, false);
                    lightsToMovesMap.put(ml, 0);
                }
                else {
                    ml.numMovesAtCollector++;
                    continue;
                }
            }
            if(!ml.hasReachedRightSide) {
                if(ml.getX() == 97 && !movementMap.get(ml)) {
                    log.error("REACHED THE OTHER SIDE !!!!!!!!");
                    AreaMap cleanMap = new AreaMap(100,100);
                    for(int i = 0; i < board.length; i++) {
                        for(int j = 0; j < board[0].length; j++) {
                            for(Line2D wall: walls) {
                                if(wall.ptSegDist(i, j) < 2.0) {
                                    if(i==99 && j==50) {
                                        log.error("REMOVE IMPORTANT NODE!!!");
                                    }
                                    cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                }
                            }
                        }
                    }
                    FHeuristic fh = new FHeuristic();
                    astar = new AStar(cleanMap, fh);
                    astar.calcShortestPath((int)ml.getX(), (int) ml.getY(), 97, 50);
                    ml.currDestinationX = 97;
                    ml.currDestinationY = 50;
                    ml.shortestPath = astar.shortestPath;
                    movementMap.put(ml, true);
                    log.error("Current x: "+ml.getX());
                    log.error("Current y: "+ml.getY());
                    if(ml.shortestPath != null) {
                        log.error("Moving to x = "+ml.shortestPath.getX(0));
                        log.error("Moving to y = "+ml.shortestPath.getY(0));
                        ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                        timesMoved++;
                        lightsToMovesMap.put(ml, 1);
                    }
                    continue;
                }
                if(ml.hasStoppedAtCorner) {
                    ml.numMovesSinceStopped++;
                    if(ml.numMovesSinceStopped == 25) {
                        ml.numMovesSinceStopped = 0;
                        ml.hasStoppedAtCorner = false;
                    }
                }
                boolean hasMovedThisTurn = false; //Boolean variable to ensure that a light can only enter a crticial code section, i.e where movement occurs, once.
                if(ml.numMovesSinceStopped == 0) {
                    for(Line2D obstacle: walls) {
                        if(!ml.hasStoppedAtCorner && (obstacle.getP1().distance(ml.getLocation()) < 4 || obstacle.getP2().distance(ml.getLocation()) < 4)) {
                            if(ml.numTurnsAtCorner >= 10) {
                                ml.numTurnsAtCorner = 0;
                                ml.hasStoppedAtCorner = true;
                                ml.numMovesSinceStopped = 1;
                            }
                            else {
                                ml.numTurnsAtCorner++;
                                hasMovedThisTurn = true;
                            }
                            break;
                        }
                    }
                }
                if(ml.getX() == ml.currDestinationX && ml.getY() == ml.currDestinationY) { //If the light has reached its A* destination
                    log.error("4");
                    if(!hasMovedThisTurn) {
                        movementMap.put(ml, false);
                        lightsToMovesMap.put(ml, 0);
                        ml.moveRight();
                        timesMoved++;
                        hasMovedThisTurn = true;
                    }
                }
                else if(!movementMap.get(ml)) { // If the light isn't currently moving on an A* path
                    boolean hasHitWall = false;
                    for(Line2D obstacle: walls) {
                        if(obstacle.ptSegDistSq(ml.getX(), ml.getY()) <= 4.0) { //We've detected an obstacle 
                            log.error("ENTERED BAD AREA!!! : "+ml);
                            hasHitWall = true;
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
                            FHeuristic fh = new FHeuristic();
                            astar = new AStar(cleanMap, fh);
                            astar.calcShortestPath((int)ml.getX(), (int) ml.getY(), (int)ml.getX()+6, (int) ml.getY());
                            ml.currDestinationX = ml.getX() + 6;
                            ml.currDestinationY = ml.getY();
                            ml.shortestPath = astar.shortestPath;
                            movementMap.put(ml, true);
                            log.error("Current x: "+ml.getX());
                            log.error("Current y: "+ml.getY());
                            if(ml.shortestPath != null && !hasHitWall) {
                                log.error("Moving to x = "+ml.shortestPath.getX(0));
                                log.error("Moving to y = "+ml.shortestPath.getY(0));
                                ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                                timesMoved++;
                                lightsToMovesMap.put(ml, 1);
                            }
                        }
                    }
                    if(!hasHitWall) {
                        log.error("3" + ml);
                        if(!hasMovedThisTurn) {
                            ml.moveRight();
                            timesMoved++;
                            hasMovedThisTurn = true;
                        }
                    }
                }
                else {
                    if(ml.shortestPath == null) {
                        log.error("2");
                        if(!hasMovedThisTurn) {
                            ml.moveRight();
                            timesMoved++;
                            hasMovedThisTurn = true;
                        }
                    }
                    else if(lightsToMovesMap.get(ml) >= ml.shortestPath.getLength()) {
                        log.error("1");
                        if(!hasMovedThisTurn) {
                            ml.moveRight();
                            timesMoved++;
                            lightsToMovesMap.put(ml, 0);
                            movementMap.put(ml, false);
                            hasMovedThisTurn = true;
                        }
                    }
                    else {
                        if(!hasMovedThisTurn) {
                            int moveNum = lightsToMovesMap.get(ml);
                            log.error("CURR: x = "+ml.getX()+ " y = "+ml.getY());
                            log.error("PATH: x = "+ml.shortestPath.getX(moveNum)+" y = "+ml.shortestPath.getY(moveNum));//Log the place we are moving to
                            ml.moveTo(ml.shortestPath.getX(moveNum), ml.shortestPath.getY(moveNum));
                            log.error(ml +"   distance: x = "+(ml.shortestPath.getX(moveNum) - ml.getX())+" y = "+(ml.shortestPath.getY(moveNum) - ml.getY())); //Log the distance between the current position and the next position.
                            moveNum++;
                            timesMoved++;
                            lightsToMovesMap.put(ml, moveNum);
                            hasMovedThisTurn = true;
                        }
                    }
                }
            }
            else {
//                if(ml.getX() == 97 && ml.getY() == 50 ) {
//                    log.error("THIS HAPPENED WOW "+ml.numMovesAtCollector2);
//                    if(ml.numMovesAtCollector2 >= 20) {
//                        log.error("CHECK");
//                        ml.hasReachedRightSide = true;
//                        movementMap.put(ml, false);
//                        lightsToMovesMap.put(ml, 0);
//                    }
//                    else {
//                        log.error("CHECK2");
//                        ml.numMovesAtCollector2++;
//                        continue;
//                    }
//                }
                if(!movementMap.get(ml)) {
                    List<Point2D.Double> mosquitoLocations = getMosquitoLocationsByDistance(board, ml);
                    if(!mosquitoLocations.isEmpty()) {
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
                        FHeuristic fh = new FHeuristic();
                        astar = new AStar(cleanMap, fh);
                        astar.calcShortestPath((int)ml.getX(), 
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
                            log.error("Moving to x = "+ml.shortestPath.getX(0));
                            log.error("Moving to y = "+ml.shortestPath.getY(0));
                            ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                            timesMoved++;
                            lightsToMovesMap.put(ml, 1);
                        }
                    }
                }
                else if(ml.getX() == ml.currDestinationX && ml.getY() == ml.currDestinationY) {
                    movementMap.put(ml, true);
                    ml.currDestinationX = 0;
                    ml.currDestinationY = 0;
                    lightsToMovesMap.put(ml, 0);
                    AreaMap cleanMap = new AreaMap(100,100);
                    for(int i = 0; i < board.length; i++) {
                        for(int j = 0; j < board[0].length; j++) {
                            for(Line2D wall: walls) {
                                if(wall.ptSegDist(i, j) < 2.0) {
                                    if(i==99 && j==50) {
                                        log.error("REMOVE IMPORTANT NODE!!!");
                                    }
                                    cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                }
                            }
                        }
                    }
                    FHeuristic fh = new FHeuristic();
                    astar = new AStar(cleanMap, fh);
                    astar.calcShortestPath((int)ml.getX(), (int) ml.getY(), 97, 50);
                    ml.currDestinationX = 97;
                    ml.currDestinationY = 50;
                    ml.shortestPath = astar.shortestPath;
                    movementMap.put(ml, true);
                    log.error("Current x: "+ml.getX());
                    log.error("Current y: "+ml.getY());
                    if(ml.shortestPath != null) {
                        log.error("Moving to x = "+ml.shortestPath.getX(0));
                        log.error("Moving to y = "+ml.shortestPath.getY(0));
                        ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                        timesMoved++;
                        lightsToMovesMap.put(ml, 1);
                    }
                    continue;
                }
                else {
                    int moveNum = lightsToMovesMap.get(ml);
                    log.error("CURR: x = "+ml.getX()+ " y = "+ml.getY());
                    log.error("PATH: x = "+ml.shortestPath.getX(moveNum)+" y = "+ml.shortestPath.getY(moveNum));//Log the place we are moving to
                    ml.moveTo(ml.shortestPath.getX(moveNum), ml.shortestPath.getY(moveNum));
                    log.error(ml +"   distance: x = "+(ml.shortestPath.getX(moveNum) - ml.getX())+" y = "+(ml.shortestPath.getY(moveNum) - ml.getY())); //Log the distance between the current position and the next position.
                    moveNum++;
                    timesMoved++;
                    lightsToMovesMap.put(ml, moveNum);
                }
            }
        }
//      log.error("Times moved: "+timesMoved);
        return lights;
    }
    
    public List<Point2D.Double> getMosquitoLocationsByDistance(int[][] board, MoveableLight ml) {
        List<Point2D.Double> result = new ArrayList<Point2D.Double>();
        for(int i = 0; i < 100; i++) {
            for(int j = 0; j < 100; j++) {
                if(i < 95 && (j < 48 || j > 52) && !isNearAnotherLight(i,j,ml)) {
                    if(board[i][j] != 0) {
                        result.add(new Point2D.Double(i, j));
                    }
                }
            }
        }
        Collections.sort(result, new Comparator<Point2D.Double>() {

            @Override
            public int compare(Point2D.Double o1, Point2D.Double o2) {
                Point2D.Double origin = new Point2D.Double(97, 50);
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
    
    public boolean isNearAnotherLight(int i, int j, MoveableLight ml) {
        for(MoveableLight other: mlights) {
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

    /*
     * Currently this is only called once (after getLights), so you cannot
     * move the Collector.
     */
    @Override
    public Collector getCollector() {
        // this one just places a collector next to the last light that was added
        Collector c = new Collector(97,50);
        return c;
    }

}