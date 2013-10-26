package mosquito.gFINAL;

import org.apache.log4j.Logger;

public class WeightedGraph {
	
     public int [][]  edges;  // adjacency matrix
 	 private Logger log = Logger.getLogger(this.getClass()); // for logging
 	
     public WeightedGraph (int n) {
        edges  = new int [n][n];
     }
  
  
     public int size() { return edges.length; }
  
     public void    addEdge    (int source, int target, int w)  { 
//    	 log.error("adding edge: (" + source + "," + target + ") with weight: " + w);
    	 edges[source][target] = w; }
     public boolean isEdge     (int source, int target)  { return edges[source][target]>0; }
     public void    removeEdge (int source, int target)  { edges[source][target] = 0; }
     public int     getWeight  (int source, int target)  { return edges[source][target]; }
  
     public int [] neighbors (int vertex) {
        int count = 0;
        for (int i=0; i<edges[vertex].length; i++) {
           if (edges[vertex][i]>0) count++;
        }
        final int[]answer= new int[count];
        count = 0;
        for (int i=0; i<edges[vertex].length; i++) {
           if (edges[vertex][i]>0) answer[count++]=i;
        }
        return answer;
     }
  
     public void print () {
        for (int j=0; j<edges.length; j++) {
        	String output = "";
        	for (int i=0; i<edges.length; i++) {
        		output = output + " , " + edges[i][j];
        	}
           log.error(output);
        }
     }
  }
