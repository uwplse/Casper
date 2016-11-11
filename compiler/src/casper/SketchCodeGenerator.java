package casper;

import java.io.IOException;
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

import casper.JavaLibModel.SketchCall;
import casper.SketchParser.KvPair;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.ArrayUpdateNode;
import casper.types.ConditionalNode;
import casper.types.ConstantNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.types.Variable;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;

public class SketchCodeGenerator {
	
	public static void generateScaffold(int id, Node n, Set<Variable> sketchFilteredOutputVars, String sketchReducerType, String reducerType) throws Exception{
		// Get node extension
		MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
		
		// Generate Utils File
		PrintWriter writer = new PrintWriter("output/utils.sk", "UTF-8");
		String text = new String(Files.readAllBytes(Paths.get("templates/utils.sk")), StandardCharsets.UTF_8);
		writer.print(text);
		writer.close();
		
		// Create scaffold file
		writer = new PrintWriter("output/main_"+reducerType+"_"+id+".sk", "UTF-8");
		
		// Read template
		text = new String(Files.readAllBytes(Paths.get("templates/main_skeleton.sk")), StandardCharsets.UTF_8);
		
		// Generate include list
		String includeList = generateIncludeList(ext, id);
		
		// Number of output variables
		String numOutVars = Integer.toString(sketchFilteredOutputVars.size());
		
		// Size of result array
		int r_size = 0;
		int keyCount = 1;
		for(Variable var : sketchFilteredOutputVars){
			String type = var.getSketchType();
			if(type.endsWith("["+Configuration.arraySizeBound+"]")){
				r_size += Configuration.arraySizeBound;
				keyCount = 2;
			}
			else{
				r_size += 1;
			}
		}
		String r_size_str = Integer.toString(r_size);
		
		// Clear block arrays
		for(int i=0; i<ext.blocks.size(); i++)
			ext.blocks.get(i).clear();
		
		// Declare input / broadcast variables
		String broadcastVarsDecl = declBroadcastVars(ext.constCount, ext.inputVars);
		
		// Generate main function args
		Map<String,Integer> argsList = new HashMap<String,Integer>();
		String mainFuncArgsDecl = generateMainFuncArgs(ext, ext.inputVars, sketchFilteredOutputVars, ext.loopCounters, argsList, sketchReducerType);
		
		// Initialize output variables
		String outputVarsInit = initMainOutput(ext,argsList, sketchFilteredOutputVars);

		// Use main function args to initialize input / broadcast variables
		String inputVarsInit = initMainInputVars(ext, argsList, ext.inputVars);

		// Use main function args to initialize input data collection
		String inputDataInit = initMainInputData(ext, argsList);
		
		// Use main function args to initialize loop counters
		String loopCountersInit = initLoopCounters(ext, argsList, ext.loopCounters);
		
		// Generate verification code
		Stmt loopBody;
		if(n instanceof While)
			loopBody = ((While)n).body();
		else
			loopBody = ((ExtendedFor)n).body();
		MyStmtExt bodyExt = ((MyStmtExt) JavaExt.ext(loopBody));
		
		String invariant = ext.invariants.get(reducerType).replaceAll("casper_data_set", new IdentifierNode(ext.inputDataSet.varName)).toString();
		
		String loopCond = "";
		String loopCounter = "";
		for(Variable v : ext.loopCounters){
			loopCond = "("+v.varName+"<"+(Configuration.arraySizeBound-1)+")";
			loopCounter = v.varName;
			break;
		}
		
		String loopCondFalse = loopCond; if(ext.condInv) loopCondFalse = "!" + loopCondFalse;
		
		String wpc = bodyExt.preConditions.get(reducerType).replaceAll("casper_data_set", new IdentifierNode(ext.inputDataSet.varName)).toString();
		
		String postC = ext.postConditions.get(reducerType).replaceAll("casper_data_set", new IdentifierNode(ext.inputDataSet.varName)).toString();
		
		String preC = ext.preConditions.get(reducerType).replaceAll("casper_data_set", new IdentifierNode(ext.inputDataSet.varName)).toString();
		
		// Generate weakest pre condition values initialization
		String wpcValuesInit = generateWPCValuesInit(ext.wpcValues,sketchFilteredOutputVars,ext.loopCounters);
				
		// 1. Assert loop invariant is true before the loop executes.
		String verifCode = "assert " + preC + ";\n\t";
		
		// 2. Assert loop invariant is preserved if the loop continues: I && loop condition is true --> wp(c, I), 
		//    where wp(c, I) is the weakest precondition of the body of the loop with I as the post-condition
		verifCode += "if(" + invariant + " && " + loopCond + ") {\n\t\t"+wpcValuesInit+"assert " + wpc + ";\n\t\t\n\t\t<block-solutions>\n\t}\n\t";
		
		// 2. Assert loop invariant implies the post condition if the loop terminates: I && loop condition is false --> POST
		verifCode += "if(" + invariant + " && " + loopCondFalse + ") {\n\t\tassert " + postC + ";\n\t}";
	
		// Generate post condition args
		String postConditionArgsDecl = generatePostConditionArgsDecl(ext.inputDataSet,sketchFilteredOutputVars,ext.loopCounters,ext.postConditionArgsOrder.get(reducerType));
		
		// Generate loop invariant args
		String loopInvariantArgsDecl = generateLoopInvariantArgsDecl(ext.inputDataSet,sketchFilteredOutputVars,ext.loopCounters,ext.postConditionArgsOrder.get(reducerType));
		
		// Generate post condition body
		String postCondition = generatePostCondition(ext.inputDataSet, sketchFilteredOutputVars,ext.loopCounters);
		
		// Generate loop invariant body
		String loopInvariant = generateLoopInvariant(ext.inputDataSet, sketchFilteredOutputVars,ext.loopCounters);
		
		// Generate int expression generator for map
		Map<String, String> blockArrays = new HashMap<String,String>();
		String mapGenerators = generateMapGenerators(sketchReducerType, keyCount, blockArrays, sketchFilteredOutputVars, ext);
		
		// Generate map function args declaration
		String mapArgsDecl = generateMapArgsDecl(ext.inputDataSet, ext.loopCounters, ext.postConditionArgsOrder.get(reducerType), keyCount, ext.valCount);
		
		// Generate map function emit code
		String mapEmits = generateDomapEmits(sketchReducerType, ext, ext.inputDataSet, ext.loopCounters, sketchFilteredOutputVars.size(), ext.useConditionals, keyCount, ext.valCount);
		
		// Generate reduce/fold expression generator
		String reduceGenerator = generateReduceGenerators(sketchReducerType, blockArrays, sketchFilteredOutputVars, ext);
			
		// Generate functions to init values in reducer
		String initFunctions = generateInitFunctions(sketchReducerType, sketchFilteredOutputVars);
		
		String casperRInit = generateCasperRInit(sketchFilteredOutputVars);
		
		// Declare key-value arrays in reduce
		String declKeysVals = generateDeclKeysVals(sketchReducerType,keyCount, ext.valCount);
		
		// Generate map function call args
		String mapArgsCall = generateMapArgsCall(ext.inputDataSet, keyCount, ext.valCount);
		
		// Initialize key variables
		String initKeys = generateInitKeys(sketchReducerType, keyCount);
		
		// Generate code to fold values by key
		String reduceByKey = generateReduceByKey(sketchFilteredOutputVars, keyCount, ext.valCount);
		
		// Generate reduce functions
		String reduceFunctions = generateReduceFunctions(sketchReducerType, sketchFilteredOutputVars, keyCount, ext.valCount);
		
		// Generate merge functions
		String mergeFunctions = generateMergeFunctions(sketchReducerType, sketchFilteredOutputVars, ext.methodOperators);
		
		// Generate code to merge output with initial values
		String mergeOutput = generateMergeOutput(sketchFilteredOutputVars, keyCount);
		
		// Generate reduce args declaration
		String reduceArgsDecl = generateReduceArgsDecl(ext.inputDataSet, sketchFilteredOutputVars,ext.loopCounters);
		
		// Generate block bit arrays declaration
		String declBlockArrays = generateDeclBlockArrays(blockArrays);
		
		// Generate code to block generated solutions
		String blockGenerated = generateBlockGenerated(ext);
		
		// Generate csg test code
		String csgTest = "";
		if(ext.valCount == 1)
			csgTest = generateCSGTestCode(sketchFilteredOutputVars);
		
		// Modify template
		text = text.replace("<decl-block-arrays>", declBlockArrays);
		text = text.replace("<output-type>", sketchReducerType);
		text = text.replace("<include-libs>",includeList);
		text = text.replace("<num-out-vars>",numOutVars);
		text = text.replace("<r-size>",r_size_str);
		text = text.replace("<decl-broadcast-vars>",broadcastVarsDecl);
		text = text.replace("<main-args-decl>",mainFuncArgsDecl);
		text = text.replace("<output-vars-initialize>",outputVarsInit);
		text = text.replace("<input-data-initialize>",inputDataInit);
		text = text.replace("<input-vars-initialize>",inputVarsInit);
		text = text.replace("<loop-counters-initialize>",loopCountersInit);
		text = text.replace("<verif-conditions>",verifCode);
		text = text.replace("<post-cond-args-decl>",postConditionArgsDecl);
		text = text.replace("<loop-inv-args-decl>",loopInvariantArgsDecl);
		text = text.replace("<post-cond-body>",postCondition);
		text = text.replace("<loop-inv-body>",loopInvariant);
		text = text.replace("<map-generators>",mapGenerators);
		text = text.replace("<map-args-decl>",mapArgsDecl);
		text = text.replace("<map-emits>",mapEmits);
		text = text.replace("<reduce-generator>", reduceGenerator);
		text = text.replace("<lc>", loopCounter);
		text = text.replace("<init-functions>", initFunctions);
		text = text.replace("<casper-r-init>", casperRInit);
		text = text.replace("<init-keys-vals>", declKeysVals);
		text = text.replace("<map-args-call>",mapArgsCall);
		text = text.replace("<init-keys>",initKeys);
		text = text.replace("<reduce-by-key>", reduceByKey);
		text = text.replace("<reduce-functions>", reduceFunctions);
		text = text.replace("<merge-functions>", mergeFunctions);
		text = text.replace("<merge-r>", mergeOutput);
		text = text.replace("<reduce-args-decl>", reduceArgsDecl);
		text = text.replace("<block-solutions>", blockGenerated);
		text = text.replace("<reduce-csg-test>", csgTest);
		
		// Save
		writer.print(text);
		writer.close();
	}

