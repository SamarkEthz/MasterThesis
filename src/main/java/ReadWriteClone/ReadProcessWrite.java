package ReadWriteClone;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class is intended to process or convert to a new format simulation output files after the simulation
* Basically, the files are loaded, can then be processed as desired, and are saved again
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.io.File;
import java.io.FileNotFoundException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.VehicleWriterV1;

import ch.ethz.matsim.students.samark.cbp.CBPII;
import ch.ethz.matsim.students.samark.networkObjects.MRoute;

public class ReadProcessWrite {

	public static void main(String[] args) throws FileNotFoundException {

		String folderName = args[0];
		String histFolder = folderName + "/zurich_1pm/Evolution/Population/HistoryLog";
		
		for (int g=0; g<185; g++) {
			// MRoute files
			String mRouteFolder = histFolder + "/Generation" + g + "/MRoutes/" ;
			for (int n=1; n<=16; n++) {
				String mRoutesFile = mRouteFolder + "MRoutesNetwork"+n+".xml";
				File mRoutesFILE = new File(mRoutesFile);
				if (mRoutesFILE.exists()) {
					Config conf = ConfigUtils.createConfig();
					conf.getModules().get("network").addParam("inputNetworkFile", mRoutesFile);
					Network mrNetwork = ScenarioUtils.loadScenario(conf).getNetwork();
					// process here and save updated if necessary
					NetworkWriter nw = new NetworkWriter(mrNetwork);
					nw.write(mRoutesFile);
				}
				for (int r=1; r<=15; r++) {
					String routeFile = mRouteFolder + "Network"+n+"_Route"+r+"_RoutesFile.xml";
					File mRouteFILE = new File(routeFile);
					if (mRouteFILE.exists()) {
						MRoute mr = XMLOps.readFromFile(MRoute.class, routeFile);
						// process here and save updated if necessary
						XMLOps.writeToFile(mr, routeFile);
					}
					String networkFile = mRouteFolder + "Network"+n+"_Route"+r+"_NetworkFile.xml";
					File networkFILE = new File(networkFile);
					if (networkFILE.exists()) {
						Config conf = ConfigUtils.createConfig();
						conf.getModules().get("network").addParam("inputNetworkFile", networkFile);
						Network mrNetworkSingle = ScenarioUtils.loadScenario(conf).getNetwork();
						// process here and save updated if necessary
						NetworkWriter nw = new NetworkWriter(mrNetworkSingle);
						nw.write(networkFile);
					}
				}
				// cbp Files
				String networkFolder = histFolder + "/Generation" + g + "/Network" + n +"/";
				String cbpFile = networkFolder + "cbpAveraged.xml";
				File cbpFILE = new File(cbpFile);
				if (cbpFILE.exists()) {
					CBPII cbp = XMLOps.readFromFile(CBPII.class, cbpFile);
					// process here and save updated if necessary
					XMLOps.writeToFile(cbp, cbpFile);
				}
				// Schedule and vehicle files
				String scheduleFile = networkFolder + "MergedSchedule.xml";
				File scheduleFILE = new File(scheduleFile);
				String vehiclesFile = networkFolder + "MergedVehicles.xml";
				File vehiclesFILE = new File(vehiclesFile);
				if (scheduleFILE.exists() && vehiclesFILE.exists()) {
					Config conf = ConfigUtils.createConfig();
					conf.getModules().get("transit").addParam("transitScheduleFile",scheduleFile);
					conf.getModules().get("transit").addParam("vehiclesFile",vehiclesFile);
					Scenario sc = ScenarioUtils.loadScenario(conf);
					TransitSchedule ts = sc.getTransitSchedule();
					// process here and save updated if necessary
					TransitScheduleWriter tsw = new TransitScheduleWriter(ts);
					tsw.writeFile(scheduleFile);					
					org.matsim.vehicles.Vehicles veh = sc.getTransitVehicles();
					// process here and save updated if necessary
//					VehicleWriterV1 vehicleWriterV1 = new VehicleWriterV1(veh);
					VehicleWriterV1 vehicleWriterV1 = new VehicleWriterV1(sc.getTransitVehicles());
					vehicleWriterV1.writeFile(vehiclesFile);
				}
			}
		}
		
	}

}
