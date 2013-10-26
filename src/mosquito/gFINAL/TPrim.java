package mosquito.gFINAL;

public class TPrim {
	 public static int[] Prim(WeightedGraph G) {
	    int[] orderedPoints = new int[G.size()];
		int[] source = new int[G.size()]; // i-th element contains number of source vertex for the edge with the lowest cost from tree T to vertex i
	    int[] dist = new int[G.size()]; //i-th element contains weight of minimal edge connecting i with source[i] 
	    boolean[] visited = new boolean[G.size()];  //if true, vertex i is in tree T

	    // Mark all vertices as NOT being in the minimum spanning tree
	    for (int i = 0; i < G.size(); i++) {
	        visited[i] = false;
	        dist[i] = Integer.MAX_VALUE;
	    }

	     //we start with vertex number 0
	    visited[0] = true;
	    dist[0] = 0;
	    int bestNeighbour = 0;// lastly added vertex to the tree T 
	    int minDist; 

	    for (int i = 0; i < G.size() - 1; i++) {
	        minDist = Integer.MAX_VALUE;
	        
	        for (int j = 0; j < G.size(); j++) {  // fill dist[] based on distance to bestNeighbour vertex
	            if (!visited[j]) {
	                int weight = G.getWeight(bestNeighbour, j);  //TODO: bestNeighbor only looks at most recent vertex
	                if (weight < dist[j]) {
	                    source[j] = bestNeighbour;
	                    dist[j] = weight;
	                }
	            }
	        }

	        for (int j = 0; j < G.size(); j++) {	// find index of min in dist[]
	            if (!visited[j]) {
	                if (dist[j] < minDist) {
	                    bestNeighbour = j;
	                    minDist = dist[j];
	                }
	            }
	        }
	        if (bestNeighbour != 0)
	        {//add the edge (bestNeighbour, dist[bestNeighbour]) to tree T
	            orderedPoints[i]=bestNeighbour;
	            visited[bestNeighbour] = true;
	        }
	    }
	    return orderedPoints;
	}
	

}