/*
 * Class to keep all configuration values.
 * 
 * - Maaz
 */

package casper;

public class Configuration {
	// Synthesizer bounds
	static public int inbits = 2;
	static public int arraySizeBound = 4;
	static public int intRange = 4;
	static public int loopUnrollBound = 4;
	
	// Grammar bounds
	static public int maxNumMROps = 5;
	static public int maxNumEmits = 5;
	static public int maxTupleSize = 5;
	static public int maxRecursionDepth = 5;
	
	// Run in debug mode
	static public boolean slow = false;
}