package mosquito.g0;

import java.util.ArrayList;

public class Section {
	  int[] boolCombo;
	  ArrayList<Integer> xPoints;
	  ArrayList<Integer> yPoints;
	  int maxX;
	  int minX;
	  int maxY;
	  int minY;
	  int midX;
	  int midY;
	  
	  boolean visited = false;
	  
	  public Section(int size) {
		  boolCombo = new int[size];
		  xPoints = new ArrayList<Integer>();
		  yPoints = new ArrayList<Integer>();
	  }
	  //boolean through; //describes if there are multiple entrances into the section;
	  void printDetails() {
//		  log.debug("boolCombo: " + Arrays.toString(boolCombo));
		  //  log.trace("Area: " + this.area + ", endpoints: " + Arrays.toString(this.endpoints) + ", through? " + through);
	  }
	  
	  void setMidpoints() {
		  int sumX = 0;
		  int sumY = 0;
		  int len = xPoints.size();
		  for (int i = 0; i < len; i++) {
			  sumX += xPoints.get(i);
			  sumY += yPoints.get(i);
		  }
		  this.midX = sumX/len;
		  this.midY = sumY/len;
	  }
}
