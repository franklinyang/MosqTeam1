package mosquito.sim;

import java.awt.geom.Line2D;
import java.util.HashSet;
import java.util.Set;

public class MoveableLight extends Light {
	public enum Corner {
		NW, NE, SE, SW
	}
	
	public MoveableLight(double x, double y, double d, double t, double s) {
		super(x, y, d, t, s);
	}
	
	public MoveableLight(double x, double y, boolean on) {
		super(x, y, 0, 0, 0);
		isLightOn = on;
	}
	
	private boolean isLightOn = true;

	@Override
	public boolean isOn(int time) {
		return isLightOn;
	}
	
	public boolean isOn() {
		return isLightOn;
	}
	
	public void turnOn() {
		isLightOn = true;
	}
	
	public void turnOff() {
		isLightOn = false;
	}
	
	public boolean moveUp() {
		if (this.y > 0) {
			if (isLegalMove(this.x, this.y-1)) {
				this.y--;
				return true;
			}
			else return false;
		}
		return false;
	}

	public boolean moveDown() {
		if (this.y < 100) {
			if (isLegalMove(this.x, this.y+1)) {
				this.y++;
				return true;
			}
			else return false;
		}
		return false;
	}

	public boolean moveLeft() {
		if (this.x > 0) {
			if (isLegalMove(this.x-1, this.y)) {
				this.x--;
				return true;
			}
			else return false;
		}
		return false;
	}
	
	public boolean moveRight() {
		if (this.x < 100) {
			if (isLegalMove(this.x+1, this.y)) {
				this.x++;
				return true;
			}
			else return false;
		}
		return false;
	}
	
	public boolean moveDiag(Line2D wall, Corner corner) {
		//This function takes the coordinates of the line that defines the 
		//diagonal slope it should traverse as its first argument
		//up is true if it is traversing upwards and false if it is traversing
		//downwards
		double xDiff = wall.getX1()-wall.getX2();
		double yDiff = wall.getY1()-wall.getY2();
		double dist = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(yDiff, 2));
		double cosT = xDiff/dist;
		double sinT = yDiff/dist;
		double xMove = Math.pow(cosT, 2);
		double yMove = Math.pow(sinT, 2);
		if (corner == Corner.NW || corner == Corner.NE) this.y-=yMove;
		else this.y+=yMove;
		if (corner == Corner.NE || corner == Corner.SE) this.x+=xMove;
		else this.x-=xMove;
		return true;
	}
	
	public boolean goTo(double x, double y) {
		//This function goes directly to the point (x,y)
		Line2D line = new Line2D.Double(this.x, this.y, x, y);
		Corner corner;
		if (this.y>y) {
			if (this.x<x) corner = Corner.NE;
			else corner = Corner.NW;
		} else {
			if (this.x<x) corner = Corner.SE;
			else corner = Corner.SW;
		}
		moveDiag(line, corner);
		return false;
	}
	
	protected boolean isLegalMove(double newX, double newY) {
		Line2D.Double pathLine = new Line2D.Double(this.x,this.y,newX,newY);
		for(Line2D l : Board.walls)
		{
			if(l.intersectsLine(pathLine))
				return false;
		}
		return true;
	}
	
	public boolean moveTo(double newX, double newY) {
		if (!isLegalMove(newX, newY)) return false;
		this.x = newX;
		this.y = newY;
		return true;
	}
}
