package net.optimization.prototype;

public class Flag {
	private int id;
	private boolean isReach;
	private static int cycle=0;
	public Flag(int id){
		this.id=id;
		this.isReach=false;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean isReach() {
		return isReach;
	}
	public void setReach(boolean isReach) {
		this.isReach = isReach;
	}
	public static int getCycle() {
		return cycle;
	}
	public static void setCycle(int cycle) {
		Flag.cycle = cycle;
	}
	public static void addCycle() {
		Flag.cycle=cycle+1;
	}
}
