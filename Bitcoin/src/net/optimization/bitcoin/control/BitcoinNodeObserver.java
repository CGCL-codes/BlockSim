package net.optimization.bitcoin.control;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import net.optimization.bitcoin.protocol.*;
import net.optimization.prototype.*;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class BitcoinNodeObserver implements Control {
	
	private static final String PAR_NODE_PROT = "node_protocol";
	private static final String PAR_MINER_PROT = "miner_protocol";
	private static final String PAR_SMINER_PROT = "selfish_miner_protocol";
	private static final String PAR_REPETITION = "repetition";
	private static final String PAR_SMINER = "p_self_miner";
	private static final String PAR_HRCPU = "hr_cpu";
	private static final String PAR_HRGPU = "hr_gpu";
	private static final String PAR_HRFPGA = "hr_fpga";
	private static final String PAR_HRASIC = "hr_asic";
	private static final String PAR_ONLYLATENCY = "only_latency";
	private static final String PAR_DELAY = "delay";
	
	private final int npid;
	private final int mpid;
	private final int smpid;
	private final int repetition;
	private final double psm;
	private final int hrcpu;
	private final int hrgpu;
	private final int hrfpga;
	private final int hrasic;
	private final boolean onlyLatency;
	private final int delay;
	
	private int cycle;
	private NetNode node;
	private final String prefix;
	
	public BitcoinNodeObserver(String prefix) {
		npid = Configuration.getPid(prefix + "." + PAR_NODE_PROT);
		mpid = Configuration.getPid(prefix + "." + PAR_MINER_PROT);
		smpid = Configuration.getPid(prefix + "." + PAR_SMINER_PROT);
		repetition = Configuration.getInt(prefix + "." + PAR_REPETITION);
		psm = Configuration.getDouble(prefix + "." + PAR_SMINER);
		hrcpu = Configuration.getInt(prefix + "." + PAR_HRCPU);
		hrgpu = Configuration.getInt(prefix + "." + PAR_HRGPU);
		hrfpga = Configuration.getInt(prefix + "." + PAR_HRFPGA);
		hrasic = Configuration.getInt(prefix + "." + PAR_HRASIC);	
		onlyLatency = Configuration.getBoolean(prefix + "." + PAR_ONLYLATENCY); 
		delay = Configuration.getInt(prefix + "." + PAR_DELAY);
		cycle = 0;
		this.prefix = prefix;
	}
	
	/**
	 * 
	 * @param n
	 * @return
	 */
	public int getHashRate(NetNode n) {
		switch (n.getMtype()) {
			case CPU: return hrcpu;
			case GPU: return hrgpu;
			case FPGA: return hrfpga;
			case ASIC: return hrasic;
			default: return 0;
		}
	}
	
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
	    int forks=0;
		int sminers=0;
		FileWriter forkStats = null;
		FileWriter blockchainStats = null;
		FileWriter hashrateStats = null;
		FileWriter rewardStats = null;
		FileWriter latencyStats = null;
		BufferedWriter bw = null;			
		cycle++;
		try 
		{	
			if (cycle == 1)  // Initialization
			{  	
				for (int i =0; i< Network.size(); i++) {
					if (((NetNode)Network.get(i)).isSelfmingMiner()) {
						node = (NetNode)Network.get(i); 
						break;
					}					
				}				
				if (onlyLatency == true)
				{
					latencyStats = new FileWriter("docs/statistics/latency_R" + repetition + 
							"_D" + delay + ".dat", false);
					bw = new BufferedWriter(latencyStats);
					bw.write("# Mined_Blocks" + " "  + "Cycle \n");				
					bw.close();	
				}
				else 
				{					
					forkStats = new FileWriter("docs/statistics/forks_R" + repetition +
							"_P" + psm + ".dat", false);
					bw = new BufferedWriter(forkStats);
					bw.write("# Forks_number" + " " + "Cycle \n");	
					bw.close();
					
					blockchainStats = new FileWriter("docs/statistics/blockchain_R" + repetition + 
							"_P" + psm + ".dat", false);
					bw = new BufferedWriter(blockchainStats);
					bw.write("# Honest_blocks" + " " + "Fraudolent_blocks" + " " + "Cycle \n");				
					bw.close();		
					
					rewardStats = new FileWriter("docs/statistics/reward_R" + repetition + 
							"_P" + psm + ".dat", false);
					bw = new BufferedWriter(rewardStats);
					bw.write("# Reward_honest" + " " + "Reward_selfish" + " " + "Cycle \n");				
					bw.close();	
					
								
					int hrsminers = 0;
					int hrhonests = 0;
					NetNode n = null;
					for (int i=0; i< Network.size(); i++) {
						n = (NetNode)Network.get(i);					
						if (n.isSelfmingMiner()) 
							hrsminers += getHashRate(n);
						else if (n.isMiner()) 					
							hrhonests += getHashRate(n);							
					}
					hashrateStats = new FileWriter("docs/statistics/hashrate_R" + repetition + 
							"_P" + psm + ".dat", false);
					bw = new BufferedWriter(hashrateStats);
					bw.write("# Honest_HR" + " " + "Fraudolent_HR" + " " + "Probability(SelfishMiner) \n");
					bw.write(hrhonests + "            " + hrsminers + "            " + psm);
					bw.close();
				}					
			}			
			
			NetNode n = null;
			for (int i=0; i< Network.size(); i++) {
				n = (NetNode)Network.get(i);				
				if (n.isSelfmingMiner()) 
					sminers++;				
				else 
					forks+=((BitcoinGeneralNodeProtocol)n.getProtocol(npid)).getNumForks();						
			}
			
			//Statistics about blockchain
			int honestBlocks, fraudolentBlocks, honestReward, fraudolentReward;
			honestBlocks = fraudolentBlocks = honestReward = fraudolentReward = 0;
			List<Block> blockchain = node.getBlockchain();
			for (Block b : blockchain) {
				if (b.getMiner().isSelfmingMiner()) {
					fraudolentBlocks++;
					fraudolentReward += b.getRevenueForBlock();
				}				
				else {
					honestBlocks++;
					honestReward += b.getRevenueForBlock();
				}					
			}
			// Add the fraudolent blocks (with the associated reward) that are in the private blockchain but not yet in the
			// public one, if any. This is an optimistic assumption, indeed they could never end up in the blockchain
			List<Block> privateBlockchain = ((BitcoinSelfMiningMinerProtocol)node.getProtocol(smpid)).getPrivateBlockchain();
			int diff = privateBlockchain.size() - blockchain.size();
			if (diff > 0) {
				fraudolentBlocks += privateBlockchain.size() - blockchain.size();
				for (int i = blockchain.size(); i < blockchain.size() + diff -1 ; i++) { 
					fraudolentReward += privateBlockchain.get(i).getRevenueForBlock();
				}
			}				
			
			if (onlyLatency == true) {
				int totalBlocks = honestBlocks + fraudolentBlocks;
				latencyStats = new FileWriter("docs/statistics/latency_R" + repetition + 
						"_D" + delay + ".dat", true);
				bw = new BufferedWriter(latencyStats);
				bw.write(totalBlocks + "            "  + cycle + "\n");
				bw.close();
			}			
			else {
				blockchainStats = new FileWriter("docs/statistics/blockchain_R" + repetition + 
						"_P" + psm + ".dat", true);
				bw = new BufferedWriter(blockchainStats);
				bw.write(honestBlocks + "            " + 
						fraudolentBlocks + "            "  + cycle + "\n");			
				bw.close();
				
				// Statistics about forks
				int honests = Network.size()- sminers;
				System.out.println("Honest nodes and miners are " + honests);
				try {
					forks = forks / honests;  // take the avg
					}
				catch(ArithmeticException e) {
					forks = ((BitcoinGeneralNodeProtocol)node.getProtocol(npid)).getNumForks();
				}
				System.out.println("Forks are " + forks + " at cycle " + cycle);
				forkStats = new FileWriter("docs/statistics/forks_R" + repetition + 
						"_P" + psm + ".dat", true);
				bw = new BufferedWriter(forkStats);
				bw.write(forks + "            " + cycle + "\n");
				bw.close();
				
				// Statistics about reward
				rewardStats = new FileWriter("docs/statistics/reward_R" + repetition + 
						"_P" + psm + ".dat", true);
				bw = new BufferedWriter(rewardStats);
				bw.write(honestReward + "            " + 
						fraudolentReward + "            "  + cycle + "\n");			
				bw.close();
				
			}			
		}
		catch (IOException e) {
			System.err.println(e);
		}				
		return false;
	}

}
