package net.optimization.bitcoin.protocol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.optimization.prototype.NetNode;
import net.optimization.prototype.Transaction;
import net.optimization.bitcoin.protocol.BitcoinGeneralNodeProtocol;
import net.optimization.prototype.Block;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

public class BitcoinSelfMiningMinerProtocol implements CDProtocol, EDProtocol{

	private static final String PAR_MAX_TRANS_BLOCK = "max_trans_block";
	private static final String PAR_REWARD = "reward";
	private static final String PAR_NODE_PROT = "node_protocol";

	private int minedBlocks;
	private boolean selected;
	private int maxTransPerBlock;
	private double reward;
	private int nodeProtocol;
	private List<Block> privateBlockchain; 
	private int privateBranchLength;
	
	public BitcoinSelfMiningMinerProtocol(String prefix) {
		minedBlocks = 0;
		maxTransPerBlock = Configuration.getInt(prefix + "." + PAR_MAX_TRANS_BLOCK);
		reward = Configuration.getDouble(prefix + "." + PAR_REWARD);
		nodeProtocol = Configuration.getPid(prefix + "." + PAR_NODE_PROT);
		privateBlockchain = new ArrayList<>();
		privateBranchLength = 0;
	}
	
	/*
	 * ----------------------------------------------------------------------------
	 * Implement the override method for the node protocol to make cycles and events
	 * -----------------------------------------------------------------------------
	 */
	@Override
	public Object clone() {
		BitcoinSelfMiningMinerProtocol bsmp = null;
		try {
			bsmp = (BitcoinSelfMiningMinerProtocol)super.clone();
			bsmp.setMinedBlocks(0);
			bsmp.setSelected(false);
			bsmp.setMaxTransPerBlock(maxTransPerBlock);
			bsmp.setReward(reward);
			bsmp.setNodeProtocol(nodeProtocol);
			bsmp.setPrivateBlockchain(new ArrayList<>());
			bsmp.setPrivateBranchLength(0);			
		}
		catch(CloneNotSupportedException  e) {
			System.err.println(e);
		}
		return bsmp;
		
	}

