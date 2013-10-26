package mosquito.g0;

import java.util.ArrayList;

public class Node implements Comparable<Node> {
	/* Nodes that this is connected to */
	AreaMap map;
	Node north;
	Node east;
	Node south;
	Node west;
	ArrayList<Node> neighborList;
	boolean visited;
	float distanceFromStart;
	double heuristicDistanceFromGoal;
	Node previousNode;
	int x;
	int y;
	boolean isObstacle;
	boolean isStart;
	boolean isGoal;
	
	Node(int x, int y) {
		neighborList = new ArrayList<Node>();
		this.x = x;
		this.y = y;
		this.visited = false;
		this.distanceFromStart = Integer.MAX_VALUE;
		this.isObstacle = false;
		this.isStart = false;
		this.isGoal = false;
	}
	
	Node (int x, int y, boolean visited, int distanceFromStart, boolean isObstical, boolean isStart, boolean isGoal) {
		neighborList = new ArrayList<Node>();
		this.x = x;
		this.y = y;
		this.visited = visited;
		this.distanceFromStart = distanceFromStart;
		this.isObstacle = isObstical;
		this.isStart = isStart;
		this.isGoal = isGoal;
	}
	
	public Node getNorth() {
		return north;
	}

	public void setNorth(Node north) {
		//replace the old Node with the new one in the neighborList
		if (neighborList.contains(this.north))
			neighborList.remove(this.north);
		neighborList.add(north);
		
		//set the new Node
		this.north = north;
	}

	public Node getEast() {
		return east;
	}

	public void setEast(Node east) {
		//replace the old Node with the new one in the neighborList
		if (neighborList.contains(this.east))
			neighborList.remove(this.east);
		neighborList.add(east);
		
		//set the new Node
		this.east = east;
	}

	public Node getSouth() {
		return south;
	}

	public void setSouth(Node south) {
		//replace the old Node with the new one in the neighborList
		if (neighborList.contains(this.south))
			neighborList.remove(this.south);
		neighborList.add(south);
		
		//set the new Node
		this.south = south;
	}

	public Node getWest() {
		return west;
	}

	public void setWest(Node west) {
		//replace the old Node with the new one in the neighborList
		if (neighborList.contains(this.west))
			neighborList.remove(this.west);
		neighborList.add(west);
		
		//set the new Node
		this.west = west;
	}

	
	public ArrayList<Node> getNeighborList() {
		return neighborList;
	}

	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public float getDistanceFromStart() {
		return distanceFromStart;
	}

	public void setDistanceFromStart(float f) {
		this.distanceFromStart = f;
	}

	public Node getPreviousNode() {
		return previousNode;
	}

	public void setPreviousNode(Node previousNode) {
		this.previousNode = previousNode;
	}
	
	public double getHeuristicDistanceFromGoal() {
		return heuristicDistanceFromGoal;
	}

	public void setHeuristicDistanceFromGoal(double d) {
		this.heuristicDistanceFromGoal = d;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	public boolean isObstical() {
		return isObstacle;
	}

	public void setObstical(boolean isObstical) {
		this.isObstacle = isObstical;
	}

	public boolean isStart() {
		return isStart;
	}

	public void setStart(boolean isStart) {
		this.isStart = isStart;
	}

	public boolean isGoal() {
		return isGoal;
	}

	public void setGoal(boolean isGoal) {
		this.isGoal = isGoal;
	}

	public boolean equals(Node node) {
		return (node.x == x) && (node.y == y);
	}

	public int compareTo(Node otherNode) {
		double thisTotalDistanceFromGoal = heuristicDistanceFromGoal + distanceFromStart;
		double otherTotalDistanceFromGoal = otherNode.getHeuristicDistanceFromGoal() + otherNode.getDistanceFromStart();
		
		if (thisTotalDistanceFromGoal < otherTotalDistanceFromGoal) {
			return -1;
		} else if (thisTotalDistanceFromGoal > otherTotalDistanceFromGoal) {
			return 1;
		} else {
			return 0;
		}
	}
}