package net.optimization.bitcoin.initializer;

import java.util.Random;

import net.optimization.prototype.MinerType;
import net.optimization.prototype.NodeType;
import net.optimization.prototype.NodeType2;
import net.optimization.prototype.NetNode;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;
import peersim.transport.Transport;
import net.optimization.bitcoin.protocol.BitcoinGeneralNodeProtocol;;
public class BitcoinNodeInitializer implements Control,NodeInitializer{

	private static final String PAR_PMINER = "pminer";
	private static final String PAR_PSMINER = "p_self_miner";
	private static final String PAR_PCPU = "pcpu";
	private static final String PAR_PGPU = "pgpu";
	private static final String PAR_PFPGA = "pfpga";
	private static final String PAR_PASIC = "pasic";
	private static final String PAR_MAX_BALANCE = "max_balance";
	private static final String PAR_MAL_RATE = "mal_rate";
	
	// Probability that a network node is a miner.
	private double pminer;
	private double psminer;
	
	// If the node is a miner, then it has different probabilities of mining through CPU, GPU, FPGA or ASIC 
	private double pcpu;
	private double pgpu;
	private double pfpga;
	private double pasic;	
	private double maxBalance;
	private double isMalicious;
	public BitcoinNodeInitializer(String prefix)
	{
		pminer = Configuration.getDouble(prefix + "." + PAR_PMINER);
		psminer = pminer = Configuration.getDouble(prefix + "." + PAR_PSMINER);
		pcpu = Configuration.getDouble(prefix + "." + PAR_PCPU);
		pgpu = Configuration.getDouble(prefix + "." + PAR_PGPU);
		pfpga = Configuration.getDouble(prefix + "." + PAR_PFPGA);
		pasic = Configuration.getDouble(prefix + "." + PAR_PASIC);
		maxBalance = Configuration.getDouble(prefix + "." + PAR_MAX_BALANCE);
//		isMalicious = Configuration.getDouble(prefix + "." + PAR_MAL_RATE);
	}
	
	@Override
	/**
	 * Initializes the nodes in the network based on the probability values received from Configuration file.
	 */
	public boolean execute() {
		
		// TODO Auto-generated method stub
		if (pcpu + pgpu + pfpga + pasic != 1) {
			System.err.println("The sum of the probabilities of the mining  HW must be equal to 1");
			return true;		
		}				
		NetNode n = null;
		Random r = new Random(0);
		for (int i=0; i< Network.size(); i++) {
			n = (NetNode)Network.get(i);
			double b = Math.random()*maxBalance;
			n.setBalance(b);
			double drandom = r.nextDouble();
			if (drandom < pminer) { // the node is a miner
				drandom = r.nextDouble();				
				if (drandom < psminer) //Node is a selfish miner
					n.setNodetype(NodeType.SELFMINING_MINER);
				else
					n.setNodetype(NodeType.MINER);				
				drandom = r.nextDouble();
				if (drandom < pcpu)
					n.setMtype(MinerType.CPU);
				else if (drandom < pcpu + pgpu)
					n.setMtype(MinerType.GPU);
				else if (drandom < pcpu + pgpu + pfpga)
					n.setMtype(MinerType.FPGA);
				else
					n.setMtype(MinerType.ASIC);	
			}
			else 
			{
				n.setNodetype(NodeType.GENERAL_NODE);
				n.setMtype(null);
			}
			if(drandom < isMalicious)
				n.setNodeType2(NodeType2.MALICIOUS_NODE);
			else 
				n.setNodeType2(NodeType2.GENERAL_NODE);
		}
		return false;
	}
	@Override
	public void initialize(Node n) {
		NetNode n0 = (NetNode) n;
		Random r = new Random(0);
		double b = Math.random()*maxBalance;
		n0.setBalance(b);
		double drandom = r.nextDouble();
		if (drandom < pminer) { // the node is a miner
			drandom = r.nextDouble();				
			if (drandom < psminer) //Node is a selfish miner
				n0.setNodetype(NodeType.SELFMINING_MINER);
			else
				n0.setNodetype(NodeType.MINER);				
			drandom = r.nextDouble();
			if (drandom < pcpu)
				n0.setMtype(MinerType.CPU);
			else if (drandom < pcpu + pgpu)
				n0.setMtype(MinerType.GPU);
			else if (drandom < pcpu + pgpu + pfpga)
				n0.setMtype(MinerType.FPGA);
			else
				n0.setMtype(MinerType.ASIC);	
		}
		else 
		{
			n0.setNodetype(NodeType.GENERAL_NODE);
			n0.setMtype(null);
		}
	}

}