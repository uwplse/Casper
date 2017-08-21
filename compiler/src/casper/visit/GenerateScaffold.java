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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	boolean arrayOutputs;
	
	PrintWriter debugLog;
	NodeFactory nf;
	
	List<String> candidateKeyTypes;
	int keyIndex;
	
	SearchConfiguration conf;
	
	Map<String,Boolean> solFound;
	
	@SuppressWarnings("deprecation")
	public GenerateScaffold(NodeFactory nf) throws IOException{
		this.debug = false;
		this.log = false;
		
		this.nf = nf;
		
		this.id = 0;
		this.arrayOutputs = false;
		
		this.candidateKeyTypes = new ArrayList<String>();
		this.keyIndex = 0;
		
		this.conf = new SearchConfiguration();
		
		this.conf.tuplesAdded = false;
		this.conf.simpleEmits = false;
		this.conf.opsAdded = false;
		
		this.conf.stageCount = 1;
		this.conf.emitCount = 1;
		this.conf.keyTupleSize = 1;
		this.conf.valuesTupleSize = 2;
		this.conf.recursionDepth = 2;
	}
	
	public class SearchConfiguration{
		public boolean tuplesAdded;
		public boolean simpleEmits;
		public boolean opsAdded;
		
		public int stageCount;
		public int keyTupleSize;
		public int valuesTupleSize;
		public int emitCount;
		public int recursionDepth;
		
		public String keyType;
	}
	
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			if(ext.interesting){
				try {
					if(debug){
						System.err.println("Attempting to translate code fragment:-");
						System.err.println("");
						
						this.debugLog = new PrintWriter("debug.txt", "UTF-8");
						this.debugLog.print("Attempting to translate code fragment (Fragment ID: " + id + ")\n");
					}
					else{
						System.err.println("==================================================================");
						System.err.println("Attempting to translate code fragment (Fragment ID: " + id + ")\n");
					}
					
					Set<String> handledTypes = new HashSet<String>();
					
					for(Variable var : ext.outputVars){
						String sketchReduceType = casper.Util.reducerType(var.getSketchType());
						String reduceType = var.getReduceType();
						
						// Have we already handled this case?
						if(handledTypes.contains(reduceType)){
							continue;
						}
						handledTypes.add(reduceType);
						
						System.err.println("Output type: " + var.varType + "\n");
						
						// Get output variables handled under this type
						Set<Variable> sketchFilteredOutputVars = new HashSet<Variable>();
						for(Variable v : ext.outputVars){
							if(v.getReduceType().equals(reduceType)){
								sketchFilteredOutputVars.add(v);
							}
						}
						
						// Number of keys to be used
						for(Variable v : sketchFilteredOutputVars){
							String type = v.getSketchType();
							if(type.endsWith("["+Configuration.arraySizeBound+"]")){
								this.conf.keyTupleSize = 2;
								this.arrayOutputs = true;
							}
						}
						
						// Key Type options
						this.candidateKeyTypes.add("int");
						for(Variable v : ext.inputDataCollections){
							addKeyOptions(ext, v);
						}
						for(Variable v : ext.inputVars){
							addKeyOptions(ext, v);
						}
						
						// Key Type
						this.conf.keyType = this.candidateKeyTypes.get(this.keyIndex);
						
						// Emit Count
						this.conf.emitCount = 1;
						
						if(log){
							debugLog.print("Output type: " + var.varType + "\n\n");
							debugLog.print("Simple Emits: "+ this.conf.simpleEmits + "\n");
							debugLog.print("Tuples: "+ this.conf.tuplesAdded + "\n");
							debugLog.print("Ops Added: "+ this.conf.opsAdded + "\n");
							debugLog.print("Emit Count: "+ this.conf.emitCount + "\n");
							debugLog.print("Key Tuple Size: "+ this.conf.keyTupleSize + "\n");
							debugLog.print("Val Tuple Size: "+ this.conf.valuesTupleSize + "\n");
							debugLog.print("Recursion Depth: "+ this.conf.recursionDepth + "\n");
							debugLog.print("Key type: "+ this.candidateKeyTypes.get(this.keyIndex) + "\n");
							debugLog.print("Number of solutions so far: "+ext.verifiedMapEmits.size() + "\n");
							debugLog.print("Time stamp: "+System.currentTimeMillis() + "\n\n\n");
							debugLog.flush();
						}
						
						while(true){
							if(debug){
								System.err.println(ext.blockExprs);
							}
							
							if(Configuration.slow)
								System.in.read();
							
							/* Generate main scaffold */
							SketchCodeGenerator.generateScaffold(id, n, sketchFilteredOutputVars, sketchReduceType, reduceType, this.conf);
							
							if(debug){
								System.err.println(ext.blocks);
							}
							
							/* Run synthesizer to generate summary */
							System.err.println("Attempting to synthesize solution...");
							int synthesizerExitCode = runSynthesizer("output/main_"+reduceType+"_"+id+".sk", ext, sketchReduceType);
							
							if(synthesizerExitCode == 0){
								/* Run theorem prover to verify summary */
								SketchParser.parseSolution("output/main_"+reduceType+"_"+id+".txt", sketchFilteredOutputVars, ext, this.conf);
								
								DafnyCodeGenerator.generateSummary(id, n, sketchFilteredOutputVars, reduceType, sketchReduceType, this.conf);
								
								int CSGverifierExitCode = 0;
								if(conf.valuesTupleSize == 1 && false)
									CSGverifierExitCode = verifySummaryCSG("output/main_"+reduceType+"_"+id+"_CSG.dfy", sketchReduceType);
								
								if(debug){
									System.err.println(ext.mapEmits);
									System.err.println(ext.reduceExps);
								}
									
								if(CSGverifierExitCode == 0){
									int VerifierExitCode = verifySummary("output/main_"+reduceType+"_"+id+".dfy", sketchReduceType);
									if(VerifierExitCode == 0){
										ext.verifiedMapEmits.add(ext.mapEmits);
										ext.verifiedInitExps.add(ext.initExps);
										ext.verifiedReduceExps.add(ext.reduceExps);
										ext.verifiedMergeExps.add(ext.mergeExps);
										ext.verifiedSolKeyTypes.add(this.conf.keyType);
										ext.verifiedCSG.add(true);
										ext.blocks.add(new ArrayList<String>());
										ext.termValuesTemp.clear();
										ext.outVarCount = sketchFilteredOutputVars.size();
										
										if(log){
											debugLog.print("Solution Mappers: "+ext.mapEmits + "\n");
											debugLog.print("Solution Reducers: "+ext.reduceExps + "\n");
											debugLog.print("Time stamp: "+System.currentTimeMillis() + "\n\n");
											debugLog.flush();
										}
										
										//ext.generateCode.put(reduceType, true);
										ext.generateCode.put(reduceType, false);
										System.err.println("\nSearch Complete. Generating Spark Code.");
										
										if(this.log) this.debugLog.close();
										
										break;
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
									debugLog.print("Output type: " + var.varType + "\n\n");
									debugLog.print("Simple Emits: "+ this.conf.simpleEmits + "\n");
									debugLog.print("Tuples: "+ this.conf.tuplesAdded + "\n");
									debugLog.print("Ops Added: "+ this.conf.opsAdded + "\n");
									debugLog.print("Emit Count: "+ this.conf.emitCount + "\n");
									debugLog.print("Key Tuple Size: "+ this.conf.keyTupleSize + "\n");
									debugLog.print("Val Tuple Size: "+ this.conf.valuesTupleSize + "\n");
									debugLog.print("Recursion Depth: "+ this.conf.recursionDepth + "\n");
									debugLog.print("Key type: "+ this.candidateKeyTypes.get(this.keyIndex) + "\n");
									debugLog.print("Number of solutions so far: "+ext.verifiedMapEmits.size() + "\n");
									debugLog.print("Time stamp: "+System.currentTimeMillis() + "\n\n\n");
									debugLog.flush();
								}
							}
							else if(synthesizerExitCode == 2){
								if(ext.verifiedMapEmits.size()==0){
									System.err.println("Casper failed to synthesize a summary for this code fragment.\nPlease submit your code example at our"
														+ " GitHub Issues tracker (https://github.com/uwplse/Casper/issues)");
									ext.generateCode.put(reduceType, false);
									
								}
								else{
									System.err.println("\nSearch Complete. Generating Spark Code.");
									ext.generateCode.put(reduceType, true);
								}
								if(log){
									debugLog.close();
								}
								break;
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

	private void addKeyOptions(MyWhileExt ext, Variable v) {
		String vtype = v.getReduceType();
		if(casper.Util.getTypeClass(vtype) == casper.Util.PRIMITIVE){
			if(!this.candidateKeyTypes.contains(vtype))
				this.candidateKeyTypes.add(vtype);
		}
		else if(casper.Util.getTypeClass(vtype) == casper.Util.ARRAY){
			vtype = vtype.replace("[]","");
			if(!this.candidateKeyTypes.contains(vtype))
				this.candidateKeyTypes.add(vtype);
		}
		else if(casper.Util.getTypeClass(vtype) == casper.Util.OBJECT){
			if(ext.globalDataTypesFields.containsKey(vtype)){
				for(Variable fdecl : ext.globalDataTypesFields.get(vtype)){
					addKeyOptions(ext, fdecl);
				}
			}
		}
		else if(casper.Util.getTypeClass(vtype) == casper.Util.OBJECT_ARRAY){
			vtype = vtype.replace("[]","");
			if(ext.globalDataTypesFields.containsKey(vtype)){
				for(Variable fdecl : ext.globalDataTypesFields.get(vtype)){
					addKeyOptions(ext, fdecl);
				}
			}
		}
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
		
		if(debug || Configuration.slow)
			System.err.println("sketch --slv-parallel --bnd-int-range "+Configuration.intRange+" --bnd-inbits "+Configuration.inbits+" --bnd-unroll-amnt "+Configuration.loopUnrollBound+" --bnd-arr-size "+Configuration.arraySizeBound+" "+ filename);
		
		Process pr = rt.exec("sketch --slv-parallel --bnd-int-range "+Configuration.intRange+" --bnd-inbits "+Configuration.inbits+" --bnd-unroll-amnt "+Configuration.loopUnrollBound+" --bnd-arr-size "+Configuration.arraySizeBound+" "+ filename);

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
        	System.err.println("Synthesizer exited with error code: "+exitVal);
        	writer.close();
        	
        	/* Increment grammar */
        	
        	// Has current grammar been checked for all types?
        	if(this.conf.keyTupleSize > 1){
        		if(this.keyIndex < this.candidateKeyTypes.size()-1){
        			this.keyIndex++;
        			this.conf.keyType = this.candidateKeyTypes.get(this.keyIndex);
        			
        			System.err.println("\nBuilding new grammar...");
        			System.err.println("Keytype changed ("+this.candidateKeyTypes.get(this.keyIndex-1)+" -> "+this.conf.keyType +").\n");
        			
        			return 1;
        		}
        		else {
        			this.keyIndex = 0;
        			this.conf.keyType = this.candidateKeyTypes.get(this.keyIndex);
        		}
        	}
        	
        	// G1 -> G2
        	if(!this.conf.opsAdded && !this.conf.simpleEmits && !this.conf.tuplesAdded){
        		this.conf.tuplesAdded = true;
        		
    			System.err.println("\nBuilding new grammar...");
        		System.err.println("Tuples enabled (G1 -> G2).\n");
        		
        		return 1;
        	}
        	// G2 -> G3
        	if(!this.conf.opsAdded && !this.conf.simpleEmits && this.conf.tuplesAdded){
        		this.conf.simpleEmits = true;
        		
    			System.err.println("\nBuilding new grammar...");
        		System.err.println("Simple emits enabled (G2 -> G3).\n");
        		
        		return 1;
        	}
        	// G3 -> G4
        	/*if(!this.conf.opsAdded && this.conf.simpleEmits && this.conf.tuplesAdded){
        		this.conf.opsAdded = true;
        		
    			System.err.println("\nBuilding new grammar...");
        		System.err.println("New operators enabled (G3 -> G4).\n");
        		
        		return 1;
        	}*/
        	
        	// Can recursive bounds be increased?
    		if(this.conf.recursionDepth < Configuration.maxRecursionDepth){
    			this.conf.recursionDepth++;
    			
    			this.conf.tuplesAdded = this.conf.simpleEmits = this.conf.opsAdded = true;
    			
    			System.err.println("\nBuilding new grammar...");
    			System.err.println("Max expression depth increased ("+(this.conf.recursionDepth-1)+" -> "+this.conf.recursionDepth+").\n");
    			
    			return 1;
    		}
    		else if(this.conf.valuesTupleSize < Configuration.maxTupleSize){
    			this.conf.valuesTupleSize++;
    			
    			this.conf.recursionDepth = 2;
    			this.conf.tuplesAdded = this.conf.simpleEmits = this.conf.opsAdded = true;
    			
    			System.err.println("\nBuilding new grammar...");
    			System.err.println("Max value tuple size increased ("+(this.conf.valuesTupleSize-1)+" -> "+this.conf.valuesTupleSize+").\n");
    			
    			return 1;
    		}
    		else if(this.conf.keyTupleSize < Configuration.maxTupleSize){
    			this.conf.keyTupleSize++;
    			
    			this.conf.valuesTupleSize = 1;
    			this.conf.recursionDepth = 2;
    			this.conf.tuplesAdded = this.conf.simpleEmits = this.conf.opsAdded = true;
    			
    			System.err.println("\nBuilding new grammar...");
    			System.err.println("Max key tuple size increased ("+(this.conf.keyTupleSize-1)+" -> "+this.conf.keyTupleSize+").\n");
    			
    			return 1;
    		}
    		else if(this.conf.emitCount < Configuration.maxNumEmits){
    			this.conf.emitCount++;
    			
    			this.conf.keyTupleSize = (this.arrayOutputs?2:1);
    			this.conf.valuesTupleSize = 1;
    			this.conf.recursionDepth = 2;
    			this.conf.tuplesAdded = this.conf.simpleEmits = this.conf.opsAdded = true;
    			
    			System.err.println("\nBuilding new grammar...");
    			System.err.println("Max emit count increased ("+(this.conf.emitCount-1)+" -> "+this.conf.emitCount+").\n");
    			
    			return 1;
    		}
    		else if(this.conf.stageCount < Configuration.maxNumMROps){
    			this.conf.stageCount++;
    			
    			this.conf.emitCount = 1;
    			this.conf.keyTupleSize = (this.arrayOutputs?2:1);
    			this.conf.valuesTupleSize = 1;
    			this.conf.recursionDepth = 2;
    			this.conf.tuplesAdded = this.conf.simpleEmits = this.conf.opsAdded = true;
    			
    			System.err.println("\nBuilding new grammar...");
    			System.err.println("Max MR operations count increased ("+(this.conf.stageCount-1)+" -> "+this.conf.stageCount+").\n");
    			
    			return 1;
    		}
    		
    		// The entire search space has been exhausted :(
    		return 2;
        	/*
        	// 1. If we have multiple keys, try other key2 types
        	if(keyCount > 1){
        		if(ext.keyIndex < ext.candidateKeyTypes.size()-1){
        			System.err.println("\nBuilding new grammar...");
        			System.err.println("Keytype changed from " + ext.candidateKeyTypes.get(ext.keyIndex) + " to " + ext.candidateKeyTypes.get(ext.keyIndex+1) + "\n");
        			ext.keyIndex++;
        			return 1;
        		}
        	}
        	// 2. Increase recursive bound until we are at 3.
        	if(ext.recursionDepth < Configuration.maxRecursionDepth){
        		if(!this.solFound.containsKey(ext.keyIndex+","+ext.useConditionals+","+ext.valCount+","+opsAdded) || true){
	        		ext.recursionDepth++;
	        		ext.keyIndex = 0;
	        		System.err.println("\nBuilding new grammar...");
	        		System.err.println("Recursion depth changed from " + (ext.recursionDepth-1) + " to " + ext.recursionDepth + "\n");
	        		return 1;
        		}
        	}
        	// 3. Turn conditionals on if they were seen in code
        	if(ext.foundConditionals && !ext.useConditionals){
        		ext.useConditionals = true;
        		ext.recursionDepth = 2;
        		ext.keyIndex = 0;
        		System.err.println("\nBuilding new grammar...");
        		System.err.println("Conditionals turned on\n");
        		return 1;
        	}
        	// 4. Increase number of values until 2.
        	if(ext.valCount < Configuration.maxValuesTupleSize){
        		if(!this.solFound.containsKey(ext.keyIndex+","+ext.useConditionals+","+opsAdded) || true){
	        		ext.valCount++;
	        		ext.recursionDepth = 2;
	        		if(ext.foundConditionals) ext.useConditionals = false;
	        		ext.keyIndex = 0;
	        		System.err.println("\nBuilding new grammar...");
	        		System.err.println("Val count changed from " + (ext.valCount-1) + " to " + ext.valCount + "\n");
	        		return 1;
        		}
        	}
        	// 5. Turn on conditionals even if they were not found in code. 
        	if(!ext.foundConditionals && !ext.useConditionals){
        		ext.useConditionals = true;
        		ext.recursionDepth = 2;
        		ext.valCount = 1;
        		ext.keyIndex = 0;
        		System.err.println("\nBuilding new grammar...");
        		System.err.println("Conditionals turned on second phase\n");
        		return 1;
        	}
        	// 6. Increase emit count 
        	if(this.emitCount < Configuration.maxEmits && (ext.mapEmits == null || ext.mapEmits.size() == 0)){
        		this.emitCount++;
        		ext.useConditionals = false;
        		ext.recursionDepth = 2;
        		ext.valCount = 1;
        		ext.keyIndex = 0;
        		System.err.println("\nBuilding new grammar...");
				System.err.println("Emit count increased from "+(this.emitCount-1)+" to "+this.emitCount + "\n");
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
            		this.emitCount = emitCountInit;
            		System.err.println("\nBuilding new grammar...");
            		System.err.println("New operators added...\n");
            		return 1;
        		default:
        			// We're done.
        			return 2;
            }*/
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
            System.err.println("Dafny timed out out after " + 120 + " seconds\n" );
            exitVal = 3;
        }
        else{
        	exitVal = pr.exitValue();
        	if(exitVal == 0){
            	System.err.println("Summary successfully verified\n");
        	}
        	else
            	System.err.println("Verifier failed with error code "+exitVal + "\n");
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
            	System.err.println("CSG Verifier failed with error code "+exitVal + "\n");
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