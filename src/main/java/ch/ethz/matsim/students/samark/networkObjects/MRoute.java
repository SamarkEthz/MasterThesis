package ch.ethz.matsim.students.samark.networkObjects;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* The MRoute is the most used class within the framework.
* As mentioned before, each MRoute holds the route of one transit line.
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ReadWriteClone.InfrastructureParameters;
import ReadWriteClone.Log;
import ReadWriteClone.XMLOps;
import ch.ethz.matsim.students.samark.cbp.CBPII;
import ch.ethz.matsim.students.samark.geometry.GeomDistance;
import ch.ethz.matsim.students.samark.transitObjects.CustomStop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;


// where MRoutes are generated/changed:
// - 

public class MRoute implements Serializable{

	private static final long serialVersionUID = 1L;

	// CAUTION: When adding to MRoute, also add in Clone.mRoute!
	public String routeID;
	public NetworkRoute networkRoute;
	public List<Id<Node>> nodeList;
	public List<Id<Link>> linkList;
	public TransitLine transitLine;
	public double routeLength;
	// from eventsFile
	public String eventsFile;
	public int nBoardings;
	public double personMetroDist;
	// from transitScheduleFile
	public double lifeTime;
	public int nDepartures;
	public double departureSpacing;
	public Boolean isInitialDepartureSpacing;
	public double firstDeparture;
	public double lastDeparture;
	public double roundtripTravelTime;
	public String transitScheduleFile;
	public double totalDrivenDist;		// [m] = old:drivenKM total distance traveled by pt vehicles of this mroute
	public double opsCost;
	public double constrCost;
	public double utilityBalance;
	
	public Double lastUtilityBalance;	// in last generation to compare if it has improved with the current generation
	public Boolean freqModOccured;		// if frequency has changed in last gen
	public Boolean significantRouteModOccured;	// if a significant change was applied to route in last evo processes
	public List<String> attemptedFrequencyModifications;
	public Integer blockedFreqModGenerations;	// how many gens the headway is blocked to be modified
	public String lastFreqMod;
	public Double probNextFreqModPositive;		// if the headway (service frequency) is changed, prob. that change is positive
	public Boolean hasBeenShortened;
	
	public double undergroundPercentage;
	public double NewUGpercentage;
	public double DevelopUGPercentage;
	public double NewOGpercentage;
	public double EquipOGPercentage;
	public double DevelopOGPercentage;
	public int vehiclesNr;
	public int nStationsNew;
	public int nStationsExtend;
	// from evolution
	public List<Id<Link>> facilityBlockedLinks;
	
	public MRoute() {
		this.lifeTime = 40.0;
		this.routeLength = Double.MAX_VALUE;
		this.eventsFile = "";
		this.undergroundPercentage = 0.0;
		this.NewUGpercentage = 0.0;
		this.DevelopUGPercentage = 0.0;
		this.NewOGpercentage = 0.0;
		this.EquipOGPercentage = 0.0;
		this.DevelopOGPercentage = 0.0;
		this.personMetroDist = 0.0;
		this.nBoardings = 0;
		this.nDepartures = 0;
		this.departureSpacing = 0.0;
		this.isInitialDepartureSpacing = true;
		this.firstDeparture = 0.0;
		this.lastDeparture = 0.0;
		this.transitScheduleFile = "";
		this.totalDrivenDist = 0.0;
		this.opsCost = 1.0E20;	// initiate with extremely high costs so that it is expensive per se and not accidentially free!
		this.constrCost = 1.0E20;
		this.utilityBalance = -1.0E20;
		this.lastUtilityBalance = -1.0E20;
		this.attemptedFrequencyModifications = new ArrayList<String>();
		this.blockedFreqModGenerations = 0;
		this.freqModOccured = false;
		this.significantRouteModOccured = false;
		this.hasBeenShortened = false;
		this.lastFreqMod = "none";
		this.probNextFreqModPositive = -1.0;
		this.vehiclesNr = 0;
		this.nStationsExtend = 0;
		this.nStationsNew = 0;
		this.roundtripTravelTime = Double.MAX_VALUE;
		this.facilityBlockedLinks = new ArrayList<Id<Link>>();
	}
	
