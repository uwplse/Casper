/*
 * Generate the scaffold for sketch. Some work has already been done
 * in the previous compiler passes.
 * 
 * - Maaz
 */

package casper.visit;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.Configuration;
import casper.DafnyCodeGenerator;
import casper.SketchCodeGenerator;
import casper.SketchParser;
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
	boolean log;
	int id;
	NodeFactory nf;
	boolean opsAdded = false;
	PrintWriter debugLog;
	Map<String,Boolean> solFound;
	
	@SuppressWarnings("deprecation")
	public GenerateScaffold(NodeFactory nf) throws IOException{
		this.debug = false;
		this.log = true;
		this.id = 0;
		this.nf = nf;
		this.opsAdded = false;
		this.solFound = new HashMap<String,Boolean>();
	}
	
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			if(ext.interesting){	
				if(debug){
					System.err.println("Attempting to translate code fragment:-");
					//n.prettyPrint(System.err);
					System.err.println("");
					
					debugLog.print("Attempting to translate code fragment (Fragment ID: " + id + ")\n");
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
						if(log){
							this.debugLog = new PrintWriter("debug.txt", "UTF-8");
							
							debugLog.print("Output type: " + var.varType + "\n\n");
							debugLog.print("Key Index: "+ext.keyIndex + "\n");
							debugLog.print("Recursion Depth: "+ext.recursionDepth + "\n");
							debugLog.print("Conditionals: "+ext.useConditionals + "\n");
							debugLog.print("Num of Vals: "+ext.valCount + "\n");
							debugLog.print("Operators Added: "+this.opsAdded + "\n");
							debugLog.print("Number of solutions so far: "+ext.verifiedMapEmits.size() + "\n");
							debugLog.print("Time stamp: "+System.currentTimeMillis() + "\n\n\n");
							debugLog.flush();
						}
						
						// Get output variables handled under this type
						Set<Variable> sketchFilteredOutputVars = new HashSet<Variable>();
						for(Variable v : ext.outputVars){
							if(v.getReduceType().equals(reduceType)){
								sketchFilteredOutputVars.add(v);
							}
						}
						
						// Number of keys to be used
						int keyCount = 1;
						for(Variable v : sketchFilteredOutputVars){
							String type = v.getSketchType();
							if(type.endsWith("["+Configuration.arraySizeBound+"]")){
								keyCount = 2;
							}
						}
						
						// Data type options
						ext.candidateKeyTypes.add("int");
						if(!ext.candidateKeyTypes.contains(sketchReduceType)) ext.candidateKeyTypes.add(sketchReduceType);
						for(Variable v : ext.inputVars){
							String vtype = v.getReduceType();
							if(ext.candidateKeyTypes.contains(vtype)) continue;
							if(vtype.equals("String") || vtype.equals("String[]"))
								ext.candidateKeyTypes.add(vtype.replace("[]", ""));
							if(casper.Util.getTypeClass(vtype) == casper.Util.OBJECT ||
									casper.Util.getTypeClass(vtype) == casper.Util.OBJECT_ARRAY){
								vtype = v.getSketchType().replace("["+Configuration.arraySizeBound+"]", "");
								if(ext.globalDataTypesFields.containsKey(vtype)){
									for(Variable fdecl : ext.globalDataTypesFields.get(vtype)){
										if(ext.candidateKeyTypes.contains(fdecl.getReduceType().replace("[]",""))) continue;
										ext.candidateKeyTypes.add(fdecl.getReduceType().replace("[]",""));
									}
								}
							}
						}
						
						// Emit Count
						ext.emitCount = sketchFilteredOutputVars.size();
						
						while(true){
							if(debug){
								System.err.println(ext.blockExprs);
							}
							
							//System.in.read();
							
							/* Generate main scaffold */
							SketchCodeGenerator.generateScaffold(id, n, sketchFilteredOutputVars, sketchReduceType, reduceType);
							
							if(debug){
								System.err.println(ext.blocks);
							}
							
							/* Run synthesizer to generate summary */
							System.err.println("Attempting to synthesize solution...");
							int synthesizerExitCode = runSynthesizer("output/main_"+reduceType+"_"+id+".sk", ext, keyCount, sketchReduceType, sketchFilteredOutputVars.size());
							
							if(synthesizerExitCode == 0){
								/* Run theorem prover to verify summary */
								SketchParser.parseSolution("output/main_"+reduceType+"_"+id+".txt", sketchFilteredOutputVars, ext, ext.emitCount);
								
								DafnyCodeGenerator.generateSummary(id, n, sketchFilteredOutputVars, reduceType, sketchReduceType);
								
								int CSGverifierExitCode = 0;
								if(ext.valCount == 1)
									CSGverifierExitCode = verifySummaryCSG("output/main_"+reduceType+"_"+id+"_CSG.dfy", sketchReduceType);
								
								System.err.println(ext.mapEmits);
								System.err.println(ext.reduceExps);
								
								if(CSGverifierExitCode == 0){
									int VerifierExitCode = verifySummary("output/main_"+reduceType+"_"+id+".dfy", sketchReduceType);
									if(VerifierExitCode == 0){
										ext.verifiedMapEmits.add(ext.mapEmits);
										ext.verifiedInitExps.add(ext.initExps);
										ext.verifiedReduceExps.add(ext.reduceExps);
										ext.verifiedMergeExps.add(ext.mergeExps);
										ext.verifiedCSG.add(true);
										ext.blocks.add(new ArrayList<String>());
										ext.termValuesTemp.clear();
										
										this.solFound.put(ext.keyIndex+","+ext.useConditionals+","+opsAdded,true);
										this.solFound.put(ext.keyIndex+","+ext.useConditionals+","+ext.valCount+","+opsAdded,true);
										
										if(log){
											debugLog.print("Solution Mappers: "+ext.mapEmits + "\n");
											debugLog.print("Solution Reducers: "+ext.reduceExps + "\n");
											debugLog.print("Time stamp: "+System.currentTimeMillis() + "\n\n");
											debugLog.flush();
										}
									}
									else{
										Map<String,String> blockExprsNew = new HashMap<String,String>();
										for(String key : ext.blockExprs.get(ext.blockExprs.size()-1).keySet()){
											String prefix = "_term_flag";;
											String postfix = "";
											if(key.startsWith("mapExp")){
												postfix = "_map"+key.substring(6);
											}
											else if(key.startsWith("reduceExp")){
												postfix = "_reduce"+key.substring(9);
											}
											for(String s : ext.termValuesTemp.keySet()){
												Pattern r = Pattern.compile(Pattern.quote(prefix)+"(.*?)"+Pattern.quote(postfix));
												Matcher m = r.matcher(s);
												if(m.matches()){
													if(ext.blockExprs.get(ext.blockExprs.size()-1).get(key).contains(m.group(1))){
														blockExprsNew.put(s, ext.termValuesTemp.get(s));
													}
												}
											}
										}
										ext.blockExprs.get(ext.blockExprs.size()-1).putAll(blockExprsNew);
										ext.blocks.add(new ArrayList<String>());
										ext.termValuesTemp.clear();
									}
								}
								else{
									// Solution failed. Register terminal values in blockedExprs.
									Map<String,String> blockExprsNew = new HashMap<String,String>();
									for(String key : ext.blockExprs.get(ext.blockExprs.size()-1).keySet()){
										String prefix = "_term_flag";;
										String postfix = "";
										if(key.startsWith("mapExp")){
											postfix = "_map"+key.substring(6);
										}
										else if(key.startsWith("reduceExp")){
											postfix = "_reduce"+key.substring(9);
										}
										for(String s : ext.termValuesTemp.keySet()){
											Pattern r = Pattern.compile(Pattern.quote(prefix)+"(.*?)"+Pattern.quote(postfix));
											Matcher m = r.matcher(s);
											if(m.matches()){
												if(ext.blockExprs.get(ext.blockExprs.size()-1).get(key).contains(m.group(1))){
													blockExprsNew.put(s, ext.termValuesTemp.get(s));
												}
											}
										}
									}
									ext.blockExprs.get(ext.blockExprs.size()-1).putAll(blockExprsNew);
									ext.blocks.add(new ArrayList<String>());
									ext.termValuesTemp.clear();
								}
							}
							else if(synthesizerExitCode == 1){
								if(log){
									debugLog.print("Key Index: "+ext.keyIndex + "\n");
									debugLog.print("Recursion Depth: "+ext.recursionDepth + "\n");
									debugLog.print("Conditionals: "+ext.useConditionals + "\n");
									debugLog.print("Num of Vals: "+ext.valCount + "\n");
									debugLog.print("Operators Added: "+this.opsAdded + "\n");
									debugLog.print("Number of solutions so far: "+ext.verifiedMapEmits.size() + "\n\n");
									debugLog.print("Time stamp: "+System.currentTimeMillis() + "\n\n\n");
									debugLog.flush();
								}
							}
							else if(synthesizerExitCode == 2){
								if(ext.verifiedMapEmits.size()==0){
									System.err.println("Casper failed to synthesize a summary for this code fragment.\nPlease submit your code example at our"
														+ " GitHub Issues tracker (https://github.com/uwplse/Casper/issues)");
									ext.generateCode.put(reduceType, false);
									System.exit(1);
								}
								else{
									ext.generateCode.put(reduceType, true);
									System.err.println(ext.verifiedMapEmits.size() + " solutions synthesized.");
									debugLog.close();
									break;
								}
							}
						}
					}
					
					// Increment id counter
					this.id++;
				} catch (Exception e) {
					e.printStackTrace();
				}
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

	private int runSynthesizer(String filename, MyWhileExt ext, int keyCount, String type, int emitCountInit) throws IOException, InterruptedException {		
		Runtime rt = Runtime.getRuntime();
		
		if(debug || true)
			System.err.println("sketch --slv-parallel --bnd-int-range 20 --bnd-inbits "+Configuration.inbits+" --bnd-unroll-amnt 6 "+ filename);
		
		Process pr = rt.exec("sketch --slv-parallel --bnd-int-range 20 --bnd-inbits "+Configuration.inbits+" --bnd-unroll-amnt 6 "+ filename);

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
        	
        	// Increment grammar

        	// 1. If we have multiple keys, try other key2 types
        	if(keyCount > 1){
        		if(ext.keyIndex < ext.candidateKeyTypes.size()-1){
        			System.err.println("Keytype changed from " + ext.candidateKeyTypes.get(ext.keyIndex) + " to " + ext.candidateKeyTypes.get(ext.keyIndex+1));
        			System.err.println("Building new grammar...");
        			ext.keyIndex++;
        			return 1;
        		}
        	}
        	// 2. Increase recursive bound until we are at 3.
        	if(ext.recursionDepth < Configuration.recursionDepth){
        		if(!this.solFound.containsKey(ext.keyIndex+","+ext.useConditionals+","+ext.valCount+","+opsAdded)){
	        		ext.recursionDepth++;
	        		ext.keyIndex = 0;
	        		System.err.println("Recursion depth changed from " + (ext.recursionDepth-1) + " to " + ext.recursionDepth);
	        		System.err.println("Building new grammar...");
	        		return 1;
        		}
        	}
        	// 3. Turn conditionals on if they were seen in code
        	if(ext.foundConditionals && !ext.useConditionals){
        		ext.useConditionals = true;
        		ext.recursionDepth = 2;
        		ext.keyIndex = 0;
        		System.err.println("Conditionals turned on");
        		System.err.println("Building new grammar...");
        		return 1;
        	}
        	// 4. Increase number of values until 2.
        	if(ext.valCount < Configuration.maxValuesTupleSize){
        		if(!this.solFound.containsKey(ext.keyIndex+","+ext.useConditionals+","+opsAdded)){
	        		ext.valCount++;
	        		ext.recursionDepth = 2;
	        		if(ext.foundConditionals) ext.useConditionals = false;
	        		ext.keyIndex = 0;
	        		System.err.println("Val count changed from " + (ext.valCount-1) + " to " + ext.valCount);
	        		System.err.println("Building new grammar...");
	        		return 1;
        		}
        	}
        	// 5. Turn on conditionals even if they were not found in code. 
        	if(!ext.foundConditionals && !ext.useConditionals){
        		ext.useConditionals = true;
        		ext.recursionDepth = 2;
        		ext.valCount = 1;
        		ext.keyIndex = 0;
        		if(debug || true)
    				System.err.println("Conditionals turned on second phase");
        		System.err.println("Building new grammar...");
        		return 1;
        	}
        	// 6. Increase emit count 
        	if(ext.emitCount < Configuration.maxEmits && (ext.mapEmits == null || ext.mapEmits.size() == 0)){
        		ext.emitCount++;
        		ext.useConditionals = false;
        		ext.recursionDepth = 2;
        		ext.valCount = 1;
        		ext.keyIndex = 0;
        		if(debug || true)
    				System.err.println("Emit count increased from "+(ext.emitCount-1)+" to "+ext.emitCount);
        		System.err.println("Building new grammar...");
        		return 1;
        	}
        	// 7. Add new operators
        	if(opsAdded) return 2;
        	this.opsAdded = true;
        	switch(type){
            	case "bit":
            		ext.binaryOperators.add("&&");
            		ext.binaryOperators.add("||");
            		ext.useConditionals = false;
            		ext.recursionDepth = 2;
            		ext.valCount = 1;
            		ext.keyIndex = 0;
            		ext.emitCount = emitCountInit;
            		System.err.println("New operators added...");
            		System.err.println("Building new grammar...");
            		return 1;
        		default:
        			// We're done.
        			return 2;
            }
        }
	}
	
	private boolean isAlive( Process p ) {
	    try
	    {
	        p.exitValue();
	        return false;
	    } catch (IllegalThreadStateException e) {
	        return true;
	    }
	}
	
	private int verifySummary(String filename, String outputType) throws IOException, InterruptedException {
		/****** Run dafny ******/
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("dafny " + filename);

		PrintWriter writer = new PrintWriter("output/outputTempDafny.txt", "UTF-8");
		
		BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));

        // Timeout wait
        long now = System.currentTimeMillis();
        long timeoutInMillis = 1000L * 120;
        long finish = now + timeoutInMillis;
        while ( isAlive( pr ) && ( System.currentTimeMillis() < finish ) )
        {
            Thread.sleep( 10 );
        }
        int exitVal;
        if ( isAlive( pr ) )
        {
            System.err.println("Dafny timed out out after " + 120 + " seconds" );
            exitVal = 3;
        }
        else{
        	exitVal = pr.exitValue();
        	if(exitVal == 0){
            	System.err.println("Summary successfully verified");
        	}
        	else
            	System.err.println("Verifier failed with error code "+exitVal);
        }
        
		writer.close();
		
		return exitVal;
	}
	
	private int verifySummaryCSG(String filename, String outputType) throws IOException, InterruptedException {
		/****** Run dafny ******/
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("dafny " + filename);

		PrintWriter writer = new PrintWriter("output/outputTempDafny.txt", "UTF-8");

        // Timeout wait
        long now = System.currentTimeMillis();
        long timeoutInMillis = 1000L * 30;
        long finish = now + timeoutInMillis;
        while ( isAlive( pr ) && ( System.currentTimeMillis() < finish ) )
        {
            Thread.sleep( 10 );
        }
        int exitVal;
        if ( isAlive( pr ) )
        {
            System.err.println("Dafny timed out out after " + 30 + " seconds" );
            exitVal = 3;
        }
        else{
        	exitVal = pr.exitValue();
        	if(exitVal == 0){
            	System.err.println("CSG successfully verified");
        	}
        	else
            	System.err.println("CSG Verifier failed with error code "+exitVal);
        }
        
		writer.close();
		
		return exitVal;
	}
	
	@Override
	public void finish(){		
		if(debug)
			System.err.println("\n************* Finished generate scaffold complier pass *************");
	}
}