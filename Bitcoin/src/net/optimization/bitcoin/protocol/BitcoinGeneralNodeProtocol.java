package net.optimization.bitcoin.protocol;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.sun.nio.sctp.SendFailedNotification;

import net.optimization.prototype.NetNode;
import net.optimization.prototype.NodeType2;
import net.optimization.prototype.Transaction;
import net.optimization.prototype.Block;
import net.optimization.prototype.Flag;
import net.optimization.prototype.MaliciousTrans;
import peersim.cdsim.CDProtocol;
import peersim.cdsim.CDState;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

/**
 * 
 * @author Weifeng Hao the main function of the general node about the Bitcoin
 *         network
 */
public class BitcoinGeneralNodeProtocol implements CDProtocol, EDProtocol {

	private static final String PAR_P_TRANS = "transaction_prob";
	private static final String PAR_SyncRate = "syncrate";
	private double transProb;
	private int numTrans;
	private boolean fork;
	private Block forked;
	private int numForks;
	private List<Block> missedBlocks;
	private int limit;
	private int syncrate;
	private static int cycle;
	private int times;
	private static int cycleIncrease;
	private double x, y;
	FileWriter BlocktimeStats = null;
	FileWriter TranstimeStats = null;
	BufferedWriter bw = null;
	private static boolean init = false;
	private static boolean isinitC = false;
	private static boolean isinit = false;
	private int size;
	private static long time = 0;
	public BitcoinGeneralNodeProtocol(String prefix) {
		transProb = Configuration.getDouble(prefix + "." + PAR_P_TRANS);
		numTrans = 0;
		syncrate = Configuration.getInt(prefix + "." + PAR_SyncRate);
		fork = false;
		forked = null;
		numForks = 0;
		missedBlocks = new ArrayList<>();
		limit = 20;
		
	}

