package ch.ethz.matsim.students.samark.visualizer;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class takes the simulation output of the reference simulation of a scenario (without metro) to get benchmark data
* It goes through the simulation, extracts data and stores it in cbp Files (CBPII)
* Also, averages are calculated over several iterations
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ReadWriteClone.XMLOps;
import ch.ethz.matsim.students.samark.cbp.CBPII;
import ch.ethz.matsim.students.samark.evo.NetworkEvolutionImpl;

// Calculate individual CBPs first for original reference simulations
// Then select individual2global to average these values over #iterations = iterationsToAverage

public class VisualizerCBP_Original {

	public static void main(String[] args) throws IOException {

		// args: 600 1 1000 individual
		Integer maxIterations = Integer.parseInt(args[0]);
		Integer iterationsToAverage = Integer.parseInt(args[1]);
		Integer populationFactor = Integer.parseInt(args[2]);
		String recalculateOriginalCBPStrategy = args[3]; // "individual", "global", "individual2global"
		Boolean shortenTooLongLegs;
		if (args[4].equals("shorten")) {
			shortenTooLongLegs = true;			
		}
		else {
			shortenTooLongLegs = false;
		}

		(new File("zurich_1pm/cbpParametersOriginal")).mkdirs();

		// Recommended to calculate individual cbp's (also for stdDevs) and then calculate the average from the individual ones
		
		// choose individual to calculate every single iteration's cbp score
		if (recalculateOriginalCBPStrategy.equals("individual")) {
			for (Integer lastIteration = 1; lastIteration <= maxIterations; lastIteration++) {
				String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
				String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + lastIteration + ".xml";
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, lastIteration,
							lastIteration, shortenTooLongLegs);
				} else {
					NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, lastIteration,
							iterationsToAverage, shortenTooLongLegs);
				}
			}
		}
		// ... or global to calculate average over all iterations in one cbp file
		else if (recalculateOriginalCBPStrategy.equals("global")) {
			String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
			String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml";
			NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, maxIterations,
						iterationsToAverage, shortenTooLongLegs);
		}
		else if (recalculateOriginalCBPStrategy.equals("individual2global")) {
			List<CBPII> CBPs = new ArrayList<CBPII>();
			for (Integer i = maxIterations-iterationsToAverage+1; i<=maxIterations; i++) {
				CBPII cbpi = XMLOps.readFromFile(CBPII.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + i + ".xml");
				CBPs.add(cbpi);
			}
			CBPII cbpGlobal = CBPII.calculateAveragesX(CBPs);
			XMLOps.writeToFile(cbpGlobal, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		}
		// This option combines both: calculates individual cbp's (also for stdDevs) and then calculates the average from the individual ones
		else if (recalculateOriginalCBPStrategy.equals("individualXglobal")) {
			// first individual
			String plansFolder = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched/ITERS";
			for (Integer lastIteration = 1; lastIteration <= maxIterations; lastIteration++) {
				String outputFile = "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + lastIteration + ".xml";
				if (lastIteration < iterationsToAverage) { // then use all available (=lastIteration) for averaging
					NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, lastIteration,
							lastIteration, shortenTooLongLegs);
				} else {
					NetworkEvolutionImpl.calculateCBAStats(plansFolder, outputFile, (int) populationFactor, lastIteration,
							iterationsToAverage, shortenTooLongLegs);
				}
			}
			// then average individuals to global value
			List<CBPII> CBPs = new ArrayList<CBPII>();
			iterationsToAverage = Integer.parseInt(args[5]);
			for (Integer i = maxIterations-iterationsToAverage+1; i<=maxIterations; i++) {
				CBPII cbpi = XMLOps.readFromFile(CBPII.class, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginal" + i + ".xml");
				CBPs.add(cbpi);
			}
			CBPII cbpGlobal = CBPII.calculateAveragesX(CBPs);
			XMLOps.writeToFile(cbpGlobal, "zurich_1pm/cbpParametersOriginal/cbpParametersOriginalGlobal.xml");
		}
		else {
			System.out.println("CAUTION: Invalid strategy. Choose from individual/global. Aborting...");
			System.exit(0);
		}

	}

}
