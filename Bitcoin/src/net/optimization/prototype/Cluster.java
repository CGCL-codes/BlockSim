package net.optimization.prototype;

import java.util.ArrayList;
import java.util.List;

public interface Cluster {
	NetNode cluster_center = new NetNode(null);
	List<NetNode> cluster_node_list = new ArrayList<NetNode>();
	
	public void getClusterCenter();
	
	public void getClusterNodeList();
}
