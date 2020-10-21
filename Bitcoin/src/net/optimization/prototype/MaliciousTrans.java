package net.optimization.prototype;


/**
 * 
 * @author Weifeng Hao
 * Write for the basic blockchain data structure(transaction)
 *
 */
public class MaliciousTrans {
	private long src;
	private long dest;
	public MaliciousTrans(long src, long dest) {
		super();
		this.src = src;
		this.dest = dest;
	}
	public long getSrc() {
		return src;
	}
	public void setSrc(long src) {
		this.src = src;
	}
	public long getDest() {
		return dest;
	}
	public void setDest(long dest) {
		this.dest = dest;
	}
	
}
