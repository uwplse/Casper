package casper.visit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import polyglot.visit.NodeVisitor;
import casper.Configuration;

public class UpdateConfigurations extends NodeVisitor {
	
	@SuppressWarnings("deprecation")
	public UpdateConfigurations() throws IOException {
		String text = "";
		
		BufferedReader br = new BufferedReader(new FileReader("../Config.txt"));
	    StringBuilder sb = new StringBuilder();
	    String line = br.readLine();

	    while (line != null) {
	        sb.append(line);
	        sb.append(System.lineSeparator());
	        line = br.readLine();
	    }
	    text = sb.toString();
		    
	    br.close();
		
		String[] options = text.split(",");
		
		for(String option : options){
			String[] keyVal = option.split("=");
			keyVal[0] = keyVal[0].trim();
			keyVal[1] = keyVal[1].trim();
			switch(keyVal[0]){
				case "Inbits":
					Configuration.inbits = Integer.parseInt(keyVal[1]);
					break;
				case "ArraySizeBound":
					Configuration.arraySizeBound = Integer.parseInt(keyVal[1]);
					break;
				case "IntRange":
					Configuration.intRange = Integer.parseInt(keyVal[1]);
					break;
				case "LoopUnrollBound":
					Configuration.loopUnrollBound = Integer.parseInt(keyVal[1]);
					break;
				case "MaxNumMROps":
					Configuration.maxNumMROps = Integer.parseInt(keyVal[1]);
					break;
				case "MaxNumEmits":
					Configuration.maxNumEmits = Integer.parseInt(keyVal[1]);
					break;
				case "MaxTupleSize":
					Configuration.maxTupleSize = Integer.parseInt(keyVal[1]);
					break;
				case "MaxRecursionDepth":
					Configuration.maxRecursionDepth = Integer.parseInt(keyVal[1]);
					break;
			}
		}
   	}
}
