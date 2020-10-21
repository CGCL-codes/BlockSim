package net.optimization.prototype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import peersim.core.*;

/**
 * 
 * @author Weifeng Hao
 * the blockchain network node
 */
public class NetNode extends GeneralNode{
    private NodeType nodeType; // the type of the node(include: general_node,miner,self-mining miner)
    private MinerType minerType; //the type of the miner(include:CPU,GPU,FPGA,ASIC)
    private double balance; // the balance of the node
    private List<Block> blockchain; // the blockchain data of the node
    private Map<String, Transaction> transPool; // the transpool of the node
    private Map<String, Block> blockPool;
    private Map<String, Block> BlockReceived;
    private List<Long> neiboursList;
    private NodeType2 nodeType2;
	public NetNode(String prefix) {
		super(prefix);
		// TODO Auto-generated constructor stub
		setBlockReceived(new HashMap<>());
		blockchain = new ArrayList<>();
    	transPool = new HashMap<>();
    	blockPool = new HashMap<>();
    	setNeiboursList(new ArrayList<>());
	}

    @Override
    public Object clone()
    {
    	NetNode clone = (NetNode)super.clone();
    	clone.setTransPool(new HashMap<>());
    	clone.setBlockchain(new ArrayList<>());
    	clone.setNeiboursList(new ArrayList<>());
    	return clone;
    }
	// node type selection
	public boolean isGeneralNode() {
		return nodeType==NodeType.GENERAL_NODE;
	}
	
	public boolean isMiner() {
		return nodeType==NodeType.MINER;
	}
	
	public boolean isSelfmingMiner() {
		return nodeType==NodeType.SELFMINING_MINER;
	}	
	
	public void setNodetype(NodeType ntype) {
		this.nodeType = ntype;
	}
	
	// miner type selection
	public MinerType getMtype() {
		return minerType;
	}

	public void setMtype(MinerType mtype) {
		this.minerType = mtype;
	}

	// balance type selection
	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}
	
	public void increaseBalance(double amount) {
		balance += amount;
	}
	
	public void decreaseBalance(double amount) {
		balance -= amount;
	}	

	// blockchain type selection
	public List<Block> getBlockchain() {
		return blockchain;
	}

	public void setBlockchain(List<Block> blockchain) {
		this.blockchain = blockchain;
	}
	
	// transaction type selection 
	public Map<String, Transaction> getTransPool() {
		return transPool;
	}
	
	public void setTransPool(Map<String, Transaction> transPool) {
		this.transPool = transPool;		
	}

	public Map<String, Block> getBlockPool() {
		return blockPool;
	}

	public void setBlockPool(Map<String, Block> blockPool) {
		this.blockPool = blockPool;
	}

	public Map<String, Block> getBlockReceived() {
		return BlockReceived;
	}

	public void setBlockReceived(Map<String, Block> blockReceived) {
		BlockReceived = blockReceived;
	}

	public NodeType2 getNodeType2() {
		return nodeType2;
	}

	public void setNodeType2(NodeType2 nodeType2) {
		this.nodeType2 = nodeType2;
	}

	public List<Long> getNeiboursList() {
		return neiboursList;
	}

	public void setNeiboursList(List<Long> neiboursList) {
		this.neiboursList = neiboursList;
	}
}
