package ch.ethz.matsim.students.samark.eventHandlerCounter;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class creates eventHandlers to count custom event types such as number of users of a specific SBB railway line
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class Metro_Processor {

	public static void main(String[] args) {
		
		int iterationToRead = 10;
		
		Map<Id<Link>, Double> metroLinkTraffic = Metro_ProcessorImpl.handleMetroLinkTraffic(iterationToRead);
		System.out.println("Metro Link Traffic is: "+metroLinkTraffic.toString());
		
		Map<String, Double> metroPeopleTraffic = Metro_ProcessorImpl.handleMetroPeopleTraffic(iterationToRead);
		System.out.println("Nr of Boardings is: "+metroPeopleTraffic.get("metroBoardingNr"));
		System.out.println("Nr of Disembarkments is: "+metroPeopleTraffic.get("metroDisembarkingNr"));
		
		String ptLine = "SBB_S";
		int numberOfLineTransits = Metro_ProcessorImpl.handleMetroLineTraffic(iterationToRead, ptLine);
		System.out.println("Nr of "+ptLine+" Line Transits is: "+numberOfLineTransits);
	}

}
