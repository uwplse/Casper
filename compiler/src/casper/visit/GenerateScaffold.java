/*
 * Generate the scaffold for sketch. Some work has already been done
 * in the previous compiler passes.
 * 
 * - Maaz
 */

package casper.visit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import casper.JavaLibModel.SketchCall;
import casper.SketchCodeGenerator;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.extension.MyWhileExt.Variable;
import casper.types.IdentifierNode;
import polyglot.ast.Node;
import polyglot.ast.While;
import polyglot.visit.NodeVisitor;

public class GenerateScaffold extends NodeVisitor{
	boolean debug;
	int id;
	Set<String> arrays;
	
	@SuppressWarnings("deprecation")
	public GenerateScaffold(){
		this.debug = false;
		this.id = 0;
		this.arrays = new HashSet<String>();
	}
	
	public static class SketchVariable{
		public String name;
		public String type;
		public int category;
		
		public SketchVariable(String n, String t, int c){
			name = n;
			type = t;
			category = c;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof SketchVariable){
				return name.equals(((SketchVariable) o).name) && type.equals(((SketchVariable) o).type) && category == ((SketchVariable)o).category;
			}
			return false;
		}
		
		@Override
    	public int hashCode(){
    		return 0;
    	}
		
		@Override
		public String toString(){
			return "("+type+","+name+","+category+")";
		}
	}
	
	public void generateScaffold(Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Node n, String outputType) throws Exception{
		// Get loop extension
		MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
		
		// Create scaffold file
		PrintWriter writer = new PrintWriter("output/main_"+outputType.replace("["+Configuration.arraySizeBound+"]", "")+id+".sk", "UTF-8");
		
		// Read template
		String text = new String(Files.readAllBytes(Paths.get("templates/main_skeleton.sk")), StandardCharsets.UTF_8);
		
		// Generate include list
		String includeList = SketchCodeGenerator.generateIncludeList(ext, outputType,id);
		
		// Generate main function args
		Map<String,Integer> argsList = new HashMap<String,Integer>();
		String mainFuncArgsDecl = SketchCodeGenerator.generateMainFuncArgs(ext, sketchInputVars, sketchOutputVars, sketchLoopCounters, argsList);
		
		// Declare input / broadcast variables
		String broadcastVarsDecl = SketchCodeGenerator.declBroadcastVars(ext, argsList, sketchInputVars, sketchOutputVars);

		// Initialize output variables
		String outputVarsInit = SketchCodeGenerator.initMainOutput(ext,argsList, sketchOutputVars);

		// Use main function args to initialize input / broadcast variables
		String inputVarsInit = SketchCodeGenerator.initMainInputVars(ext, argsList, sketchInputVars, sketchOutputVars);

		// Use main function args to initialize input data collection
		String inputDataInit = SketchCodeGenerator.initMainInputData(ext, argsList, sketchInputVars, sketchOutputVars, sketchLoopCounters);
		
		// Use main function args to initialize loop counters
		String loopCountersInit = SketchCodeGenerator.initLoopCounters(ext, argsList, sketchLoopCounters);
		
		// Generate verification code
		MyStmtExt bodyExt = ((MyStmtExt) JavaExt.ext(((While)n).body()));
		String invariant = ext.invariants.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		String loopCond = ext.terminationCondition.toString(); if(!ext.condInv) loopCond = "!" + loopCond; loopCond = "("+sketchLoopCounters.get(0).name+"<"+(Configuration.arraySizeBound-1)+")";
		String loopCondFalse = loopCond; if(ext.condInv) loopCondFalse = "!" + loopCondFalse;
		String wpc = bodyExt.preConditions.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		String postC = ext.postConditions.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		String preC = ext.preConditions.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		
		// Generate weakest pre condition values initialization
		String wpcValuesInit = SketchCodeGenerator.generateWPCValuesInit(ext.wpcValues,sketchOutputVars,sketchLoopCounters);
				
		// 1. Assert loop invariant is true before the loop executes.
		String verifCode = "assert " + preC + ";\n\t";
		
		// 2. Assert loop invariant is preserved if the loop continues: I && loop condition is true --> wp(c, I), 
		//    where wp(c, I) is the weakest precondition of the body of the loop with I as the post-condition
		verifCode += "if(" + invariant + " && " + loopCond + ") {\n\t\t"+wpcValuesInit+"assert " + wpc + ";\n\t}\n\t";
		
		// 2. Assert loop invariant implies the post condition if the loop terminates: I && loop condition is false --> POST
		verifCode += "if(" + invariant + " && " + loopCondFalse + ") {\n\t\tassert " + postC + ";\n\t}";
	
		// Generate post condition args
		String postConditionArgsDecl = SketchCodeGenerator.generatePostConditionArgsDecl(ext,sketchOutputVars,sketchLoopCounters,ext.postConditionArgsOrder.get(outputType));
		
		// Generate map function call args
		String mapArgsCall = SketchCodeGenerator.generateMapArgsCall(ext,sketchOutputVars,sketchLoopCounters,ext.postConditionArgsOrder.get(outputType));
		
		// Generate collect function args. Give initial values for output variables.
		String collecArgsDecl = SketchCodeGenerator.generateCollectArgsDecl(outputType,sketchOutputVars);
		
		// Generate post condition body
		String postCondition = SketchCodeGenerator.generatePostCondition(outputType,sketchOutputVars);
		
		// Generate loop invariant args
		String loopInvariantArgsDecl = SketchCodeGenerator.generateLoopInvariantArgsDecl(ext,sketchOutputVars,sketchLoopCounters,ext.postConditionArgsOrder.get(outputType));
		
		// Generate loop invariant body
		String loopInvariant = SketchCodeGenerator.generateLoopInvariant(outputType,sketchOutputVars,sketchLoopCounters,ext.loopInvariantArgsOrder.get(outputType),ext.hasInputData);
		
		// Generate map function args declaration
		String mapArgsDecl = SketchCodeGenerator.generateMapArgsDecl(ext,sketchOutputVars,sketchLoopCounters,ext.postConditionArgsOrder.get(outputType));
		
		// Generate map function loop inits
		String mapLoopInit = SketchCodeGenerator.generateMapLoopInit(sketchLoopCounters,ext.terminationCondition);
		
		// Generate map function loop condition
		String mapLoopCond = SketchCodeGenerator.generateMapLoopCond(sketchLoopCounters,ext.terminationCondition,ext.hasInputData);
		
		// Generate do map function args
		String domapFuncArgsCall = SketchCodeGenerator.generateDomapFuncArgsCall(ext,sketchLoopCounters);
		
		// Generate map function loop increment
		String mapLoopIncr = SketchCodeGenerator.generateMapLoopIncr(sketchLoopCounters,ext.incrementExps);
		
		// Output reconstruction
		String outputRecon = SketchCodeGenerator.reconstructOutput(outputType,sketchOutputVars);
				
		// Generate map function args declaration code
		String domapFuncArgsDecl = SketchCodeGenerator.generateDomapFuncArgsDecl(ext,sketchLoopCounters);
		
		// Generate domap function emit code
		String domapEmits = SketchCodeGenerator.generateDomapEmits(ext,sketchLoopCounters,outputType);
		
		// Generate int expression generator for map
		String intMapGen = SketchCodeGenerator.generateMapGrammarInlined("int",sketchInputVars,sketchOutputVars,sketchLoopCounters,ext);
		
		// Generate bit expression generator for map
		String bitMapGen = SketchCodeGenerator.generateMapGrammarInlined("bit",sketchInputVars,sketchOutputVars,sketchLoopCounters,ext);
		
		// Generate string expression generator for map
		String stringMapGen = SketchCodeGenerator.generateMapGrammarInlined("String",sketchInputVars,sketchOutputVars,sketchLoopCounters,ext);
		
		// Generate reduce function emit code
		String doreduceEmits = SketchCodeGenerator.generateDoreduceEmits(ext,sketchLoopCounters,sketchOutputVars.size(),outputType);
		
		// Generate reduce/fold expression generator
		String reduceGen = SketchCodeGenerator.generateReduceGrammarInlined(ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,outputType);
		
		// Modify template
		text = text.replace("<include-libs>",includeList);
		text = text.replace("<decl-broadcast-vars>",broadcastVarsDecl);
		text = text.replace("<main-args-decl>",mainFuncArgsDecl);
		text = text.replace("<output-vars-initialize>",outputVarsInit);
		text = text.replace("<input-data-initialize>",inputDataInit);
		text = text.replace("<input-vars-initialize>",inputVarsInit);
		text = text.replace("<loop-counters-initialize>",loopCountersInit);
		text = text.replace("<verif-conditions>",verifCode);
		text = text.replace("<post-cond-args-decl>",postConditionArgsDecl);
		text = text.replace("<map-args-call>",mapArgsCall);
		text = text.replace("<collect-args-call>",collecArgsDecl.replace(outputType+" ", ""));
		text = text.replace("<post-cond-body>",postCondition);
		text = text.replace("<loop-inv-args-decl>",loopInvariantArgsDecl);
		text = text.replace("<loop-inv-body>",loopInvariant);
		text = text.replace("<generate-int-map>",intMapGen);
		text = text.replace("<generate-bit-map>",bitMapGen);
		text = text.replace("<generate-string-map>",stringMapGen);
		text = text.replace("<domap-func-args-decl>",domapFuncArgsDecl);
		text = text.replace("<do-map-emits>",domapEmits);
		text = text.replace("<generate-reduce-exp>", reduceGen);
		text = text.replace("<do-reduce-emits>",doreduceEmits);
		text = text.replace("<map-args-decl>",mapArgsDecl);
		text = text.replace("<map-loop-init>",mapLoopInit);
		text = text.replace("<map-loop-cond>",mapLoopCond);
		text = text.replace("<domap-func-args-call>",domapFuncArgsCall);
		text = text.replace("<map-loop-increment>",mapLoopIncr);
		text = text.replace("<collect-func-args-decl>",collecArgsDecl);
		text = text.replace("<output-reconstruction>",outputRecon);
		
		// Save
		writer.print(text);
		writer.close();
	}
	
	/*
	// Generate custom array of given typeName
	public void generateArray(String typeName) throws Exception{
		// Create array class file
		PrintWriter writer = new PrintWriter("output/"+typeName+"Array.sk", "UTF-8");
		
		// Read template
		String text = new String(Files.readAllBytes(Paths.get("templates/array_skeleton.sk")), StandardCharsets.UTF_8);
		
		// Modify template
		text = text.replace("<var-name>", typeName);
		text = text.replace("<var-name-sc>", typeName.toLowerCase());
		text = text.replace("<include-libs>","include \"" + typeName + ".sk\";");
		
		// Save
		writer.print(text);
		writer.close();
	}
	*/
	
	/*
	// Generate code that defines the required map class
	private void generateMap(String type) throws IOException {
		String patternString = "(Str|Int){1}List|(Str|Int){2}Map";
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(type);
		if(matcher.matches()){
			String mapType = type.substring(0,6);
			String mapTypeFunc = mapType.toLowerCase();
			String keyType = javatosketch.Util.convertAbbrToType(mapTypeFunc.substring(0,3));
			String valType = javatosketch.Util.convertAbbrToType(mapTypeFunc.substring(3,6));
			
			// Create array class file
			PrintWriter writer = new PrintWriter("output/"+type+".sk", "UTF-8");
			
			// Read template
			String text = new String(Files.readAllBytes(Paths.get("templates/map_skeleton.sk")), StandardCharsets.UTF_8);
			
			String verifCode = "";
			if(javatosketch.Util.isBasicType(valType) == 1 || javatosketch.Util.isBasicType(valType) == 2){
				verifCode += "assert ptr1.value == ptr2.value;";
			}
			else if(javatosketch.Util.isBasicType(valType) == 3){
				verifCode += "assert "+valType.substring(0,6).toLowerCase()+"_map_equal(ptr1.value, ptr2.value);";
			}	
			
			// Modify template
			text = text.replace("<map-type>", mapType);
			text = text.replace("<map-type-func>",mapTypeFunc);
			text = text.replace("<key-type>", keyType);
			text = text.replace("<value-type>",valType);
			text = text.replace("<value-compare-assert>",verifCode);
			
			// Save
			writer.print(text);
			writer.close();
		}
	}
	*/

	// Translate input variables to sketch types
	public void generateSketchInputVars(Node n, Set<SketchVariable> sketchInputVars) throws Exception{
		MyWhileExt ext = (MyWhileExt)JavaExt.ext(n);
		for(Variable var : ext.inputVars){
			var.varName = var.varName.replace("$", "");
			switch(var.getType()){
				case "boolean":
				case "char":
				case "short":
				case "byte":
				case "int":
				case "long":
				case "float":
				case "double":
				case "String":
					sketchInputVars.add(new SketchVariable(var.varName,casper.Util.getSketchType(var.getType()),var.category));
					break;
				case "boolean[]":
				case "char[]":
				case "short[]":
				case "byte[]":
				case "int[]":
				case "long[]":
				case "float[]":
				case "double[]":
				case "String[]":
					sketchInputVars.add(new SketchVariable(var.varName,casper.Util.getSketchType(var.getType()),var.category));
					break;
				default:
					if(var.category == Variable.VAR){
						sketchInputVars.add(new SketchVariable(var.varName,var.getType(),var.category));
					}
					else if(var.category == Variable.FIELD_ACCESS){
						sketchInputVars.add(new SketchVariable(var.varName,var.getType(),var.category));
					}
					else if(var.category == Variable.ARRAY_ACCESS){
						sketchInputVars.add(new SketchVariable(var.varName, var.getType().replace("[]", "["+Configuration.arraySizeBound+"]"),var.category));
					}
					else if(var.category == Variable.CONST_ARRAY_ACCESS){
						sketchInputVars.add(new SketchVariable(var.varName, var.getType().replace("[]", "["+Configuration.arraySizeBound+"]"),var.category));
					}
					break;
			}
		}
	}
	
	// Translate output variables to sketch types
	public void generateSketchOutputVars(Node n, Set<SketchVariable> sketchOutputVars) throws Exception{
		MyWhileExt ext = (MyWhileExt)JavaExt.ext(n);
		for(Variable var : ext.outputVars){
			var.varName = var.varName.replace("$", "");
			switch(var.getType()){
				case "boolean":
				case "char":
				case "short":
				case "byte":
				case "int":
				case "long":
				case "float":
				case "double":
				case "String":
					sketchOutputVars.add(new SketchVariable(var.varName,casper.Util.getSketchType(var.getType()),var.category));
					break;
				case "boolean[]":
				case "char[]":
				case "short[]":
				case "byte[]":
				case "int[]":
				case "long[]":
				case "float[]":
				case "double[]":
				case "String[]":
					sketchOutputVars.add(new SketchVariable(var.varName,casper.Util.getSketchType(var.getType()),var.category));
					break;
				default:
					if(var.category == Variable.VAR){
						sketchOutputVars.add(new SketchVariable(var.varName,var.getType(),var.category));
					}
					else if(var.category == Variable.FIELD_ACCESS){
						sketchOutputVars.add(new SketchVariable(var.varName,var.getType(),var.category));
					}
					else if(var.category == Variable.ARRAY_ACCESS){
						sketchOutputVars.add(new SketchVariable(var.varName, var.getType().replace("[]", "["+Configuration.arraySizeBound+"]"),var.category));
					}
					else if(var.category == Variable.CONST_ARRAY_ACCESS){
						sketchOutputVars.add(new SketchVariable(var.varName, var.getType().replace("[]", "["+Configuration.arraySizeBound+"]"),var.category));
					}
					break;
			}
		}
	}
	
	// Translate loop counters to sketch types
	public void generateSketchLoopCounters(Node n, List<SketchVariable> sketchLoopCounters) throws Exception{
		MyWhileExt ext = (MyWhileExt)JavaExt.ext(n);
		for(Variable var : ext.loopCounters){
			var.varName = var.varName.replace("$", "");
			switch(var.getType()){
				case "boolean":
				case "char":
				case "short":
				case "byte":
				case "int":
				case "long":
				case "float":
				case "double":
				case "String":
					sketchLoopCounters.add(new SketchVariable(var.varName,casper.Util.getSketchType(var.getType()),var.category));
					break;
				case "boolean[]":
				case "char[]":
				case "short[]":
				case "byte[]":
				case "int[]":
				case "long[]":
				case "float[]":
				case "double[]":
				case "String[]":
					sketchLoopCounters.add(new SketchVariable(var.varName,casper.Util.getSketchType(var.getType()),var.category));
					break;
				default:
					if(var.category == Variable.VAR){
						sketchLoopCounters.add(new SketchVariable(var.varName,var.getType(),var.category));
					}
					else if(var.category == Variable.FIELD_ACCESS){
						sketchLoopCounters.add(new SketchVariable(var.varName,var.getType(),var.category));
					}
					else if(var.category == Variable.ARRAY_ACCESS){
						sketchLoopCounters.add(new SketchVariable(var.varName, var.getType().replace("[]", "["+Configuration.arraySizeBound+"]"),var.category));
					}
					else if(var.category == Variable.CONST_ARRAY_ACCESS){
						sketchLoopCounters.add(new SketchVariable(var.varName, var.getType().replace("[]", "["+Configuration.arraySizeBound+"]"),var.category));
					}
					break;
			}
		}
	}
	
	// Generate code that defines the output class
	public void generateOutputClass(Set<SketchVariable> sketchOutputVars) throws Exception{
		// Create output class file
		PrintWriter writer = new PrintWriter("output/output"+id+".sk", "UTF-8");
		
		// Read template
		String text = new String(Files.readAllBytes(Paths.get("templates/output_skeleton.sk")), StandardCharsets.UTF_8);
		
		// Modify template
		String varList = "";
		for(SketchVariable var : sketchOutputVars){
			switch(var.type){
				case "boolean":
				case "char":
				case "short":
				case "byte":
				case "int":
				case "long":
				case "float":
				case "double":
				case "String":
					varList += var.type + " " + var.name + ";\n\t";
					break;
				case "boolean[]":
				case "char[]":
				case "short[]":
				case "byte[]":
				case "int[]":
				case "long[]":
				case "float[]":
				case "double[]":
				case "String[]":
					varList += var.type + " " + var.name + ";\n\t";
					break;
				default:
					varList += var.type + " " + var.name + ";\n\t";
					break;
			}
		}

		text = text.replace("<output-variable-list>", varList);
		writer.print(text);
		writer.close();
	}
	
	// Generate code that defines the kv-pair list class - used by mapreduce module
	private void generateKvListClass(Node n, String outputType) throws IOException {
		// Create scaffold file
		PrintWriter writer = new PrintWriter("output/kvpair_list_"+outputType.replace("["+Configuration.arraySizeBound+"]", "")+id+".sk", "UTF-8");
		
		// Read template
		String text = new String(Files.readAllBytes(Paths.get("templates/kvpair_list_skeleton.sk")), StandardCharsets.UTF_8);
			
		// Modify template
		text = text.replace("<value-type>",outputType.replace("["+Configuration.arraySizeBound+"]", ""));
		
		switch(outputType.replace("["+Configuration.arraySizeBound+"]", "")){
			case "bit":
				text = text.replace("<expressions>","bit option0 = ptr.value || initial;\n\tbit option1 = ptr.value && initial;");
				text = text.replace("<options>"," {| option0 | option1 |}");
				break;
			case "int":
			case "float":
			case "double":
				text = text.replace("<expressions>",outputType.replace("["+Configuration.arraySizeBound+"]", "") + " option0 = ptr.value + initial;");
				text = text.replace("<options>"," {| option0 |}");
				break;
			case "char":
			default:
				text = text.replace("<expressions>","");
				text = text.replace("<options>","ptr.value");
				break;
		}
			
		// Save
		writer.print(text);
		writer.close();
	}

	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While){
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
					Set<SketchVariable> sketchInputVars = new HashSet<SketchVariable>();
					Set<SketchVariable> sketchOutputVars = new HashSet<SketchVariable>();
					List<SketchVariable> sketchLoopCounters = new ArrayList<SketchVariable>();
					
					/* Translate input variables to sketch appropriate types */
					generateSketchInputVars(n, sketchInputVars);
					
					/* Translate output variables to sketch appropriate types */
					generateSketchOutputVars(n, sketchOutputVars);
					
					/* Translate loop counters to sketch appropriate types */
					generateSketchLoopCounters(n, sketchLoopCounters);
					
					/* Generate output class */
					generateOutputClass(sketchOutputVars);
					
					for(SketchVariable var : sketchOutputVars){
						// Have we already handled this case?
						if(handledTypes.contains(var.type)){
							continue;
						}
						handledTypes.add(var.type);
						
						for(Variable v : ext.outputVars){
							if(v.varName.equals(var.name)){
								System.err.println("Output type: " + v.varType);
								break;
							}
						}
						
						Set<SketchVariable> sketchFilteredOutputVars = new HashSet<SketchVariable>();
						for(SketchVariable v : sketchOutputVars){
							if(v.type == var.type){
								sketchFilteredOutputVars.add(v);
							}
						}
						
						// Calculate number of emits
						Configuration.emitCount = 0;
						for(SketchVariable v : sketchOutputVars){ if(var.type.equals(v.type)) Configuration.emitCount++; }
						
						/* Generate Key-Value pair list */
						generateKvListClass(n, var.type);
						
						while(true){
							/* Generate main scaffold */
							generateScaffold(sketchInputVars, sketchFilteredOutputVars, sketchLoopCounters, n, var.type);
							
							/* Run synthesizer to generate summary */
							int exitCode = runSynthesizer("output/main_"+var.type.replace("["+Configuration.arraySizeBound+"]","")+id+".sk",var.type,ext);
							if(exitCode == 0){
								/* Run theorem prover to verify summary */
								verifySummary("output/main_"+var.type.replace("["+Configuration.arraySizeBound+"]","")+id+".dfy", n, ext, sketchInputVars, sketchFilteredOutputVars, sketchLoopCounters, var.type);
								ext.generateCode.put(var.type, true);
								break;
							}
							else if(exitCode == 1){
								// Clear data structures before next run
								ext.inputDataCollections.clear();
							}
							else if(exitCode == 2){
								System.err.println("Casper failed to synthesize a summary for this code fragment.\nPlease submit your code example at our"
													+ " GitHub Issues tracker (https://github.com/uwplse/Casper/issues)");
								ext.generateCode.put(var.type, false);
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

	private int runSynthesizer(String filename, String type, MyWhileExt ext) throws IOException, InterruptedException {		
		Runtime rt = Runtime.getRuntime();
		
		if(debug)
			System.err.println("sketch --slv-simiters 200 --slv-lightverif --slv-parallel --bnd-int-range 20 -V 10 --bnd-inbits "+Configuration.inbits+" --bnd-unroll-amnt "+(((int)Math.pow(Configuration.inbits,2)-1)*Configuration.emitCount)+" "+ filename);
		
		Process pr = rt.exec("sketch --slv-simiters 200 --slv-lightverif --slv-parallel --bnd-int-range 20 -V 10 --bnd-inbits "+Configuration.inbits+" --bnd-unroll-amnt "+(((int)Math.pow(Configuration.inbits,2)-1)*Configuration.emitCount)+" "+ filename);

		PrintWriter writer = new PrintWriter("output/outputTempSketch.txt", "UTF-8");
		
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
	
	private String resolve(String exp, List<String> mapLines, int i, MyWhileExt ext) {
		String[] binaryOps = {"\\+","\\-","\\*","\\/","\\%","\\&\\&","\\|\\|","\\=\\=","\\!\\=","\\>","\\>\\=","\\<","\\<\\=","\\^","\\&","\\|","\\>\\>\\>","\\>\\>","\\<\\<","instanceof"};
		String[] unaryOps = {"!"};
		
		// Remove ";" at the end
		if(exp.charAt(exp.length()-1) == ';'){
			exp = exp.substring(0,exp.length()-1);
		}
		
		// If binary expression
		for(String op_esc : binaryOps){
			String op = op_esc.replace("\\", "");
			if(exp.contains(op)){
				String[] expComponents = exp.split(op_esc);
				return resolve(expComponents[0].trim(),mapLines,i,ext) + " " + op + " " + resolve(expComponents[1].trim(),mapLines,i,ext);
			}
		}
		// If unary expression
		for(String op : unaryOps){
			if(exp.contains(op)){
				String[] expComponents = exp.split(op);
				return op + " " + resolve(expComponents[0].trim(),mapLines,i,ext);
			}
		}
		// If generated variable
		Pattern r = Pattern.compile("^[_][a-zA-Z_$0-9]*$");
		Matcher m = r.matcher(exp);
		if(m.matches()){
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
				}
				// Function call
				r = Pattern.compile("^([a-zA-Z_$][a-zA-Z_$0-9]*)\\((..*)\\);$");
				m = r.matcher(mapLines.get(i));
				if(m.matches()){
					String funcName = m.group(1);
					List<String> args = new ArrayList<String>();
					for(String arg : m.group(2).split(",")){
						args.add(arg.trim());
					}
					List<String> argsR = new ArrayList<String>();
					for(String arg : args){
						if(!arg.equals(exp)){
							argsR.add(resolve(arg,mapLines,i,ext));
						}
						else {
							argsR.add(exp);
						}
					}
					for(SketchCall op : ext.methodOperators){
						if(funcName.equals(op.name)){
							String expR = op.resolve(exp,argsR);
							if(!expR.equals(exp)){
								return expR;
							}
							break;
						}
					}
				}
				
				i = i - 1;
			}
		}
		// If original variable
		r = Pattern.compile("^[a-zA-Z$][a-zA-Z_$0-9]*$");
		m = r.matcher(exp);
		if(m.matches()){
			// Sketch appends this to global variables
			if(exp.contains("__ANONYMOUS")){
				exp = exp.substring(0,exp.indexOf("__ANONYMOUS"));
			}
			return exp;
		}
		// If object field, with generated container
		r = Pattern.compile("^([_][a-zA-Z_$0-9]*).([a-zA-Z_$][a-zA-Z_$0-9]*)$");
		m = r.matcher(exp);
		if(m.matches()){
			String container = m.group(1);
			String field = m.group(2);
			
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){					
					String[] stmt = mapLines.get(i).split("=");
					
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+container+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim() + "." + field,mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		// If object field with original container
		r = Pattern.compile("^([a-zA-Z$][a-zA-Z_$0-9]*).([a-zA-Z_$][a-zA-Z_$0-9]*)$");
		m = r.matcher(exp);
		if(m.matches()){
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		// If array access with generated container
		r = Pattern.compile("^([_][a-zA-Z_$0-9]*)\\[(.*?)\\]$");
		m = r.matcher(exp);
		if(m.matches()){
			String container = m.group(1);
			String index = m.group(2);
			
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+container+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim()+"["+index+"]",mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+index+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(container+"["+stmt[1].trim()+"]",mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		// If array access with original container
		r = Pattern.compile("^([a-zA-Z$][a-zA-Z_$0-9]*)\\[(.*?)\\]$");
		m = r.matcher(exp);
		if(m.matches()){
			String container = m.group(1);
			String index = m.group(2);
			
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+container+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim()+"["+index+"]",mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+index+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(container+"["+stmt[1].trim()+"]",mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		
		return exp;
	}
	
	public class KvPair{
		public String key;
		public String key2;
		public String value;
		KvPair(String k, String k2, String v){ key = k; key2 = k2; value = v; }
	}
	
	private void verifySummary(String filename, Node n, MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, String outputType) throws IOException, InterruptedException {
		// Read sketch output
		BufferedReader br = new BufferedReader(new FileReader("output/outputTempSketch.txt"));
		
		String text;
		try {
		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		        sb.append(line);
		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
		    text = sb.toString();
		} finally {
		    br.close();
		}
		text = text.replace("@KvPairList", "");
		
		/****** Extract candidate summary from sketch output ******/
		
		// Extract map function
		Pattern r = Pattern.compile("void do_map(.*?)\\{(.*?)return;\n\\}",Pattern.DOTALL);
		Matcher m = r.matcher(text);
		m.find();
		String map = m.group(0);
			
		List<String> mapLines = new ArrayList<String>();
		for(String line : map.split("\n")){
			mapLines.add(line.trim());
		}

		// Extract map emits
		r = Pattern.compile("Pair(.*?)= new Pair\\(\\);");
		m = r.matcher(map);
		List<String> mapKvpNames = new ArrayList<String>();
		while(m.find()){
			mapKvpNames.add(m.group(1).trim());
		}
		
		List<KvPair> mapEmits = new ArrayList<KvPair>();
		String mapKeyType = "";
		for(String kvpName : mapKvpNames){
			String mapKey = "";
			String mapKey2 = "";
			String mapValue = "";
			mapKeyType = "";
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".intkey = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					mapKey = m.group(1).trim();
					mapKey = resolve(mapKey,mapLines,i,ext);
					mapKeyType = "int";
					break;
				}
			}
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".intkey2 = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					mapKey2 = m.group(1).trim();
					mapKey2 = resolve(mapKey2,mapLines,i,ext);
					mapKeyType = "("+mapKeyType+",int)";
					break;
				}
			}
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".stringkey = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					if(mapKeyType.equals("")){
						mapKey = m.group(1).trim();
						mapKey = resolve(mapKey,mapLines,i,ext);
						mapKeyType = "string";
					}
					else{
						mapKey2 = m.group(1).trim();
						mapKey2 = resolve(mapKey2,mapLines,i,ext);
						mapKeyType = "("+mapKeyType+",string)";
					}
					break;
				}
			}
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".(.*?)value = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					mapValue = m.group(2).trim();
					mapValue = resolve(mapValue,mapLines,i,ext);
					if(outputType.equals("bit")){
						switch(mapValue){
							case "0":
								mapValue = "false";
								break;
							case "1":
								mapValue = "true";
								break;
						}
					}
					break;
				}
			}
			if(debug){
				if(mapKey2.equals("")){
					System.err.println(mapKey + ", " + mapValue);
					System.err.println(mapKeyType);
				}
				else{
					System.err.println("(" + mapKey + "," + mapKey2 + "), " + mapValue);
					System.err.println(mapKeyType);
				}
			}
		
			mapEmits.add(new KvPair(mapKey,mapKey2,mapValue));
		}
		
		// Extract reduce emits
		r = Pattern.compile("void do_reduce(.*?)\\{(.*?)return;\n\\}",Pattern.DOTALL);
		m = r.matcher(text);
		m.find();
		String reduce = m.group(0);
		
		List<String> reduceLines = new ArrayList<String>();
		for(String line : reduce.split("\n")){
			reduceLines.add(line.trim());
		}
		
		r = Pattern.compile("while\\(values != \\(null\\)\\)\\s*\\{(.*?)\\}",Pattern.DOTALL);
		m = r.matcher(text);
		m.find();
		String agg = m.group(0);
		
		List<String> aggLines = new ArrayList<String>();
		for(String line : agg.split("\n")){
			aggLines.add(line.trim());
		}
		
		String reduceInitValue = "";
		for(int i=0; i<reduceLines.size(); i++){
			r = Pattern.compile("_out.(.*?)value = (.*);");
			m = r.matcher(reduceLines.get(i));
			if(m.find()){
				reduceInitValue = m.group(2).trim();
				if(outputType.equals("bit")){
					switch(reduceInitValue){
						case "0":
							reduceInitValue = "false";
							break;
						case "1":
							reduceInitValue = "true";
							break;
					}
				}
				break;
			}
		}
		String reduceValue = "";
		for(int i=0; i<aggLines.size(); i++){
			r = Pattern.compile("_out.(.*?)value = (.*)");
			m = r.matcher(aggLines.get(i));
			if(m.find()){
				reduceValue = m.group(2).trim();
				reduceValue = resolve(reduceValue,aggLines,i,ext);
				if(outputType.equals("bit")){
					switch(reduceValue){
						case "0":
							reduceValue = "false";
							break;
						case "1":
							reduceValue = "true";
							break;
					}
				}
				break;
			}
		}
		
		if(debug)
			System.err.println(reduceInitValue + ", " + reduceValue);
		
		/****** Generate dafny verification file ******/
		
		// Generate main harness args
		String harnessArgs = DafnyCodeGenerator.generateDafnyHarnessArgs(ext,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		
		// Generate require statements for dafny main function
		String mainReqStmts = DafnyCodeGenerator.generateRequireStatements(sketchOutputVars,sketchLoopCounters,ext.wpcValues);
		
		// Init variables in main
		String initVars = DafnyCodeGenerator.generateVarInit(ext,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		
		// Generate verification code
		String preC = DafnyCodeGenerator.generatePreCondition(ext,outputType,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		MyStmtExt bodyExt = ((MyStmtExt) JavaExt.ext(((While)n).body()));
		String loopCond = "("+sketchLoopCounters.get(0).name+"<|"+ext.inputDataCollections.get(0).name+"|)";
		String loopCondFalse = loopCond; if(ext.condInv) loopCondFalse = "!" + loopCondFalse;
		String invariant = DafnyCodeGenerator.generateInvariant(ext,outputType,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		String lemma = invariant.replace("loopInvariant", "Lemma");
		String wpc = DafnyCodeGenerator.generateWPC(ext,outputType,sketchInputVars,sketchOutputVars,sketchLoopCounters,bodyExt);
		String postC = DafnyCodeGenerator.generatePostCondStmt(ext,outputType,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		
		// Weakest pre condition value updates
		String wpcInits = DafnyCodeGenerator.generateWPCInits(ext.wpcValues,sketchOutputVars,sketchLoopCounters);
		
		// 1. Assert loop invariant is true before the loop executes.
		String verifCode = "assert " + preC + ";\n\n\t";
		
		// 2. Assert loop invariant is preserved if the loop continues: I && loop condition is true --> wp(c, I), 
		//    where wp(c, I) is the weakest precondition of the body of the loop with I as the post-condition
		verifCode += "if(" + invariant + " && " + loopCond + ")\n\t{\n\t\t" + lemma + ";\n\t\t"+wpcInits+"assert " + wpc + ";\n\t}\n\n\t";
		
		// 2. Assert loop invariant implies the post condition if the loop terminates: I && loop condition is false --> POST
		verifCode += "if(" + invariant + " && " + loopCondFalse + ")\n\t{\n\t\tassert " + postC + ";\n\t}";
		
		// Generate args for invariant and post condition
		String invPcArgs = DafnyCodeGenerator.generateInvPcAargs(ext,ext.postConditionArgsOrder.get(outputType),sketchOutputVars,sketchLoopCounters,sketchInputVars);
		
		// Generate invariant
		String loopInv = DafnyCodeGenerator.generateLoopInv(ext, outputType, sketchOutputVars, sketchLoopCounters, sketchInputVars, reduceValue);
		
		// Generate post condition
		String postCond = DafnyCodeGenerator.generatePostCond(ext, outputType, sketchOutputVars, sketchLoopCounters, sketchInputVars, reduceValue);
		
		// Generate mapper function args declaration
		String mapperArgsDecl = DafnyCodeGenerator.generateMapperArgsDecl(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		
		// Generate mapper function args call
		String mapperArgsCall = DafnyCodeGenerator.generateMapperArgsCall(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		String mapperArgsCallInd = DafnyCodeGenerator.generateMapperArgsCallInd(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		String mapperArgsCallInd2 = DafnyCodeGenerator.generateMapperArgsCallInd2(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		
		// Generate domap pre condition
		String preCondDomap = DafnyCodeGenerator.generateDomapPreCond(ext, sketchLoopCounters);
		
		// Generate map pre condition
		String preCondMap = DafnyCodeGenerator.generateMapPreCond(ext, sketchLoopCounters);
		
		// Generate map emits
		String domapEmits = DafnyCodeGenerator.generateMapEmits(ext, mapEmits);
		
		// Do reduce args declaration
		String reducerArgsDecl = DafnyCodeGenerator.generatedReducerArgsDecl(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		
		// Do reduce args call
		String reducerArgsCall = DafnyCodeGenerator.generatedReducerArgsCall(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		
		// Generate map outputType
		String domapEmitType = DafnyCodeGenerator.generateDomapEmitType(mapKeyType,outputType,mapEmits);

		// Generate do reduce key type
		String doreduceKeyType = DafnyCodeGenerator.generateDoreduceKeyType(mapKeyType,mapEmits);
		
		// Generate reduce expression
		String reduceExp = DafnyCodeGenerator.generateReduceExp(reduceValue,reduceInitValue);
		
		// Generate reduce expression for lemma
		String reduceExpLemma = DafnyCodeGenerator.generateReduceExpLemma(reduceValue);
		
		// Generate lemma proof for map emits
		String emitLemmas = DafnyCodeGenerator.generateEmitLemmas(outputType,mapEmits,mapperArgsCall,mapperArgsCallInd2,reduceValue);
		
		// Generate terminate condition for map recursion
		String tCond = DafnyCodeGenerator.generateMapTerminateCondition(sketchLoopCounters);
		
		// Plug all the generated code into the template
		String template = new String(Files.readAllBytes(Paths.get("templates/dafny_skeleton.dfy")), StandardCharsets.UTF_8);
		PrintWriter writer = new PrintWriter(filename, "UTF-8");
		
		template = template.replace("<harness-args>", harnessArgs);
		template = template.replace("<init-vars>", initVars);
		template = template.replace("<verif-code>", verifCode);
		template = template.replace("<inv-pc-args>", invPcArgs);
		template = template.replace("<loop-inv>", loopInv);
		template = template.replace("<post-cond>", postCond);
		template = template.replace("<mapper-args-decl>", mapperArgsDecl);
		template = template.replace("<mapper-args-call>", mapperArgsCall);
		template = template.replace("<loop-counter-range-domap>", preCondDomap);
		template = template.replace("<loop-counter-range-map>", preCondMap);
		template = template.replace("<domap-emits>", domapEmits);
		template = template.replace("<domap-emit-type>", domapEmitType);
		template = template.replace("<mapper-args-call-inductive>", mapperArgsCallInd);
		template = template.replace("<doreduce-key-type>", doreduceKeyType);
		template = template.replace("<output-type>", casper.Util.getDafnyType(outputType.replace("["+Configuration.arraySizeBound+"]","")));
		template = template.replace("<reduce-init-value>", reduceInitValue);
		template = template.replace("<reduce-exp>",reduceExp);
		template = template.replace("<reduce-exp-lemma>",reduceExpLemma);
		template = template.replace("<invariant>",invariant + " && " + loopCond);
		template = template.replace("<wpc>",wpc);
		template = template.replace("<mapper-args-call-inductive-2>", mapperArgsCallInd2);
		template = template.replace("<emit-lemmas>", emitLemmas);
		template = template.replace("<terminate-condition>", tCond);
		template = template.replace("<main-requires>", mainReqStmts);
		template = template.replace("<inv-requires>", mainReqStmts);
		template = template.replace("<pcond-requires>", mainReqStmts);
		template = template.replace("<lemma-requires>", mainReqStmts);
		template = template.replace("<reducer-args-decl>", reducerArgsDecl);
		template = template.replace("<reducer-args-call>", reducerArgsCall);
		
		writer.print(template);
		writer.close();
		
		/****** Run dafny ******/
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("dafny "+ filename);

		writer = new PrintWriter("output/outputTempDafny.txt", "UTF-8");
		
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
		
		// If summary successfully verifies, save relevant info
		ext.mapEmits = mapEmits;
		ext.mapKeyType = mapKeyType;
		ext.reduceExp = reduceValue;
	}

	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		return n;
	}
	
	@Override
	public void finish(){
		/* Generate required array data structures */
		try {
			// Generate any arrays that were used in the generated scaffold
			// This is only necessary when using custom array types
			//for(String array : arrays){
				//generateArray(array);
			//}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(debug)
			System.err.println("\n************* Finished generate scaffold complier pass *************");
	}
}