# BlockSim
A blockchain network simulator developed based on [PeerSim](http://peersim.sourceforge.net/), which can be used for blockchain network protocol verification. 

## Core Parts
- ***simulation-network***: To simulate different network environments, developers can implement the interfaces provided by BlockSim as needed, which include topological connection, latency setting, network broadcast algorithm and so on.

- ***simulation-consensus***: Diverse consensus protocols can be implemented according to the specific blockchain system.

- ***simulation-data***: Common blockchain data structures can be customized, such as transactions and blocks.

## Running the simulator
Running the simulator means running an experiment of PeerSim. After defining the parameters in configuration file `ConfigForBitcoin.txt`, open the terminal and type 

`java -jar Bitcoin.jar peersim.Simulator ConfigForBitcoin.txt` 

to run the simulator.
