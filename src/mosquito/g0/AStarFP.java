package mosquito.g0;

import mosquito.g0.AStarShortestPath.AStarFunctionProvider;
import java.awt.geom.Point2D.Double;

public class AStarFP<Point2D> implements AStarFunctionProvider<Point2D> {

	@Override
	public double getHeuristicCost(Point2D start, Point2D goal) {
		// TODO Auto-generated method stub		
		return 0;
	}

	@Override
	public double getPathCost(Point2D neighbor, Point2D goal) {
		// TODO Auto-generated method stub
		return 0;
	}
	
}