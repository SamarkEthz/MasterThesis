package ch.ethz.matsim.students.samark;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import ReadWriteClone.InfrastructureParameters;
import ReadWriteClone.Log;
import ReadWriteClone.XMLOps;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.students.samark.evo.NetworkEvolutionImpl;
import ch.ethz.matsim.students.samark.evo.NetworkScoreLog;
import ch.ethz.matsim.students.samark.matsimOps.NetworkEvolutionRunSim;
import ch.ethz.matsim.students.samark.matsimOps.RunnableRunSim;
import ch.ethz.matsim.students.samark.networkObjects.CustomMetroLinkAttributes;
import ch.ethz.matsim.students.samark.networkObjects.MNetwork;
import ch.ethz.matsim.students.samark.networkObjects.MNetworkPop;
import ch.ethz.matsim.students.samark.visualizer.Visualizer;


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class initiates the evolutionary simulation.
* After specifying the parameters, an initial metro environment is built on top of the Zurich scenario of 2015.
* The network is simulated with the MATSim simulation and then evaluated for overall welfare.
* After the scores evaluation, the network evolves by evolutionary operators such as cross-over or mutations and go for the next simulation.
* This procedure is performed simultaneously for a number of parallel networks, that can interact in the evo ops section.
* 
* The arguments are built as follows. Note that the first arguments with model-type and fallback-behaviour are for the MATSim-configuration.
* Input args description |	args[] = --model-type tour --fallback-behaviour IGNORE_AGENT nNetworks nNewMetroRoutes metroRad initHeadway nEvoGenerations iterToAveragePerGen nIterPerGen popSize transitScheduleModStrategy costFactor
* Exemplary IDE args     |  args[] = --model-type tour --fallback-behaviour IGNORE_AGENT 		16 	   		  	 8	   6000 		420 			150 				 20 		 50 	3pm 					  none 		 0.35
* Linux terminal command | java -Xmx120G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT 10 12 4000 300 50 25 50 1pct 0.2 
* The following lines show examples of commands to run different classes in a Linux terminal.
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


/* java -Xmx40G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT nLinesInit rad depSapcing popCensus globalCostFactor
 * java -Xmx90G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT 10 9 5000 300 50 7 22 3pm rail 0.18
 * java -Xmx100G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT 10 12 4000 300 50 25 50 1pct 0.2 
 * cp -avr /nas/samark/Simulations/25_CostStudy/samark-0.0.1-SNAPSHOT.jar /nas/samark/Simulations/25_CostStudy/100percent
 * java -Xmx30G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.VisualizerIterFluctuations Network1 1 200 1 1000 false false individual
 * java -Xmx20G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.VisualizerCBP_Original 1000 1 100 individual
 * java -Xmx40G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.Run_VC --model-type tour --fallback-behaviour IGNORE_AGENT
 * // java -Xmx60G -cp samark-0.0.1-SNAPSHOT.jar ch.ethz.matsim.students.samark.NetworkEvolution --model-type tour --fallback-behaviour IGNORE_AGENT 1 25 8000 300 1 1 200 1pm none 1.0
 *
 */


