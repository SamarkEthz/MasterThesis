package ReadWriteClone;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
/* 
* This class deletes the MATSim simulation iterations of a network to save some storage space
* It does not delete every 100th, the second last and any other user defined iteration
*/
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%  DESCRIPTION   %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class IterDeleter {

	public static void main(String[] args) throws IOException {
		String simFolder = args[0];
		Integer networkNr = Integer.parseInt(args[1]);
		String iterFolder = simFolder+"/zurich_1pm/Evolution/Population/Network"+networkNr+"/Simulation_Output/ITERS/";
		String[] keepIterationsArray = args[2].split(",");
		List<Integer> keepIterations = new ArrayList<Integer>();
		for (String keepIt : keepIterationsArray) {
			keepIterations.add(Integer.parseInt(keepIt));
		}
		Integer lastIter = 0;
		for (Integer f = 0; f<=1000; f++) {
			String dirName = iterFolder+"it."+f;
			if (!(new File(dirName)).exists()) {
				lastIter = f-1;
				break;
			}
			else {
				continue;
			}
		}
		for (Integer f = 0; f<=lastIter; f++) {
			String dirName = iterFolder+"it."+f;
			if (keepIterations.contains(f) || (f%100==0 && f!=0) || f==lastIter-1) {
				continue;
			}
			else {
				FileUtils.deleteDirectory(new File(dirName));
			}
		}
		
		
	}

}