	public MRoute(String name) {
		this();
		this.routeID = name;
	}
	
	// runs along links and checks for every link in which region it is (UG/OG, new/equip/develop)
	public void calculatePercentages(Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes,
			Coord UGcenterCoord, double UGradius, double OGdevelopRadius) throws IOException {
		double totalLength = 0.0;
		double ugLength = 0.0;
		double ugNewLength = 0.0;
		double ugDevelopLength = 0.0;
		double ogLength = 0.0;
		double ogNewLength = 0.0;
		double ogDevelopLength = 0.0;
		double ogEquipLength = 0.0;
		
		for (Id<Link> linkId : this.linkList.subList(0, (int) this.linkList.size()/2)) {
			Link link = globalNetwork.getLinks().get(linkId);
			totalLength += link.getLength();
//			if (metroLinkAttributes.get(linkId) == null) {
//				Log.write("Link NOT FOUND in metroLinkAttributes: "+linkId);
//				continue;
//			}
//			else if (metroLinkAttributes.get(linkId).type == null) {
//				Log.write("Link has no TYPE metroLinkAttributes: "+linkId);
//				continue;
//			}
			if (GeomDistance.calculate(link.getFromNode().getCoord(), UGcenterCoord) < UGradius) {
				ugLength += link.getLength();
//				Log.write("LinkType = "+metroLinkAttributes.get(linkId).type);
				if (metroLinkAttributes.get(linkId).type.equals("rail2newMetro")) {
					ugDevelopLength += link.getLength();
//					Log.write("Adding ugDevelopLength = "+link.getLength());
				}
				else {
					ugNewLength += link.getLength();
//					Log.write("Adding ugNewLength = "+link.getLength());
				}
			}
			else {
				ogLength += link.getLength();
//				Log.write("LinkType = "+metroLinkAttributes.get(linkId).type);
				if (metroLinkAttributes.get(linkId).type.equals("rail2newMetro")) {
					if (GeomDistance.calculate(link.getFromNode().getCoord(), UGcenterCoord) < OGdevelopRadius) {
						ogDevelopLength += link.getLength();
//						Log.write("Adding ogDevelopLength = "+link.getLength());
					}
					else {
						ogEquipLength += link.getLength();
//						Log.write("Adding ogNewLength = "+link.getLength());
					}
				}
				else {
					ogNewLength += link.getLength();
//					Log.write("Adding ogNewLength = "+link.getLength());
				}
			}
		}
		
		this.routeLength = totalLength;
		
		if (totalLength > 0.0) {
			this.undergroundPercentage = ugLength/totalLength;			
		}
		else {
			this.undergroundPercentage = 0.0;
		}
		
		if (ugLength > 0.0) {
			this.NewUGpercentage = ugNewLength/ugLength;
			this.DevelopUGPercentage = ugDevelopLength/ugLength;			
		}
		else {
			this.NewUGpercentage = 0.0;
			this.DevelopUGPercentage = 0.0;
		}
		if (ogLength > 0.0) {
			this.NewOGpercentage = ogNewLength/ogLength;
			this.EquipOGPercentage = ogEquipLength/ogLength;
			this.DevelopOGPercentage = ogDevelopLength/ogLength;			
		}
		else {
			this.NewOGpercentage = 0.0;
			this.EquipOGPercentage = 0.0;
			this.DevelopOGPercentage = 0.0;
		}
//		Log.write(Double.toString(this.undergroundPercentage));
//		Log.write(Double.toString(this.NewUGpercentage));
//		Log.write(Double.toString(this.DevelopUGPercentage));
//		Log.write(Double.toString(this.NewOGpercentage));
//		Log.write(Double.toString(this.EquipOGPercentage));		
//		Log.write(Double.toString(this.DevelopOGPercentage));
	}

	

	
	@SuppressWarnings("unchecked")
	// sums up all stations on the route and differentiates between the stops built into an existing rail stop and all new stops
	public void sumStations() throws FileNotFoundException {
//		System.out.println("Summing new stops found on: "+this.routeID.toString());
		Map<String, CustomStop> railStops = new HashMap<String, CustomStop>();
		// all original rail stops:
		railStops.putAll(XMLOps.readFromFile(railStops.getClass(), "zurich_1pm/Evolution/Population/BaseInfrastructure/railStopAttributes.xml"));
		List<Id<TransitStopFacility>> stopFacilities = new ArrayList<Id<TransitStopFacility>>();
		for (TransitRoute tr : this.transitLine.getRoutes().values()) {
			stopsLoop:
			for (TransitRouteStop trs : tr.getStops()) {	// check for all stops on route if they come from an original rail stop facility
				TransitStopFacility tsf = trs.getStopFacility();
				if (!stopFacilities.contains(tsf.getId())) {
					stopFacilities.add(tsf.getId());
					for (CustomStop cs : railStops.values()) {
						if (cs.transitStopFacility.getId().equals(tsf.getId())) {
							// increase total by one for railway stations that are extended with a metro
							this.nStationsExtend ++;
//							System.out.println("StopFacility based on railStop +1-"+tsf.getId().toString()+" ("+tsf.getName().toString()+")");
							continue stopsLoop;
						}
					}
					// if not assigned to an existing railway stop, a new stop has to be built
					this.nStationsNew ++; // if did not manage to assign stop to a priorly existing rail stop
//					System.out.println("StopFacility to be built new +1-"+tsf.getId().toString()+" ("+tsf.getName().toString()+")");
				}
			}
		}
	}

