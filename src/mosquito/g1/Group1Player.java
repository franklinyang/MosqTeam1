package mosquito.g1;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;
import mosquito.sim.MoveableLight.Corner;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class Group1Player extends mosquito.sim.Player {

	private int numLights;
	private Point2D.Double lastLight;
	private int numRounds = 0;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	
	@Override
	public String getName() {
		return "Random Player";
	}
	
	private Set<Light> lights;
	private Set<Line2D> walls;
	
	private HashMap<Light, Boolean> wasMovingUp = new HashMap<Light, Boolean>();
	private HashMap<Light, Integer> numMovesOffPath = new HashMap<Light, Integer>();
	private HashMap<Light, Line2D> lastObstacleEncountered = new HashMap<Light, Line2D>();
	
	private HashMap<Light, Boolean> movementMap = new HashMap<Light, Boolean>();
	private HashMap<Integer, Integer> numLightsToSpacingMap = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> numLightsToMaxRoundsMap = new HashMap<Integer, Integer>();
	
	private Stack<Integer> problemStack = new Stack<Integer>();
	
	private MoveableLight l1;
	private MoveableLight l2;
	private char l1MoveMode = 'R';
	private char l2MoveMode = 'R';
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
		numLightsToSpacingMap.put(10, 6);
		numLightsToSpacingMap.put(9, 7);
		numLightsToSpacingMap.put(8, 8);
		numLightsToSpacingMap.put(7, 10);
		numLightsToSpacingMap.put(6, 13);
		numLightsToSpacingMap.put(5, 18);
		numLightsToSpacingMap.put(4, 22);
		numLightsToSpacingMap.put(3, 33);
		
		numLightsToMaxRoundsMap.put(10, 150);
		numLightsToMaxRoundsMap.put(9, 150);
		numLightsToMaxRoundsMap.put(8, 150);
		numLightsToMaxRoundsMap.put(7, 150);
		numLightsToMaxRoundsMap.put(6, 150);
		numLightsToMaxRoundsMap.put(5, 150);
		numLightsToMaxRoundsMap.put(4, 150);
        numLightsToMaxRoundsMap.put(3, 160);
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
	    if(numLights > 2) {
    	    int x = 0; 
    	    int y = numLightsToSpacingMap.get(numLights);
    		lights = new HashSet<Light>();
    		for(int i = 0; i < numLights; i++) {
    		    lastLight = new Point2D.Double(x, y);
    		    y += numLightsToSpacingMap.get(numLights);
    		    MoveableLight l = new MoveableLight(lastLight.getX(), lastLight.getY(), true);
    		    wasMovingUp.put(l, false);
    		    numMovesOffPath.put(l, 0);
    		    lights.add(l);
    		    movementMap.put(l, false);
    		}
	    }
	    else {
	        int x1 = 15;
	        int y1 = 15;
	        
	        int x2 = 36;
	        int y2 = 36;
	        lights = new HashSet<Light>();
	        l1 = new MoveableLight(x1, y1, true);
	        l2 = new MoveableLight(x2, y2, true);
	        lights.add(l1);
	        lights.add(l2);
	    }
	    boolean flag = false;
	    for(int i = 0; i < board.length; i++) {
	        for(int j = 0; j < board[0].length; j++) {
	            flag = false;
	            for(Line2D wall: walls) {
	                if(wall.ptSegDist(i, j) < 2.0) {
	                    flag = true;
	                }
	            }
	            if(!flag) {
	                //Add to the graph
	            }
	        }
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
		
	    numRounds++;
	    
	    if(numLights > 1) {
    	    if(numRounds < 300) {
        		for (Light l : lights) {
        			MoveableLight ml = (MoveableLight) l;
        			boolean hasHitWall = false;
        			for(Line2D obstacle: walls) {
        			    Line2D tempLine = new Line2D.Double(ml.getX(), ml.getY(), ml.getX()+8, ml.getY());
        			    if(obstacle.intersectsLine(tempLine)) {
        			        ml.moveDiag(obstacle, true);
        			        hasHitWall = true;
        			        wasMovingUp.put(ml, true);
        			        lastObstacleEncountered.put(ml, obstacle);
        			        break;
        			    }
        			}
        			if(!wasMovingUp.get(ml)) {
        			    numMovesOffPath.put(ml, 0);
                        ml.moveRight();
        			}
        			else if(!hasHitWall) {
        			    if(numMovesOffPath.get(ml) < 10) {
            			    int numMoves = numMovesOffPath.get(ml);
                            numMoves++;
                            numMovesOffPath.put(ml, numMoves);
                            Line2D obstacle = lastObstacleEncountered.get(ml);
                            ml.moveDiag(obstacle, true);
                            wasMovingUp.put(ml, true);
        			    }
        			    else {
        			        numMovesOffPath.put(ml, 0);
        			        wasMovingUp.put(ml, false);
        			        ml.moveRight();
        			    }
        			}
//        			if(ml.getX() < 10 || !movementMap.get(ml)) {
//        			    ml.moveRight();
//        			    movementMap.put(ml, false);
//        			}
//        			if(board[0].length - ml.getX() < 10 || movementMap.get(ml)) {
//        			    ml.moveLeft();
//        			    movementMap.put(ml, true);
//        			}
        		}
    	    }
//    	    else {
//    	        for(Light l: lights) {
//    	            MoveableLight ml = (MoveableLight) l;
//    	            if(ml.getY() < 50 && ml.getX() > 40 && ml.getX() < 60) {
//    	                ml.moveDown();
//    	            }
//    	            else if(ml.getY() > 50 && ml.getX() > 40 && ml.getX() < 60) {
//    	                ml.moveUp();
//    	            }
//    	            else if(ml.getY() == 50) {
//    	                if(ml.getX() == 50) {
//    	                    continue;
//    	                }
//    	                else if(ml.getX() > 40 && ml.getX() < 50) {
//    	                    ml.moveRight();
//    	                }
//    	                else if(ml.getX() > 50 && ml.getX() < 60) {
//    	                    ml.moveLeft();
//    	                }
//    	            }
//    	            else {
//    	                if(ml.getX() > 50) {
//    	                    ml.moveLeft();
//    	                }
//    	                else {
//    	                    ml.moveRight();
//    	                }
//    	            }
//    	        }
//    	    }
	    }
//	    else {
//	        if(numRounds < 250) {
//    	        twoLightsHelperL1(numRounds, board);
//    	        twoLightsHelperL2(numRounds, board);
//	        }
//	        else {
//    	        if(l1.getX() == 50 && l1.getY() < 50) {
//    	            l1.moveDown();
//    	        }
//    	        else if(l1.getX() == 50 && l1.getY() > 50) {
//    	            l1.moveUp();
//    	        }
//    	        else if(l1.getY() == 50 && l1.getX() < 50) {
//    	            l1.moveRight();
//    	        }
//    	        else if(l1.getY() == 50 && l1.getX() > 50) {
//    	            l1.moveLeft();
//    	        }
//    	        else if(l1.getX() == 50 && l1.getY() == 50) {
//    	            
//    	        }
//    	        else {
//    	            twoLightsHelperL1(numRounds, board);
//    	        }
//    	        
//    	        if(l2.getX() == 50 && l2.getY() < 50) {
//                    l2.moveDown();
//                }
//                else if(l2.getX() == 50 && l2.getY() > 50) {
//                    l2.moveUp();
//                }
//                else if(l2.getY() == 50 && l2.getX() < 50) {
//                    l2.moveRight();
//                }
//                else if(l2.getY() == 50 && l2.getX() > 50) {
//                    l2.moveLeft();
//                }
//                else if(l2.getX() == 50 && l2.getY() == 50) {
//                    
//                }
//                else {
//                    twoLightsHelperL2(numRounds, board);
//                }
//	        }
//	    }
		
		return lights;
	}
	
	public void twoLightsHelperL2(int numRounds, int[][] board) {
        if(l2.getX() == 36 && l2.getY() == (board[0].length - 36)) {
            l2MoveMode = 'U';
            l2.moveUp();
        }
        else if(l2.getX() == 36 && l2.getY() == 36) {
            l2MoveMode = 'R';
            l2.moveRight();
        }
        else if(l2.getX() == (board[0].length - 36) && l2.getY() == 36) {
            l2MoveMode = 'D';
            l2.moveDown();
        }
        else if(l2.getX() == (board[0].length - 36) && l2.getY() == (board.length -36)) {
            l2MoveMode = 'L';
            l2.moveLeft();
        }
        else {
            switch(l2MoveMode) {
            case 'L': l2.moveLeft(); break;
            case 'R': l2.moveRight(); break;
            case 'U': l2.moveUp(); break;
            case 'D': l2.moveDown(); break;
            }
        }
	}
	
	public void twoLightsHelperL1(int numRounds, int[][] board) {
	    if(l1.getX() == 15 && l1.getY() == (board[0].length - 15)) {
            l1MoveMode = 'U';
            l1.moveUp();
        }
        else if(l1.getX() == 15 && l1.getY() == 15) {
            l1MoveMode = 'R';
            l1.moveRight();
        }
        else if(l1.getX() == (board[0].length - 15) && l1.getY() == 15) {
            l1MoveMode = 'D';
            l1.moveDown();
        }
        else if(l1.getX() == (board[0].length - 15) && l1.getY() == (board.length -15)) {
            l1MoveMode = 'L';
            l1.moveLeft();
        }
        else {
            switch(l1MoveMode) {
            case 'L': l1.moveLeft(); break;
            case 'R': l1.moveRight(); break;
            case 'U': l1.moveUp(); break;
            case 'D': l1.moveDown(); break;
            }
        }
	}

	/*
	 * Currently this is only called once (after getLights), so you cannot
	 * move the Collector.
	 */
	@Override
	public Collector getCollector() {
		// this one just places a collector next to the last light that was added
		Collector c = new Collector(100,50);
		return c;
	}

}