	private static String generateCSGTestCode(Set<Variable> sketchFilteredOutputVars) {
		String code = "\n\n\t";
		for(Variable var : sketchFilteredOutputVars){
			code += "assert (reduce_"+var.varName+"(csg_test_val1,csg_test_val2) == reduce_"+var.varName+"(csg_test_val2,csg_test_val1)) || (reduce_"+var.varName+"(csg_test_val1,csg_test_val2) == csg_test_val2 && reduce_"+var.varName+"(csg_test_val2,csg_test_val1) == csg_test_val1);\n\t";
		}
		return code;
	}

	// Generate code that includes all necessary files
	public static String generateIncludeList(MyWhileExt ext, int id) {
		String includeList = "";
		
		for(String dataType : ext.globalDataTypes){
			includeList += "include \"output/" + dataType + ".sk\";\n";
		}
		if(ext.inputDataCollections.size()>1){
			includeList += "include \"output/CasperDataRecord.sk\";\n";
		}
		
		return includeList;
	}
	
	// Generate code that initializes main function args
	
	public static String generateMainFuncArgs(MyWhileExt ext, Set<Variable> sketchInputVars, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters, Map<String, Integer> argsList, String sketchReducerType) {
		String mainFuncArgs = "";
		
		for(Variable var : sketchInputVars){
			if(!sketchOutputVars.contains(var)){
				if(!ext.initVals.containsKey(var.varName) || !(ext.initVals.get(var.varName) instanceof ConstantNode) || ((ConstantNode)ext.initVals.get(var.varName)).type == ConstantNode.STRINGLIT){
					handleVarArgs(var.getSketchType(),var.category,ext,argsList,1,true);
				}
			}
		}
		for(Variable var : sketchOutputVars){
			if(!ext.initVals.containsKey(var.varName) || !(ext.initVals.get(var.varName) instanceof ConstantNode)){
				handleVarArgs(var.getSketchType(),var.category,ext,argsList,2,false);
			}
			else{
				handleVarArgs(var.getSketchType(),var.category,ext,argsList,1,false);
			}
		}
		for(Variable var : sketchLoopCounters){
			if(!ext.initVals.containsKey(var.varName)){
				handleVarArgs(var.getSketchType(),var.category,ext,argsList,2,false);
			}
			else{
				handleVarArgs(var.getSketchType(),var.category,ext,argsList,1,false);
			}
		}
		argsList.put("int", argsList.get("int")+ext.constCount);
		
		for(String type : argsList.keySet()){
			mainFuncArgs += type + "[" + argsList.get(type) + "] " + type + "Set, ";
		}
		if(mainFuncArgs.length() > 0)
			mainFuncArgs = mainFuncArgs.substring(0, mainFuncArgs.length()-2);
		
		mainFuncArgs += ", "+sketchReducerType+" csg_test_val1, "+sketchReducerType+" csg_test_val2";
		
		return mainFuncArgs;
	}
	
	// Determines what arguments need to be passed to the main function to generate
	// the provided variable type instance
	
	public static void handleVarArgs(String vartypeOrig, int category, MyWhileExt ext, Map<String, Integer> argsList, int increment, boolean isInputVar){
		if(casper.Util.getTypeClass(vartypeOrig) == casper.Util.PRIMITIVE){
			String vartype = vartypeOrig;
			if(argsList.containsKey(vartype)){
				argsList.put(vartype, argsList.get(vartype) + increment);
			}
			else {
				argsList.put(vartype, increment);
			}
		}
		else if(casper.Util.getTypeClass(vartypeOrig) == casper.Util.ARRAY){
			String vartype = vartypeOrig.substring(0, vartypeOrig.length()-("["+Configuration.arraySizeBound+"]").length());
			
			if(category != Variable.CONST_ARRAY_ACCESS){
				if(isInputVar)
					increment = Configuration.arraySizeBound - 1;
				else
					increment = Configuration.arraySizeBound * increment;
			}
			
			if(argsList.containsKey(vartype)){
				argsList.put(vartype, argsList.get(vartype) + increment);
			}
			else {
				argsList.put(vartype, increment);
			}
		}
		// Look at the member field datatypes
		else if(casper.Util.getTypeClass(vartypeOrig) == casper.Util.OBJECT){
			String vartype = vartypeOrig;
			
			for(Variable fdecl : ext.globalDataTypesFields.get(vartype)){
				String fieldType = fdecl.getSketchType();
				handleVarArgs(fieldType, category, ext, argsList, increment, isInputVar);
			}
		}
		else if(casper.Util.getTypeClass(vartypeOrig) == casper.Util.OBJECT_ARRAY){
			String vartype = vartypeOrig.substring(0, vartypeOrig.length()-("["+Configuration.arraySizeBound+"]").length());
			
			if(category != Variable.CONST_ARRAY_ACCESS){
				increment = increment * (Configuration.arraySizeBound-1);
			}
			
			for(Variable fdecl : ext.globalDataTypesFields.get(vartype)){
				String fieldType = fdecl.getSketchType();
				handleVarArgs(fieldType, category, ext, argsList, increment, isInputVar);
			}
		}
	}
		
	// Generate code that declares the input broadcast variables using main function args
	
