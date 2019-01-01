package ch.ethz.matsim.students.samark.networkObjects;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

// this is an important class and holds an overview of all potential metro locations and their attributes
public class CustomLinkAttributes {

	public Double totalTraffic;
	//Double railTraffic;
	//Double tramTraffic;
	//Double busTraffic;
	public String dominantMode;
	public TransitStopFacility dominantStopFacility;	// from the original ref scenario, which stopFacility was dominant on this link
	public TransitStopFacility nextRailwayStopFacility;
	public double distance2nextRailwayStopFacility;
	
	public CustomLinkAttributes() {
		this.totalTraffic = 0.0;
		this.dominantMode = null;
		this.dominantStopFacility = null;
		this.nextRailwayStopFacility = null;
		this.distance2nextRailwayStopFacility = Double.MAX_VALUE;
	}
	
	public double getTotalTraffic() {
		return this.totalTraffic;
	}
	
	public void setTotalTraffic(double totalTrafficNew) {
		this.totalTraffic = totalTrafficNew;
	}
	
	public String getDominantMode() {
		return this.dominantMode;
	}
	

	public void setDominantMode(String dominantModeNew) {
		this.dominantMode = dominantModeNew;
	}
	
	public TransitStopFacility getDominantStopFacility() {
		return this.dominantStopFacility;
	}
	

	public void setDominantStopFacility(TransitStopFacility dominantStopFacilityNew) {
		this.dominantStopFacility = dominantStopFacilityNew;
	}
}