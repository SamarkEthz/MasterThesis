package ch.ethz.matsim.students.samark.virtualCity;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class runs the standard ref scenario simulation without metro for the Virtual City.
* It is analogous to matsimOps.RunZurichScenarioEnriched.
* The sim output will be used as benchmark as a steady-state traffic situation.
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.config.CommandLine;
import ch.ethz.matsim.baseline_scenario.config.CommandLine.ConfigurationException;
import ch.ethz.matsim.baseline_scenario.traffic.BaselineTrafficModule;
import ch.ethz.matsim.baseline_scenario.transit.BaselineTransitModule;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRoute;
import ch.ethz.matsim.baseline_scenario.transit.routing.DefaultEnrichedTransitRouteFactory;
import ch.ethz.matsim.baseline_scenario.zurich.ZurichModule;
import ch.ethz.matsim.papers.mode_choice_paper.CustomModeChoiceModule;
import ch.ethz.matsim.papers.mode_choice_paper.utils.LongPlanFilter;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

	public class Run_VC {
		
		static public void main(String[] args) throws ConfigurationException, IOException  {

			CommandLine cmd = new CommandLine.Builder(args)
					.allowOptions("model-type", "fallback-behaviour")
					.build();
			
			int lastIteration = Integer.parseInt(args[4]);
			String simulationPath = "zurich_1pm/Zurich_1pm_SimulationOutputEnriched";
//			String simulationPath = "zurich_1pm/VC_files/Zurich_1pm_SimulationOutputEnriched";
			new File(simulationPath).mkdirs();
			Config config = ConfigUtils.loadConfig("zurich_1pm/zurich_config.xml");
			config.getModules().get("controler").addParam("outputDirectory", simulationPath);
			config.getModules().get("controler").addParam("overwriteFiles", "overwriteExistingFiles");
			config.getModules().get("controler").addParam("lastIteration", Integer.toString(lastIteration));
			config.getModules().get("controler").addParam("writeEventsInterval", "1");
			config.getModules().get("controler").addParam("writePlansInterval", "1");
//				String inputNetworkFile = "zurich_1pm/VC_files/virtualCityNetwork.xml";
//			config.getModules().get("network").addParam("inputNetworkFile", inputNetworkFile);
//			config.getModules().get("transit").addParam("transitScheduleFile","Evolution/Population/"+mNetwork.networkID+"/MergedScheduleSpeedSBahn.xml");			
//			config.getModules().get("transit").addParam("vehiclesFile","Evolution/Population/"+mNetwork.networkID+"/MergedVehicles.xml");
		//	config.getModules().get("qsim").addParam("flowCapacityFactor", "10000");
			
		
			// See old versions BEFORE 06.09.2018 for how to load specific mergedNetworks OD/Random instead of Global Network with all links
			
			Scenario scenario = ScenarioUtils.createScenario(config);
			scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DefaultEnrichedTransitRoute.class,
						new DefaultEnrichedTransitRouteFactory());
			ScenarioUtils.loadScenario(scenario);
		
			
			// Do this to delete initial plans in order to have same chances of success for metro as other traffic!
			TripsToLegsAlgorithm t2l = new TripsToLegsAlgorithm(new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE), new MainModeIdentifierImpl());
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Leg) {
							Leg leg = (Leg) element;
							leg.setRoute(null);
						}
					}
					
					t2l.run(plan);
				}
			}
				
			// filters too long plans, which bloat simulation time
			new LongPlanFilter(10, new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE)).run(scenario.getPopulation());
		
			// new mode choice strategies in order to consider metro as other pt
			config.strategy().clearStrategySettings();
			config.strategy().setMaxAgentPlanMemorySize(1);
		    StrategySettings strategy;
		    // See MATSIM-766 (https://matsim.atlassian.net/browse/MATSIM-766)
		    strategy = new StrategySettings();
		    strategy.setStrategyName("SubtourModeChoice");
		    strategy.setDisableAfter(0);
		    strategy.setWeight(0.0);
		    config.strategy().addStrategySettings(strategy);
		
		    strategy = new StrategySettings();
		    strategy.setStrategyName("custom");
		    strategy.setWeight(0.15);
		    config.strategy().addStrategySettings(strategy);
		
		    strategy = new StrategySettings();
		    strategy.setStrategyName("ReRoute");
		    strategy.setWeight(0.05);
		    config.strategy().addStrategySettings(strategy);
		
		    strategy = new StrategySettings();
		    strategy.setStrategyName("KeepLastSelected");
		    strategy.setWeight(0.85);
		    config.strategy().addStrategySettings(strategy);
		
		    boolean bestResponse = true;
			
		    Controler controler = new Controler(scenario);
		    controler.addOverridingModule(new SwissRailRaptorModule());
			controler.addOverridingModule(new BaselineModule());
			controler.addOverridingModule(new BaselineTransitModule());
			controler.addOverridingModule(new ZurichModule());
			controler.addOverridingModule(new BaselineTrafficModule(3.0));
			controler.addOverridingModule(new CustomModeChoiceModule(cmd, bestResponse));
			controler.run();
			
		}
	}