	public static String declBroadcastVars(int constCount, Set<Variable> sketchInputVars) {
		String declBrdcstVars = "";
		
		for(Variable var : sketchInputVars){
			if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.PRIMITIVE){
				declBrdcstVars += var.getSketchType() + " " + var.varName + ";\n";
			}
			else if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.ARRAY){
				if(var.category == Variable.CONST_ARRAY_ACCESS)
					declBrdcstVars += var.getSketchType().replace("["+Configuration.arraySizeBound+"]", "") + " " + var.varName + ";\n";
			}
			else if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.OBJECT){
				if(var.getSketchType().endsWith("["+Configuration.arraySizeBound+"]")){
					if(var.category == Variable.CONST_ARRAY_ACCESS)
						declBrdcstVars += var.getSketchType().replace("["+Configuration.arraySizeBound+"]", "") + " " + var.varName + ";\n";
				}
				else {
					declBrdcstVars += var.getSketchType() + " " + var.varName + " = new " + var.getSketchType() + "();\n";
				}
			}
		}
		for(int i=0; i<constCount; i++){
			declBrdcstVars += "int casperConst" + i+";\n";
		}
		
		return declBrdcstVars;
	}
		
	// Create and Initialize output variables
	
	public static String initMainOutput(MyWhileExt ext, Map<String, Integer> argsList, Set<Variable> sketchOutputVars) {
		String ret = "";
		for(Variable var : sketchOutputVars){
			if(ext.initVals.containsKey(var.varName) && ext.initVals.get(var.varName) instanceof ConstantNode){
				if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.PRIMITIVE){
					ret += var.getSketchType() + " "+ var.varName + "0 = " + ext.initVals.get(var.varName) + ";\n\t";
				}
				else if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.ARRAY){
					ret += var.getSketchType() + " "+ var.varName + "0;\n\t";
					for(int i=0; i<Configuration.arraySizeBound; i++)
						ret += var.varName + "0["+i+"] = " + ext.initVals.get(var.varName) + ";\n\t";
				}
			}
			else{
				if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.PRIMITIVE){
					ret += var.getSketchType() + " "+ var.varName + "0 = " + var.getSketchType() + "Set[" + (argsList.get(var.getSketchType())-1) + "];\n\t";
					argsList.put(var.getSketchType(), argsList.get(var.getSketchType()) - 1 );
				}
				else if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.ARRAY){
					String vartype = var.getSketchType().replace("["+Configuration.arraySizeBound+"]", "");
					ret += var.getSketchType() + " "+ var.varName + "0;\n\t";
					for(int i=0; i<Configuration.arraySizeBound; i++){
						ret += var.varName + "0["+i+"] = " + vartype + "Set[" + (argsList.get(vartype)-1) + "];\n\t";
						argsList.put(vartype, argsList.get(vartype) - 1 );
					}
				}
			}
			
			if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.PRIMITIVE){
				ret += var.getSketchType() + " "+ var.varName + " = " + var.getSketchType() + "Set[" + (argsList.get(var.getSketchType())-1) + "];\n\t";
				argsList.put(var.getSketchType(), argsList.get(var.getSketchType()) - 1 );
			}
			else if(casper.Util.getTypeClass(var.getSketchType()) == casper.Util.ARRAY){
				String vartype = var.getSketchType().replace("["+Configuration.arraySizeBound+"]", "");
				ret += var.getSketchType() + " "+ var.varName + ";\n\t";
				for(int i=0; i<Configuration.arraySizeBound; i++){
					ret += var.varName + "["+i+"] = " + vartype + "Set[" + (argsList.get(vartype)-1) + "];\n\t";
					argsList.put(vartype, argsList.get(vartype) - 1 );
				}
			}
		}
		return ret.substring(0, ret.length()-2);
	}
	
	// Generate code that Initializes the input class using main function args
	
	public static String initMainInputVars(MyWhileExt ext, Map<String, Integer> argsList, Set<Variable> sketchInputVars) {
		String inputInit = "";
		
		for(Variable var : sketchInputVars){
			// If the input variable is a constant index of an array, broadcast it
			if(var.category == Variable.CONST_ARRAY_ACCESS){
				inputInit += handleInputVarInit(var.getSketchType().replace("["+Configuration.arraySizeBound+"]", ""),var.varName,ext,argsList);
			}
			// If the input variable is a field of an object, broadcast it
			else if(var.category == Variable.FIELD_ACCESS){
				inputInit += handleInputVarInit(var.getSketchType(),var.varName,ext,argsList);
			}
			// If the input variable is a primitive variable, broadcast it
			else if(var.category == Variable.VAR){
				inputInit += handleInputVarInit(var.getSketchType(),var.varName,ext,argsList);
			}
		}
		for(int i=0; i<ext.constCount; i++){
			inputInit += "casperConst"+i+" = intSet["+(argsList.get("int")-1)+"];\n\t";
			argsList.put("int", argsList.get("int") - 1);
			
		}
		
		return inputInit;
	}
		
	// Generates code for input variable initialization in main function. This function initializes
	// using main func args.
	
	public static String handleInputVarInit(String vartype, String varname, MyWhileExt ext, Map<String, Integer> argsList ){
		String inputInit = "";
		if(casper.Util.getTypeClass(vartype) == casper.Util.PRIMITIVE){
			if(ext.initVals.containsKey(varname)){
				CustomASTNode initVal = ext.initVals.get(varname);
				if(initVal instanceof ConstantNode && ((ConstantNode) initVal).type == ConstantNode.STRINGLIT){
					inputInit += varname + " = " + vartype + "Set["+(argsList.get(vartype)-1)+"];\n\t";
					argsList.put(vartype, argsList.get(vartype) - 1);
				}
				else{
					inputInit += varname + " = " + ext.initVals.get(varname) +";\n\t";
				}
			}
			else{
				inputInit += varname + " = " + vartype + "Set["+(argsList.get(vartype)-1)+"];\n\t";
				argsList.put(vartype, argsList.get(vartype) - 1);
			}
		}
		else if(casper.Util.getTypeClass(vartype) == casper.Util.ARRAY){
			vartype = vartype.substring(0, vartype.length()-("["+Configuration.arraySizeBound+"]").length());
			for(int i=0; i<Configuration.arraySizeBound; i++){
				if(ext.initVals.containsKey(varname+"0")){
					inputInit += varname + "["+i+"] = " + ext.initVals.get(varname+"0") +";\n\t";
				}
				else{
					inputInit += varname + "["+i+"] = " + vartype + "Set["+(argsList.get(vartype)-1)+"];\n\t";
					argsList.put(vartype, argsList.get(vartype) - 1);
				}
			}
		}
		else if(casper.Util.getTypeClass(vartype) == casper.Util.OBJECT){
			if(vartype.endsWith("["+Configuration.arraySizeBound+"]")){
				vartype = vartype.substring(0, vartype.length()-("["+Configuration.arraySizeBound+"]").length());
				for(int i=0; i<Configuration.arraySizeBound; i++){
					String varname2 = varname+"["+i+"]";
					inputInit += varname2 + " = new " + vartype + "();\n\t";
					inputInit += handleInputVarInit(vartype,varname2,ext,argsList);
				}
			}
			else {
				if(ext.globalDataTypesFields.containsKey(vartype)){
					for(Variable fdecl : ext.globalDataTypesFields.get(vartype)){
						String fieldname = fdecl.varName;
						String fieldtype = fdecl.getSketchType();
						inputInit += handleInputVarInit(fieldtype,varname+"."+fieldname,ext,argsList);
					}
				}
			}
		}
		
		return inputInit;
	}
	
	// Generate code that Initializes the input class using main function args
	
	public static String initMainInputData(MyWhileExt ext, Map<String, Integer> argsList) throws IOException {
		String inputInit = "";
		
		if(ext.hasInputData && ext.initInpCollection){
			if(ext.inputDataCollections.size() == 1){
				ext.inputDataSet = ext.inputDataCollections.get(0);
			}
			else if(ext.inputDataCollections.size() > 1){
				ext.inputDataSet = new Variable("casper_data_set","java.util.List<CasperDataRecord>","",Variable.ARRAY_ACCESS);
				if(!ext.globalDataTypes.contains("CasperDataRecord")){
					ext.globalDataTypes.add("CasperDataRecord");
					ext.globalDataTypesFields.put("CasperDataRecord", new HashSet<Variable>());
				
					String fields = "";
					for(Variable var : ext.inputDataCollections){
						ext.globalDataTypesFields.get("CasperDataRecord").add(new Variable(var.varName,casper.Util.reducerType(var.getSketchType()),"",Variable.VAR));
						fields += casper.Util.reducerType(var.getSketchType()) + " " + var.varName + ";";
					}
					
					PrintWriter writer = new PrintWriter("output/CasperDataRecord.sk", "UTF-8");
					String text = "struct CasperDataRecord{ "+fields+" }";
					writer.print(text);
					writer.close();
				}
			}
			
			inputInit = ext.inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + ext.inputDataSet.varName + ";\n\t";
			for(int i=0; i<Configuration.arraySizeBound-1; i++){
				inputInit += handleInputDataInit(ext.inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", ""),ext.inputDataSet.varName+"["+i+"]",ext,argsList);
			}
		}
		else{
			if(ext.inputDataCollections.size() == 1){
				ext.inputDataSet = new Variable("casper_data_set",ext.inputDataCollections.get(0).varType,"",Variable.ARRAY_ACCESS);
				inputInit = ext.inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + ext.inputDataSet.varName + ";\n\t";
				for(int i=0; i<Configuration.arraySizeBound-1; i++){
					inputInit += ext.inputDataSet.varName+"["+i+"] = "+ext.inputDataCollections.get(0).varName+"["+i+"];\n\t";
				}
			}
			else if(ext.inputDataCollections.size() > 1 || true){
				ext.inputDataSet = new Variable("casper_data_set","java.util.List<CasperDataRecord>","",Variable.ARRAY_ACCESS);
				if(!ext.globalDataTypes.contains("CasperDataRecord")){
					ext.globalDataTypes.add("CasperDataRecord");
					ext.globalDataTypesFields.put("CasperDataRecord", new HashSet<Variable>());
				
					String fields = "";
					for(Variable var : ext.inputDataCollections){
						ext.globalDataTypesFields.get("CasperDataRecord").add(new Variable(var.varName,casper.Util.reducerType(var.getSketchType()),"",Variable.VAR));
						fields += casper.Util.reducerType(var.getSketchType()) + " " + var.varName + ";";
					}
					
					PrintWriter writer = new PrintWriter("output/CasperDataRecord.sk", "UTF-8");
					String text = "struct CasperDataRecord{ "+fields+" }";
					writer.print(text);
					writer.close();
				}
				
				inputInit = ext.inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + ext.inputDataSet.varName + ";\n\t";
				for(int i=0; i<Configuration.arraySizeBound-1; i++){
					for(Variable dcol : ext.inputDataCollections){
						inputInit += ext.inputDataSet.varName+"["+i+"]."+dcol.varName+" = "+dcol.varName+"["+i+"];\n\t";
					}
				}
			}
		}
		
		return inputInit;
	}
	
	
	public static String handleInputDataInit(String vartype, String varname, MyWhileExt ext, Map<String, Integer> argsList){
		String ret = "";

		if(casper.Util.getTypeClass(vartype) == casper.Util.PRIMITIVE){
			ret += varname + " = " + vartype + "Set["+(argsList.get(vartype)-1)+"];\n\t";
			argsList.put(vartype, argsList.get(vartype) - 1);
		}
		else if(casper.Util.getTypeClass(vartype) == casper.Util.OBJECT){
			ret += varname + " = new " + vartype + "();\n\t";
			for(Variable fdecl : ext.globalDataTypesFields.get(vartype)){
				String fieldType = fdecl.getSketchType();
				String fieldName = fdecl.varName;
				ret += handleInputDataInit(fieldType, varname+"."+fieldName,ext,argsList);
			}
		}
		
		return ret;
	}
	
	
	public static String initLoopCounters(MyWhileExt ext, Map<String, Integer> argsList, Set<Variable> sketchLoopCounters) {
		String ret = "";

		for(Variable var : sketchLoopCounters){
			if(ext.initVals.containsKey(var.varName)){
				ret += var.getSketchType() + " "+ var.varName + "0 = " + ext.initVals.get(var.varName) + ";\n\t";
			}
			else{
				ret += var.getSketchType() + " "+ var.varName + "0 = " + var.getSketchType() + "Set[" + (argsList.get(var.getSketchType())-1) + "];\n\t";
				argsList.put(var.getSketchType(), argsList.get(var.getSketchType()) - 1 );
			}
			ret += var.getSketchType() + " "+ var.varName + " = " + var.getSketchType() + "Set[" + (argsList.get(var.getSketchType())-1) + "];\n\t";
			argsList.put(var.getSketchType(), argsList.get(var.getSketchType()) - 1 );
		}
		
		if(ret == "")
			return ret;
		
		return ret.substring(0, ret.length()-2);
	}
	
	
	public static String generateWPCValuesInit(Map<String,CustomASTNode> wpcValues, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters) {
		String code = "";
		for(String varname : wpcValues.keySet())
		{
			boolean found = false;
			for(Variable var : sketchOutputVars){
				if(var.varName.equals(varname)){
					code += var.getSketchType() + " ind_" + varname + " = " + varname + ";\n\t\t";
					CustomASTNode value = wpcValues.get(varname);
					if(value instanceof ConditionalNode){
						code += ((ConditionalNode)value).toString("ind_" + varname + " = ");
					}
					else if(value instanceof ArrayUpdateNode){
						code += wpcValues.get(varname).toString() + ";\n\t\t";
					}
					else{
						code += "ind_" + varname + " = " + wpcValues.get(varname) + ";\n\t\t";
					}
					found = true;
					break;
				}
			}
			if(found) continue;
			for(Variable var : sketchLoopCounters){
				if(var.varName.equals(varname)){
					code += var.getSketchType() + " ind_" + varname + " = " + varname + ";\n\t\t";
					CustomASTNode value = wpcValues.get(varname);
					if(value instanceof ConditionalNode){
						code += ((ConditionalNode)value).toString("ind_" + varname + " = ");
					}
					else if(value instanceof ArrayUpdateNode){
						code += wpcValues.get(varname).toString() + ";\n\t\t";
					}
					else{
						code += "ind_" + varname + " = " + wpcValues.get(varname) + ";\n\t\t";
					}
					found = true;
					break;
				}
			}
		}
		return code;
	}
	
	// Generate post condition function args	
	public static String generatePostConditionArgsDecl(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters, List<String> postConditionArgsOrder) {
		String pcArgs = inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + inputDataSet.varName;
		
		for(String nextVarName : postConditionArgsOrder){
			for(Variable var : sketchOutputVars){
				if(nextVarName.equals(var.varName)){
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "") + "0";
				}
			}
			for(Variable var : sketchLoopCounters){
				if(nextVarName.equals(var.varName)){
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "") + "0";
				}
			}
		}
		
		return pcArgs;
	}
	
	// Generate loop invariant function args
	public static String generateLoopInvariantArgsDecl(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters, List<String> loopInvariantArgsOrder) {
		String pcArgs = inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + inputDataSet.varName;
		
		for(String nextVarName : loopInvariantArgsOrder){
			for(Variable var : sketchOutputVars){
				if(nextVarName.equals(var.varName)){
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "") + "0";
				}
			}
			for(Variable var : sketchLoopCounters){
				if(nextVarName.equals(var.varName)){
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.getSketchType() + " " + nextVarName.replace(".", "") + "0";
				}
			}
		}
		
		return pcArgs;
	}
	
	// Generate post condition function body
	public static String generatePostCondition(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters) {
		String postCond = "";
		
		int index = 0;
		for(Variable var : sketchOutputVars){
			if(var.getSketchType().endsWith("["+Configuration.arraySizeBound+"]")){
				for(int i=0; i<Configuration.arraySizeBound; i++)
					postCond += "casper_r["+index+++"] = " + var.varName + "["+i+"];\n\t";
			}
			else{
				postCond += "casper_r["+index+++"] = " + var.varName + ";\n\t";
			}
		}
		
		String reduce_args = inputDataSet.varName;
		for(Variable var : sketchOutputVars){
			reduce_args += ", " + var.varName;
			reduce_args += ", " + var.varName + "0";
		}
		for(Variable var : sketchLoopCounters){
			reduce_args += ", " + var.varName;
			reduce_args += ", " + var.varName + "0";
		}
		
		postCond += "return reduce("+reduce_args+") == casper_r;";
		
		return postCond;
	}
	
	public static String generateLoopInvariant(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters) {
		String inv = "";
		
		int index = 0;
		for(Variable var : sketchOutputVars){
			if(var.getSketchType().endsWith("["+Configuration.arraySizeBound+"]")){
				for(int i=0; i<Configuration.arraySizeBound; i++)
					inv += "casper_r["+index+++"] = " + var.varName + "["+i+"];\n\t";
			}
			else{
				inv += "casper_r["+index+++"] = " + var.varName + ";\n\t";
			}
		}
		
		String reduce_args = inputDataSet.varName;
		for(Variable var : sketchOutputVars){
			reduce_args += ", " + var.varName;
			reduce_args += ", " + var.varName + "0";
		}
		for(Variable var : sketchLoopCounters){
			reduce_args += ", " + var.varName;
			reduce_args += ", " + var.varName + "0";
		}
		
		for(Variable var : sketchLoopCounters){
			inv += "return "+var.varName+"0 <= " + var.varName + " && " + var.varName + " <= " + (Configuration.arraySizeBound-1) + " && reduce("+reduce_args+") == casper_r;";
			break;
		}
		
		return inv;
	}
	
	// Generate do map grammars
	public static String generateMapGrammarInlined(MyWhileExt ext, String type, String index, Map<String, String> blockArrays) {
		String generator = "";
			
		/******** Generate terminal options *******/
		Map<String,List<String>> terminals = new HashMap<String,List<String>>();
		
		for(Variable var : ext.loopCounters){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				String keyType = "String";
				if(!var.getOriginalType().replace("[]", "").equals("String"))
					keyType = casper.Util.reducerType(var.getSketchType());
				if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
				terminals.get(keyType).add(var.varName);
			}
		}
		for(Variable var : ext.inputVars){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				String keyType = "String";
				if(!var.getOriginalType().replace("[]", "").equals("String"))
					keyType = casper.Util.reducerType(var.getSketchType());
				if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
				terminals.get(keyType).add(var.varName);
			}
			else if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 0){
				for(String globalType : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalType.equals(var.getOriginalType())){
						// Add an option for each field that matches type
	        			for(Variable field : ext.globalDataTypesFields.get(globalType)){
	        				if(casper.Util.compatibleTypes(type,field.getOriginalType()) == 1){
	        					String keyType = "String";
	        					if(!var.getOriginalType().replace("[]", "").equals("String"))
	        						keyType = casper.Util.reducerType(var.getSketchType());
	        					if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
	        					terminals.get(keyType).add(var.varName + "." + field.varName);
	        				}
	        			}
	        		}
				}
			}
		}
		if(casper.Util.getTypeClass(ext.inputDataSet.getSketchType()) == casper.Util.OBJECT_ARRAY){
			for(String globalType : ext.globalDataTypes){
				if(globalType.equals(ext.inputDataSet.getOriginalType().replace("[]", ""))){
					for(Variable field : ext.globalDataTypesFields.get(globalType)){
						// Add an option for each field (of an arbitrary array index) that matches type
						if(casper.Util.compatibleTypes(type,field.getOriginalType()) == 1){
							String keyType = "String";
							if(!field.getOriginalType().replace("[]", "").equals("String"))
								keyType = casper.Util.reducerType(field.getSketchType());
							for(Variable lc : ext.loopCounters){
								if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
								terminals.get(keyType).add(ext.inputDataSet.varName + "["+lc.varName+"]." + field.varName);
							}
						}
					}
				}
    		}
		}
		else if(casper.Util.getTypeClass(ext.inputDataSet.getSketchType()) == casper.Util.ARRAY){
			if(casper.Util.compatibleTypes(type,ext.inputDataSet.getOriginalType()) == 2){
				String keyType = "String";
				if(!ext.inputDataSet.getOriginalType().replace("[]", "").equals("String"))
					keyType = casper.Util.reducerType(ext.inputDataSet.getSketchType());
				for(Variable lc : ext.loopCounters){
					if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
					terminals.get(keyType).add(ext.inputDataSet.varName + "["+lc.varName+"]");
				}
			}
		}
			
		for(int i=0; i<ext.constCount; i++){
			if(casper.Util.compatibleTypes(type,"int") == 1){
				if(!terminals.containsKey(type)) terminals.put(type, new ArrayList());
				terminals.get("int").add("casperConst" + i);
			}
		}
		
		/********** Generate type expressions ********/
		String sketchType = type;
		if(type.equals("String"))
			sketchType = "int";
		
		String typeName = type.toLowerCase();
		if(sketchType.equals("bit[32]"))
			typeName = "bitInt";
		
		for(String ptype : ext.candidateKeyTypes){
			if(!terminals.containsKey(ptype))
				terminals.put(ptype, new ArrayList<String>());
		}
		
		// Terminal names
		Map<String,List<String>> terminalNames = new HashMap<String,List<String>>();
		for(String ttype : terminals.keySet()){
			String ttypeName = ttype.toLowerCase();
			if(ttypeName == "bit[32]")
				ttypeName = "bitInt";
			
			terminalNames.put(ttype, new ArrayList<String>());
			for(int i=0; i<Math.pow(2.0, ext.recursionDepth-1); i++){
				terminalNames.get(ttype).add("_"+ttypeName+"_terminal"+(i));
			}
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		getMapExpressions(	type,
							ext.binaryOperators,
							ext.unaryOperators,
							ext.methodOperators,
							terminalNames,
							exprs,
							ext.recursionDepth
						);
		
		if(sketchType.equals("bit") && !index.startsWith("_c")){
			exprs.add("CASPER_TRUE");
			exprs.add("CASPER_FALSE");
		}
		
		/******** Generate args decl code *******/
		
		String argsDecl = ext.inputDataSet.getSketchType().replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataSet.varName;
		for(Variable var : ext.loopCounters){
			argsDecl += ", " + var.getSketchType() + " " + var.varName;
		}
		
		/******** Generate terminals code *******/
		Map<String,List<String>> terminalOptions = new HashMap<String,List<String>>();
		for(String ttype : terminals.keySet()){
			terminalOptions.put(ttype, new ArrayList<String>());
			for(int i=0; i<terminals.get(ttype).size(); i++){
				terminalOptions.get(ttype).add(terminals.get(ttype).get(i));
			}	
			switch(ttype){
				case "int":
					terminalOptions.get(ttype).add("??");
					break;
				case "bit[32]":
					terminalOptions.get(ttype).add("casper_genRandBitVec()");
					break;
				case "String":
					terminalOptions.get(ttype).add("??");
					break;
			}
		}
		
		String terminalsCode = "";
		Map<String,Integer> termIndex = new HashMap<String,Integer>();
		for(String ttype : terminals.keySet()){
			String ttypeName = ttype.toLowerCase();;
			if(ttypeName == "bit[32]")
				ttypeName = "bitInt";
			String ttype2 = ttype;
			if(ttype2.equals("String")) ttype2 = "int";
			termIndex.put(ttype, (int) Math.pow(2.0, ext.recursionDepth-1));
			for(int i=0; i<Math.pow(2.0, ext.recursionDepth-1); i++){
				terminalsCode += ttype2+" _"+ttypeName+"_terminal"+(i)+";\n\t";
				terminalsCode += "int  _"+ttypeName+"_terminal"+(i)+"c = ??("+(int)Math.ceil(Math.log(terminalOptions.get(ttype).size())/Math.log(2))+");\n\t";
				int optIndex = 0;
				for(String opt : terminalOptions.get(ttype)){
					String prefix = "else if";
					if(optIndex == 0) prefix = "if";
					if(!ttypeName.equals("string") && opt.equals("??")){
						terminalsCode += prefix+"(_"+ttypeName+"_terminal"+(i)+"c == "+optIndex+") { _term_flag_"+ttypeName+"_terminal"+(i)+"_map"+index+"["+optIndex+"] =  true; _"+ttypeName+"_terminal"+(i)+" = " + opt + "; assert _"+ttypeName+"_terminal"+(i)+" != 0; }\n\t";
						optIndex++;
					}
					else{
						terminalsCode += prefix+"(_"+ttypeName+"_terminal"+(i)+"c == "+optIndex+") { _term_flag_"+ttypeName+"_terminal"+(i)+"_map"+index+"["+optIndex+"] =  true; _"+ttypeName+"_terminal"+(i)+" = " + opt + "; }\n\t";
						optIndex++;
					}
				}
				terminalsCode += "else { assert false; }\n\t";
				blockArrays.put("_term_flag_"+ttypeName+"_terminal"+(i)+"_map"+index, Integer.toString(terminalOptions.get(ttype).size()));
			}
		}
		
		/******** Generate expressions code *******/
		String expressions = "int c = ??("+(int)Math.ceil(Math.log(exprs.size())/Math.log(2))+");\n\t";
		int c = 0;
		ext.grammarExps.put("mapExp"+index, new ArrayList<String>());
		for(String expr : exprs){
			for(String ttype : terminals.keySet()){
				String ttypeName = ttype.toLowerCase();
				if(ttypeName == "bit[32]")
					ttypeName = "bitInt";			
					
				int i=0;
				while(expr.contains("<casper-"+ttype+"-term>")){
					int st_ind = expr.indexOf("<casper-"+ttype+"-term>");
					expr = expr.substring(0,st_ind) + terminalNames.get(ttype).get(i++) + expr.substring(st_ind+("<casper-"+ttype+"-term>").length(),expr.length());
				}
			}
			
			ext.grammarExps.get("mapExp"+index).add(expr);
			int solID = 0;
			for(Map<String,String> sol : ext.blockExprs){
				if(expr.equals(sol.get("mapExp"+index))){
					ext.blocks.get(solID).add("mapExp"+index+"["+c+"]");
				}
				solID++;
			}
			
			if(c==0)
				expressions += "if(c=="+c+"){ mapExp"+index+"["+c+"]=true; return " + expr + "; }\n\t";
			else
				expressions += "else if(c=="+c+"){ mapExp"+index+"["+c+"]=true; return " + expr + "; }\n\t";
			c++;
		}
		expressions += "else { assert false; }\n\t";
		
		/****** Generate final output code ******/
		generator += "generator "+sketchType+" " +typeName+"MapGenerator"+index+"("+argsDecl+"){\n\t" + terminalsCode + expressions + "\n}";
		
		blockArrays.put("mapExp"+index, Integer.toString(exprs.size()));
		return generator;
	}
	
	private static void getMapExpressions(String type, Set<String> binaryOps, Set<String> unaryOps, Set<SketchCall> methodOps, Map<String,List<String>> terminals, List<String> exprs, int depth) {
		if(depth == 0){
			return;
		}
		if(depth == 1){
			exprs.add("(<casper-"+type+"-term>)");
			return;
		}
		else{
			exprs.add("(<casper-"+type+"-term>)");
			
			for(String op : binaryOps){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					if(type.equals("bit")){
						if(casper.Util.operandTypes(op) == casper.Util.BIT_ONLY){
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
						else if(casper.Util.operandTypes(op) == casper.Util.INT_ONLY){
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions("int",binaryOps,unaryOps,methodOps,terminals,subExprs,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
						else if(casper.Util.operandTypes(op) == casper.Util.ALL_TYPES){
							for(String ttype : terminals.keySet()){
								List<String> subExprs = new ArrayList<String>();
								getMapExpressions(ttype,binaryOps,unaryOps,methodOps,terminals,subExprs,depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
											continue;
										if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
											continue;
										exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
									}
								}
							}
						}
					}
					else if(type.equals("bit[32]")){
						if(casper.Util.operandTypes(op) == casper.Util.VEC_ONLY){
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
						else if(casper.Util.operandTypes(op) == casper.Util.VEC_INT){
							List<String> lhsSubExprs = new ArrayList<String>();
							List<String> rhsSubExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,lhsSubExprs,depth-1);
							getMapExpressions("int",binaryOps,unaryOps,methodOps,terminals,rhsSubExprs,depth-1);
							for(String exprLeft : lhsSubExprs){
								for(String exprRight : rhsSubExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
					}
					else{
						if(casper.Util.isAssociative(op)){
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
						else{
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
					}
				}
			}
			for(String op : unaryOps){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					//getMapExpressions(type,binaryOps,unaryOps,methodOps,inputDataCollection,sketchInputVars,sketchOutputVars,sketchLoopCounters,terminals,exprs,constants,depth-1);
				}
			}
			for(SketchCall op : methodOps){
				if(casper.Util.getOpClassForType(casper.Util.getSketchTypeFromRaw(op.returnType)) == casper.Util.getOpClassForType(type)){
					Map<String,List<String>> subExprs = new HashMap<String,List<String>>();	
					for(String argType : op.args){
						Variable temp = new Variable("arg",argType,"",Variable.VAR);
						if(subExprs.keySet().contains(temp.getSketchType())) 
							continue;
						List<String> subExprsList = new ArrayList<String>();
						getMapExpressions(temp.getSketchType(),binaryOps,unaryOps,methodOps,terminals,subExprsList,depth-1);
						subExprs.put(temp.getSketchType(), subExprsList);
					}
					List<List<String>> callarr = new ArrayList<List<String>>();
					for(String argType : op.args){
						Variable temp = new Variable("arg",argType,"",Variable.VAR);
						callarr.add(subExprs.get(temp.getSketchType()));
					}
					String expr = op.name+"(";
					List<String> subExprsList = new ArrayList<String>();
					buildExpressions(callarr,expr,subExprsList,0);
					exprs.addAll(subExprsList);
				}
			}
			
			return;
		}
	}
		
	private static void buildExpressions(List<List<String>> callarr, String expr, List<String> exprs, int i) {
		if(i >= callarr.size()){
			exprs.add(expr.substring(0,expr.length()-1)+")");
			return;
		}
			
		for(int j=0; j<callarr.get(i).size(); j++){
			buildExpressions(callarr,expr + callarr.get(i).get(j)+",",exprs,i+1);}
	}

	public static String generateMapArgsDecl(Variable inputDataSet, Set<Variable> sketchLoopCounters, List<String> postConditionArgsOrder, int keyCount, int valCount) {
		String mapArgs = inputDataSet.getSketchType().replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + inputDataSet.varName;
		
		for(Variable var : sketchLoopCounters){
			mapArgs += ", int " + var.varName; break;
		}
		for(int i=0; i<keyCount; i++){
			mapArgs += ", ref int[CASPER_NUM_OUTVARS] keys"+i;
		}
		for(int i=0; i<valCount; i++){
			mapArgs += ", ref int[CASPER_NUM_OUTVARS] values"+i;
		}
		
		return mapArgs;
	}
	
	public static String generateDomapEmits(String type, MyWhileExt ext, Variable inputDataSet, Set<Variable> sketchLoopCounters, int emitCount, boolean useConditionals, int keyCount, int valCount) {
		String emits = "";
		
		// Generate args for generator functions
		
		String lcName = "";
		for(Variable var : sketchLoopCounters){
			lcName = var.varName;
			break;
		}
		String args = inputDataSet.varName + ", " + lcName;
		
		String sketchType = type;
		if(type == "String")
			sketchType = "int";
		
		String typeName = type.toLowerCase();
		if(sketchType == "bit[32]")
			typeName = "bitInt";
		
		// Include conditionals?
		if(useConditionals){
			int indexC = 0;
			int indexK = 0;
			int indexV = 0;
			// Generate emit code
			for(int i=0; i<emitCount; i++){
				emits += 	"if(bitMapGenerator_c"+indexC+++"("+args+")){\n\t\t";
				for(int j=0; j<keyCount; j++){
					if(j==0)
						emits += "keys"+j+"["+i+"] = ??;\n\t\t";
					else
						emits += "keys"+j+"["+i+"] = "+ext.candidateKeyTypes.get(ext.keyIndex).toLowerCase()+"MapGenerator_k"+indexK+++"("+inputDataSet.varName+", "+lcName+");\n\t\t";
				}
				for(int j=0; j<valCount; j++){
					emits += "values"+j+"["+i+"] = "+typeName+"MapGenerator_v"+indexV+++"("+inputDataSet.varName+", "+lcName+");\n\t\t";
				}
				emits = emits.substring(0,emits.length()-1);
				emits += "}";
			}
		}
		else{
			int indexK = 0;
			int indexV = 0;
			// Generate emit code
			for(int i=0; i<emitCount; i++){
				for(int j=0; j<keyCount; j++){
					if(j==0)
						emits += "keys"+j+"["+i+"] = ??;\n\t";
					else
						emits += "keys"+j+"["+i+"] = "+ext.candidateKeyTypes.get(ext.keyIndex).toLowerCase()+"MapGenerator_k"+indexK+++"("+inputDataSet.varName+", "+lcName+");\n\t";
				}
				for(int j=0; j<valCount; j++){
					emits += "values"+j+"["+i+"] = "+typeName+"MapGenerator_v"+indexV+++"("+inputDataSet.varName+", "+lcName+");\n\t";
				}
			}
		}
		
		return emits;
	}
	
	public static String generateReduceGrammarInlined(String type, MyWhileExt ext, int index, Map<String, String> blockArrays) {
		String generator = "";
			
		/******** Generate terminal options *******/
		Map<String,List<String>> terminals = new HashMap<String,List<String>>();
		terminals.put(type, new ArrayList());
		
		terminals.get(type).add("val1");
		for(int i=2; i<ext.valCount+2; i++)
			terminals.get(type).add("val"+i);
		
		for(Variable var : ext.inputVars){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				String keyType = "String";
				if(!var.getOriginalType().replace("[]", "").equals("String"))
					keyType = casper.Util.reducerType(var.getSketchType());
				if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
				terminals.get(keyType).add(var.varName);
			}
			else if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 0){
				for(String globalType : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalType.equals(var.getOriginalType())){
						// Add an option for each field that matches type
	        			for(Variable field : ext.globalDataTypesFields.get(globalType)){
	        				if(casper.Util.compatibleTypes(type,field.getOriginalType()) == 1){
	        					String keyType = "String";
	        					if(!var.getOriginalType().replace("[]", "").equals("String"))
	        						keyType = casper.Util.reducerType(var.getSketchType());
	        					if(!terminals.containsKey(keyType)) terminals.put(keyType, new ArrayList());
	        					terminals.get(keyType).add(var.varName + "." + field.varName);
	        				}
	        			}
	        		}
				}
			}
		}
		
		for(int i=0; i<ext.constCount; i++){
			if(casper.Util.compatibleTypes(type,"int") == 1){
				terminals.get(type).add("casperConst" + i);
			}
		}
		
		/********** Generate type expressions ********/
		String sketchType = type;
		if(type == "String")
			sketchType = "int";
		
		String typeName = type.toLowerCase();
		if(sketchType == "bit[32]")
			typeName = "bitInt";
		
		// Terminal names
		Map<String,List<String>> terminalNames = new HashMap<String,List<String>>();
		for(String ttype : terminals.keySet()){
			String ttypeName = ttype.toLowerCase();
			if(ttypeName == "bit[32]")
				ttypeName = "bitInt";
			
			terminalNames.put(ttype, new ArrayList<String>());
			for(int i=0; i<Math.pow(2.0, ext.recursionDepth-1); i++){
				terminalNames.get(ttype).add("_"+ttypeName+"_terminal"+(i));
			}
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		getMapExpressions(	type,
							ext.binaryOperators,
							ext.unaryOperators,
							ext.methodOperators,
							terminalNames,
							exprs,
							ext.recursionDepth
						);
		
		if(sketchType.equals("bit")){
			exprs.add("CASPER_TRUE");
			exprs.add("CASPER_FALSE");
		}
		
		/******** Generate args decl code *******/
		
		String argsDecl = type+" val1";
		for(int i=2; i<ext.valCount+2; i++)
			argsDecl += ", "+type+" val"+i;
		
		/******** Generate terminals code *******/
		Map<String,List<String>> terminalOptions = new HashMap<String,List<String>>();
		for(String ttype : terminals.keySet()){
			terminalOptions.put(ttype, new ArrayList<String>());
			for(int i=0; i<terminals.get(ttype).size(); i++){
				terminalOptions.get(ttype).add(terminals.get(ttype).get(i));
			}	
			switch(ttype){
				case "int":
					terminalOptions.get(ttype).add("??");
					break;
				case "bit[32]":
					terminalOptions.get(ttype).add("casper_genRandBitVec()");
					break;
				case "String":
					terminalOptions.get(ttype).add("??");
					break;
			}
		}
		
		String terminalsCode = "";
		Map<String,Integer> termIndex = new HashMap<String,Integer>();
		for(String ttype : terminals.keySet()){
			String ttypeName = ttype.toLowerCase();;
			if(ttypeName == "bit[32]")
				ttypeName = "bitInt";
			String ttype2 = ttype;
			if(ttype2.equals("String")) ttype2 = "int";
			termIndex.put(ttype, (int) Math.pow(2.0, ext.recursionDepth-1));
			for(int i=0; i<Math.pow(2.0, ext.recursionDepth-1); i++){
				terminalsCode += ttype2+" _"+ttypeName+"_terminal"+(i)+";\n\t";
				terminalsCode += "int  _"+ttypeName+"_terminal"+(i)+"c = ??("+(int)Math.ceil(Math.log(terminalOptions.get(ttype).size())/Math.log(2))+");\n\t";
				int optIndex = 0;
				for(String opt : terminalOptions.get(ttype)){
					String prefix = "else if";
					if(optIndex == 0) prefix = "if";
					if(!ttypeName.equals("string") && opt.equals("??")){
						terminalsCode += prefix+"(_"+ttypeName+"_terminal"+(i)+"c == "+optIndex+") { _term_flag_"+ttypeName+"_terminal"+(i)+"_reduce"+index+"["+optIndex+"] =  true; _"+ttypeName+"_terminal"+(i)+" = " + opt + "; assert _"+ttypeName+"_terminal"+(i)+" != 0; }\n\t";
						optIndex++;
					}
					else{
						terminalsCode += prefix+"(_"+ttypeName+"_terminal"+(i)+"c == "+optIndex+") { _term_flag_"+ttypeName+"_terminal"+(i)+"_reduce"+index+"["+optIndex+"] =  true; _"+ttypeName+"_terminal"+(i)+" = " + opt + "; }\n\t";
						optIndex++;
					}
						
				}
				terminalsCode += "else { assert false; }\n\t";
				blockArrays.put("_term_flag_"+ttypeName+"_terminal"+(i)+"_reduce"+index, Integer.toString(terminalOptions.get(ttype).size()));
			}
		}
		
		/******** Generate expressions code *******/
		String expressions = "int c = ??("+(int)Math.ceil(Math.log(exprs.size())/Math.log(2))+");\n\t";
		int c = 0;
		ext.grammarExps.put("reduceExp"+index, new ArrayList<String>());
		for(String expr : exprs){
			for(String ttype : terminals.keySet()){
				String ttypeName = ttype.toLowerCase();
				if(ttypeName == "bit[32]")
					ttypeName = "bitInt";			
					
				int i=0;
				while(expr.contains("<casper-"+ttype+"-term>")){
					int st_ind = expr.indexOf("<casper-"+ttype+"-term>");
					expr = expr.substring(0,st_ind) + terminalNames.get(ttype).get(i++) + expr.substring(st_ind+("<casper-"+ttype+"-term>").length(),expr.length());
				}
			}
			
			ext.grammarExps.get("reduceExp"+index).add(expr);
			int solID = 0;
			for(Map<String,String> sol : ext.blockExprs){
				if(expr.equals(sol.get("reduceExp"+index))){
					ext.blocks.get(solID).add("reduceExp"+index+"["+c+"]");
				}
				solID++;
			}
			if(c==0)
				expressions += "if(c=="+c+"){ reduceExp"+index+"["+c+"]=true; return " + expr + "; }\n\t";
			else
				expressions += "else if(c=="+c+"){ reduceExp"+index+"["+c+"]=true; return " + expr + "; }\n\t";
			c++;
		}
		expressions += "else { assert false; }\n\t";
		
		/****** Generate final output code ******/
		generator += "generator "+sketchType+" " +typeName+"ReduceGenerator"+index+"("+argsDecl+"){\n\t" + terminalsCode + expressions + "\n}";
		
		blockArrays.put("reduceExp"+index, Integer.toString(exprs.size()));
		return generator;
	}

	private static String generateInitFunctions(String type, Set<Variable> sketchOutputVars) {
		String code = "";
		
		for(Variable var : sketchOutputVars){
			switch(casper.Util.reducerType(var.getSketchType())){
				case "int":
					code += type + " init_" + var.varName + "("+type + " "+var.varName+"0){\n\treturn {| 0 | 1 | "+var.varName+"0 |};\n}";
					break;
				case "bit":
					code += type + " init_" + var.varName + "("+type + " "+var.varName+"0){\n\treturn {| CASPER_TRUE | CASPER_FALSE | "+var.varName+"0 |};\n}";
					break;
				case "bit[32]":
					code += type + " init_" + var.varName + "("+type + " "+var.varName+"0){\n\treturn {| genRandBitVec() | "+var.varName+"0 |};\n}";
					break;
				case "String":
					code += type + " init_" + var.varName + "("+type + " "+var.varName+"0){\n\treturn {| 0 | "+var.varName+"0 |};\n}";
					break;
			}
			code += "\n\n";
		}
		
		return code.substring(0,code.length()-2);
	}
	
	private static String generateCasperRInit(Set<Variable> sketchOutputVars) {
		String code = "";
		int index = 0;
		for(Variable var : sketchOutputVars){
			if(var.getSketchType().endsWith("["+Configuration.arraySizeBound+"]")){
				for(int i=0; i<Configuration.arraySizeBound; i++){
					code += "casper_r["+index+++"] = init_" + var.varName + "("+var.varName+"0["+i+"]);\n\t";
				}
			}
			else{
				code += "casper_r["+index+++"] = init_" + var.varName + "("+var.varName+"0);\n\t";
			}
		}
		return code;
	}
		
	private static String generateDeclKeysVals(String type, int keyCount, int valCount) {
		String code = "";
		for(int i=0; i<keyCount; i++){
			if(i==0)
				code += "int[CASPER_NUM_OUTVARS] keys"+i+";\n\t\t";
			else
				code += type+"[CASPER_NUM_OUTVARS] keys"+i+";\n\t\t";
		}
		for(int i=0; i<valCount; i++){
			code += type+"[CASPER_NUM_OUTVARS] values"+i+";\n\t\t";
		}
		return code;
	}

	public static String generateMapArgsCall(Variable inputDataSet, int keyCount, int valCount) {
		String mapArgs = inputDataSet.varName;
		
		mapArgs += ", casper_i";
		
		for(int i=0; i<keyCount; i++){
			mapArgs += ", keys" + i;
		}
		
		for(int i=0; i<valCount; i++){
			mapArgs += ", values" + i;
		}
		
		return mapArgs;
	}
	
	public static String generateInitKeys(String type, int keyCount){
		String code = "";
		
		for(int i=0; i<keyCount; i++){
			if(i==0){
				code += "int key0 = keys0[casper_j];\n\t\t\t";
			}
			else{
				code += type + " key"+i+" = keys"+i+"[casper_j];\n\t\t\t";
			}
		}
		
		return code;
	}
	
	public static String generateReduceByKey(Set<Variable> sketchOutputVars, int keyCount, int valCount){
		String code = "";
		int index = 0;
		int varID = 1;
		for(Variable var : sketchOutputVars){
			if(var.getSketchType().endsWith("["+Configuration.arraySizeBound+"]")){
				for(int i=0; i<Configuration.arraySizeBound; i++){
					String valargs = "";
					for(int j=0; j<valCount; j++) valargs += "values"+j+"[casper_j],"; valargs = valargs.substring(0,valargs.length()-1);
					code += "else if (key0 == "+varID+" && key1 == "+i+"){ casper_r["+index+"] = reduce_"+var.varName+"(casper_r["+index+"], "+valargs+"); }\n\t\t\t";
					index++;
				}
			}
			else{
				String valargs = "";
				for(int j=0; j<valCount; j++) valargs += "values"+j+"[casper_j],"; valargs = valargs.substring(0,valargs.length()-1);
				if(keyCount == 1)
					code += "else if (key0 == "+varID+"){ casper_r["+index+"] = reduce_"+var.varName+"(casper_r["+index+"], "+valargs+"); }\n\t\t\t";
				else
					code += "else if (key0 == "+varID+" && key1 == 0){ casper_r["+index+"] = reduce_"+var.varName+"(casper_r["+index+"], "+valargs+"); }\n\t\t\t";
				index++;
			}
			varID++;
		}
		return code;
		
		
	}
	
	private static String generateReduceFunctions(String type, Set<Variable> sketchOutputVars, int keyCount, int valCount) {
		String code = "";
		
		int index = 0;
		for(Variable var : sketchOutputVars){
			String valargs = type + " val1";
			String valargs2 = "val1";
			for(int j=2; j<valCount+2; j++) { valargs += ", " + type + " val"+j; valargs2 += ", val"+j; }
			code += type + " reduce_"+var.varName+"("+valargs+"){\n\treturn "+type+"ReduceGenerator"+index+++"("+valargs2+");\n}\n\n";
		}
		
		return code;
	}
	
	private static String generateMergeFunctions(String type, Set<Variable> sketchOutputVars, Set<SketchCall> methodOperators) {
		String code = "";
		
		for(Variable var : sketchOutputVars){
			switch(type){
				case "int":
					String minMax = "";
					for(SketchCall c : methodOperators){
						if(c.name.equals("casper_math_max")) 
							minMax += " | casper_math_max(val1,val2)";
						else if(c.name.equals("casper_math_min"))
							minMax += " | casper_math_min(val1,val2)";
					}
					code += type + " merge_"+var.varName+"(int val1, int val2){\n\treturn {| val1 | val1+val2"+minMax+" |};\n}\n";
					break;
				case "String":
					code += type + " merge_"+var.varName+"(int val1, int val2){\n\treturn {| val1 |};\n}\n";
					break;
				case "bit":
					code += type + " merge_"+var.varName+"(bit val1, bit val2){\n\tbit option1 = val1 || val2; bit option2 = val1 && val2; return {| val1 | option1 | option2 |};\n}\n";
					break;
				case "bit[32]":
					code += type + " merge_"+var.varName+"(bit[32] val1, bit[32] val2){\n\treturn {| val1 |};\n}\n";
					break;
			}
		}
			
		return  code;
	}
	
	private static String generateMergeOutput(Set<Variable> sketchOutputVars, int keyCount) {
		String code = "";
		int index = 0;
		for(Variable var : sketchOutputVars){
			if(var.getSketchType().endsWith("["+Configuration.arraySizeBound+"]")){
				for(int i=0; i<Configuration.arraySizeBound; i++){
					code += "casper_r["+index+"] = merge_" + var.varName + "(casper_r["+index+"],"+var.varName+"0["+i+"]);\n\t";
					index++;
				}
			}
			else{
				code += "casper_r["+index+"] = merge_" + var.varName + "(casper_r["+index+"],"+var.varName+"0);\n\t";
				index++;
			}
		}
		return code;
	}
	
	private static String generateReduceArgsDecl(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> loopCounters) {
		String reduce_args = inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + inputDataSet.varName;

		for(Variable var : sketchOutputVars){
			reduce_args += ", " + var.getSketchType() + " " + var.varName;
			reduce_args += ", " + var.getSketchType() + " " + var.varName + "0";
		}
		for(Variable var : loopCounters){
			reduce_args += ", " + var.getSketchType() + " " + var.varName;
			reduce_args += ", " + var.getSketchType() + " " + var.varName + "0";
		}
		
		return reduce_args;
	}
	
	private static String generateBlockGenerated(MyWhileExt ext) {
		String code = "";
		
		int solID = 0;
		for(Map<String,String> sol : ext.blockExprs){
			for(String bitArrayName : sol.keySet()){
				if(bitArrayName.startsWith("_term_flag")){
					ext.blocks.get(solID).add(bitArrayName+"["+sol.get(bitArrayName)+"]");
				}
			}
			solID++;
		}
		
		for(List<String> solution : ext.blocks){
			code += "if(";
			for(String bit : solution){
				code += bit + " && ";
			}
			code = code.substring(0,code.length()-4) + ") assert false;\n\t\t";
		}
		
		return code;
	}
	
	private static String generateMapGenerators(String sketchReducerType, int keyCount, Map<String, String> blockArrays, Set<Variable> sketchFilteredOutputVars, MyWhileExt ext) {
		String mapGenerators = "";
		int indexC = 0;
		int indexK = 0;
		int indexV = 0;
		for(int i=0; i<sketchFilteredOutputVars.size(); i++){
			if(ext.useConditionals){ 
				mapGenerators += generateMapGrammarInlined(ext, "bit", "_c"+indexC++, blockArrays) + "\n\n";
			}
			for(int j=1; j<keyCount; j++){
				mapGenerators += generateMapGrammarInlined(ext, ext.candidateKeyTypes.get(ext.keyIndex), "_k"+indexK++, blockArrays) + "\n\n";
			}
			for(int j=0; j<ext.valCount; j++){
				mapGenerators += generateMapGrammarInlined(ext, sketchReducerType, "_v"+indexV++, blockArrays) + "\n\n";
			}
		}
		return mapGenerators;
	}
	
	private static String generateReduceGenerators(String sketchReducerType, Map<String, String> blockArrays, Set<Variable> sketchFilteredOutputVars, MyWhileExt ext) {
		String reduceGenerators = "";
		int index = 0;
		for(int i=0; i<sketchFilteredOutputVars.size(); i++){
			reduceGenerators += generateReduceGrammarInlined(sketchReducerType, ext, index++, blockArrays) + "\n\n";
		}
		return reduceGenerators;
	}

	private static String generateDeclBlockArrays(Map<String, String> blockArrays) {
		String code = "";
		
		for(String arrName : blockArrays.keySet()){
			code += "bit["+blockArrays.get(arrName)+"] "+arrName+" = {false};\n";
		}
		
		return code;
	}
}
