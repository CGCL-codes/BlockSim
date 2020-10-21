package net.optimization.prototype;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Weifeng Hao
 * Write for the basic blockchain data structure(transaction)
 *
 */
public class Transaction {
	private final String tid; // the id of the transaction
	private final NetNode input; // the input of the transaction
	private final NetNode output; // the output of the transaction
	private final double amount; // the balance of the transaction 
	private final double fee; // the fee 
	private final boolean isFalse; // the fee 
	private int SyncTimes=0;
	private long BroadcastTime=0;
	public boolean init=false;
	public List<Flag> FlagList=new ArrayList<>();
	public long src;
	public long dest;
	public static List<Transaction> transacions=new ArrayList<Transaction>();
	public static List<MaliciousTrans> transacion=new ArrayList<MaliciousTrans>();
	public Transaction(String id, NetNode input, NetNode output, double amount, double fee ,boolean isFalse) {
		tid = id;
		this.input = input;
		this.output = output;
		this.amount = amount;
		this.fee = fee;
		this.isFalse = isFalse;
	}
	
	public String getTid() {
		return tid;
	}
	public int getSyncTimes() {
		return SyncTimes;
	}
	public void AddBroadcastTime(long l)
	{
		this.BroadcastTime+=l;
	}
	public long getBroadcastTime()
	{
		return BroadcastTime;
	}
	public void AddTimes() {
		this.SyncTimes ++;
	}
	public NetNode getOutput() {
		return output;
	}
	
	public double getAmount() {
		return amount;
	}

	public double getFee() {
		return fee;
	}
	
	@Override
	public String toString() {
		return "Transaction " + tid + ": Source = " + input.getID() + ", Destination = " +
				output.getID() + ", amount = " + amount + ", fee = " + fee;
	}

	public boolean isFalse() {
		return isFalse;
	}

}
