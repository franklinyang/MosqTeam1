package mosquito.g0;

public class FHeuristic implements AStarHeuristic {
	
	public FHeuristic(){}
	
	@Override
	public double getEstimatedDistanceToGoal(double startX, double startY,
			double goalX, double goalY) {
		double diffx = goalX - startX;
		double diffy = goalY - startY;
		return Math.sqrt(Math.pow(diffy,2) + Math.pow(diffx,2));
	}

}
