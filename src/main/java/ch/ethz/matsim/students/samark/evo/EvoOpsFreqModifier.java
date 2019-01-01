package ch.ethz.matsim.students.samark.evo;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class performs frequency modification for the services on the individual routes
* Please refer to the thesis and the extended flow diagram to go through the below algorithm
*/ 
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import ch.ethz.matsim.students.samark.networkObjects.MNetwork;
import ch.ethz.matsim.students.samark.networkObjects.MNetworkPop;
import ch.ethz.matsim.students.samark.networkObjects.MRoute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import ReadWriteClone.Log;

public class EvoOpsFreqModifier {

	public EvoOpsFreqModifier() {
	}
	
	public static MNetworkPop applyFrequencyModification2(MNetworkPop newPopulation, String eliteNetworkName, Double routeDisutilityLimit) throws IOException {
		for(MNetwork mn : newPopulation.networkMap.values()) {
			// do not modify if elite network
			if (mn.networkID.toString().equals(eliteNetworkName)) {
				continue;
			}
			boolean hasHadMutation = false;
			Iterator<MRoute> mRouteIter = mn.routeMap.values().iterator();
			while (mRouteIter.hasNext()) {
				MRoute mRoute = mRouteIter.next();
				// Most important parameter is significantChangeOccured. Make sure this is false if you don't want to modify, especially for newly created Routes!
				// Use blockedFreqModGenerations to block modification for nGenerations
				
				// if significant route change has occurred, forget history and make normal new freqModifications
				if (mRoute.significantRouteModOccured.equals(true)) {	
					mRoute.significantRouteModOccured = false;
					mRoute.attemptedFrequencyModifications = new ArrayList<String>();	// clean attemptedModifications entirely and have a clean start.
					// 1. check if route profitable
					if (mRoute.utilityBalance > routeDisutilityLimit) {
						// 2. mark route's probability to be frequency modified
						// 	  a pos. mod. is smaller headways and vice versa for neg. mod., which is 1-probNextFreqModPositive
						mRoute.probNextFreqModPositive = 0.75;
						// 3. perform actual frequency modification and return boolean if a modification has occured
						hasHadMutation = mRoute.modifyFrequency(mRoute.probNextFreqModPositive);
					}
					else {
						mRoute.probNextFreqModPositive = 0.25; // and neg mod prob. = 0.75 inherently
						hasHadMutation = mRoute.modifyFrequency(mRoute.probNextFreqModPositive);					
					}
				}
				else {	// if no significant change has occurred,
					// continue if freqMod is still blocked, but remove one blocked gen
					if (mRoute.blockedFreqModGenerations > 0) {		
						mRoute.blockedFreqModGenerations--;
						continue;
					}
					else {											// if mRoute is free to modify again
						if (mRoute.freqModOccured.equals(true)) {	// if a specific frequencyMod has been set, apply such
							// this means last frequency modification is to be evaluated for improvement (because freqMod has occured and more dominant criteria
							// of significantRouteChangeOccured does not overrule prior changes)	
							if (mRoute.lastFreqMod.equals("none")) {
								Log.write("CAUTION: LastModification = none. Setting freqModOccured=false.");
								mRoute.lastFreqMod = "none";
							}
							else if (mRoute.utilityBalance > mRoute.lastUtilityBalance) {
								// keep modification and attempt another one in same direction in next move (maybe lock before doing so)
								if (mRoute.lastFreqMod.equals("positive")) {
									mRoute.blockedFreqModGenerations = 0;
									mRoute.probNextFreqModPositive = 1.0;
									mRoute.attemptedFrequencyModifications = new ArrayList<String>();
									// keep freqModOccured = true
								}
								else if (mRoute.lastFreqMod.equals("negative")) {
									// Two options here to slow down route vehicle removing so that it doesn't die out too soon:
									// 1) block 0, but skip modification if route has not been shortened in the mean time
										mRoute.blockedFreqModGenerations = 0;
										if(mRoute.hasBeenShortened) {
											mRoute.hasBeenShortened = false;
											mRoute.probNextFreqModPositive = 0.0;											
										}
										else {
											mRoute.probNextFreqModPositive = -1.0;
										}
									// 2) block n(probably=1) generations, but don't have to wait for route to be shortened
										mRoute.blockedFreqModGenerations = 1;
										mRoute.probNextFreqModPositive = 0.0;
									// ---
									mRoute.attemptedFrequencyModifications = new ArrayList<String>();
									// keep freqModOccured = true
								}
							}
							else { // undo modification and try opposite modification if not attempted already! If already attempted, set long blockage on freqMod of this route
								if (mRoute.lastFreqMod.equals("positive")) {
									mRoute.vehiclesNr--;														// undo positive vehicle change again
									hasHadMutation = true;
									if ( ! mRoute.attemptedFrequencyModifications.contains("negative")) {		// if haven't tried opposite modification yet
										mRoute.probNextFreqModPositive = 0.0;									// then try opposite modification
										mRoute.blockedFreqModGenerations = 0;									// try immediately
									}
									else {
										mRoute.attemptedFrequencyModifications = new ArrayList<String>();
										mRoute.probNextFreqModPositive = -1.0;									// if positive/negative have both been tried, set on hold
										mRoute.blockedFreqModGenerations = 5;									// this means new default freqMod will be applied in 5GENs
										mRoute.freqModOccured = false;
									}
								}
								else if (mRoute.lastFreqMod.equals("negative")) {
									mRoute.vehiclesNr++;
									hasHadMutation = true;
									if ( ! mRoute.attemptedFrequencyModifications.contains("positive")) {		// if haven't tried opposite modification yet
										mRoute.probNextFreqModPositive = 1.0;									// then try opposite modification
										mRoute.blockedFreqModGenerations = 0;									// try immediately
									}
									else {
										mRoute.attemptedFrequencyModifications = new ArrayList<String>();
										mRoute.probNextFreqModPositive = -1.0;									// if positive/negative have both been tried, set on hold
										mRoute.blockedFreqModGenerations = 5;									// this means new default freqMod will be applied in 5GENs
										mRoute.freqModOccured = false;
									}
								}
							}
							hasHadMutation = mRoute.modifyFrequency(mRoute.probNextFreqModPositive);
						}
						else{										// if nothing has been set go ahead and do the default freqModificationProcedure
							if (mRoute.utilityBalance > routeDisutilityLimit) {
								mRoute.probNextFreqModPositive = 0.75;
								hasHadMutation = mRoute.modifyFrequency(mRoute.probNextFreqModPositive);
							}
							else {
								mRoute.probNextFreqModPositive = 0.25;
								hasHadMutation = mRoute.modifyFrequency(mRoute.probNextFreqModPositive);						
							}
						}
					}
					
				}
				if (mRoute.vehiclesNr < 1) {
					Log.write("Oops, " + mRoute.routeID + " has died due to no more vehicles. Removing it from network.");
					mRouteIter.remove();
				}
			}
			if (hasHadMutation && newPopulation.modifiedNetworksInLastEvolution.contains(mn.networkID) == false) {
				newPopulation.modifiedNetworksInLastEvolution.add(mn.networkID);
			}
		}
		return newPopulation;
	}
	
	
	
