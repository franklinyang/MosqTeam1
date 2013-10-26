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

    class Section {
        Point2D ul;
        Point2D ur;
        Point2D bl;
        Point2D br;
        
        private Section(Point2D ul, Point2D ur, Point2D bl, Point2D br) {
            this.ul = ul;
            this.ur = ur;
            this.bl = bl;
            this.br = br;
        }
        
        public boolean contains(Point2D x) {
            if(x.getX() >= ul.getX() && x.getY() >= ul.getY()
                    && x.getX() <= ur.getX() && x.getY() >= ur.getY()
                    && x.getX() >= bl.getX() && x.getY() <= bl.getY()
                    && x.getX() <= br.getX() && x.getY() <= br.getY()) {
                return true;
            }
            return false;
        }

        public boolean equals(Section other) {
            if(other.bl.getX() == this.bl.getX()
                    && other.bl.getY() == this.bl.getY()
                    && other.br.getX() == this.br.getX()
                    && other.br.getY() == this.br.getY()
                    && other.ul.getX() == this.ul.getX()
                    && other.ul.getY() == this.ul.getY()
                    && other.ur.getX() == this.ur.getX()
                    && other.ur.getY() == this.ur.getY()) {
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public String toString() {
            return new String(this.ul.getX()+","+this.ur.getY()+"), "+
                    "("+this.ur.getX()+","+this.ur.getY()+"), "+
                    "("+this.bl.getX()+","+this.bl.getY()+"), "+
                    "("+this.br.getX()+","+this.br.getY()+")");
        }
        
    }
    private int numLights;
    
    private Light[] allLights;
    private Point2D.Double lastLight;
    private Logger log = Logger.getLogger(this.getClass()); // for logging
    
    @Override
    public String getName() {
        return "bears";
    }
    
    private Point2D.Double getMidpoint(Line2D line) {
        return new Point2D.Double((line.getX1() + line.getX2()) / 2, (line.getY1() + line.getY2()) / 2);
    }
    
    private Set<Light> lights;
    private Set<MoveableLight> mlights;
    private Set<Line2D> walls;
    private int phase = 0;
    private AStar astar;
    private int move = 0;
    private int numMovesToFlushMosquitos = 0;
    
    private boolean canUseHeuristic = true;
    
    private HashMap<Integer, Integer> numLightsToSpacingMap = new HashMap<Integer, Integer>();
    private HashMap<MoveableLight, Boolean> movementMap = new HashMap<MoveableLight, Boolean>();
    private HashMap<MoveableLight, Integer> lightsToMovesMap = new HashMap<MoveableLight, Integer>();
    
    private LinkedList<Section> mosquitoLocationsBeingUsed = new LinkedList<Section>();
    private HashSet<Section> sections = new HashSet<Section>();
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
        numLightsToSpacingMap.put(9, 9);
        numLightsToSpacingMap.put(8, 11);
        numLightsToSpacingMap.put(7, 13);
        numLightsToSpacingMap.put(6, 15);
        numLightsToSpacingMap.put(5, 17);
        numLightsToSpacingMap.put(4, 22);
        numLightsToSpacingMap.put(3, 33);
        numLightsToSpacingMap.put(2, 35);
        numLightsToSpacingMap.put(1, 9);
        
        for(int i = 0; i < 100; i = i + 25) {
            for(int j = 0; j < 100; j = j + 25) {
                Point2D.Double ul = new Point2D.Double(i,j);
                Point2D.Double ur = new Point2D.Double(i+25,j);
                Point2D.Double bl = new Point2D.Double(i, j+25);
                Point2D.Double br = new Point2D.Double(i+25, j+25);
                lines.add(new Line2D.Double(ul, ur));
                lines.add(new Line2D.Double(ul, bl));
                lines.add(new Line2D.Double(bl, br));
                lines.add(new Line2D.Double(ur, br));
                sections.add(new Section(ul, ur, bl, br));
            }
        }
        
        for (Line2D w : walls) {
            if(w.getP1().getX() < 0.5 || w.getP1().getY() < 0.5 || w.getP1().getX() > 99.5 || w.getP1().getY() > 99.5
                    || w.getP2().getX() < 0.5 || w.getP2().getY() < 0.5 || w.getP2().getX() > 99.5 || w.getP2().getY() > 99.5) {
                canUseHeuristic = false;
                break;
            }
        }
        AreaMap cleanMap = new AreaMap(101,101, walls);
        log.error("CAN USE HEURISTIC: "+canUseHeuristic);
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
            AreaMap cleanMap = new AreaMap(101,101, walls);
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

            lights.add(l);
            
            lightsToMovesMap.put(l, 0);
            mlights.add(l);
            movementMap.put(l, false);
        }
        AreaMap cleanMap = new AreaMap(101, 101, walls);
        for(int i = 0; i <= board.length; i++) {
            for(int j = 0; j <= board[0].length; j++) {
                for(Line2D wall: walls) {
                    if(canUseHeuristic) {
                        if(i > 1 && i < 99 && j > 1 && j < 99) {
                            if(wall.ptSegDist(i, j) <= 1.0) {
                                cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                            }
                        }
                    }
                    else {
                        if(wall.ptSegDist(i, j) <= 1.0 && i != 0 && j != 0 && i != 100 && j != 100) {
                            cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                        }
                    }
                }
            }
        }
        for(int i = 0; i < 101; i++) {
            String line = "";
            for(int j = 0; j < 101; j++) {
                if(cleanMap.getNodes().get(j).get(i).isObstacle) {
                    line = line + "-";
                }
                else {
                    line = line + "+";
                }
            }
            log.error(line);
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
    public Set<Light> updateLights(int[][] board) {
        if(numMovesToFlushMosquitos >= 40) {
            numMovesToFlushMosquitos = 0;
            mosquitoLocationsBeingUsed.clear();
        }
        else {
            numMovesToFlushMosquitos++;
        }
        for(MoveableLight ml: mlights) {
            if(ml.getX() == 97 && ml.getY() == 50) { //If you've reached the collector, stay there for 15 moves
                if(ml.numMovesAtCollector >= 15) { //If you've stayed at the collector for 15 moves then time to move on
                    ml.hasFinishedPhaseOne = true;
                    ml.currDestinationX = 0;
                    ml.currDestinationY = 0;
                    ml.turnOff();
                    movementMap.put(ml, false); //This is a hashmap that tells us whether each light is currently on an A* path
                    lightsToMovesMap.put(ml, 0); //This hashmap tells us what move number is the current light in, in its A* path
                }
                else {
                    ml.numMovesAtCollector++; //If you haven't stayed for 15 moves yet, stay put and increement your movesAtCollector
                    continue;
                }
            }
            if(!ml.hasFinishedPhaseOne) { //If we are still sweeping from left to right (i.e Phase 1 of the strategy)
                if(ml.getX() == 97 && !movementMap.get(ml)) { //If we've reached the right side of the board after sweeping from left to right AND we aren't on an A* path as it is
                    handlePhaseOneCompletion(ml, board);
                    continue;
                }
                if(ml.hasStoppedAtCorner) { //Code to ensure that we only stop at a corner once to wait for mosquitos to catch up
                    ml.numMovesSinceStopped++;
                    if(ml.numMovesSinceStopped == 25) {
                        ml.numMovesSinceStopped = 0;
                        ml.hasStoppedAtCorner = false;
                    }
                }
                boolean hasMovedThisTurn = false; //Boolean variable to ensure that a light can only enter a critical code section, i.e where movement occurs, once.
                if(ml.numMovesSinceStopped == 0) { //Check to see if we need to stop at a corner assuming we haven't seen a corner in the last 25 moves
                    for(Line2D obstacle: walls) {
                        if(!ml.hasStoppedAtCorner && (obstacle.getP1().distance(ml.getLocation()) < 4 || obstacle.getP2().distance(ml.getLocation()) < 4)) {
                            if(ml.numTurnsAtCorner >= 18) { //Wait for obstacle for 10 moves
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
                if(ml.getX() == ml.currDestinationX && ml.getY() == ml.currDestinationY) { //If the light has reached its A* destination in our left to right sweep
                    log.error("4");
                    if(ml.getX() > 95) {
                        log.error("PHASE ONE COMPLETED");
                        handlePhaseOneCompletion(ml, board);
                    }
                    else {
                        if(!hasMovedThisTurn) {
                            log.error("PHASE ONE NOT COMPLETED YET");
                            movementMap.put(ml, false);
                            lightsToMovesMap.put(ml, 0);
                            ml.moveRight();
                            hasMovedThisTurn = true;
                        }
                    }
                }
                else if(!movementMap.get(ml)) { // If the light isn't currently moving on an A* path
                    boolean hasHitWall = false;
                    for(Line2D obstacle: walls) { //Check for obstacles and see if we are near an obstacle
                        if(obstacle.ptSegDistSq(ml.getX(), ml.getY()) <= 2.0) { //We've detected an obstacle 
                            log.error("ENTERED BAD AREA!!! : "+ml);
                            hasHitWall = true;
                            AreaMap cleanMap = new AreaMap(101,101, walls);
//                            for(int i = 0; i < board.length; i++) {
//                                for(int j = 0; j <= board[0].length; j++) {
//                                    for(Line2D wall: walls) {
//                                        if(wall.ptSegDist(i, j) <= 1.0) {
//                                            cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
//                                        }
//                                    }
//                                }
//                            }
                            for(int i = 0; i <= board.length; i++) {
                                for(int j = 0; j <= board[0].length; j++) {
                                    for(Line2D wall: walls) {
                                        if(canUseHeuristic) {
                                            if(i > 1 && i < 99 && j > 1 && j < 99) {
                                                if(wall.ptSegDist(i, j) <= 1.0) {
                                                    cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                                }
                                            }
                                        }
                                        else {
                                            if(wall.ptSegDist(i, j) <= 1.0) {
                                                cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                            }
                                        }
                                    }
                                }
                            }
                            FHeuristic fh = new FHeuristic();
                            astar = new AStar(cleanMap, fh);
                            int xCoordToMoveTo = (int) ml.getX() + 3;
                            for(int i = xCoordToMoveTo; i < 101; i++) {
                                if(!cleanMap.getNodes().get(i).get((int)ml.getY()).isObstacle) {
                                    xCoordToMoveTo = i;
                                    break;
                                }
                            }
                            astar.calcShortestPath((int)ml.getX(), (int) ml.getY(), xCoordToMoveTo, (int) ml.getY()); //Since we've seen an obstacle we need to move the point 6 units to the right
                            ml.currDestinationX = xCoordToMoveTo;
                            ml.currDestinationY = ml.getY();
                            ml.shortestPath = astar.shortestPath;
                            movementMap.put(ml, true);
                            log.error("Curr x: "+ml.getX());
                            log.error("xCoordtoMoveTo: "+xCoordToMoveTo);
//                            log.error("Current x: "+ml.getX());
//                            log.error("Current y: "+ml.getY());
                            if(ml.shortestPath != null && !hasHitWall) {
//                                log.error("Moving to x = "+ml.shortestPath.getX(0));
//                                log.error("Moving to y = "+ml.shortestPath.getY(0));
                                ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                                lightsToMovesMap.put(ml, 1);
                            }
                            else if(ml.shortestPath == null){
                                ml.turnOff();
                            }
                        }
                    }
                    if(!hasHitWall) { //If there were no obstacles just move right
                        log.error("3" + ml);
                        if(!hasMovedThisTurn) {
                            ml.moveRight();
                            hasMovedThisTurn = true;
                        }
                    }
                }
                else {
                    if(ml.shortestPath == null) { //If there were no obstacles move right
                        log.error("2");
                        if(!hasMovedThisTurn) {
                            ml.moveRight();
                            hasMovedThisTurn = true;
                        }
                    }
                    else if(lightsToMovesMap.get(ml) >= ml.shortestPath.getLength()) { //If we've come to the end of our A* path then reset the A* moves and mvoe right
                        log.error("1");
                        if(!hasMovedThisTurn) {
                            ml.moveRight();
                            lightsToMovesMap.put(ml, 0);
                            movementMap.put(ml, false);
                            hasMovedThisTurn = true;
                        }
                    }
                    else { //If we are still on our A* path then move to the next position in the A* path
                        if(!hasMovedThisTurn) {
                            int moveNum = lightsToMovesMap.get(ml);
//                            log.error("CURR: x = "+ml.getX()+ " y = "+ml.getY());
//                            log.error("PATH: x = "+ml.shortestPath.getX(moveNum)+" y = "+ml.shortestPath.getY(moveNum));//Log the place we are moving to
                            ml.moveTo(ml.shortestPath.getX(moveNum), ml.shortestPath.getY(moveNum));
//                            log.error(ml +"   distance: x = "+(ml.shortestPath.getX(moveNum) - ml.getX())+" y = "+(ml.shortestPath.getY(moveNum) - ml.getY())); //Log the distance between the current position and the next position.
                            moveNum++;
                            lightsToMovesMap.put(ml, moveNum);
                            hasMovedThisTurn = true;
                        }
                    }
                }
            }
            else { //If we've finished phase one (i.e reached the collector once) then do this
                if(ml.getLocation().distance(new Point2D.Double(ml.currDestinationX, ml.currDestinationY)) < 5) {
                    ml.turnOn();
                }
                if(ml.getX() == 97 && ml.getY() == 50) { //If you've reached the collector, stay there for 15 moves
                    log.error("REACHED COLLECTOR IN PHASE 2!");
                    if(ml.numMovesAtCollector >= 15) { //If you've stayed at the collector for 15 moves then time to move on
                        ml.turnOff();
                        ml.numMovesAtCollector = 0;
                        movementMap.put(ml, false); //This is a hashmap that tells us whether each light is currently on an A* path
                        lightsToMovesMap.put(ml, 0); //This hashmap tells us what move number is the current light in, in its A* path
                    }
                    else {
                        ml.numMovesAtCollector++; //If you haven't stayed for 15 moves yet, stay put and increement your movesAtCollector
                        continue;
                    }
                }
                if(!movementMap.get(ml)) { // If we aren't on an A* path
                    List<Point2D.Double> mosquitoLocations = getMosquitoLocationsByDistance(board, ml); //Get locations of all the mosquitos ordered in descending order by distance
                    if(!mosquitoLocations.isEmpty()) {
                        AreaMap cleanMap = new AreaMap(101,101, walls);
//                        for(int i = 0; i <= board.length; i++) {
//                          for(int j = 0; j <= board[0].length; j++) {
//                              for(Line2D wall: walls) {
//                                  if(wall.ptSegDist(i, j) <= 1.0) {
//                                      cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
//                                  }
//                              }
//                          }
//                      }
                        for(int i = 0; i <= board.length; i++) {
                            for(int j = 0; j <= board[0].length; j++) {
                                for(Line2D wall: walls) {
                                    if(canUseHeuristic) {
                                        if(i > 1 && i < 99 && j > 1 && j < 99) {
                                            if(wall.ptSegDist(i, j) <= 1.0) {
                                                cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                            }
                                        }
                                    }
                                    else {
                                        if(wall.ptSegDist(i, j) <= 1.0 && i != 0 && j != 0 && i != 100 && j != 100) {
                                            cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                        }
                                    }
                                }
                            }
                        }
                        Point2D.Double furthestMosquitoLocation = null;
                        for(Point2D.Double mosquitoLocation: mosquitoLocations) {
//                            log.error("Furthest mosquito location = "+mosquitoLocation.getX()+","+mosquitoLocation.getY());
                            Section currSection = getSectionOfMosquito(mosquitoLocation);
                            log.error("It's section is = ("+currSection.ul.getX()+","+currSection.ur.getY()+"), "+
                                        "("+currSection.ur.getX()+","+currSection.ur.getY()+"), "+
                                        "("+currSection.bl.getX()+","+currSection.bl.getY()+"), "+
                                        "("+currSection.br.getX()+","+currSection.br.getY()+")");
                            log.error("Size of mosquitoLocationsBeingUsed = "+mosquitoLocationsBeingUsed.size());
                            for(Section section: mosquitoLocationsBeingUsed) {
                                log.error("lol: "+section);
                            }
                            if(!mosquitoLocationsBeingUsed.contains(currSection)) {
                                furthestMosquitoLocation = mosquitoLocation;
                                mosquitoLocationsBeingUsed.add(currSection);
                                break;
                            }
                        }       
                        if(furthestMosquitoLocation != null) {
                            FHeuristic fh = new FHeuristic();
                            astar = new AStar(cleanMap, fh);
                            astar.calcShortestPath((int)ml.getX(),    //Calculate a* path to the farthest mosquito
                                    (int) ml.getY(), 
                                    (int)furthestMosquitoLocation.getX(), 
                                    (int)furthestMosquitoLocation.getY());
                            ml.currDestinationX = furthestMosquitoLocation.getX(); 
                            ml.currDestinationY = furthestMosquitoLocation.getY();
                            ml.shortestPath = astar.shortestPath;
                            movementMap.put(ml, true);
//                            log.error("Current x: "+ml.getX());
//                            log.error("Current y: "+ml.getY());
                            if(ml.shortestPath != null) {
//                                log.error("Moving to x = "+ml.shortestPath.getX(0)); //Start moving towards farthest mosquito
//                                log.error("Moving to y = "+ml.shortestPath.getY(0));
                                log.error("SHORTEST PATH WAS FOUND");
                                ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                                lightsToMovesMap.put(ml, 1);
                            }
                        }
                    }
                }
                else if(ml.getX() == ml.currDestinationX && ml.getY() == ml.currDestinationY) { //Once we've reached the farthest mosquito we now go back to the collector
                    Section section = getSectionOfMosquito(new Point2D.Double(ml.getX(), ml.getY()));
                    if(!mosquitoLocationsBeingUsed.remove(section)) {
                        log.error("LOCATION BEING REMOVED NOT FOUND");
                    }
                    ml.turnOn();
                    movementMap.put(ml, true);
                    ml.currDestinationX = 0;
                    ml.currDestinationY = 0;
                    lightsToMovesMap.put(ml, 0);
                    AreaMap cleanMap = new AreaMap(101,101, walls);
//                    for(int i = 0; i < board.length; i++) {
//                        for(int j = 0; j <= board[0].length; j++) {
//                            for(Line2D wall: walls) {
//                                if(wall.ptSegDist(i, j) <= 1.0) {
//                                    cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
//                                }
//                            }
//                        }
//                    }
                    for(int i = 0; i <= board.length; i++) {
                        for(int j = 0; j <= board[0].length; j++) {
                            for(Line2D wall: walls) {
                                if(canUseHeuristic) {
                                    if(i > 1 && i < 99 && j > 1 && j < 99) {
                                        if(wall.ptSegDist(i, j) <= 1.0) {
                                            cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                        }
                                    }
                                }
                                else {
                                    if(wall.ptSegDist(i, j) <= 1.0 && i != 0 && j != 0 && i != 100 && j != 100) {
                                        cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                                    }
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
//                    log.error("Current x: "+ml.getX());
//                    log.error("Current y: "+ml.getY());
                    if(ml.shortestPath != null) {
//                        log.error("Moving to x = "+ml.shortestPath.getX(0));
//                        log.error("Moving to y = "+ml.shortestPath.getY(0));
                        ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0));
                        lightsToMovesMap.put(ml, 1);
                    }
                    continue;
                }
                else { //If we haven't reached the furthest mosquito then continue moving towards it using the A* path
                    int moveNum = lightsToMovesMap.get(ml);
//                    log.error("CURR: x = "+ml.getX()+ " y = "+ml.getY());
//                    log.error("PATH: x = "+ml.shortestPath.getX(moveNum)+" y = "+ml.shortestPath.getY(moveNum));//Log the place we are moving to
                    if(moveNum < ml.shortestPath.getLength()) {
                        ml.moveTo(ml.shortestPath.getX(moveNum), ml.shortestPath.getY(moveNum));
    //                    log.error(ml +"   distance: x = "+(ml.shortestPath.getX(moveNum) - ml.getX())+" y = "+(ml.shortestPath.getY(moveNum) - ml.getY())); //Log the distance between the current position and the next position.
                        moveNum++;
                        lightsToMovesMap.put(ml, moveNum);
                    }
                }
            }
        }
        return lights; 
    }
    
//    public boolean doesPointHaveObstaclesAround(int x, int y) {
//        for(Line2D wall: walls) {
//           Line2D  
//        }
//    }
    
    public Section getSectionOfMosquito(Point2D.Double mosq) {
        for(Section section: sections) {
            log.error("Section: "+section);
            if(section.contains(mosq)) {
                log.error("SECTION CHOSEN!");
                return section;
            }
        }
        return null;
    }
    
    public void handlePhaseOneCompletion(MoveableLight ml, int[][] board) {
        AreaMap cleanMap = new AreaMap(101,101, walls);
//        for(int i = 0; i < board.length; i++) {
//            for(int j = 0; j <= board[0].length; j++) {
//                for(Line2D wall: walls) {
//                    if(wall.ptSegDist(i, j) <= 1.0) {
//                        cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
//                    }
//                }
//            }
//        }
        for(int i = 0; i <= board.length; i++) {
            for(int j = 0; j <= board[0].length; j++) {
                for(Line2D wall: walls) {
                    if(canUseHeuristic) {
                        if(i > 1 && i < 99 && j > 1 && j < 99) {
                            if(wall.ptSegDist(i, j) <= 1.0) {
                                cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                            }
                        }
                    }
                    else {
                        if(wall.ptSegDist(i, j) <= 1.0) {
                            cleanMap.getNodes().get(i).get(j).isObstacle = true; // nay on the current node
                        }
                    }
                }
            }
        }
        FHeuristic fh = new FHeuristic();
        astar = new AStar(cleanMap, fh);
        astar.calcShortestPath((int)ml.getX(), (int) ml.getY(), 97, 50); //Compute the A* path from here to the collector
        ml.currDestinationX = 97;
        ml.currDestinationY = 50;
        ml.shortestPath = astar.shortestPath;
        movementMap.put(ml, true);
//        log.error("Current x: "+ml.getX());
//        log.error("Current y: "+ml.getY());
        if(ml.shortestPath != null) {
//            log.error("Moving to x = "+ml.shortestPath.getX(0));
//            log.error("Moving to y = "+ml.shortestPath.getY(0));
            ml.moveTo(ml.shortestPath.getX(0), ml.shortestPath.getY(0)); //Move once in the A* path to initiate the A* movement sequence
            lightsToMovesMap.put(ml, 1);
        }
        // THIS IS A HACK!!!!! 
        // ANYONE WHO READS THIS MUST KNOW THAT IF YOU DO SUCH THINGS IN REAL WORLD PROJECTS 
        // KITTENS WILL DIE SPONTANEOUSLY
        else {
            ml.turnOff();
        }
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
    
    public boolean isNearAnotherLight(int i, int j, MoveableLight ml) { //Returns if the the mosquitos at this point are near another light (to avoid random bugs like one light moving to another light
       for(MoveableLight other: mlights) {                              //unnecessarily because the other light would have already caught the mosquitos).
            if(other.getLocation() != ml.getLocation() && other.isOn()) {
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