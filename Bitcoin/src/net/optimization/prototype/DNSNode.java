package net.optimization.prototype;

import java.util.ArrayList;
import java.util.List;

/**
 * the DNSNode interface to serve for the blockchain network
 * @author Weifeng Hao
 *
 */
public interface DNSNode {
	List<NetNode> all_node_list = new ArrayList<NetNode>();
	List<Cluster> cluster_list = new ArrayList<Cluster>();
	
	/**
	 * define the function of getRandomClusterCenterList from the InitialClusterCenterList
	 * @param InitialClusterCenterList
	 * @return
	 */
	public List<Cluster> getRandomClusterCenterList(List<NetNode> initialClusterCenterList);

	/**
	 *define the function of kmeansClustering to form the cluster called by the blockchain node one by one
	 * @param nodeList
	 */
	public void kmeansClustering(List<NetNode> nodeList);
	
	/**
	 * define the function when the blockchain node join the network
	 * @param newNode
	 */
	public void join(NetNode newNode);
	
	/**
	 * define the function when the blockchain node leave the network
	 * @param leaveNode
	 */
	public void leave(NetNode leaveNode);
	
	/**
	 * define the function when dnsnode merge two small(size < limit) cluster
	 * @param cluster1
	 * @param cluster2
	 * @return
	 */
	public Cluster merge(Cluster cluster1,Cluster cluster2);
	
	/**
	 * define the function when dnsnode split a big cluster into two cluster
	 * @param cluster
	 * @return
	 */
	public List<Cluster> split(Cluster cluster);
	
}
