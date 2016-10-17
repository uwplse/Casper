/*
 * Generate the scaffold for sketch. Some work has already been done
 * in the previous compiler passes.
 * 
 * - Maaz
 */

package casper.visit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import casper.Configuration;
import casper.SketchCodeGenerator;
import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class GenerateScaffold extends NodeVisitor{
	boolean debug;
	int id;
	NodeFactory nf;
	
	@SuppressWarnings("deprecation")
	public GenerateScaffold(NodeFactory nf){
		this.debug = false;
		this.id = 0;
		this.nf = nf;
	}
	
	@SuppressWarnings("deprecation")
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			if(ext.interesting){	
				if(debug){
					System.err.println("Attempting to translate code fragment:-");
					n.prettyPrint(System.err);
					System.err.println("");
				}
				else{
					System.err.println("Attempting to translate code fragment (Fragment ID: " + id + ")");
				}
				
				Set<String> handledTypes = new HashSet<String>();
				try {					
					for(Variable var : ext.outputVars){
						String sketchReduceType = casper.Util.reducerType(var.getSketchType());
						String reduceType = var.getReduceType();
						
						// Have we already handled this case?
						if(handledTypes.contains(reduceType)){
							continue;
						}
						handledTypes.add(reduceType);
						
						System.err.println("Output type: " + var.varType);
						
						// Get output variables handled under this type
						Configuration.emitCount = 0;
						Set<Variable> sketchFilteredOutputVars = new HashSet<Variable>();
						for(Variable v : ext.outputVars){
							if(v.getReduceType().equals(reduceType)){
								sketchFilteredOutputVars.add(v);
								Configuration.emitCount++;
							}
						}
						
						while(true){
							/* Generate main scaffold */
							SketchCodeGenerator.generateScaffold(id, n, sketchFilteredOutputVars, sketchReduceType, reduceType);
							
							/* Run synthesizer to generate summary */
							int synthesizerExitCode = runSynthesizer("output/main_"+reduceType+"_"+id+".sk", ext, sketchReduceType);
							
							if(synthesizerExitCode == 0){
								/* Run theorem prover to verify summary */
								//DafnyCodeGenerator.generateSummary(id, ext, sketchFilteredOutputVars, sketchReduceType);
								
								int verifierExitCode = 0;//verifySummary("output/main_"+reduceType+id+".dfy", sketchReduceType);
								
								if(verifierExitCode == 0)
									ext.generateCode.put(reduceType, true);
								
								break;
							}
							else if(synthesizerExitCode == 1){
								// Clear data structures before next run
								ext.inputDataCollections.clear();
							}
							else if(synthesizerExitCode == 2){
								System.err.println("Casper failed to synthesize a summary for this code fragment.\nPlease submit your code example at our"
													+ " GitHub Issues tracker (https://github.com/uwplse/Casper/issues)");
								ext.generateCode.put(reduceType, false);
								break;
							}
						}
					}
					
					// Increment id counter
					this.id++;
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Configuration.useConditionals = false;
			}
		}		
		
		return this;
	}

	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		return n;
	}
	
	public class ReadStream implements Runnable {
	    String name;
	    InputStream is;
	    Thread thread;      
	    PrintWriter writer;
	    
	    public ReadStream(String name, InputStream is, PrintWriter writer) {
	        this.name = name;
	        this.is = is;
	        this.writer = writer;
	    }       
	    public void start () {
	        thread = new Thread (this);
	        thread.start ();
	    }       
	    public void run () {
	        try {
	            InputStreamReader isr = new InputStreamReader (is);
	            BufferedReader br = new BufferedReader (isr);   
	            while (true) {
	                String s = br.readLine ();
	                if (s == null) break;
	                if (name.equals("stdin"))
	                	writer.print(s+"\n");
	            }
	            is.close ();    
	        } catch (Exception ex) {
	            System.out.println ("Problem reading stream " + name + "... :" + ex);
	            ex.printStackTrace ();
	        }
	    }
	}

	private int runSynthesizer(String filename, MyWhileExt ext, String type) throws IOException, InterruptedException {		
		Runtime rt = Runtime.getRuntime();
		
		if(debug || true)
			System.err.println("sketch --slv-parallel --bnd-int-range 20 --slv-simiters 200 -bnd-inbits "+Configuration.inbits+" "+ filename);
		
		Process pr = rt.exec("sketch --slv-parallel --bnd-int-range 20 --slv-simiters 200 -bnd-inbits "+Configuration.inbits+" "+ filename);

		PrintWriter writer = new PrintWriter(filename.replace(".sk", ".txt"), "UTF-8");
		
		ReadStream instream = new ReadStream("stdin", pr.getInputStream(),writer);
		ReadStream errstream = new ReadStream("stderr", pr.getErrorStream(),null);
		instream.start();
		errstream.start();

        int exitVal = pr.waitFor();
        
        if(exitVal == 0){
        	System.err.println("Summary successfully synthesized");
        	writer.close();
        	return 0;
        }
        else{
        	System.err.println("Synthesizer exited with error code "+exitVal);
        	writer.close();
        	
        	// TODO: Add machinery for incrementally adding options (something more sophisticated / less ad hoc)
        	
        	if(ext.useConditionals && !Configuration.useConditionals){
        		System.err.println("Incrementing grammar...");
        		Configuration.useConditionals = true;
        		return 1;
        	}
        	else{
        		switch(type){
	            	case "bit":
	            		ext.unaryOperators.add("!");
	            		ext.binaryOperators.add("&&");
	            		ext.binaryOperators.add("||");
	            		ext.binaryOperators.add("==");
	            		System.err.println("Incrementing grammar...");
	            		return 1;
	        		default:
	        			// TODO: Add more cases
	        			return 2; // Give up after first try - break the loop.
            	}
        	}
        }
	}
	
	private int verifySummary(String filename, String outputType) throws IOException, InterruptedException {
		/****** Run dafny ******/
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("dafny "+ filename);

		PrintWriter writer = new PrintWriter("output/outputTempDafny.txt", "UTF-8");
		
		BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			 
        String line=null;
        while((line=input.readLine()) != null) {
        	writer.print(line+"\n");
        }

        int exitVal = pr.waitFor();
        if(exitVal == 0)
        	System.err.println("Summary successfully verified");
        else
        	System.err.println("Verifier exited with error code "+exitVal);
        
		writer.close();
		
		return exitVal;
	}
	
	@Override
	public void finish(){		
		if(debug)
			System.err.println("\n************* Finished generate scaffold complier pass *************");
	}
}