public class NetworkEvolution {
	

/* CAUTION & NOTES
 * CAUTION: Put back in applyMutations jump over eliteNetwork
 * - Run Standard scenario (e.g. ZH) with RunScenario before this class to have simulation output that can be analyzed.
 *   --> Run the standard simulation as long as iterationToReadOriginalNetwork given that the steady state will be measured from the last iter.
 * - Parameters: Tune well, and start at default settings. Unreasonable parameters can lead to nullPointerExceptions
 * - For OD: minTerminalRadiusFromCenter = 0.00*metroCityRadius
 * - Do not use "noModificationInLastEvolution" if lastIteration changes over evolutions, because this would give another result from the simulations
 * - Crossover: minCrossingDistanceFactorFromRouteEnd must have a MINIMUM = 0.25
 */

	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ConfigurationException, IOException, InterruptedException {
		// Prepare empty defaultLog file for run (logs all important events)
		PrintWriter pwDefault = new PrintWriter("zurich_1pm/Evolution/Population/LogDefault.txt");
		// Prepare empty evoLog file for run (logs all infrastr evolutionary events)
		PrintWriter pwEvo = new PrintWriter("zurich_1pm/Evolution/Population/LogEvo.txt");
		Log.write("START TIME = "+(new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()));
		
	
	// INITIALIZATION
	// - Initiate N networks to make a population
		// % Parameters for Network Population & Strategy: %
		Integer populationSize = Integer.parseInt(args[4]);						// DEFAULT = 12 (for ZH scenario); how many networks should be developed in parallel
		String populationName = "evoNetworks";									// DEFAULT = "evoNetworks"; This is a random name, that will be referred to within the framework.
		Integer initialRoutesPerNetwork = Integer.parseInt(args[5]);			// DEFAULT = 6;	how many (metro) routes shall initially be built
		Integer maxRouteNumber = (int) (1.5*initialRoutesPerNetwork);			// DEFAULT = (int) (1.5*initialRoutesPerNetwork);	max. nr. of (metro) routes allowed within the network.
																				// --> even if all routes are profitable, no more routes are allowed to be built
		Boolean mergeMetroWithRailway = true;									// whether the railway network shall be accessible to the metro, so that it can use it for its routings.
		String ptRemoveScenario = args[12];										// Strategy with which the original transit schedule shall be modified.
																				// Options include "tram", "bus", "rail", "subway", "funicular", "quarter",
																				// where "tram" basically means removing all trams.
		Boolean useFastSBahnModule = false;										// set true if railway (S-Bahn) should jump over (non-dominant) stops that are
																				// served on a parallel metro route
		Boolean varyInitRouteSize = true;										// set true to vary the size of newborn routes for instance to half length
		Boolean enableThreading = true;											// set true for machines with high RAM e.g. external servers
		Integer nThreads = 4;													// if true: recommended nThreads = availableRAM/(6*populationSize)
		String inputPlanStrategy = "default";									// "default", "simEquil", "lastPlan" (see thesis for details)
																				// default for using basic scenario initial population files
																				// simEquil for using steady state reference scenario agent plans
																				// lastPlan for using selected output plans after last generation simulation
		Boolean recallSimulation = false;										// set true if the simulation should not start from scratch but be recalled at
																				// a specific previous generation e.g. if the execution failed
		int generationToRecall = 999;											// it is recommended to use the Generation before the one that failed in order
																				// to make sure it's data is complete and ready for next clean generation
		String inputScenario = "zurich";										// set "zurich" if input is Zurich, set "VC" if a virtualCity scenario is used
		Boolean extendMetroGrid = false;										// set true to fill metro networks with potential stop locations where currently none are featured
		Boolean entireMetroGrid = false;										// set true to make a square grid with potential metro stops throughout the entire map
		Boolean manualODInput = false;											// for test applications or pseudo-optimal networks this can be set true in order to
																				// build manually certain routes. The exact routes are to be specified in NetworkEvolutionImpl.createInitialRoutesManual
		String shortestPathStrategy = "Dijkstra2";									// Options: {"Dijkstra1","Dijkstra2"} -- Both work nicely.
		String initialRouteType = "Random";											// Options: {"OD","Random"}	-- Choose method to create initial routes 																						[OD=StrongestOriginDestinationShortestPaths, Random=RandomTerminals in outer frame of 																						specified network]
		Boolean useOdPairsForInitialRoutes = false;									// For OD make sure to also set below the following: minTerminalRadiusFromCenter = 0.00*metroCityRadius
		if (initialRouteType.equals("OD")) { useOdPairsForInitialRoutes = true; }
		Integer iterationToReadOriginalNetwork = 100;								// This is the iteration for the simulation output from the reference sim that should be used as benchmark output for highly frequented links
		Double lifeTime = 40.0;														// lifetime of the infrastructure. this is important for the annual cost calculation
		
		// %% Parameters for NetworkRoutes %%
		Coord zurich_NetworkCenterCoord = new Coord(2683360.00, 1248100.00);		// Somewhere around Seilbahn Rigiblick. This is used to do better justice to Zurich Nord
																					// --> default Coord(2683360.00, 1248100.00);
		Double xOffset = 1733436.0; 												// required for OD-pairs, which are defined in another COS in the input data
		Double yOffset = -4748525.0;												// add the specified values to QGis to get MATSim		// Right upper corner of Zürisee -- X_QGis=950040; 	X_MATSim= 2683476;  //  Y_QGis=5995336; Y_MATSim= 1246811;
		Double metroCityRadius = Double.parseDouble(args[6]);						// DEFAULT = 4000; specifies the "inner-city" radius, where is denser metro network is reasonable
		Double minMetroRadiusFactor = 0.00;											// DEFAULT = 0.00
		Double maxMetroRadiusFactor = 1.70;											// DEFAULT = 1.70; the network design gets some flexibility by allowing also farther lines and stops
		Double minMetroRadiusFromCenter = metroCityRadius * minMetroRadiusFactor; 	// DEFAULT = set 0.00 to not restrict metro network in city center
		Double maxMetroRadiusFromCenter = metroCityRadius * maxMetroRadiusFactor;	// this is rather large for an inner city network but more realistic to pull inner city network 																						into outer parts to better connect inner/outer city
		Double maxExtendedMetroRadiusFromCenter = 2.3*maxMetroRadiusFromCenter;		// Defines within which radius the metro is allowed to use railway links DEFAULT = [1, 2.1]*maxMetroRadiusFromCenter; (2.1 for mergeMetroWithRailway=true, 1 for =false) 																						far a metro can travel on railwayNetwork
		Integer nMostFrequentLinks = (int) (metroCityRadius/20.0);					// Defines how many (most frequented) links are initially selected for potential metro stops. DEFAULT = (int) (metroCityRadius/20.0) (or 70; will further be reduced during merging procedure for close facilities)
		Double maxNewMetroLinkDistance = Math.min(2100.0,Math.max(0.33*metroCityRadius, 1400));	// Highest bee-line distance between two metro stops	// DEFAULT = Math.max(0.33*metroCityRadius, 1400)
		Double minTerminalRadiusFromCenter = 0.00*metroCityRadius; 					// Closest end terminal placement to city center; DEFAULT = 0.00/0.20*metroCityRadius for OD-Pairs/RandomRoutes
		Double maxTerminalRadiusFromCenter = maxExtendedMetroRadiusFromCenter;		// Farthest end terminal placement from city center; DEFAULT = maxExtendedMetroRadiusFromCenter
		Double minInitialTerminalRadiusFromCenter = 0.30*metroCityRadius; 			// Same as above, just for the initial (random) routes; DEFAULT = 0.30*metroCityRadius | put in parameter file and in routes creation file! alt: 0.20*maxExtendedMetroRadiusFromCenter
		Double maxInitialTerminalRadiusFromCenter = 1.80*metroCityRadius;			// Same as above, just for the initial (random) routes; DEFAULT = 1.20*metroCityRadius | put in parameter file and in routes creation file! alt: 0.80*maxExtendedMetroRadiusFromCenter
		Double minInitialTerminalDistance = 
		   (minInitialTerminalRadiusFromCenter+maxInitialTerminalRadiusFromCenter); // Minimum bee-line distance between two terminals (--> min. route length) DEFAULT = minInitialTerminalRadiusFromCenter+maxInitialTerminalRadiusFromCenter (OLD=0.80*maxMetroRadiusFromCenter)
		Double railway2metroCatchmentArea = 150.0;									// Distance within which a new metro stop may be merged with an existing railway stop; DEFAULT = 150 or metroProximityRadius/3
		Double metro2metroCatchmentArea = 400.0;									// Min. dist. between two new metro stops. They are merged if closer; DEFAULT = 400  (merge metro stops within 400 meters)
		Double odConsiderationThreshold = 0.10;										// Traffic threshold for considering an OD-pair (percentage of max. traffic count on a link): DEFAULT = 0.10 (from which threshold onwards odPairs can be considered for adding to developing 																						routes)
		
		// %% Parameters for Vehicles, StopFacilities & Departures %% (self-explanatory)
		String vehicleTypeName = "metro";  Double maxVelocity = 75.0/3.6 /*[m/s]*/;
		Double vehicleLength = 200.0;  int vehicleSeats = 100; Integer vehicleStandingRoom = 600;
		Double initialDepSpacing = Double.parseDouble(args[7]);	 Double tFirstDep = 6.0*60*60;  Double tLastDep = 21.5*60*60; 	// DEFAULT: initialDepSpacing = 5.0*60.0;
		Double stopTime = 30.0; /*stopDuration [s];*/  String defaultPtMode = "metro";  boolean blocksLane = false;
		
		// %% Parameters Simulation, Events & Plans Processing %%
		Integer firstGeneration = 1;												// numbering of the first evo generation (default = 1)
		Integer lastGeneration = Integer.parseInt(args[8]);	//						// last generation of the evolution
		Integer lastIterationOriginal = Integer.parseInt(args[10]);					// last MATSim iteration of one generation
		Integer lastIteration = lastIterationOriginal;			
		Integer iterationsToAverage = Integer.parseInt(args[9]);					// number of iterations to average output stats over
		
		if (lastIterationOriginal < iterationsToAverage || lastIteration < iterationsToAverage)
			// number of averaging iterations is too high
			{Log.writeAndDisplay(" iterationsToAverage > lastIterationSimulated. Aborting"); System.exit(0);}
		Integer storeScheduleInterval = 1;	// every X generations the mergedSchedule/Vehicles are saved for continuation of simulation after undesired breakdown

		// %% Parameters Events & Plans Processing, Scores %%
		Double averageTravelTimePerformanceGoal = 40.0;
		Integer maxConsideredTravelTimeInSec = 4*3600;
		// Shorten unreasonably long legs if this is set true as MATSim may form very very long walk/bike legs in a selected plans
		// CAUTION: If this is changed, it also has to be adapted in VC_Evolution and iter/cbpOriginal arguments!!
		Boolean shortenTooLongLegs = false; 
		String censusSize = args[11]; // The population size in the MATSim scenario e.g. "1pct","1pm". Make sure the option is featured in the list below.
		Integer populationFactor;	// default 1000 for 1pm scenario 
		if (censusSize.equals("1pct")) { populationFactor = 100; }
		else if (censusSize.equals("0.4pm")) {populationFactor = 2500;}
		else if (censusSize.equals("0.5pm")) {populationFactor = 2000;}
		else if (censusSize.equals("0.6pm")) {populationFactor = 1667;}
		else if (censusSize.equals("1pm")) {populationFactor = 1000;}
		else if (censusSize.equals("3pm")) {populationFactor = 333;}
		else if (censusSize.equals("6pm")) {populationFactor = 167;}
		else {populationFactor = 100; Log.writeAndDisplay(" CensusSize invalid. Aborting!"); System.exit(0);}

		// %% Parameters Evolution %%
		Double alphaXover = 1.3;									// parameter for log. selection strategy in NetworkEvolutionImpl.selectMNetworkByRoulette;
																	// DEFAULT = 1.3; Sensitive param for RouletteWheel-XOverProb Interval=[1.0, 2.0].
																	// The higher, the more strong networks are favored!
		// CAUTION: adapt pCrossOver & pMutation in EvoCrossover
		Double pCrossOver = 0.08; //								// probability with which a crossover operation takes place. DEFAULT = 0.08
																	// annealing of pCrossOver can be specified manually in evo.EvoOpsCrossover
		Double minCrossingDistanceFactorFromRouteEnd = 0.25; 		// Crossovers make sense if there is an actual cross-over, not just touching ends.
																	// This is ensured by allowing cross-overs only if they occur min. a quarter into the route.
																	// DEFAULT = 0.30; MINIMUM = 0.25
		Double maxConnectingDistance = 2000.0;						// isolated routes may be linked to overall network if one of its stops is within
																	// this range of another served network stop
		Boolean logEntireRoutes = false;							// if true, after crossover, the new routes of the children are written out in the log file
		Double maxCrossingAngle = 110.0; 							// maximum turning angle of a metro; DEFAULT = 110
		Double pMutation = 0.4;										// initial average probability that a random route is mutated; pMutation <= (N+1)/(2*N) !!!
																	// annealing of pMutation can be specified manually in evo.EvoOpsMutator
		boolean keepBestRoutes = false;								// for enhanced evolution, keep (block!) the best routes of a network
		if (pMutation>1.0*(initialRoutesPerNetwork+1)/(2*initialRoutesPerNetwork)) {System.out.println("pMutation too high. Choose lower. Aborting."); System.exit(0);}
		Double pBigChange = 0.30;									// if change is applied to a route, the probability that the mutation is big; DEFAULT = 0.25
		Double pSmallChange = 1.0-pBigChange;
		String crossoverRouletteStrategy = "tournamentSelection3";	// Strategy to select networks for crossover parents. See class evo.EvoOpsCrossover for details
																	// DEFAULT: tournamentSelection3; Options: allPositiveProportional, rank, tournamentSelection3, logarithmic
																		
		Double routeDisutilityLimit = -0.0E7;						// Route are developed if profitable and minimized if unprofitable
																	// default profitability threshold is 0 welfare by definition, but to simulate 
																	// subsidies, where unprof. routes are also developed, a lower threshold < 0 is specified
		Integer blockFreqModGENs = 10;								// for how many generations the headway is not mutated in contrast to line routing
		Integer stopUnprofitableRoutesReplacementGEN = 20;			// DEAFULT TBD; After this generation, a route that dies is not replaced by a newborn!
		
		// %% Infrastructure Parameters %% --> Please refer to thesis for additional info
		final Coord UGcenterCoord = new Coord(2683466.0, 1249967.0);		// center for underground region
		final double UGradius = 5000.0;										// radius within which metro must operate is underground
		final double OGdevelopRadius = UGradius*1.5;						// radius, within which overground railway network can be developed to allow metro usage
		final double globalCostFactor = Double.parseDouble(args[13]);		// factor by which expenses are scaled e.g. to simulate network despite unreasonably high costs
		final double ConstrCostUGnew = globalCostFactor*1.5E5;				// construction cost per meter within UG radius, new rails
		final double ConstrCostUGdevelop = globalCostFactor*2.25E4;			// construction cost per meter within UG radius, extend existing train rails
		final double ConstrCostOGnew = globalCostFactor*4.0E4;				// construction cost per meter within OG radius, new rails
		final double ConstrCostOGdevelop = globalCostFactor*6.0E3;			// construction cost per meter within OG radius, extend existing train rails
		final double ConstrCostOGequip = globalCostFactor*6.0E3;			// construction cost per meter within OG radius, only equip existing rails
		final double ConstrCostPerStationNew = globalCostFactor*1.6E5;		// construction cost per new station
		final double ConstrCostPerStationExtend = globalCostFactor*0.1E5;	// construction cost per station to extend for metro use
		final double costVehicle = globalCostFactor*13.0E6;					// cost per vehicle; x2 because assumed to be replaced once for 40y total lifetime (=2x20y)
		final double OpsCostPerVehDistUG = globalCostFactor*20.5/1000;
		final double OpsCostPerVehDistOG = globalCostFactor*20.5/1000;
		final double occupancyRate = 1.40; 									// personsPerVehicle
		final double ptPassengerCostPerDist = 0.1407/1000; 					// average price/km to buy a ticket for a trip with a certain distance
		final double taxPerVehicleDist = 0.06/1000;
		final double carCostPerVehDist = 0.7/1000; 							//(0.1403 + 0.11 + 0.13 + 0.32)/1000; 		// CHF/vehicleKM generalCost(repair etc.) + fuel + write-off
		final double externalCarCosts = 0.077/1000;  						// CHF/personKM  [noise, pollution, climate, accidents, energy]    OLD:(0.0111 + 0.0179 + 0.008 + 0.03)/1000
		final double externalPtCosts = 0.032/1000;							// CHF/personKM [noise, pollution, climate, accidents] + [energyForInfrastructure]   || OLD: 0.023/1000 + EnergyCost*energyPerPtPersDist;
		final double ptTrafficIncreasePercentage = 0.28; 					// by 2040 --> because we build infrastructure anyways, this is just higher ticket revenue!!
		final double VATPercentage = 0.08;
		final double utilityOfTimePT = 14.43/3600;							// CHF/s
		final double utilityOfTimeCar = 23.29/3600;							// CHF/s
		
		// store infrastructure parameters here
		XMLOps.writeToFile(new InfrastructureParameters(ConstrCostUGnew, ConstrCostUGdevelop, ConstrCostOGnew, ConstrCostOGdevelop, ConstrCostOGequip,
				ConstrCostPerStationNew, ConstrCostPerStationExtend, costVehicle, OpsCostPerVehDistUG, OpsCostPerVehDistOG, occupancyRate,
				ptPassengerCostPerDist, taxPerVehicleDist, carCostPerVehDist, externalCarCosts, externalPtCosts, ptTrafficIncreasePercentage,
				VATPercentage, utilityOfTimePT, utilityOfTimeCar), "zurich_1pm/Evolution/Population/BaseInfrastructure/infrastructureCost.xml");
		
		// INITIALIZE OBJECTS
		// population of networks, that is fed through the simulation and evolution modules
		MNetworkPop latestPopulation;
		// every metro link is specified with some custom attributes such as available stops
		Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes = new HashMap<Id<Link>, CustomMetroLinkAttributes>();
		// to store network scores after every gen
		List<Map<String, NetworkScoreLog>> networkScoreMaps = new ArrayList<Map<String, NetworkScoreLog>>();

		if (!recallSimulation) {
			pwDefault.close();
			pwEvo.close();
			FileUtils.cleanDirectory(new File("zurich_1pm/Evolution/Population/HistoryLog")); 
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "NETWORK CREATION - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			// scan reference case sceanrio traffic
			// build metro environment and infrastructure with potential links and stops (from most frequent)
			// initialize a number of parallel networks by means of installing initial metro routes and services
			latestPopulation = NetworkEvolutionImpl.createMNetworks(
				populationName, populationSize, initialRoutesPerNetwork, initialRouteType, shortestPathStrategy, iterationToReadOriginalNetwork, lastIterationOriginal,
				iterationsToAverage, 
				minMetroRadiusFromCenter, maxMetroRadiusFromCenter, maxExtendedMetroRadiusFromCenter, zurich_NetworkCenterCoord, metroCityRadius,
				nMostFrequentLinks, extendMetroGrid, entireMetroGrid,
				maxNewMetroLinkDistance, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter, minInitialTerminalDistance, 
				minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter, varyInitRouteSize, mergeMetroWithRailway, railway2metroCatchmentArea,
				metro2metroCatchmentArea, odConsiderationThreshold, useOdPairsForInitialRoutes, xOffset, yOffset, 1.0*populationFactor, vehicleTypeName, vehicleLength, maxVelocity, 
				vehicleSeats, vehicleStandingRoom, defaultPtMode, blocksLane, stopTime, maxVelocity, tFirstDep, tLastDep, initialDepSpacing, lifeTime,
				shortenTooLongLegs, manualODInput, inputScenario
			);
			// fill metroLinkAttributes here from file, which was saved within the MNetwork creation process
			metroLinkAttributes.putAll(XMLOps.readFromFile(metroLinkAttributes.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/metroLinkAttributes.xml"));
			// initialize pedigree tree which will be built step by step with every generation and shows the dominant parent of every network
			List<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
			XMLOps.writeToFile(pedigreeTree, "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml");
		}
		else {
			// RECALL MODULE - Uncomment "LogCleaner" & "Network Creation"
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "RECALL - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			firstGeneration = generationToRecall;
			latestPopulation = new MNetworkPop(populationName);
			// recall simulation recalls routes and services thereon for every parallel network
			NetworkEvolutionRunSim.recallSimulation(latestPopulation, metroLinkAttributes, generationToRecall, networkScoreMaps, 
					"evoNetworks", populationSize, initialRoutesPerNetwork);			
		}
		
				
		
	// EVOLUTIONARY PROCESS
		// load global network (reference default network + metro network) created in MNetwork creation
		Config config = ConfigUtils.createConfig();
		config.getModules().get("network").addParam("inputNetworkFile", "zurich_1pm/Evolution/Population/BaseInfrastructure/GlobalNetwork.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network globalNetwork = scenario.getNetwork();

		
		for (int generationNr=firstGeneration; generationNr<=lastGeneration; generationNr++) {
			Log.write("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    " + "GENERATION - " + generationNr + " - START" + "    %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
			NetworkEvolutionImpl.saveCurrentMRoutes2HistoryLog(latestPopulation, generationNr, globalNetwork, storeScheduleInterval);			
			
			// - MATSIM SIMULATION LOOP:
			Log.write("SIMULATION of GEN"+generationNr+": ("+lastIteration+" iterations)");
			Log.write("  >> A modification has occured for networks: "+latestPopulation.modifiedNetworksInLastEvolution.toString());
			String initialConfig = "zurich_1pm/zurich_config.xml";
			
			if (enableThreading) {
				ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
				for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
					// if network had no modification in last generation, it does not necessarily have to be simulated again
					// resimulation, however, is recommended to make sure that scores do not result from a simulation fluctuation peak
					if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {continue;}
					mNetwork.evolutionGeneration = generationNr;
					// because we use threading, a Runnable is used in every thread to simulate a network 
					RunnableRunSim MATSimRunnable = new RunnableRunSim(
							args, mNetwork, initialRouteType, initialConfig, lastIteration,
							useFastSBahnModule, ptRemoveScenario, inputPlanStrategy);
					executorService.execute(MATSimRunnable);
				} // End Network Simulation Loop
				executorService.shutdown();
				try { executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); } catch (InterruptedException e) {}
			}
			else { // % Normal approach! (See before 11.10.2018 for alternative threading approaches incl. executorMethod)
				for (MNetwork mNetwork : latestPopulation.getNetworks().values()) {
					// must not simulate this loop again, because it has not been changed in last evolution
					// Comment this if lastIteration changes over evolutions !!
					if (latestPopulation.modifiedNetworksInLastEvolution.contains(mNetwork.getNetworkID())==false) {
						continue;
					}
					mNetwork.evolutionGeneration = generationNr;
					NetworkEvolutionRunSim.run(args, mNetwork, initialRouteType, initialConfig, lastIteration, useFastSBahnModule, ptRemoveScenario, inputPlanStrategy);
				} // End Network Simulation Loop
			}
			Log.write("Completed all MATSim runs.");
			
		// - EVENTS PROCESSING: 
			Log.write("EVENTS PROCESSING of GEN"+generationNr+"");
			int lastEventIteration = lastIteration; // CAUTION: make sure it is not higher than lastIteration above resp. the last simulated iteration!
			MNetworkPop evoNetworksToProcess = latestPopulation;
			//	[for testing]		NetworkEvolutionRunSim.runEventsProcessingMetroOnly(evoNetworksToProcess, lastEventIteration,
			//	[for testing]		globalNetwork, "zurich_1pm/Evolution/Population/", populationFactor);
			// process MATSim events for all events related to metro usage
			evoNetworksToProcess = NetworkEvolutionRunSim.runEventsProcessing(evoNetworksToProcess, lastEventIteration, iterationsToAverage,
					globalNetwork, "zurich_1pm/Evolution/Population/", populationFactor);

		// - PLANS PROCESSING:
			Log.write("PLANS PROCESSING of GEN"+generationNr+"");
			// process all (selected) agent output plans to assess travel times and other key network performance indicators
			latestPopulation = NetworkEvolutionRunSim.peoplePlansProcessingM(latestPopulation, maxConsideredTravelTimeInSec,
					lastIterationOriginal, iterationsToAverage, populationFactor, shortenTooLongLegs, "zurich_1pm/Evolution/Population/");
			
		// - TOTAL SCORE CALCULATOR & HISTORY LOGGER & SCORE CHECK: hand over score to a separate score map for sorting scores	and store most important data of each iteration	
			Log.write("LOGGING SCORES of GEN"+generationNr+":");
			String historyFileLocation = "zurich_1pm/Evolution/Population/HistoryLog/Generation"+generationNr;
			String networkScoreMapGeneralLocation = "zurich_1pm/Evolution/Population/networkScoreMaps.xml";
			// log all network scores
			// performance goals can be ignored as networks are evolved not by performance goal but number of evolutions
			boolean performanceGoalAccomplished = NetworkEvolutionImpl.logResults(networkScoreMaps, historyFileLocation, networkScoreMapGeneralLocation, 
					latestPopulation, averageTravelTimePerformanceGoal, generationNr, lastIterationOriginal, 1.0*populationFactor, 
					globalNetwork, metroLinkAttributes, lifeTime,
					UGcenterCoord, UGradius, OGdevelopRadius);
			if(performanceGoalAccomplished == true) {		// 
				break;
			}
			
		// - EVOLUTION: If PerformanceGoal not yet achieved, change routes and network here according to their scores!
			Log.write("EVOLUTION at the end of GEN"+generationNr+":");
			Log.writeEvo("%%%    EVOLUTION OF GEN_"+generationNr+"    %%%");
			
			// develop routes and networks with different evolutionary operators and mutators
			if (generationNr != lastGeneration) {
				latestPopulation = NetworkEvolutionImpl.developGeneration(globalNetwork, metroLinkAttributes, networkScoreMaps.get(generationNr-1),
						latestPopulation, populationName, alphaXover, pCrossOver, crossoverRouletteStrategy, initialDepSpacing,
						useOdPairsForInitialRoutes, initialRoutesPerNetwork, maxRouteNumber, vehicleTypeName, vehicleLength, maxVelocity, vehicleSeats, vehicleStandingRoom,
						defaultPtMode, stopTime, blocksLane, logEntireRoutes, minCrossingDistanceFactorFromRouteEnd, maxCrossingAngle,
						zurich_NetworkCenterCoord, lastIterationOriginal, pMutation, pBigChange, pSmallChange, routeDisutilityLimit, keepBestRoutes,
						shortestPathStrategy, minInitialTerminalRadiusFromCenter, minTerminalRadiusFromCenter, maxTerminalRadiusFromCenter,
						minInitialTerminalRadiusFromCenter, maxInitialTerminalRadiusFromCenter, minInitialTerminalDistance, metroCityRadius, varyInitRouteSize, 
						tFirstDep, tLastDep, odConsiderationThreshold, xOffset, yOffset, stopUnprofitableRoutesReplacementGEN, blockFreqModGENs,
						generationNr, lastGeneration, maxConnectingDistance, inputScenario);
			}		
		}

	// PLOT RESULTS
		int generationsToPlot = lastGeneration;
		Visualizer.writeChartAverageTravelTimes(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				"zurich_1pm/Evolution/Population/networkScoreMaps.xml", "zurich_1pm/Evolution/Population/networkTravelTimesEvo.png");
		Visualizer.writeChartNetworkScore(generationsToPlot, populationSize, initialRoutesPerNetwork, lastIteration,
				"zurich_1pm/Evolution/Population/networkScoreMaps.xml", "zurich_1pm/Evolution/Population/networkScoreEvo.png");
	
	// LOG GLOBAL SIMULATION PARAMETERS
		PrintWriter pwParams = new PrintWriter("zurich_1pm/Evolution/Population/runParameters.txt");	pwParams.close();	// Prepare empty defaultLog file for run
		Log.write("zurich_1pm/Evolution/Population/runParameters.txt",
				"populationSize="+populationSize  + ";\r\n" + 
				"initialRoutesPerNetwork="+initialRoutesPerNetwork + ";\r\n" + 
				"populationName="+populationName  + ";\r\n" + 
				"mergeMetroWithRailway="+mergeMetroWithRailway  + ";\r\n" + 
				"shortestPathStrategy="+shortestPathStrategy  + ";\r\n" + 
				"initialRouteType="+initialRouteType  + ";\r\n" + 
				"useOdPairsForInitialRoutes="+useOdPairsForInitialRoutes  + ";\r\n" + 
				"iterationToReadOriginalNetwork="+iterationToReadOriginalNetwork  + ";\r\n" + 
				"lifeTime="+lifeTime  + ";\r\n" + 
				"zurich_NetworkCenterCoord="+zurich_NetworkCenterCoord.toString()  + ";\r\n" + 
				"xOffset="+xOffset  + ";\r\n" + 
				"yOffset="+yOffset  + ";\r\n" + 
				"metroCityRadius="+metroCityRadius  + ";\r\n" + 
				"minMetroRadiusFactor="+minMetroRadiusFactor  + ";\r\n" + 
				"maxMetroRadiusFactor="+maxMetroRadiusFactor  + ";\r\n" + 
				"minMetroRadiusFromCenter="+minMetroRadiusFromCenter  + ";\r\n" + 
				"maxMetroRadiusFromCenter="+maxMetroRadiusFromCenter  + ";\r\n" + 
				"maxExtendedMetroRadiusFromCenter="+maxExtendedMetroRadiusFromCenter  + ";\r\n" + 
				"nMostFrequentLinks="+nMostFrequentLinks  + ";\r\n" + 
				"maxNewMetroLinkDistance="+maxNewMetroLinkDistance  + ";\r\n" + 
				"minTerminalRadiusFromCenter="+minTerminalRadiusFromCenter  + ";\r\n" + 
				"maxTerminalRadiusFromCenter="+maxTerminalRadiusFromCenter  + ";\r\n" + 
				"minTerminalDistance="+minInitialTerminalRadiusFromCenter  + ";\r\n" + 
				"minInitialTerminalRadiusFromCenter="+minInitialTerminalRadiusFromCenter  + ";\r\n" + 
				"maxInitialTerminalRadiusFromCenter="+maxInitialTerminalRadiusFromCenter  + ";\r\n" + 
				"minInitialTerminalDistance="+minInitialTerminalDistance  + ";\r\n" + 				
				"railway2metroCatchmentArea="+railway2metroCatchmentArea  + ";\r\n" + 
				"metro2metroCatchmentArea="+metro2metroCatchmentArea  + ";\r\n" + 
				"odConsiderationThreshold="+odConsiderationThreshold  + ";\r\n" + 
				"vehicleTypeName="+vehicleTypeName  + ";\r\n" + 
				"maxVelocity="+maxVelocity  + ";\r\n" + 
				"vehicleLength="+vehicleLength  + ";\r\n" + 
				"vehicleSeats="+vehicleSeats  + ";\r\n" + 
				"vehicleStandingRoom="+vehicleStandingRoom  + ";\r\n" + 
				"initialDepSpacing="+initialDepSpacing  + ";\r\n" + 
				"tFirstDep="+tFirstDep  + ";\r\n" + 
				"tLastDep="+tLastDep  + ";\r\n" + 
				"stopTime="+stopTime  + ";\r\n" + 
				"defaultPtMode="+defaultPtMode  + ";\r\n" + 
				"blocksLane="+blocksLane  + ";\r\n" + 
				"firstGeneration="+firstGeneration  + ";\r\n" + 
				"lastGeneration="+lastGeneration  + ";\r\n" + 
				"lastIterationOriginal="+lastIterationOriginal  + ";\r\n" + 
				"lastIteration="+lastIteration  + ";\r\n" + 
				"storeScheduleInterval="+storeScheduleInterval  + ";\r\n" + 
				"averageTravelTimePerformanceGoal="+averageTravelTimePerformanceGoal  + ";\r\n" + 
				"maxConsideredTravelTimeInSec="+maxConsideredTravelTimeInSec  + ";\r\n" + 
				"populationFactor="+populationFactor  + ";\r\n" + 
				"censusSize="+censusSize + ";\r\n" +
				"alphaXover="+alphaXover  + ";\r\n" + 
				"pCrossOver="+pCrossOver  + ";\r\n" + 
				"minCrossingDistanceFactorFromRouteEnd="+minCrossingDistanceFactorFromRouteEnd  + ";\r\n" + 
				"maxConnectingDistance="+maxConnectingDistance  + ";\r\n" + 
				"logEntireRoutes="+logEntireRoutes  + ";\r\n" + 
				"maxCrossingAngle="+maxCrossingAngle  + ";\r\n" + 
				"pMutation="+pMutation  + ";\r\n" + 
				"pBigChange="+pBigChange  + ";\r\n" + 
				"pSmallChange="+pSmallChange  + ";\r\n" + 
				"crossoverRouletteStrategy="+crossoverRouletteStrategy  + ";\r\n" +
				"routeDisutilityLimit="+routeDisutilityLimit  + ";\r\n" +
				"blockFreqModGENs="+blockFreqModGENs + ";\r\n" +
				"stopUnprofitableRoutesReplacementGEN="+stopUnprofitableRoutesReplacementGEN + ";\r\n" +
				"ConstrCostUGnew="+ConstrCostUGnew + ";\r\n" +
				"ConstrCostUGdevelop="+ ConstrCostUGdevelop+ ";\r\n" +
				"ConstrCostOGnew="+ ConstrCostOGnew+ ";\r\n" +
				"ConstrCostOGdevelop="+ ConstrCostOGdevelop+ ";\r\n" + 
				"ConstrCostOGequip="+ConstrCostOGequip+ ";\r\n" + 
				"ConstrCostPerStationNew="+ConstrCostPerStationNew+ ";\r\n" + 
				"ConstrCostPerStationExtend="+ConstrCostPerStationExtend+ ";\r\n" +
				"costVehicle="+costVehicle+ ";\r\n" +
				"OpsCostPerVehDistUG="+OpsCostPerVehDistUG+ ";\r\n" +
				"OpsCostPerVehDistOG="+ OpsCostPerVehDistOG+ ";\r\n" +
				"occupancyRate="+occupancyRate+ ";\r\n" + 
				"ptPassengerCostPerDist="+ptPassengerCostPerDist+";\r\n" +
				"taxPerVehicleDist="+taxPerVehicleDist+ ";\r\n" +
				"carCostPerVehDist="+carCostPerVehDist+ ";\r\n" + 
				"externalCarCosts="+externalCarCosts+ ";\r\n" +
				"externalPtCosts="+externalPtCosts+ ";\r\n" +
				"ptTrafficIncreasePercentage="+ptTrafficIncreasePercentage+ ";\r\n" + 
				"VATPercentage="+VATPercentage+ ";\r\n" +
				"utilityOfTimePT="+utilityOfTimePT+ ";\r\n" +
				"utilityOfTimeCar="+utilityOfTimeCar+ ";\r\n" +
				"globalCostFactor="+globalCostFactor+ ";\r\n" +
				"mergeMetroWithRailway="+mergeMetroWithRailway+ ";\r\n" +
				"useFastSBahnModule="+useFastSBahnModule+ ";\r\n" +
				"varyInitRouteSize="+varyInitRouteSize+ ";\r\n" +
				"enableThreading="+enableThreading+ ";\r\n" +
				"nThreads="+nThreads
				);

		// Free space after successful run - deletes all sim data except a compressed and thinned version of the routes and schedules
		//		Integer keepGenerationInterval = 10;
		//		NetworkEvolutionImpl.freeSpace(lastGeneration, keepGenerationInterval, populationSize);

		
		Log.write("END TIME = "+(new SimpleDateFormat("HH:mm:ss")).format(Calendar.getInstance().getTime()));
	} // end Main Method

} // end NetworkEvolution Class
