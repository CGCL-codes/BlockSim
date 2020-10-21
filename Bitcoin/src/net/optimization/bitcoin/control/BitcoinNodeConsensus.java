package net.optimization.bitcoin.control;

import java.util.Random;

import net.optimization.bitcoin.protocol.*;
import net.optimization.prototype.*;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class BitcoinNodeConsensus implements Control {

	private static final String PAR_P2 = "prob_2_miners";	
	private static final String PAR_HRCPU = "hr_cpu";
	private static final String PAR_HRGPU = "hr_gpu";
	private static final String PAR_HRFPGA = "hr_fpga";
	private static final String PAR_HRASIC = "hr_asic";
	private static final String PAR_MINER_PROT = "miner_protocol";
	
	private final String prefix;
	private final double p2;
	private double pcpu;
	private double pgpu;
	private double pfpga;
	private double pasic;
	private Random r;
	private final int minerPid;
	private double forknodes = 0;
	private double forkpercentage = 0;
	public BitcoinNodeConsensus(String prefix) {
		// TODO Auto-generated constructor stub
		p2 = Configuration.getDouble(prefix + "." + PAR_P2);
		minerPid = Configuration.getPid(prefix + "." + PAR_MINER_PROT);
		pcpu = pgpu = pfpga = pasic = -1.0;	
		this.prefix = prefix;
		r = new Random(0);
	}

	/**
	 * 
	 * @return
	 */
	private boolean initializeProb() {
		int hrcpu = Configuration.getInt(prefix + "." + PAR_HRCPU);
		int hrgpu = Configuration.getInt(prefix + "." + PAR_HRGPU);
		int hrfpga = Configuration.getInt(prefix + "." + PAR_HRFPGA);
		int hrasic = Configuration.getInt(prefix + "." + PAR_HRASIC);	
		if (hrcpu < 0 || hrgpu < 0 || hrfpga < 0 || hrasic < 0) {
			System.err.println("Hash rates cannot be negative!");
			return false;
		}
		int ncpu, ngpu, nfpga, nasic;
		ncpu = ngpu = nfpga = nasic = 0;
		
		for (int i=0; i< Network.size(); i++) {
			NetNode n = (NetNode) Network.get(i);
			if (!n.isGeneralNode()) {
				switch(n.getMtype()) {
				case CPU :  ncpu++;
						    break;
				case GPU :  ngpu++;
						    break;
				case FPGA : nfpga++;
				            break;
		                case ASIC : nasic++;
				            break;
				}
			}
		}
		// I get the probabilities of choosing cpu/gpu/fpga/asic miner
		int thr = (ncpu*hrcpu + ngpu*hrgpu + nfpga*hrfpga + nasic*hrasic);
		pcpu = ((double) hrcpu * ncpu) / ((double)  thr);
		pgpu = ((double) hrgpu * ngpu) / ((double)  thr);
		pfpga = ((double) hrfpga * nfpga) / ((double)  thr);
		pasic = ((double) hrasic * nasic) / ((double)  thr);
		return true;
	}
	
	/**
	 * 
	 * @return
	 */
	private MinerType getMinerType() 
	{
		double rd = r.nextDouble();	
		if (rd < pcpu)
			return MinerType.CPU;
		else if (rd < pcpu + pgpu)
			return MinerType.GPU;
		else if (rd < pcpu + pgpu + pfpga)
			return MinerType.FPGA;
		else
			return MinerType.ASIC;	
	}
	
	/** One miner of the given type is chosen randomly. The randomness is achieved by shuffling
	 *  the nodes in the network and then taking the first miner node with appropriate type.
	 *  @return the miner node which has mined the block
	 */
	private Node chooseMinerNode(MinerType m) {
		Network.shuffle();
		for (int i=0; i< Network.size(); i++) {
			NetNode n = (NetNode) Network.get(i);
				if (n.getMtype() == m)
					return n;	
		}
		return null;
	}
	
	@Override
	public boolean execute() {
		// TODO Auto-generated method stub
		/* Each miner has a given probability of being selected by the oracle. For each type of miner, I define 
		 * the probability of the type as P = total_hash_rate_miner_type / total_hash_rate. So I initialize 
		 * the probabilities the first time that execute() is invoked and not in the constructor, 
		 * because I must be sure that the network has been initialized	*/		
			if (pcpu == -1.0) 
			{
				boolean initSuccess = initializeProb();
				if (!initSuccess)
					return true; 
			}
			Flag.addCycle();
			MinerType m1, m2;
			m1 = getMinerType();       // Always choose one miner
			NetNode mn1 = (NetNode)chooseMinerNode(m1);
			if (mn1.isMiner())  
				((BitcoinMinerProtocol)mn1.getProtocol(minerPid)).setSelected(true);	
			double rd = r.nextDouble();
			if (rd < p2) {              // two miners solved PoW concurrently
				m2 = getMinerType();
				NetNode mn2 = (NetNode)chooseMinerNode(m2);
				if (mn2.isMiner())
					((BitcoinMinerProtocol)mn2.getProtocol(minerPid)).setSelected(true); 
			}
			forkpercentage = (double)forknodes/Network.size();
			//System.out.println("Percentage of fork:"+forkpercentage+"!Fork nodes are:"+forknodes+"!");
			return false;
	}

}