	// THIS IS ONE-TO-ONE the same as the analysis in MNetworks --> see there for comments
	public double performCostBenefitAnalysisRoute(CBPII refCase, CBPII newCase, MNetwork mNetwork,
			Network globalNetwork, Map<Id<Link>, CustomMetroLinkAttributes> metroLinkAttributes) throws IOException {
				
		InfrastructureParameters infrastructureParameters =
				XMLOps.readFromFile(InfrastructureParameters.class, "zurich_1pm/Evolution/Population/BaseInfrastructure/infrastructureCost.xml");
		
		final double ConstrCostUGnew = infrastructureParameters.ConstrCostUGnew;
		final double ConstrCostUGdevelop = infrastructureParameters.ConstrCostUGdevelop;
		final double ConstrCostOGnew = infrastructureParameters.ConstrCostOGnew;
		final double ConstrCostOGdevelop = infrastructureParameters.ConstrCostOGdevelop;
		final double ConstrCostOGequip = infrastructureParameters.ConstrCostOGequip;
		final double ConstrCostPerStationNew = infrastructureParameters.ConstrCostPerStationNew;
		final double ConstrCostPerStationExtend = infrastructureParameters.ConstrCostPerStationExtend;
		final double costVehicle = infrastructureParameters.costVehicle;
		
		final double OpsCostPerVehDistUG = infrastructureParameters.OpsCostPerVehDistUG;
		final double OpsCostPerVehDistOG = infrastructureParameters.OpsCostPerVehDistOG;
		final double occupancyRate = infrastructureParameters.occupancyRate;
		final double ptPassengerCostPerDist = infrastructureParameters.ptPassengerCostPerDist;
		final double taxPerVehicleDist = infrastructureParameters.taxPerVehicleDist;
		final double carCostPerVehDist = infrastructureParameters.carCostPerVehDist;
		final double externalCarCosts = infrastructureParameters.externalCarCosts;
		final double externalPtCosts = infrastructureParameters.externalPtCosts;

		final double ptTrafficIncreasePercentage = infrastructureParameters.ptTrafficIncreasePercentage;
		final double VATPercentage = infrastructureParameters.VATPercentage;
		final double utilityOfTimePT = infrastructureParameters.utilityOfTimePT;
		final double utilityOfTimeCar = infrastructureParameters.utilityOfTimeCar;

		double lengthUG = this.routeLength*this.undergroundPercentage;
		double lengthOG = this.routeLength*(1-this.undergroundPercentage);
		double lengthOGnew = lengthOG*this.NewOGpercentage;
		double lengthOGequip = lengthOG*this.EquipOGPercentage;
		double lengthOGdevelopExisting = lengthOG*this.DevelopOGPercentage;
		double lengthUGnew = lengthUG*this.NewUGpercentage;
		double lengthUGdevelopExisting = lengthUG*this.DevelopUGPercentage;		
		double ptVehicleLengthDrivenUGdaily = this.totalDrivenDist*this.undergroundPercentage;
		double ptVehicleLengthDrivenOGdaily = this.totalDrivenDist*(1-this.undergroundPercentage);
		
		double totalPersonMetroDist = mNetwork.personMetroDist;
		
// NEW utility module
		// TRAFFIC MODEL SIMULATION
			double thisRouteMetroTravelContributionFactor;
			// do this for the case that totalPersonMetroDist=0 and we can't divide by zero
			if (totalPersonMetroDist > 0.0) {thisRouteMetroTravelContributionFactor = (this.personMetroDist/totalPersonMetroDist);}	
			else {thisRouteMetroTravelContributionFactor = 0.0;}
			double discountFactor = 1.02;
			double averageDiscountFactor = MNetwork.getAverageDiscountFactor(discountFactor, lifeTime);			//	[-], used to average discount over lifetime of yearly recurring cost
//			double annualDeltaCarPersonDist2020 = 0.0;
//			double annualDeltaPtPersonDist2020 = 0.0;
//			double annualDeltaCarPersonTime2020 = 0.0;
//			double annualDeltaPtPersonTime2020 = 0.0;
//			if (totalPersonMetroDist > 0.0) { // do this in case totalPersonMetroDist = 0.0, which would give INFINITY
//				annualDeltaCarPersonDist2020 = thisRouteMetroTravelContributionFactor*250*(newCase.carPersonDist-refCase.carPersonDist);	//  [m/y], double annualDeltaCarVehicleDist2020 = annualDeltaCarPersonDist2020/occupancyRate
//				annualDeltaPtPersonDist2020 = thisRouteMetroTravelContributionFactor*250*(newCase.ptPersonDist-refCase.ptPersonDist);		//  [m/y]
//				annualDeltaCarPersonTime2020 = thisRouteMetroTravelContributionFactor*250*(newCase.carTimeTotal-refCase.carTimeTotal);	//  [s/y], double annualDeltaCarVehicleDist2020 = annualDeltaCarPersonDist2020/occupancyRate
//				annualDeltaPtPersonTime2020 = thisRouteMetroTravelContributionFactor*250*(newCase.ptTimeTotal-refCase.ptTimeTotal);		//  [s/y]
//			}
//			List<Double> annualDeltaCarPersonDist20xx = MNetwork.makeMptUsagePrognosis(annualDeltaCarPersonDist2020);	//  [m/y]	// initiate with expected annual deltaCarPersonDist with 2020 MATSim result
//			List<Double> annualDeltaPtPersonDist20xx = MNetwork.makePtUsagePrognosis(annualDeltaPtPersonDist2020);		//  [m/y]	// initiate with expected annual deltaPtPersonDist with 2020 MATSim result
//			List<Double> annualDeltaCarPersonTime20xx = MNetwork.makeMptUsagePrognosis(annualDeltaCarPersonTime2020);	//  [s/y]	// initiate with expected annual deltaCarPersonDist with 2020 MATSim result
//			List<Double> annualDeltaPtPersonTime20xx = MNetwork.makePtUsagePrognosis(annualDeltaPtPersonTime2020);		//  [s/y]	// initiate with expected annual deltaPtPersonDist with 2020 MATSim result
		// COST
			double constructionCost = (ConstrCostPerStationNew*this.nStationsNew + ConstrCostPerStationExtend*this.nStationsExtend +
					ConstrCostUGnew*lengthUGnew + ConstrCostUGdevelop*lengthUGdevelopExisting +
					ConstrCostOGnew*lengthOGnew + ConstrCostOGdevelop*lengthOGdevelopExisting + ConstrCostOGequip*lengthOGequip)/lifeTime; // [CHF/year]
			double opsCost = averageDiscountFactor*365*(OpsCostPerVehDistUG*ptVehicleLengthDrivenUGdaily + OpsCostPerVehDistOG*ptVehicleLengthDrivenOGdaily); // 
			
			double landCost = 0.01*constructionCost;					// [CHF/year], construction cost is already divided by its lifetime
			double maintenanceCost = opsCost/6.0;						// [CHF/year], opsCost already includes averageDiscountFactor
			double repairCost = opsCost/6.0;		// [CHF/year], opsCost already includes averageDiscountFactor	
			double rollingStockCost = this.vehiclesNr*costVehicle*(1+1/Math.pow(discountFactor,lifeTime/2))/lifeTime;	// [CHF/year] averaged over lifetime; replaced at discount after 20 years
			double externalCost = 0.0;
			double ptPassengerCost = 0.0;
		// BENEFIT
			double vehicleSavings = thisRouteMetroTravelContributionFactor*newCase.customVariable4; // [CHF/year]
			double extCostSavings = thisRouteMetroTravelContributionFactor*newCase.extCostSavings; // [CHF/year]
			Double travelTimeGainsCar = thisRouteMetroTravelContributionFactor*newCase.travelTimeGainsCar; // [CHF/year]
			Double travelTimeGainsPt = thisRouteMetroTravelContributionFactor*newCase.travelTimeGainsPt; // [CHF/year]
			Double travelTimeGainsWalkBike = thisRouteMetroTravelContributionFactor*newCase.customVariable2;
			Double travelTimeGains = thisRouteMetroTravelContributionFactor*newCase.travelTimeGains;
			double ptVatIncrease = newCase.ptVatIncrease;
			double congestionSavings = 0.0;
			// ---- annual total cost change
			Double totalCost = constructionCost+landCost+rollingStockCost + opsCost + maintenanceCost + repairCost + externalCost + ptPassengerCost;
			// ---- annual total utility change
			Double totalUtility = vehicleSavings + extCostSavings + ptVatIncrease + travelTimeGains + congestionSavings;
			this.utilityBalance = totalUtility-totalCost;
			Log.write("---------  "+ this.routeID);
			Log.write("TotalMetroRouteLength / Vehicles = "+this.routeLength/1000+" / "+this.vehiclesNr);
			Log.write("lengthUG (%new / %develop) [Km] = "+lengthUG/1000 + " ("+this.NewUGpercentage+" / "+this.DevelopUGPercentage+")");
			Log.write("lengthOG (%new / %develop) [Km] = "+lengthOG/1000 + " ("+this.NewOGpercentage+" / "+this.DevelopOGPercentage+")");
			Log.write("VehicleMetroDistance = " + this.totalDrivenDist);
			Log.write("PersonMetroDistanceDaily [Km] = " + this.personMetroDist/1000);
			Log.write("this.personMetroDist/totalPersonMetroDist = "+this.personMetroDist/totalPersonMetroDist);
			if (totalPersonMetroDist <= 0.0) {
				Log.write("this.personMetroDist/totalPersonMetroDist above =INFINITY because totalPersonMetroDist=0. Proceeding without utility, but all the cost for construction/operation.");
			}
			Log.write("--Annual Cost (-) [Construction/Operation] = " + totalCost +  " ["+constructionCost+" / "+opsCost+"]");
			if (totalPersonMetroDist <= 0.0) { Log.write("  Annual Utility (+) = 0.0 (--> no metro users!)"); }
			else { Log.write("--Annual Utility (+) [TravelTimeGains PT / Car + OtherGains] = " + totalUtility +  
						" [ "+travelTimeGainsPt + " / "+travelTimeGainsCar+ " + " + (vehicleSavings+extCostSavings+ptVatIncrease)+" ]"); }
			Log.write("---UTILITY BALANCE " + this.routeID + " = "+(totalUtility-totalCost));

		// Return this in any case.
		return this.utilityBalance;
	}
	
	
	