	/*
	 * ----------------------------------------------------------------------------
	 * Implement the override method for the node protocol to make cycles and events
	 * -----------------------------------------------------------------------------
	 */
	@Override
	public Object clone() {
		BitcoinGeneralNodeProtocol node = null;

		try {
			node = (BitcoinGeneralNodeProtocol) super.clone();
			node.setTransProb(transProb);
			node.setNumTrans(0);
			node.setFork(false);
			node.setForked(null);
			node.setNumForks(0);
			node.setMissedBlocks(new ArrayList<>());
			node.setLimit(limit);
			x = setY(-1);
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return node;
	}

	@Override
	public void nextCycle(Node node, int pid) {
		if(!isinitC) {
		for (int i=0; i< Network.size(); i++) {
		NetNode node1 = (NetNode)Network.get(i);
		int linkableID = FastConfig.getLinkable(pid);
		Linkable linkable = (Linkable) node1.getProtocol(linkableID);
		for (int j = 0; j < linkable.degree(); j++) {
			Node peer = linkable.getNeighbor(j);
			node1.getNeiboursList().add(peer.getID());
				}
			}
		isinitC=true;
		}
		// TODO Auto-generated method stub
		NetNode tnode = (NetNode) node;
		double balance = tnode.getBalance();
		// I assume that if a node has less than 1 coin cannot make a transaction
		// (Substitutes the test for empty balance, and allows to avoid very small,
		// fractional transactions)
		if (balance < 1) {
			return;
		}
		double r = Math.random();
		// At each cycle, each node generates a transaction with a given probability
		if (r < transProb) {
			String tid = node.getID() + "@" + numTrans;
			numTrans++;
			// Randomly choose one recipient
			Network.shuffle();
			NetNode recipient = (NetNode) Network.get(0);
			double totalSpent = Math.random() * balance;
			double amount = totalSpent * (9.0 / 10.0);
			double fee = totalSpent - amount;
			double rd = Math.random();
			boolean isFalse=false;
			if(rd<0.1||tnode.getNodeType2()==NodeType2.MALICIOUS_NODE) {
				isFalse=true;
			}
			Transaction t = new Transaction(tid, tnode, recipient, amount, fee,isFalse);
			// System.out.println(t.toString());
			// Transaction has been created, so update balance and insert into local pool of
			// unconfirmed transactions
			tnode.getTransPool().put(tid, t);
			balance -= totalSpent;
			// Send the transaction to all neighbor nodes
			sendTransactionToNeighbors(node, pid, t);
		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
	// TODO Auto-generated method stub
		if (!isinit) {
			for (int k = 10; k <= 300; k += 10) {
				try {
					BlocktimeStats = new FileWriter("docs/time" + "_" + k + ".dat", false);
					bw = new BufferedWriter(BlocktimeStats);
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			isinit = true;
		}
		cycle = Flag.getCycle();
		if (!init) {
			size = Network.size();
			times = 0;
			cycleIncrease = cycle + 1;
			System.out.println("cycle:" + cycle);
			init = true;
		}
		if (cycle == cycleIncrease) {
			init = false;
		}
		
		if (event instanceof Transaction) {			
			Transaction t = (Transaction) event;
			Map<String, Transaction> transPool = ((NetNode) node).getTransPool();
			String tid = t.getTid();
			// If never received the transaction, broadcast it to the neighbors
			if (!transPool.containsKey(tid)) {
				t.AddTimes();
				if(!Transaction.transacions.contains(t))
				Transaction.transacions.add(t);
				transPool.put(tid, t);
//				if(t.isFalse()==false)
				t.AddBroadcastTime(sendTransactionToNeighbors(node, pid, t));
				//System.out.println(tid);
			}
			
			if (times < 30) {
				try {
					float size = Network.size();
					float percentage = t.getSyncTimes() / size * 100;
					if (!t.init) {
						for (int k = 10; k <= 300; k += 10) {
							t.FlagList.add(new Flag(k));
						}
						t.init = true;
					}
					int j = 0;
					for (int i = 10; i <= 300; i += 10) {
						if (t.getBroadcastTime() > i * 1000000) {
							if (!t.FlagList.get(j).isReach()) {
								BlocktimeStats = new FileWriter("docs/time" + "_" + i + ".dat", true);
								bw = new BufferedWriter(BlocktimeStats);
								bw.write(percentage + "\n");
								// System.out.println("block "+t.getTid()+" time reach
								// "+i+"ms"+"!Percentageis:"+percentage+"%!"+t.getSyncTimes());
								bw.close();
								t.FlagList.get(j).setReach(true);
							}
						}
						j++;
					}
				} catch (IOException e) {
					System.err.println(e);
				}
				times++;
			}
			
		} else if (event instanceof Block) {
			NetNode tnode = (NetNode) node;
			List<Block> blockchain = tnode.getBlockchain();
			Block b = (Block) event;
			Map<String, Block> BlockReceived = ((NetNode) node).getBlockReceived();
			String Bid = b.getBid();
			if (!BlockReceived.containsKey(Bid)) {
				BlockReceived.put(Bid, b);
			}
			String last = blockchain.size() == 0 ? null : blockchain.get(blockchain.size() - 1).getBid();
			if (tnode.isSelfmingMiner()) {
				
			}  else {
				// If the parent field of the block is valid, then the honest node adds the
				// block
				// to its blockchain and removes the transactions inside the block from the
				// pool.
				if (last == b.getParent() || (fork == true && forked.getBid() == b.getParent())) {
					if (fork == true) {
						if (forked.getBid() == b.getParent()) {
							Block lastb = blockchain.get(blockchain.size() - 1);
							blockchain.remove(lastb);
							addTransactionsToPool(tnode, lastb);
							tnode.decreaseBalance(lastb.getTransactionsAmountIfRecipient(tnode));
							if (tnode == lastb.getMiner())
								tnode.decreaseBalance(lastb.getRevenueForBlock());
							blockchain.add(forked);
							// No need to add the revenue for mining the block, because a honest miner
							// always
							// takes the revenue as soon as it mines the block
							tnode.increaseBalance(forked.getTransactionsAmountIfRecipient(tnode));
						}
						fork = false; // Fork is resolved, regardless of which is the extended branch
						forked = null;
					}
					blockchain.add(b);
					b.AddSyncTimes();
					if (!missedBlocks.isEmpty())
						attachMissedBlocksToBlockchain(tnode);
					tnode.increaseBalance(b.getTransactionsAmountIfRecipient(tnode));
					removeTransactionsFromPool(tnode, b);

					// Finally (if block is valid) send the block to all the neighbor nodes
					sendBlockToNeighbors(node, pid, b);
				} else if (blockchain.size() >= 2 && blockchain.get(blockchain.size() - 2).getBid() == b.getParent()
						&& blockchain.get(blockchain.size() - 1).getBid() != b.getBid() && fork == false) {
					fork = true;
					forked = b;
					numForks++;
					sendBlockToNeighbors(node, pid, b);
					solveForkWithMissedBlocks(tnode);
				} else if (last != b.getParent())
					addMissedBlockToPool(b);
			}

		}
	}
	/*
	 * -------------------------------------------------------------- Implement the
	 * basic node data exchange & data broadcast method.
	 * --------------------------------------------------------------
	 */

	/**
	 * add the transactions to the miner pool
	 * 
	 * @param tn The basic netnode
	 * @param b  The basic block
	 */
	public void addTransactionsToPool(NetNode tn, Block b) {
		Map<String, Transaction> transPool = tn.getTransPool();
		// add the transactions belong to the block to the transpool
		for (Transaction t : b.getTransactions()) {
			transPool.putIfAbsent(t.getTid(), t);
		}
	}

	/**
	 * remove the transactions from the miner pool
	 * 
	 * @param tn
	 * @param b
	 */
	public void removeTransactionsFromPool(NetNode tn, Block b) {
		Map<String, Transaction> transPool = tn.getTransPool();
		// remove the transactions belong to the block from the transpool
		for (Transaction t : b.getTransactions()) {
			transPool.remove(t.getTid());
		}
	}

	/**
	 * Sends a transaction t to the protocol pid of all the neighbor nodes
	 * 
	 * @param sender The sender node
	 * @param pid    The id of the protocol the message is directed to
	 * @param t      The transaction to be sent
	 */
	public long sendTransactionToNeighbors(Node sender, int pid, Transaction t) {
		long time = 0;
		int linkableID = FastConfig.getLinkable(pid);
		Linkable linkable = (Linkable) sender.getProtocol(linkableID);
		NetNode node = (NetNode)sender;
		for (int i = 0; i < linkable.degree(); i++) {
			Node peer = linkable.getNeighbor(i);
			if(node.getNeiboursList().contains(peer.getID())) {
				t.src=node.getID();
				t.dest=peer.getID();
				MaliciousTrans mTrans = new MaliciousTrans(node.getID(),peer.getID());
				if(t.isFalse())
					Transaction.transacion.add(mTrans);
			((Transport) sender.getProtocol(FastConfig.getTransport(pid))).send(sender, peer, t, pid);
			time += ((Transport)sender.getProtocol(FastConfig.getTransport(pid))).getLatency(sender, peer);
			}
		}
		return time;
	}


	/**
	 * Sends a block b to the protocol pid of all the neighbor nodes
	 * 
	 * @param sender The sender node
	 * @param pid    The id of the protocol the message is directed to
	 * @param b      The block to be sent
	 */
	public void sendBlockToNeighbors(Node sender, int pid, Block b) {
		long time = 0;
		int linkableID = FastConfig.getLinkable(pid);
		Linkable linkable = (Linkable) sender.getProtocol(linkableID);
		for (int i = 0; i < linkable.degree(); i++) {
			Node peer = linkable.getNeighbor(i);
			((Transport) sender.getProtocol(FastConfig.getTransport(pid))).send(sender, peer, b, pid);
			time += ((Transport) sender.getProtocol(FastConfig.getTransport(pid))).getLatency(sender, peer);
		}
		b.AddBroadcastTime(time);
	}

	/**
	 * 
	 * @param tn
	 * @return
	 */
	public boolean solveForkWithMissedBlocks(NetNode tn) {
		List<Block> blockchain = tn.getBlockchain();
		for (int i = 0; i < missedBlocks.size(); i++) {
			if (missedBlocks.get(i).getParent() == forked.getBid()) {
				Block lastb = blockchain.get(blockchain.size() - 1);
				blockchain.remove(lastb);
				tn.decreaseBalance(lastb.getTransactionsAmountIfRecipient(tn));
				addTransactionsToPool(tn, lastb);
				blockchain.add(forked);
				Block head = missedBlocks.remove(i);
				blockchain.add(head);
				removeTransactionsFromPool(tn, head);
				tn.increaseBalance(head.getTransactionsAmountIfRecipient(tn));
				fork = false;
				forked = null;
				attachMissedBlocksToBlockchain(tn);
				return true;
			}
		}
		return false;
	}

	/**
	 * Scans the list of missed blocks trying to find some blocks that can be
	 * attached to the head of the blockchain
	 * 
	 * @param tn
	 */
	public void attachMissedBlocksToBlockchain(NetNode tn) {
		List<Block> blockchain = tn.getBlockchain();
		Block head = blockchain.get(blockchain.size() - 1);
		int i = 0;
		while (i < missedBlocks.size()) {
			if (missedBlocks.get(i).getParent() == head.getBid()) {
				head = missedBlocks.remove(i);
				blockchain.add(head);
				removeTransactionsFromPool(tn, head);
				tn.increaseBalance(head.getTransactionsAmountIfRecipient(tn));
				i = 0; // The head of the blockchain changed, so we restart scanning the missed blocks
			} else
				i++;
		}
	}

	/**
	 * 
	 * @param missed
	 */
	public void addMissedBlockToPool(Block missed) {
		if (missedBlocks.size() == limit) // If reached the limit, empty it
			missedBlocks.removeAll(missedBlocks);
		if (!missedBlocks.contains(missed))
			missedBlocks.add(missed);
	}

	/**
	 * 
	 * @param privateBlockchain
	 * @param blockchain
	 * @return
	 */
	private boolean onlyAddTheBlock(List<Block> privateBlockchain, List<Block> blockchain) {
		if (privateBlockchain.size() == 0 || blockchain.get(blockchain.size() - 1).getParent() == privateBlockchain
				.get(privateBlockchain.size() - 1).getBid())
			return true;
		else
			return false;
	}

	/*
	 * -----------------------------------------------------------------------------
	 * -- Implement the Get & Set method for the basic node data exchange in the
	 * protocol.
	 * -----------------------------------------------------------------------------
	 * --
	 */
	public double getTransProb() {
		return transProb;
	}

	public void setTransProb(double transProb) {
		this.transProb = transProb;
	}

	public void setNumTrans(int numTrans) {
		this.numTrans = numTrans;
	}



	public void setFork(boolean fork) {
		this.fork = fork;
	}

	public void setForked(Block forked) {
		this.forked = forked;
	}

	public int getNumForks() {
		return numForks;
	}

	public void setNumForks(int numForks) {
		this.numForks = numForks;
	}

	public void setMissedBlocks(List<Block> missedBlocks) {
		this.missedBlocks = missedBlocks;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public double setY(double y) {
		this.y = y;
		return y;
	}

}
