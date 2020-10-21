/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package peersim.dynamics;

import peersim.config.Configuration;
import peersim.core.*;
import peersim.graph.Graph;
import peersim.graph.GraphFactory;
import peersim.reports.GraphObserver;

/**
 * This {@link Control} can change the size of networks by adding and removing
 * nodes. Can be used to model churn. This class supports only permanent removal
 * of nodes and the addition of brand new nodes. That is, temporary downtime
 * is not supported by this class.
 */
public class DynamicNetwork extends GraphObserver
{

// --------------------------------------------------------------------------
// Parameters
// --------------------------------------------------------------------------

/**
 * Config parameter which gives the prefix of node initializers. An arbitrary
 * number of node initializers can be specified (Along with their parameters).
 * These will be applied
 * on the newly created nodes. The initializers are ordered according to
 * alphabetical order if their ID.
 * Example:
 * <pre>
control.0 DynamicNetwork
control.0.init.0 RandNI
control.0.init.0.k 5
control.0.init.0.protocol somelinkable
...
 * </pre>
 * @config
 */
private static final String PAR_INIT = "init";

private static final String PAR_POISSON_MEAN = "poissonMean";

/**
 * If defined, nodes are substituted (an existing node is removed, a new one is
 * added. That is, first the number of nodes to add (or remove if
 * {@value #PAR_ADD} is negative) is calculated, and then exactly the same
 * number of nodes are removed (or added) immediately so that the network size
 * remains constant.
 * Not set by default.
 * @config
 */
private static final String PAR_SUBST = "substitute";

/**
 * Specifies the number of nodes to add or remove. It can
 * be negative in which case nodes are removed. If its absolute value is
 * less than one, then it is interpreted as a proportion of current network
 * size, that is, its value is substituted with <tt>add*networksize</tt>.
 * Its value is rounded to an integer.
 * @config
 */
private static final String PAR_ADD = "add";

/**
 * Nodes are added until the size specified by this parameter is reached. The
 * network will never exceed this size as a result of this class.
 * If not set, there will be no limit on the size of the network.
 * @config
 */
private static final String PAR_MAX = "maxsize";

/**
 * Nodes are removed until the size specified by this parameter is reached. The
 * network will never go below this size as a result of this class.
 * Defaults to 0.
 * @config
 */
private static final String PAR_MIN = "minsize";

// --------------------------------------------------------------------------
// Fields
// --------------------------------------------------------------------------

/** value of {@value #PAR_ADD} */
protected final double add;

/** value of {@value #PAR_SUBST} */
protected final boolean substitute;

/** value of {@value #PAR_MIN} */
protected final int minsize;

/** value of {@value #PAR_MAX} */
protected final int maxsize;

/** node initializers to apply on the newly added nodes */
protected final NodeInitializer[] inits;

/** poisson args*/
protected final int poissonMean;

// --------------------------------------------------------------------------
// Protected methods
// --------------------------------------------------------------------------

/**
 * Adds n nodes to the network. Extending classes can implement any algorithm to
 * do that. The default algorithm adds the given number of nodes after calling
 * all the configured initializers on them.
 * 
 * @param n
 *          the number of nodes to add, must be non-negative.
 */
protected void add(Graph g,int n)
{
	for (int i = 0; i < n; ++i) {
		Node newnode = (Node) Network.prototype.clone();
		for (int j = 0; j < inits.length; ++j) {
			inits[j].initialize(newnode);
		}
		Network.add(newnode);
	}
	GraphFactory.wireKOut(g,20,CommonState.r);
}

// ------------------------------------------------------------------

/**
 * Removes n nodes from the network. Extending classes can implement any
 * algorithm to do that. The default algorithm removes <em>random</em>
 * nodes <em>permanently</em> simply by calling {@link Network#remove(int)}.
 * @param n the number of nodes to remove
 */
protected void remove(int n)
{
	for (int i = 0; i < n; ++i) {
		Network.remove(CommonState.r.nextInt(Network.size()));
	}
}


// --------------------------------------------------------------------------
// Initialization
// --------------------------------------------------------------------------

/**
 * Standard constructor that reads the configuration parameters.
 * Invoked by the simulation engine.
 * @param prefix the configuration prefix for this class
 */
public DynamicNetwork(String prefix)
{
	super(prefix);
	add = Configuration.getDouble(prefix + "." + PAR_ADD);
	substitute = Configuration.contains(prefix + "." + PAR_SUBST);
	Object[] tmp = Configuration.getInstanceArray(prefix + "." + PAR_INIT);
	inits = new NodeInitializer[tmp.length];
	for (int i = 0; i < tmp.length; ++i) {
		//System.out.println("Inits " + tmp[i]);
		inits[i] = (NodeInitializer) tmp[i];
	}
	maxsize=Configuration.getInt(prefix+"."+PAR_MAX,Integer.MAX_VALUE);
	minsize = Configuration.getInt(prefix + "." + PAR_MIN, 0);
	
	poissonMean = Configuration.getInt(prefix+"."+PAR_POISSON_MEAN, 0);
}

// --------------------------------------------------------------------------
// Public methods
// --------------------------------------------------------------------------

/**
 * Calls {@link #add(int)} or {@link #remove} with the parameters defined by the
 * configuration.
 * @return always false 
 */
public final boolean execute()
{
	updateGraph();
	if (add == 0)
		return false;
	if (!substitute) {
		if ((maxsize <= Network.size() && add > 0)
				|| (minsize >= Network.size() && add < 0))
			return false;
	}
	int toadd = 0;
	int toremove = 0;
	
	/* poisson add or remove */
	if(poissonMean!=0) {
		double poisson = Math.abs(add);
		int psToAdd = CommonState.r.nextPoisson(poisson);
		if(add>0) {
			toadd = (int) Math.round(psToAdd < 1 ? psToAdd * Network.size() : psToAdd);
			if (!substitute && toadd > maxsize - Network.size())
				toadd = maxsize - Network.size();
			if (substitute)
				toremove = toadd;
		}else if(add<0) {
			psToAdd=(-1)*psToAdd;
			toremove = (int) Math.round(psToAdd > -1 ? -psToAdd * Network.size() : -psToAdd);
			if (!substitute && toremove > Network.size() - minsize)
				toremove = Network.size() - minsize;
			if (substitute)
				toadd = toremove;
		}
		
		remove(toremove);
		add(g, toadd);
		return false;
	}
	
	if (add > 0) {
		toadd = (int) Math.round(add < 1 ? add * Network.size() : add);
		if (!substitute && toadd > maxsize - Network.size())
			toadd = maxsize - Network.size();
		if (substitute)
			toremove = toadd;
	} else if (add < 0) {
		toremove = (int) Math.round(add > -1 ? -add * Network.size() : -add);
		if (!substitute && toremove > Network.size() - minsize)
			toremove = Network.size() - minsize;
		if (substitute)
			toadd = toremove;
	}
	remove(toremove);
	add(g,toadd);
	System.out.println(Network.size());
	return false;
}

// --------------------------------------------------------------------------

}