	public String getId() {
		return this.routeID;
	}
	public void setId(String stringID) {
		this.routeID = stringID;
	}
	
	
	public NetworkRoute getNetworkRoute() {
		return this.networkRoute;
	}
	public void setNetworkRoute(NetworkRoute networkRoute) {
		this.networkRoute = networkRoute;
	}
	
	public List<Id<Node>> getNodeList() {
		return this.nodeList;
	}
	public void setNodeList(List<Id<Node>> list) {
		this.nodeList = list;
	}
	
	public List<Id<Link>> getLinkList() {
		return this.getLinkList();
	}
	public void setLinkList(List<Id<Link>> list) {
		this.linkList = list;
	}
	
	public String getTransitScheduleFile() {
		return this.transitScheduleFile;
	}
	public void setTransitScheduleFile(String transitScheduleFile) {
		this.transitScheduleFile = transitScheduleFile;
	}
	
	public String getEventsFile() {
		return this.eventsFile;
	}
	public void setEventsFile(String eventsFile) {
		this.eventsFile = eventsFile;
	}
	
	public double getTotalDrivenDist() {
		return this.totalDrivenDist;
	}
	public void setTotalDrivenDist(double totalDrivenDist) {
		this.totalDrivenDist = totalDrivenDist;
	}
	
