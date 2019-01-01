package ch.ethz.matsim.students.samark.dijkstra;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* Called by DemoDijkstra
* Pretty much the same thing as a MATSim-Link
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


public class DemoEdge {

	public final DemoVertex target;
	public final double weight;

	public DemoEdge(DemoVertex target, double weight) {
		this.target = target;
		this.weight = weight;
	}

}