	@Override
	public void nextCycle(Node node, int pid) {
		// TODO Auto-generated method stub
		if (isSelected()) 
		{
			setSelected(false);
			NetNode tnode = (NetNode)node;
			Map<String, Transaction> transPool = tnode.getTransPool();			
			// Create a new block
			Block b = createBlock(transPool, tnode);
			String last = privateBlockchain.size()==0 ? null : privateBlockchain.get(privateBlockchain.size()-1).getBid();			
			if (isValidBlock(last, b)) {								
				// Announce the block either to the selfish miners or to all the neighbor nodes based on convenience
				List<Block> blockchain = tnode.getBlockchain();
				int prevDifference = privateBlockchain.size() - blockchain.size();
				privateBlockchain.add(b);
				privateBranchLength ++;
				// If there was a fork, publish both blocks of the private branch to win the tie break
				if (prevDifference == 0 && privateBranchLength == 2 ) {
					copyPrivateBlockchain(tnode); 
					for (int i = privateBranchLength; i > 0; i--) 
						sendBlockToNeighbors(node, nodeProtocol, privateBlockchain.get(privateBlockchain.size()-i));
					privateBranchLength = 0;				
				}
				else 
					sendBlockToSelfishMiners(node, pid, b);			
				System.out.println(b.getBid()+"Mined a block!" );
			}			
		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		// TODO Auto-generated method stub
		NetNode tnode = (NetNode)node;
		List<Block> blockchain = tnode.getBlockchain();
		int prevDifference = privateBlockchain.size() - blockchain.size();
		Block b = (Block)event;
		String last = privateBlockchain.size()==0 ? null : privateBlockchain.get(privateBlockchain.size()-1).getBid();
		if (isValidBlock(last, b)) {
			privateBlockchain.add(b);
			privateBranchLength++;
			if (prevDifference == 0 && privateBranchLength == 2 ) {
				copyPrivateBlockchain(tnode);
				for (int i = privateBranchLength; i > 0; i--) 
					sendBlockToNeighbors(node, nodeProtocol, privateBlockchain.get(privateBlockchain.size()-i));
				privateBranchLength = 0;				
			}
			sendBlockToSelfishMiners(node, pid, b);	
		}	
	}
	
	/*
	 * --------------------------------------------------------------
	 * Implement the basic node data exchange & data broadcast method.
	 * --------------------------------------------------------------
	 */
	/**
	 * 
	 * @param transPool
	 * @param tnode
	 * @return
	 */
	private Block createBlock(Map<String, Transaction> transPool, NetNode tnode) {
		minedBlocks++;
		int transInBlock = Math.min(transPool.size(), maxTransPerBlock);
		String bid = "B" + tnode.getID() + minedBlocks;			
		String parent = privateBlockchain.size()== 0 
				? null : privateBlockchain.get(privateBlockchain.size()-1).getBid();
		List<Transaction> trans = new ArrayList<>(transInBlock);		
		Iterator<String> iter = tnode.getTransPool().keySet().iterator();
		for (int i=0; i< transInBlock; i++) {
			String key = iter.next();
			Transaction t = transPool.get(key);
			iter.remove();
			trans.add(t);
			if (t.getOutput() == tnode) { 
				tnode.increaseBalance(t.getAmount());
			}
		}
		return new Block(bid, parent,trans,  tnode, reward);	
	}
	
	/**
	 * 
	 * @param last
	 * @param toBeAdded
	 * @return
	 */
	public boolean isValidBlock(String last, Block toBeAdded) {
		if ( last != toBeAdded.getParent()) {
			try {
				throw new Exception("Parent node of the new block is different from the last"
						+ "node of the blockchain");
			} catch (Exception e) {				
				return false;
			}
		}
		return true;
	}	
	
	/**
	 * 
	 * @param tnode
	 */
	public void copyPrivateBlockchain(NetNode tnode) {
		List<Block> blockchain = tnode.getBlockchain();
		Block last = blockchain.get(blockchain.size() - 1);
		blockchain.remove(last); //remove last item  
		tnode.decreaseBalance(last.getTransactionsAmountIfRecipient(tnode));
		((BitcoinGeneralNodeProtocol)tnode.getProtocol(nodeProtocol)).addTransactionsToPool(tnode, last);
		if (tnode == last.getMiner())
			tnode.decreaseBalance(last.getRevenueForBlock()); //remove block reward			
		for (int i = privateBranchLength; i > 0; i--) {
			Block b = privateBlockchain.get(privateBlockchain.size() - i);
			blockchain.add(b); 
			((BitcoinGeneralNodeProtocol)tnode.getProtocol(nodeProtocol)).removeTransactionsFromPool(tnode, b);
			tnode.increaseBalance(b.getTransactionsAmountIfRecipient(tnode));
			if (tnode == b.getMiner())
				tnode.increaseBalance(b.getRevenueForBlock()); //get reward for the added block
		}
	}

	/**
	 * Update the private blockchain to be a copy of the public one, discarding the last item of the private one
	 * @param tnode
	 */
	public void copyPublicBlockchain(NetNode tnode) {
		List<Block> blockchain = tnode.getBlockchain();
		if (privateBlockchain.size() != 0)
			privateBlockchain.remove(privateBlockchain.size() - 1); //remove last item
		for (int i = privateBranchLength; i >= 0; i--) {
			Block b = blockchain.get(blockchain.size() - (i+1));
			privateBlockchain.add(b);
		}
	}	
	
	
	/** 
	 * Sends a block b to the protocol pid of all the neighbor nodes
	 * @param sender The sender node
	 * @param pid The id of the protocol the message is directed to
	 * @param b The block to be sent
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
	 * @param sender The sender node
	 * @param pid The id of the protocol the message is directed to
	 * @param b The block to be sent
	 */
	public void sendBlockToSelfishMiners(Node sender, int pid, Block b) {		
			long time = 0;
			int linkableID = FastConfig.getLinkable(pid);
			Linkable linkable = (Linkable) sender.getProtocol(linkableID);
			for (int i = 0; i < linkable.degree(); i++) {
				Node peer = linkable.getNeighbor(i);
				if ( ((NetNode)peer).isSelfmingMiner())
				((Transport) sender.getProtocol(FastConfig.getTransport(pid))).send(sender, peer, b, pid);
				time += ((Transport) sender.getProtocol(FastConfig.getTransport(pid))).getLatency(sender, peer);
			}
			b.AddBroadcastTime(time);
	}
	
	
	/*
	 * -------------------------------------------------------------------------------
	 * Implement the Get & Set method for the basic node data exchange in the protocol.
	 * -------------------------------------------------------------------------------
	 */
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	public void setPrivateBranchLength(int privateBranchLength) {
		this.privateBranchLength = privateBranchLength;
	}
	
	public void setPrivateBlockchain(List<Block> privateBlockchain) {
		this.privateBlockchain = privateBlockchain;
	}
	
	public void setMaxTransPerBlock(int maxTransPerBlock) {
		this.maxTransPerBlock = maxTransPerBlock;
	}

	public void setReward(double reward) {
		this.reward = reward;
	}

	public void setNodeProtocol(int nodeProtocol) {
		this.nodeProtocol = nodeProtocol;
	}

	public int getMinedBlocks() {
		return minedBlocks;
	}

	public void setMinedBlocks(int minedBlocks) {
		this.minedBlocks = minedBlocks;
	}
	
	public List<Block> getPrivateBlockchain() {
		return privateBlockchain;
	}

	public int getPrivateBranchLength() {
		return privateBranchLength;
	}
}