	public double getPersonMetroKM() {
		return this.personMetroDist;
	}
	public void setMetroPersonKM(double personKM) {
		this.personMetroDist = personKM;
	}
	
	public Double getUndergroundPercentage() {
		return this.undergroundPercentage;
	}
	public void setUGPercentage(double UGPercentage) {
		this.undergroundPercentage = UGPercentage;
	}
	
	public Integer getBoardingNr() {
		return this.nBoardings;
	}
	public void setBoardingNr(int nPassengers) {
		this.nBoardings = nPassengers;
	}
	
	public Double getRouteLength() {
		return this.routeLength;
	}
	public void setRouteLength(double routeLength) {
		this.routeLength = routeLength;
	}
	
	public void setRouteLength(Network network) {
		double totalLength = 0.0;
		for (Id<Link> linkID : this.linkList) {
			totalLength += network.getLinks().get(linkID).getLength();
		}
		this.routeLength = totalLength;
	}

	public TransitLine getTransitLine() {
		return this.transitLine;
	}
	
	public void setTransitLine(TransitLine transitLine) {
		this.transitLine = transitLine;
	}

	
	public boolean modifyFrequency(Double probPositiveFreqMod) throws IOException {
		if (probPositiveFreqMod < 0.0) {
			Log.write("CAUTION: BLOCKED! Applying no frequency modification to "+this.routeID); // Caution: ProbPositiveFreqMod < 0
			return false;
		}
		if ((new Random()).nextDouble() < probPositiveFreqMod) {
			this.vehiclesNr++;
			this.lastFreqMod = "positive";
			this.attemptedFrequencyModifications.add("positive");
		}
		else {
			this.vehiclesNr--;
			this.lastFreqMod = "negative";
			this.attemptedFrequencyModifications.add("negative");
			this.blockedFreqModGenerations = 3;
		}
		this.lastUtilityBalance = this.utilityBalance;
		this.freqModOccured = true;
		return true;
	}
	
}

