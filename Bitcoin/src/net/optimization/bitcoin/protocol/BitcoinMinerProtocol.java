package net.optimization.bitcoin.protocol;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.optimization.prototype.Block;
import net.optimization.prototype.MaliciousTrans;
import net.optimization.prototype.NetNode;
import net.optimization.prototype.Transaction;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.transport.Transport;

/**
 * 
 * @author Weifeng Hao
 * the main function of the  miner node about the Bitcoin network
 */
public class BitcoinMinerProtocol implements CDProtocol{

	private static final String PAR_MAX_TRANS_BLOCK = "max_trans_block";
	private static final String PAR_REWARD = "reward";
	private static final String PAR_NODE_PROT = "node_protocol";

	private int minedBlocks;
	private boolean selected;
	private int maxTransPerBlock;
	private double reward;
	private int nodeProtocol;	
	FileWriter BlocktimeStats = null;
	BufferedWriter bw = null;

	private static boolean isinit = false;
	
	public BitcoinMinerProtocol(String prefix) {
		minedBlocks = 0;
		maxTransPerBlock = Configuration.getInt(prefix + "." + PAR_MAX_TRANS_BLOCK);
		reward = Configuration.getDouble(prefix + "." + PAR_REWARD);
		nodeProtocol = Configuration.getPid(prefix + "." + PAR_NODE_PROT);
	}
	
	/*
	 * ----------------------------------------------------------------------------
	 * Implement the override method for the node protocol to make cycles and events
	 * -----------------------------------------------------------------------------
	 */
	@Override
	public Object clone() {
		BitcoinMinerProtocol bmp = null;
		try {
			bmp = (BitcoinMinerProtocol)super.clone();
			bmp.setMinedBlocks(0);
			bmp.setSelected(false);
			bmp.setMaxTransPerBlock(maxTransPerBlock);
			bmp.setReward(reward);
			bmp.setNodeProtocol(nodeProtocol);			
		}
		catch(CloneNotSupportedException  e) {
			System.err.println(e);
		}
		return bmp;
		
	}

	@Override
	public void nextCycle(Node node, int arg1) {
		// TODO Auto-generated method stub
		NetNode tnode = (NetNode)node;
		
		if (!tnode.isMiner())
			return;
		
		if (isSelected()) 
		{
			setSelected(false);			
			Map<String, Transaction> transPool = tnode.getTransPool();
			List<Block> blockchain = tnode.getBlockchain();
			// Create a new block and announce it to all the neighbors			
			Block b = createBlock(transPool, tnode, blockchain);	
			blockchain.add(b);
			tnode.increaseBalance(b.getRevenueForBlock()); //the reward for mining the block is given to the miner
			tnode.increaseBalance(b.getTransactionsAmountIfRecipient(tnode));
			sendBlockToNeighbors(node, nodeProtocol, b);				
//			System.out.println(b.getBid()+"Mined a block!");
			double successfulNum=0.0;
			double FalseNum=0.0;
			List<Transaction> transactions = Transaction.transacions;
			for (int i = 0; i < transactions.size(); i++) {
				Transaction t = transactions.get(i);
				if(t.isFalse()==true)
					FalseNum++;
				else 
					successfulNum++;
			}
			Double rateDouble =successfulNum/(successfulNum+FalseNum)*100;
			System.out.println("Mined a block!,and the successful rate is:"+rateDouble+"%"+successfulNum+"|"+FalseNum);
			if(!isinit) {
				try {
					BlocktimeStats = new FileWriter("docs/successfulRate.dat", false);
					bw = new BufferedWriter(BlocktimeStats);
					bw.close();
					isinit=true;
				} catch (IOException e) {
					e.printStackTrace();
					}
				}
				try {
					BlocktimeStats = new FileWriter("docs/successfulRate.dat", true);
					bw = new BufferedWriter(BlocktimeStats);
					bw.write("the successful rate is:"+rateDouble+"% "+successfulNum+"|"+FalseNum + "\n");
					bw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				UpdateTrust();
		}	
	}
	
	/*
	 * --------------------------------------------------------------
	 * Implement the basic node data exchange & data broadcast method.
	 * --------------------------------------------------------------
	 */
	/**
	 * The miner create the block
	 * @param transPool
	 * @param tnode
	 * @param blockchain
	 * @return
	 */
	private Block createBlock(Map<String, Transaction> transPool, NetNode tnode, List<Block> blockchain) {
		minedBlocks++;
		int transInBlock = Math.min(transPool.size(), maxTransPerBlock);
		String bid = "B" + tnode.getID() + minedBlocks;			
		String parent = blockchain.size()== 0 
				? null : blockchain.get(blockchain.size()-1).getBid();
		List<Transaction> trans = new ArrayList<>(transInBlock);
		Iterator<String> iter = tnode.getTransPool().keySet().iterator();
		for (int i=0; i< transInBlock; i++) {
			String key = iter.next();
			Transaction t = transPool.get(key);
			iter.remove();
			trans.add(t);			
		}
		return new Block(bid, parent,trans,  tnode, reward);	
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
	
	/*
	 * -------------------------------------------------------------------------------
	 * Implement the Get & Set method for the basic node data exchange in the protocol.
	 * -------------------------------------------------------------------------------
	 */
	
	public int getMinedBlocks() {
		return minedBlocks;
	}

	public void setMinedBlocks(int minedBlocks) {
		this.minedBlocks = minedBlocks;
	}
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
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

	private void UpdateTrust() {
		List<MaliciousTrans> transacions = Transaction.transacion;
		System.out.println(Transaction.transacion.size());
			for(int i=0;i<Network.size();i++) {
				NetNode node = (NetNode) Network.get(i);
						for (int k = 0; k < transacions.size(); k++) {
							MaliciousTrans t = transacions.get(k);
							if(t.getSrc()==node.getID()) {
								if(node.getNeiboursList().contains(t.getDest())&&node.getNeiboursList().size()>0)
									node.getNeiboursList().remove(t.getDest());
								}
							}
//				if(node.getNeiboursList().size()<20)
//					System.out.println(node.getNeiboursList().size());
				}
			Transaction.transacion.clear();
			}
}
