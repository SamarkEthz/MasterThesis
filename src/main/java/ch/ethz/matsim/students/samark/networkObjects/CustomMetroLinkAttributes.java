package ch.ethz.matsim.students.samark.networkObjects;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class holds an overview of all potential metro links and their attributes.
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CustomMetroLinkAttributes {

	public TransitStopFacility fromNodeStopFacility;	// a stopFacility on its fromNode
	public TransitStopFacility toNodeStopFacility;		// a stopFacility on its toNode
	public TransitStopFacility singleRefStopFacility;	// a stopFacility on the link but not on its nodes
	public Id<Link> originalLinkId;
	public double cost;
	public String type;									// particularly if resulted from copying a railway link or making a new metro link
	
	public CustomMetroLinkAttributes() {
		this.fromNodeStopFacility = null;
		this.toNodeStopFacility = null;
		this.singleRefStopFacility = null;
		this.originalLinkId = null;
		this.cost = Double.MAX_VALUE;
	}
	
	public CustomMetroLinkAttributes(String type) {
		this.fromNodeStopFacility = null;
		this.toNodeStopFacility = null;
		this.singleRefStopFacility = null;
		this.originalLinkId = null;
		this.cost = Double.MAX_VALUE;
		this.type = type;
	}
	
	public CustomMetroLinkAttributes(String type, Id<Link> originalLinkId) {
		this.fromNodeStopFacility = null;
		this.toNodeStopFacility = null;
		this.singleRefStopFacility = null;
		this.originalLinkId = originalLinkId;
		this.cost = Double.MAX_VALUE;
		this.type = type;
	}
	
}