	@Deprecated	// was used previously for testing purposes
	public static MNetworkPop applyFrequencyModification(MNetworkPop newPopulation, String eliteNetworkName) throws IOException {
		for(MNetwork mn : newPopulation.networkMap.values()) {
			if (mn.networkID.toString().equals(eliteNetworkName)) {
				continue;
			}
			boolean hasHadMutation = false;
			Map<String, Double> routePerformances = new HashMap<String, Double>();
			for (MRoute mr : mn.routeMap.values()) {
				routePerformances.put(mr.routeID, mr.utilityBalance);
			}
			List<String> routePerformanceOrder = NetworkEvolutionImpl.sortMapByValueScore(routePerformances);
			if (mn.routeMap.size()>3) {
				Random r1 = new Random();
				Random r2 = new Random();
				if (r1.nextDouble() < 0.67) {
					hasHadMutation = true;
					Log.write("Shifting one vehicle from weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to strongest " + routePerformanceOrder.get(0));
					mn.routeMap.get(routePerformanceOrder.get(0)).vehiclesNr++;
					mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr--;
					if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr < 1) {
						Log.write("Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " has died due to no more vehicles");
						mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-1));
					}
				}
				if (r2.nextDouble() < 0.33) {
					hasHadMutation = true;
					Log.write("Shifting one vehicle from second weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to second strongest" + routePerformanceOrder.get(0));
					mn.routeMap.get(routePerformanceOrder.get(1)).vehiclesNr++;
					mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-2)).vehiclesNr--;
					if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-2)).vehiclesNr < 1) {
						Log.write("Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-2) + " has died due to no more vehicles");
						mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-2));
					}
				}
			}
			else if ( 4 > mn.routeMap.size() && mn.routeMap.size() > 1 ) {
				Random r1 = new Random();
				if (r1.nextDouble() < 0.67) {
					hasHadMutation = true;
					// Log.write("Shifting one vehicle from weakest " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " to strongest " + routePerformanceOrder.get(0));
					mn.routeMap.get(routePerformanceOrder.get(0)).vehiclesNr++;
					mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr--;
					if (mn.routeMap.get(routePerformanceOrder.get(routePerformanceOrder.size()-1)).vehiclesNr < 1) {
						Log.write("   >> Oops, " + routePerformanceOrder.get(routePerformanceOrder.size()-1) + " has died due to no more vehicles");
						mn.routeMap.remove(routePerformanceOrder.get(routePerformanceOrder.size()-1));
					}
				}
			}
			if (hasHadMutation && newPopulation.modifiedNetworksInLastEvolution.contains(mn.networkID) == false) {
				newPopulation.modifiedNetworksInLastEvolution.add(mn.networkID);
			}
		}
		return newPopulation;
	}
	
}
