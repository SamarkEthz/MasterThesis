package ch.ethz.matsim.students.samark.networkOps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import ch.ethz.matsim.students.samark.evo.NetworkEvolutionImpl;
import ch.ethz.matsim.students.samark.networkObjects.CustomLinkAttributes;

public class Metro_NetworkImpl {

	// take a network and list all its links within a map, where the links can be assigned specific attributes
	public static Map<Id<Link>, CustomLinkAttributes> createCustomLinkMap(Network network, String fileName) {
		Map<Id<Link>, CustomLinkAttributes> customLinkMap = new HashMap<Id<Link>, CustomLinkAttributes>(
				network.getLinks().size());
		Iterator<Id<Link>> iterator = network.getLinks().keySet().iterator(); // take network and put all links in
																				// linkTrafficMap
		while (iterator.hasNext()) {
			Id<Link> thisLinkID = iterator.next();
			customLinkMap.put(thisLinkID, new CustomLinkAttributes()); // - initiate traffic with default attributes
		}

		if (fileName != null) {
			NetworkEvolutionImpl.createNetworkFromCustomLinks(customLinkMap, network, fileName);
		}

		return customLinkMap;
	}
	
	public static Map<Id<Link>, CustomLinkAttributes> copyCustomMap(Map<Id<Link>, CustomLinkAttributes> customMap) {
		Map<Id<Link>, CustomLinkAttributes> customMapCopy = new HashMap<Id<Link>, CustomLinkAttributes>();
		for (Entry<Id<Link>, CustomLinkAttributes> entry : customMap.entrySet()) {
			customMapCopy.put(entry.getKey(), entry.getValue());
		}
		return customMapCopy;
	}


	public static List<Id<Link>> nodeListToNetworkLinkList(Network network, ArrayList<Node> nodeList) {
		List<Id<Link>> linkList = new ArrayList<Id<Link>>(nodeList.size() - 1);
		for (int n = 0; n < (nodeList.size() - 1); n++) {
			for (Link l : nodeList.get(n).getOutLinks().values()) {
				if (l.getToNode().getId().equals(nodeList.get(n + 1).getId())) {
					linkList.add(l.getId());
				}
			}
		}
		return linkList;
	}

	public static Id<Node> metroNodeFromOriginalLink(Id<Link> originalLinkRefID) {
		Id<Node> metroNodeId = Id.createNodeId("MetroNodeLinkRef_"+originalLinkRefID.toString());
		return metroNodeId;
	}
	
	public static Id<Link> orginalLinkFromMetroNode(Id<Node> metroNodeId){
		String metroString = metroNodeId.toString();
		Id<Link> originalLinkId = Id.createLinkId(metroString.substring(metroString.indexOf("_")+1));
		return originalLinkId;
	}
	
	public static ArrayList<Id<Link>> networkRouteToLinkIdList(NetworkRoute networkRoute){
		ArrayList<Id<Link>> linkList = new ArrayList<Id<Link>>(networkRoute.getLinkIds().size()+2);
		linkList.add(networkRoute.getStartLinkId());
		linkList.addAll(networkRoute.getLinkIds());
		linkList.add(networkRoute.getEndLinkId());
		return linkList;
	}

	public static double getAverageTrafficOnLinks(Map<Id<Link>, CustomLinkAttributes> customLinkMap) {
		double totalTraffic = 0.0;
		for (Id<Link> linkID : customLinkMap.keySet()) {
			totalTraffic += customLinkMap.get(linkID).getTotalTraffic();
		}
		return totalTraffic / customLinkMap.size();
	}
	
}