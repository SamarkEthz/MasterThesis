package ch.ethz.matsim.students.samark.evo;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class performs the entire cross-over operation between two networks
*/ 
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import ch.ethz.matsim.students.samark.networkObjects.MNetwork;
import ch.ethz.matsim.students.samark.networkObjects.MNetworkPop;
import ch.ethz.matsim.students.samark.networkObjects.MRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;

import ReadWriteClone.Clone;
import ReadWriteClone.Log;
import ReadWriteClone.XMLOps;

public class EvoOpsCrossover {

	public EvoOpsCrossover() {
	}

	@SuppressWarnings("unchecked")
	public static MNetworkPop applyCrossovers(Integer currentGEN, Network globalNetwork, Map<String, NetworkScoreLog> networkScoreMap,
			MNetworkPop newPopulation, String populationName, 
			MNetwork eliteMNetwork, double alpha, double pCrossOver, String crossoverRouletteStrategy, boolean useOdPairsForInitialRoutes, 
			String vehicleTypeName, double vehicleLength, double maxVelocity, int vehicleSeats,
			int vehicleStandingRoom, String defaultPtMode, double stopTime, boolean blocksLane, boolean logEntireRoutes,
			double minCrossingDistanceFactorFromRouteEnd, double maxCrossingAngle, String inputScenario) throws IOException {

		// anneal the crossover frequency with higher generations
		// set here manually
		// differentiate between the zurich and a VC scenario
		if (inputScenario.equals("zurich")) {
			// ZH scenario
			if (currentGEN >= 70) {
				pCrossOver *= (0.02/0.11);
			}	
			else if (currentGEN >= 50) {
				pCrossOver *= (0.04/0.11);
			}			
			else if (currentGEN >= 30) {
				pCrossOver *= (0.07/0.11);
			}
		}
		else if(inputScenario.equals("VC")) {
			// VC scenario
			if (currentGEN >= 125) {
				pCrossOver = 0.0;
			}
			else if (currentGEN >= 105) {
				pCrossOver *= (0.02/0.08);
			}
			else if (currentGEN >= 70) {
				pCrossOver *= (0.04/0.08);
			}
			else if (currentGEN >= 35) {
				pCrossOver *= (0.06/0.08);
			}
		}

		int nOldPop = newPopulation.networkMap.size();
		Log.writeEvo("START CROSS-OVER");
		if (nOldPop<2) {
			Log.write("Not enough network parents for crossover. Proceeding to next evolutionOperator...");
			Log.writeEvo("Not enough network parents for crossover. Proceeding to next evolutionOperator...");
			return newPopulation;
//			System.exit(0);
		}
		// how many crossover attempts are maximally performed (divide by two as it always requires two parents for one crossover)
		int nCrossOverCandidates = (int) Math.ceil(0.5*nOldPop);
		List<MNetwork> newOffspring = new ArrayList<MNetwork>();
		System.out.println("We will try nCrossOverCandidates="+nCrossOverCandidates);
		
		// prepare for pedigree tree EvoLogging. Set dominantParent network to itself for now (default)
		// --> if new networks are bred (crossed), find dominant parent
		for (MNetwork mNetwork : newPopulation.getNetworks().values()) {
			mNetwork.dominantParent = mNetwork.networkID;	// every network is its own parent (-> case if it is not crossed, only modified)
		}
		List<String> processedNetworks = new ArrayList<String>();
		Map<Integer, List<String>> executedMergers = new HashMap<Integer, List<String>>();
		CrossOverLoop:
		for (int n=0; n<nCrossOverCandidates; n++) {
			int nTries = 0;
			Random r = new Random();
			if (r.nextDouble()<pCrossOver) {
				String nameParent1;
				String nameParent2;
				do {
					//choose random parents by Roulette wheel and strategy specified within
					nameParent1 = NetworkEvolutionImpl.selectMNetworkByRoulette(newPopulation, alpha, networkScoreMap, crossoverRouletteStrategy);
					System.out.println("ParentName 1="+nameParent1);
					do{
						nameParent2 = NetworkEvolutionImpl.selectMNetworkByRoulette(newPopulation, alpha, networkScoreMap, crossoverRouletteStrategy);
						System.out.println("ParentName 2="+nameParent2);
						nTries ++;
						if (nTries > 2000) {
							continue CrossOverLoop;
						}
					}while(nameParent1.equals(nameParent2)); // cannot have the same parent
					// make sure this crossover has not already been performed
				}while(NetworkEvolutionImpl.mergerHasBeenExecutedPreviously(executedMergers, nameParent1, nameParent2));
				executedMergers.put(n, Arrays.asList(nameParent1, nameParent2));
				Log.writeAndDisplay("  > Crossing:  " + nameParent1 + " X " + nameParent2);
				Log.writeEvo(" > Crossing Parents:  " + nameParent1 + " X " + nameParent2);
				// clone MNetworks before crossover so that they can be mutated to children
				MNetwork parentMNetwork1 = Clone.mNetwork(newPopulation.getNetworks().get(nameParent1));
				MNetwork parentMNetwork2 = Clone.mNetwork(newPopulation.getNetworks().get(nameParent2));
				// prepare storage for the two parents and the weight of their genes (link length) within the children
				ParentNetworksWeight parentNetworksWeight = new ParentNetworksWeight();
				MNetwork[] childrenMNetworks = NetworkEvolutionImpl.crossMNetworks(globalNetwork, parentMNetwork1, parentMNetwork2,
						vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom, defaultPtMode,
						stopTime, blocksLane, useOdPairsForInitialRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle,
						parentNetworksWeight);
				childrenMNetworks[0].setParents(nameParent1, nameParent2);
				childrenMNetworks[1].setParents(nameParent1, nameParent2);
				Log.write("Parent gene contribution Child_1: "+nameParent1+"="+parentNetworksWeight.child1.get(1)+" / "+nameParent2+"="+parentNetworksWeight.child1.get(2));
				Log.write("Parent gene contribution Child_2: "+nameParent1+"="+parentNetworksWeight.child2.get(1)+" / "+nameParent2+"="+parentNetworksWeight.child2.get(2));
				childrenMNetworks[0].dominantParent = parentNetworksWeight.getDominantParentOfChild1(nameParent1, nameParent2);
				childrenMNetworks[1].dominantParent = parentNetworksWeight.getDominantParentOfChild2(nameParent1, nameParent2);
				// save the two new children to the newOffspring
				newOffspring.add(childrenMNetworks[0]);
				newOffspring.add(childrenMNetworks[1]);
			}
		}
		// replace the worst networks by the new crossover children
		int nNewOffspring = newOffspring.size();
		System.out.println("nNewOffspring="+nNewOffspring);
		if(nNewOffspring != 0) {
			List<String> deletedNetworkNames = RemoveWeakestNetworks(newPopulation, nNewOffspring);
			processedNetworks.addAll(deletedNetworkNames);
			Log.write("  >> Replace weakest networks: " + deletedNetworkNames.toString() + " by "+nNewOffspring+" nNewOffspring");
			Log.writeEvo(" > Replacing weakest parents: " + deletedNetworkNames.toString());
			// when replacing an old week network, make sure to rename the new one according to the removed one so that no Network name e.g. Network2 is duplicated.
			for (int i=0; i<newOffspring.size(); i++) {
				RenameOffspring(deletedNetworkNames.get(i), newOffspring.get(i));	// renaming offspring with its MNetworkId and the Id of all its MRoutes
				newPopulation.addNetwork(newOffspring.get(i));
				Log.writeEvo(" >> New offspring network: " + newOffspring.get(i).networkID + "   parents=["+newOffspring.get(i).parents.get(0)+" / "+newOffspring.get(i).parents.get(1)+"]");
				//Log.write("   >>> Putting New Offspring Network = " + newOffspring.get(i).networkID);
			}
		}
		// for the unlikely event that all networks are replaced by crossovers, the elite network is removed within the above loop.
		// therefore it is restored in the following kicking out the child network with the same name in the network map of the network population
		if (nNewOffspring == nOldPop) {										// check with this condition if all old networks have been deleted for new offspring
			eliteMNetwork.dominantParent = eliteMNetwork.networkID;
			newPopulation.addNetwork(eliteMNetwork);						// if also elite network has been deleted, add manually again (it will replace the new one with the same name)
			processedNetworks.remove(eliteMNetwork.networkID);							// because this network remains unchanged for this generation as if it were not processed
			Log.write("   >>> Putting back removed ELITE NETWORK = " + eliteMNetwork.networkID);
			Log.writeEvo(" >> Putting back removed ELITE NETWORK = " + eliteMNetwork.networkID);
		}
		// log pedigree tree: load pedigree tree, add new generation, save with new generation
		Map<String, String> dominantParents = new HashMap<String, String>();
		for (MNetwork mNetwork : newPopulation.getNetworks().values()) {
			dominantParents.put(mNetwork.networkID, mNetwork.dominantParent);
		}
		List<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
		pedigreeTree.addAll(XMLOps.readFromFile(pedigreeTree.getClass(), "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml"));
		pedigreeTree.add(dominantParents);
		XMLOps.writeToFile(pedigreeTree, "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");
		
//		Log.writeEvo(" >> Networks without crossover modifications: ");
//		for (String networkName : newPopulation.networkMap.keySet()) {
//			if (processedNetworks.contains(networkName)==false) {
//				Log.writeEvo("    > "+networkName + 
//					"   parents=["+newPopulation.networkMap.get(networkName).parents.get(0)+" / "+newPopulation.networkMap.get(networkName).parents.get(1)+"]"  );
//			}
//		}
		
		// this is used to display the entire new routes in the log file and used oly during development
		if (logEntireRoutes) {
			for (MNetwork mn : newPopulation.networkMap.values()) {
				for (String mString : mn.routeMap.keySet()) {
					MRoute mr = mn.routeMap.get(mString);
					Log.writeAndDisplay(
							"   >>> " + mString + " = " + mr.linkList.subList(0, mr.linkList.size() / 2).toString());
				}
			}
		}
		// mark the new networks as modified so that they certainly have to go through simulation in the next round
		for(String networkName : processedNetworks) {
			if (newPopulation.modifiedNetworksInLastEvolution.contains(networkName)==false) {
				newPopulation.modifiedNetworksInLastEvolution.add(networkName);
			}
		}
		return newPopulation;
	}
	
	
	public static void RenameOffspring(String newNetworkName, MNetwork mNetwork) {
		mNetwork.networkID = newNetworkName;
		//thisNewNetworkName+"_Route"+lineNr
		Map<String, MRoute> newRoutesMap = new HashMap<String, MRoute>();
		int counter = 1;
		for (MRoute mRoute : mNetwork.routeMap.values()) {
			MRoute mrTemp = Clone.mRoute(mRoute);
			mrTemp.routeID = newNetworkName+"_Route"+counter;
			newRoutesMap.put(mrTemp.routeID, mrTemp);
			counter++;
		}
		mNetwork.routeMap = newRoutesMap;
	}

	public static List<String> RemoveWeakestNetworks(MNetworkPop newPopulation, int nDelete) {
		List<String> deletedNetworks = new ArrayList<String>();
		for (int n=0; n<nDelete; n++) {
			String weakestNetworkName = "";
			Double weakestScore = Double.MAX_VALUE;
			for (String networkName : newPopulation.networkMap.keySet()) {
				if (newPopulation.networkMap.get(networkName).overallScore < weakestScore) {
					weakestNetworkName = networkName;
					weakestScore = newPopulation.networkMap.get(networkName).overallScore;
				}
			}
			deletedNetworks.add(weakestNetworkName);
			newPopulation.networkMap.remove(weakestNetworkName);
		}		
		return deletedNetworks;
	}
	
}
