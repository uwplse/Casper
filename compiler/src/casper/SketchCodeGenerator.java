package casper;

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
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.ArrayUpdateNode;
import casper.types.ConditionalNode;
import casper.types.ConstantNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.types.Variable;
import polyglot.ast.FieldDecl;
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
		
		// Declare input / broadcast variables
		String broadcastVarsDecl = declBroadcastVars(ext.constCount, ext.inputVars);
		
		// Generate main function args
		Map<String,Integer> argsList = new HashMap<String,Integer>();
		String mainFuncArgsDecl = generateMainFuncArgs(ext, ext.inputVars, sketchFilteredOutputVars, ext.loopCounters, argsList);
		
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
		verifCode += "if(" + invariant + " && " + loopCond + ") {\n\t\t"+wpcValuesInit+"assert " + wpc + ";\n\t}\n\t";
		
		// 2. Assert loop invariant implies the post condition if the loop terminates: I && loop condition is false --> POST
		verifCode += "if(" + invariant + " && " + loopCondFalse + ") {\n\t\tassert " + postC + ";\n\t}";
	
		// Generate post condition args
		String postConditionArgsDecl = generatePostConditionArgsDecl(ext.inputDataSet,sketchFilteredOutputVars,ext.loopCounters,ext.postConditionArgsOrder.get(reducerType));
		
		// Generate loop invariant args
		String loopInvariantArgsDecl = generateLoopInvariantArgsDecl(ext.inputDataSet,sketchFilteredOutputVars,ext.loopCounters,ext.postConditionArgsOrder.get(reducerType));
		
		// Generate post condition body
		String postCondition = generatePostCondition(ext.inputDataSet, sketchFilteredOutputVars,ext.loopCounters, ext.postConditionArgsOrder.get(reducerType));
		
		// Generate loop invariant body
		String loopInvariant = generateLoopInvariant(ext.inputDataSet, sketchFilteredOutputVars,ext.loopCounters, ext.postConditionArgsOrder.get(reducerType));
	
		// Generate int expression generator for map
		String mapGenerators = generateMapGrammarInlined(reducerType, ext);
		
		// Generate map function args declaration
		String mapArgsDecl = generateMapArgsDecl(ext.inputDataSet, ext.loopCounters, ext.postConditionArgsOrder.get(reducerType), keyCount, 1);
		
		// Generate map function emit code
		String mapEmits = generateDomapEmits(reducerType, ext.inputDataSet, ext.loopCounters, sketchFilteredOutputVars.size(), keyCount, 1);
		
		// Generate reduce/fold expression generator
		String reduceGenerator = generateReduceGrammarInlined(reducerType, ext);
			
		// Generate functions to init values in reducer
		String initFunctions = generateInitFunctions(sketchReducerType, sketchFilteredOutputVars);
		
		String casperRInit = generateCasperRInit(sketchFilteredOutputVars);
		
		// Declare key-value arrays in reduce
		String declKeysVals = generateDeclKeysVals(keyCount, 1);
		
		// Generate map function call args
		String mapArgsCall = generateMapArgsCall(ext.inputDataSet, keyCount, 1);
		
		// Initialize key variables
		String initKeys = generateInitKeys(sketchReducerType, keyCount);
		
		// Generate code to fold values by key
		String reduceByKey = generateReduceByKey(sketchFilteredOutputVars, keyCount, 1);
		
		// Generate reduce functions
		String reduceFunctions = generateReduceFunctions(sketchReducerType, sketchFilteredOutputVars, keyCount, 1);
		
		// Generate merge functions
		String mergeFunctions = generateMergeFunctions(sketchReducerType, sketchFilteredOutputVars);
		
		// Generate code to merge output with initial values
		String mergeOutput = generateMergeOutput(sketchFilteredOutputVars, keyCount);
		
		// Modify template
		text = text.replace("<output-type>", reducerType);
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
		
		// Save
		writer.print(text);
		writer.close();
	}
	





	// Generate code that includes all necessary files
	public static String generateIncludeList(MyWhileExt ext, int id) {
		String includeList = "";
		
		for(String dataType : ext.globalDataTypes){
			includeList += "include \"output/" + dataType + ".sk\";\n";
		}
		
		return includeList;
	}
	
	// Generate code that initializes main function args
	
	public static String generateMainFuncArgs(MyWhileExt ext, Set<Variable> sketchInputVars, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters, Map<String, Integer> argsList) {
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
	
	public static String initMainInputData(MyWhileExt ext, Map<String, Integer> argsList) {
		String inputInit = "";
		
		if(ext.hasInputData){
			if(ext.inputDataCollections.size() == 1){
				ext.inputDataSet = ext.inputDataCollections.get(0);
			}
			else if(ext.inputDataCollections.size() > 1){
				ext.inputDataSet = new Variable("casper_data_set","CasperDataRecord["+Configuration.arraySizeBound+"]","",Variable.ARRAY_ACCESS);
				ext.globalDataTypes.add("CasperDataRecord");
				ext.globalDataTypesFields.put("CasperDataRecord", new HashSet<Variable>());
				for(Variable var : ext.inputDataCollections){
					ext.globalDataTypesFields.get("CasperDataRecord").add(new Variable(var.varName,var.getReduceType(),"",Variable.VAR));
				}
			}
			
			inputInit = ext.inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", "["+(Configuration.arraySizeBound-1)+"]") + " " + ext.inputDataSet.varName + ";\n\t";
			for(int i=0; i<Configuration.arraySizeBound-1; i++){
				inputInit += handleInputDataInit(ext.inputDataSet.getSketchType().replace("["+Configuration.arraySizeBound+"]", ""),ext.inputDataSet.varName+"["+i+"]",ext,argsList);
			}
		}
		// No collection accessed. Generate collection.
		else{			
			/*
			ext.inputDataCollections.add(new Variable("input_data","int["+Configuration.arraySizeBound+"]",0));
			ext.hasInputData = false;
			
			Variable lc = sketchLoopCounters.get(0);
			
			if(sketchLoopCounters.size() > 1)
				System.err.println(">>>>>>>>>>>>>>> MULTIPLE LOOP COUNTERS <<<<<<<<<<<<<<");
			if(sketchLoopCounters.size() == 0)
				System.err.println(">>>>>>>>>>>>>>> NO LOOP COUNTERS <<<<<<<<<<<<<<");
			if(!ext.initVals.containsKey(lc.name))
				System.err.println(">>>>>>>>>>>>>>> LOOP COUNTER INITIAL VALUE UNKNOWN <<<<<<<<<<<<<<");
			
			inputInit += "int["+(Configuration.arraySizeBound-1)+"] input_data;\n\t";
			inputInit += "int " + lc.name + "_it = " + ext.initVals.get(lc.name) + ";\n\t";
			inputInit += "for(int it=0; it<"+(Configuration.arraySizeBound-1)+";it++){\n\t\t";
			inputInit += "input_data[it] = " + lc.name + "_it;\n\t\t";
			inputInit += lc.name + "_tt = " + ext.incrementExps.get(0).replaceAll(lc.name, new IdentifierNode(lc.name+"_tt")) + ";\n\t";
			inputInit += "}";
			*/
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
	
	public static String generatePostCondition(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters, List<String> postConditionArgsOrder) {
		String postCond = "";
		
		int index = 0;
		for(String nextVarName : postConditionArgsOrder){
			for(Variable var : sketchOutputVars){
				if(nextVarName.equals(var.varName)){
					postCond += "casper_r["+index+++"] = " + var.varName + ";\n\t";
				}
			}
		}
		
		String reduce_args = inputDataSet.varName;
		for(String nextVarName : postConditionArgsOrder){
			for(Variable var : sketchOutputVars){
				if(nextVarName.equals(var.varName)){
					reduce_args += ", " + nextVarName;
					reduce_args += ", " + nextVarName + "0";
				}
			}
			for(Variable var : sketchLoopCounters){
				if(nextVarName.equals(var.varName)){
					reduce_args += ", " + nextVarName;
					reduce_args += ", " + nextVarName + "0";
				}
			}
		}
		
		postCond += "return reduce("+reduce_args+") == casper_r;";
		
		return postCond;
	}
	
	
	public static String generateLoopInvariant(Variable inputDataSet, Set<Variable> sketchOutputVars, Set<Variable> sketchLoopCounters, List<String> loopInvariantArgsOrder) {
		String inv = "";
		
		int index = 0;
		for(String nextVarName : loopInvariantArgsOrder){
			for(Variable var : sketchOutputVars){
				if(nextVarName.equals(var.varName)){
					inv += "casper_r["+index+++"] = " + var.varName + ";\n\t";
				}
			}
		}
		
		String reduce_args = inputDataSet.varName;
		for(String nextVarName : loopInvariantArgsOrder){
			for(Variable var : sketchOutputVars){
				if(nextVarName.equals(var.varName)){
					reduce_args += ", " + nextVarName;
					reduce_args += ", " + nextVarName + "0";
				}
			}
			for(Variable var : sketchLoopCounters){
				if(nextVarName.equals(var.varName)){
					reduce_args += ", " + nextVarName;
					reduce_args += ", " + nextVarName + "0";
				}
			}
		}
		
		for(Variable var : sketchLoopCounters){
			inv += "return 0 <= " + var.varName + " && " + var.varName + " <= " + (Configuration.arraySizeBound-1) + " && reduce("+reduce_args+") == casper_r;";
			break;
		}
		
		return inv;
	}
	
	// Generate do map grammars
	
	public static String generateMapGrammarInlined(String type, MyWhileExt ext) {
		String generator = "";
			
		/******** Generate terminal options *******/
		Map<String,List<String>> terminals = new HashMap<String,List<String>>();
		terminals.put(type, new ArrayList());
		
		for(Variable var : ext.loopCounters){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				terminals.get(type).add(var.varName);
			}
		}
		for(Variable var : ext.inputVars){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				terminals.get(type).add(var.varName);
			}
			else if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 0){
				for(String globalType : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalType.equals(var.getOriginalType())){
						// Add an option for each field that matches type
	        			for(Variable field : ext.globalDataTypesFields.get(globalType)){
	        				if(casper.Util.compatibleTypes(type,field.getSketchType()) == 1){
	        					terminals.get(type).add(var.varName + "." + field.varName);
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
						if(casper.Util.compatibleTypes(type,field.getSketchType()) == 1){
							for(Variable lc : ext.loopCounters){
								terminals.get(type).add(ext.inputDataSet.varName + "["+lc.varName+"]." + field.varName);
							}
						}
					}
				}
    		}
			
			for(Variable lc : ext.loopCounters){
				terminals.get(type).add(ext.inputDataSet.varName + "["+lc.varName+"]");
			}
		}
		else if(casper.Util.getTypeClass(ext.inputDataSet.getSketchType()) == casper.Util.ARRAY){
			for(Variable lc : ext.loopCounters){
				terminals.get(type).add(ext.inputDataSet.varName + "["+lc.varName+"]");
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
			terminalNames.put(ttype, new ArrayList<String>());
			for(int i=0; i<terminals.get(ttype).size(); i++){
					terminalNames.get(ttype).add("_"+typeName+"_terminal"+i);
			}
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		Map<String,Integer> constants = new HashMap<String,Integer>();
		getConstantsRequired(	type,
								ext.binaryOperators,
								ext.unaryOperators,
								ext.methodOperators,
								constants,
								Configuration.recursionDepth
							);
		getMapExpressions(	type,
							ext.binaryOperators,
							ext.unaryOperators,
							ext.methodOperators,
							terminalNames,
							exprs,
							constants,
							Configuration.recursionDepth
						);
		
		/******** Generate args decl code *******/
		
		String argsDecl = ext.inputDataSet.getSketchType().replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataSet.varName;
		for(Variable var : ext.loopCounters){
			argsDecl += ", " + var.getSketchType() + " " + var.varName;
		}
		
		/******** Generate terminals code *******/
		String terminalsCode = "";
		int termIndex = 0;
		for(termIndex=0; termIndex<terminals.get(type).size(); termIndex++){
			terminalsCode += sketchType+" _"+typeName+"_terminal"+(termIndex)+" = "+terminals.get(type).get(termIndex)+";\n\t";
		}
		for(String cType : constants.keySet()){
			for(int i=termIndex; i<termIndex+constants.get(cType); i++){
				String ctypeName = cType.toLowerCase();
				if(cType == "bit[32]")
					ctypeName = "bitInt";
				
				if(cType.equals(type)){
					switch(cType){
						case "int":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = ??;\n\t";
							break;
						case "bit":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = {| true | false |};\n\t";
							break;
						case "bit[32]":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = genRandBitVec();\n\t";
							break;
						case "String":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = ??;\n\t";
							break;
					}
				}
				else{
					String args = ext.inputDataSet.varName;
					for(Variable var : ext.loopCounters){
						argsDecl += ", " + var.varName;
					}
					switch(cType){
						case "int":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = intMapGenerator("+args+");\n\t";
							break;
						case "bit":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = bitMapGenerator("+args+");\n\t";
							break;
						case "bit[32]":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = bitIntMapGenerator("+args+")\n\t";
							break;
						case "String":
							terminalsCode += "int _"+ctypeName+"_terminal"+(i)+" = stringMapGenerator("+args+");\n\t";
							break;
					}
				}
			}
		}
		
		/******** Generate expressions code *******/
		String expressions = "int c = ??("+(int)Math.ceil(Math.log(exprs.size())/Math.log(2))+");\n\t";
		int c = 0;
		for(String expr : exprs){
			for(String ttype : constants.keySet()){
				String ttypeName = ttype.toLowerCase();
				if(ttypeName == "bit[32]")
					ttypeName = "bitInt";			
					
				int i=terminals.get(ttype).size();
				while(expr.contains("<"+ttype+"-term>")){
					int st_ind = expr.indexOf("<"+ttype+"-term>");
					expr = expr.substring(0,st_ind) + "_"+ttypeName+"_terminal"+(i++) + expr.substring(st_ind+("<"+ttype+"-term>").length(),expr.length());
				}
			}
			
			
			expressions += "if(c=="+c+++"){ return " + expr + "; }\n\t";
		}
		
		/****** Generate final output code ******/
		generator += "generator "+sketchType+" " +typeName+"MapGenerator("+argsDecl+"){\n\t" + terminalsCode + expressions + "\n}";
		
		return generator;
	}
	
	
	private static void getConstantsRequired(String type, Set<String> binaryOps, Set<String> unaryOps, Set<SketchCall> methodOps, Map<String, Integer> constants, int depth) {
		if(depth == 0){
			return;
		}
		if(depth == 1){
			constants.put(type, 1);
			return;
		}
		else{
			for(String op : binaryOps){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					if(type.equals("bit")){
						if(casper.Util.operandTypes(op) == casper.Util.BIT_ONLY){
							Map<String,Integer> subConstants = new HashMap<String,Integer>();
							getConstantsRequired(type,binaryOps,unaryOps,methodOps,subConstants,depth-1);
							for(String ctype : subConstants.keySet()){
								if(!constants.containsKey(ctype)) constants.put(ctype, 0);
								if(depth == 2)
									constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)));
								else
									constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)*2));
							}
						}
						
					}
					else if(type.equals("bit[32]")){
						if(casper.Util.operandTypes(op) == casper.Util.VEC_ONLY){
							Map<String,Integer> subConstants = new HashMap<String,Integer>();
							getConstantsRequired(type,binaryOps,unaryOps,methodOps,subConstants,depth-1);
							for(String ctype : subConstants.keySet()){
								if(!constants.containsKey(ctype)) constants.put(ctype, 0);
								if(depth == 2)
									constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)));
								else
									constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)*2));
							}
						}
						else if(casper.Util.operandTypes(op) == casper.Util.VEC_INT){
							Map<String,Integer> subConstants = new HashMap<String,Integer>();
							getConstantsRequired(type,binaryOps,unaryOps,methodOps,subConstants,depth-1);
							getConstantsRequired("int",binaryOps,unaryOps,methodOps,subConstants,depth-1);
							for(String ctype : subConstants.keySet()){
								if(!constants.containsKey(ctype)) constants.put(ctype, 0);
								if(depth == 2)
									constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)));
								else
									constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)*2));
							}
						}
					}
					else{
						Map<String,Integer> subConstants = new HashMap<String,Integer>();
						getConstantsRequired(type,binaryOps,unaryOps,methodOps,subConstants,depth-1);
						for(String ctype : subConstants.keySet()){
							if(!constants.containsKey(ctype)) constants.put(ctype, 0);
							if(depth == 2)
								constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)));
							else
								constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)*2));
						}
					}
				}
			}
			for(String op : unaryOps){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					Map<String,Integer> subConstants = new HashMap<String,Integer>();
					getConstantsRequired(type,binaryOps,unaryOps,methodOps,subConstants,depth-1);
					for(String ctype : subConstants.keySet()){
						if(!constants.containsKey(ctype)) constants.put(ctype, 0);
						if(depth == 2)
							constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)));
						else
							constants.put(ctype, Math.max(constants.get(ctype),subConstants.get(ctype)*2));
					}
				}
			}
			for(SketchCall op : methodOps){
				// TODO
			}
			
			return;
		}
	}

	
	private static void getMapExpressions(String type, Set<String> binaryOps, Set<String> unaryOps, Set<SketchCall> methodOps, Map<String,List<String>> terminals, List<String> exprs, Map<String, Integer> constants, int depth) {
		if(depth == 0){
			return;
		}
		if(depth == 1){
			if(terminals.containsKey(type)){
				for(String terminal : terminals.get(type))
					exprs.add(terminal);
			}
			exprs.add("<"+type+"-term>");
			return;
		}
		else{
			if(terminals.containsKey(type)){
				for(String terminal : terminals.get(type))
					exprs.add(terminal);
			}
			
			exprs.add("<"+type+"-term>");
			
			for(String op : binaryOps){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					if(type.equals("bit")){
						if(casper.Util.operandTypes(op) == casper.Util.BIT_ONLY){
							if(casper.Util.isAssociative(op)){
								List<String> subExprs = new ArrayList<String>();
								getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
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
							else {
								List<String> subExprs = new ArrayList<String>();
								getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
									}
								}
							}
						}
						
					}
					else if(type.equals("bit[32]")){
						if(casper.Util.operandTypes(op) == casper.Util.VEC_ONLY){
							if(casper.Util.isAssociative(op)){
								List<String> subExprs = new ArrayList<String>();
								getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
											continue;
										if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
											continue;
										boolean rhsIsTerminal = true;
										boolean lhsIsTerminal = true;
										for(String ttype : terminals.keySet()){
											if(terminals.get(ttype).contains(exprRight)){
												rhsIsTerminal = false;
												break;
											}
										}
										for(String ttype : terminals.keySet()){
											if(terminals.get(ttype).contains(exprLeft)){
												lhsIsTerminal = false;
												break;
											}
										}
										if(depth == 2 && lhsIsTerminal && rhsIsTerminal)
											continue;
										exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
									}
								}
							}
							else {
								List<String> subExprs = new ArrayList<String>();
								getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										boolean rhsIsTerminal = true;
										boolean lhsIsTerminal = true;
										for(String ttype : terminals.keySet()){
											if(terminals.get(ttype).contains(exprRight)){
												rhsIsTerminal = false;
												break;
											}
										}
										for(String ttype : terminals.keySet()){
											if(terminals.get(ttype).contains(exprLeft)){
												lhsIsTerminal = false;
												break;
											}
										}
										if(lhsIsTerminal && rhsIsTerminal)
											continue;
										exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
									}
								}
							}
						}
						else if(casper.Util.operandTypes(op) == casper.Util.VEC_INT){
							List<String> lhsSubExprs = new ArrayList<String>();
							List<String> rhsSubExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,lhsSubExprs,constants,depth-1);
							getMapExpressions("int",binaryOps,unaryOps,methodOps,terminals,rhsSubExprs,constants,depth-1);
							for(String exprLeft : lhsSubExprs){
								for(String exprRight : rhsSubExprs){
									exprRight = exprRight.replace("<-expr->", "intTerminals()");
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									boolean rhsIsConstant = true;
									boolean lhsIsConstant = true;
									for(String ttype : terminals.keySet()){
										if(terminals.get(ttype).contains(exprRight)){
											rhsIsConstant = false;
											break;
										}
									}
									for(String ttype : terminals.keySet()){
										if(terminals.get(ttype).contains(exprLeft)){
											lhsIsConstant = false;
											break;
										}
									}
									if(lhsIsConstant && rhsIsConstant)
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
					}
					else{
						if(casper.Util.isAssociative(op)){
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
										continue;
									boolean rhsIsNotTerminal = true;
									boolean lhsIsNotTerminal = true;
									for(String ttype : terminals.keySet()){
										if(terminals.get(ttype).contains(exprRight)){
											rhsIsNotTerminal = false;
											break;
										}
									}
									for(String ttype : terminals.keySet()){
										if(terminals.get(ttype).contains(exprLeft)){
											lhsIsNotTerminal = false;
											break;
										}
									}
									if(lhsIsNotTerminal && rhsIsNotTerminal && depth == 2)
										continue;
									exprs.add("("+exprLeft + " " + op + " " + exprRight + ")");
								}
							}
						}
						else{
							List<String> subExprs = new ArrayList<String>();
							getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									boolean rhsIsTerminal = true;
									boolean lhsIsTerminal = true;
									for(String ttype : terminals.keySet()){
										if(terminals.get(ttype).contains(exprRight)){
											rhsIsTerminal = false;
											break;
										}
									}
									for(String ttype : terminals.keySet()){
										if(terminals.get(ttype).contains(exprLeft)){
											lhsIsTerminal = false;
											break;
										}
									}
									if(lhsIsTerminal && rhsIsTerminal)
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
					String call = op.name+"(";
					for(String argType : op.args){
						if(casper.Util.getSketchTypeFromRaw(argType).equals(type)){
							call += "<-expr->,";
						}
						else{
							// Generate args for generator functions
							String args = "";//inputDataCollection.name;
							//for(Variable var : sketchLoopCounters){
							//	args += ", " + var.varName + ", ";
							//}
							args = args.substring(0, args.length()-2);
							if(argType.equals("java.lang.String"))
								call += "stringMapGenerator("+args+"),";
							else
								call += casper.Util.getSketchTypeFromRaw(argType).toLowerCase()+"MapGenerator("+args+"),";
						}
						 
					}
					call = call.substring(0,call.length()-1) + ")";
					List<String> subExprs = new ArrayList<String>();
					getMapExpressions(type,binaryOps,unaryOps,methodOps,terminals,subExprs,constants,depth-1);
					List<String> newExprs = new ArrayList<String>();
					String newExprString = call;	
					fillExpressions(exprs,subExprs,newExprs,newExprString,0);
					exprs.addAll(newExprs);
				}
			}
			
			return;
		}
	}
	
	
	private static void fillExpressions(List<String> exprs, List<String> subExprs, List<String> newExprs, String newExprString, int st_index) {
		int index = newExprString.indexOf("<-expr->",st_index);
		
		if(index == -1){
			newExprs.add(newExprString);
			return;
		}
		
		for(String expr : subExprs){
			String newExprStringUpdated = newExprString.substring(0,index) + expr + newExprString.substring(index+8,newExprString.length());
			fillExpressions(exprs,subExprs,newExprs,newExprStringUpdated,index-8+expr.length()+1);
		}
	}
	
	
	public static String generateMapArgsDecl(Variable inputDataSet, Set<Variable> sketchLoopCounters, List<String> postConditionArgsOrder, int keyCount, int valCount) {
		String mapArgs = inputDataSet.getSketchType().replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + inputDataSet.varName;
		
		for(Variable var : sketchLoopCounters){
			mapArgs += ", " + var.getSketchType() + " " + var.varName;
		}
		for(int i=0; i<keyCount; i++){
			mapArgs += ", ref int[CASPER_NUM_OUTVARS] keys"+i;
		}
		for(int i=0; i<valCount; i++){
			mapArgs += ", ref int[CASPER_NUM_OUTVARS] values"+i;
		}
		
		return mapArgs;
	}
	
	
	public static String generateDomapEmits(String type, Variable inputDataSet, Set<Variable> sketchLoopCounters, int emitCount, int keyCount, int valCount) {
		String emits = "";
		
		// Generate args for generator functions
		String args = inputDataSet.varName;
		for(Variable var : sketchLoopCounters){
			args += ", " + var.varName + ", ";
		}
		args = args.substring(0, args.length()-2);
		
		String sketchType = type;
		if(type == "String")
			sketchType = "int";
		
		String typeName = type.toLowerCase();
		if(sketchType == "bit[32]")
			typeName = "bitInt";
		
		// Include conditionals?
		if(Configuration.useConditionals){
			// Generate emit code
			for(int i=0; i<emitCount; i++){
				emits += 	"if(bitMapGenerator("+args+")){\n\t\t";
				for(int j=0; j<keyCount; j++){
					if(j==0)
						emits += "keys["+i+"] = ??;\n\t\t";
					else
						emits += "keys["+i+"] = "+typeName+"MapGenerator(data, i);\n\t\t";
				}
				for(int j=0; j<valCount; j++){
					emits += "values["+i+"] = "+typeName+"MapGenerator(data, i);\n\t\t";
				}
				emits = emits.substring(0,emits.length()-1);
				emits += "};";
			}
		}
		else{
			// Generate emit code
			for(int i=0; i<emitCount; i++){
				for(int j=0; j<keyCount; j++){
					if(j==0)
						emits += "keys"+j+"["+i+"] = ??;\n\t";
					else
						emits += "keys"+j+"["+i+"] = "+typeName+"MapGenerator(data, i);\n\t";
				}
				for(int j=0; j<valCount; j++){
					emits += "values"+j+"["+i+"] = "+typeName+"MapGenerator(data, i);\n\t";
				}
			}
		}
		
		return emits;
	}
	
	// Generate do map grammars
	
	public static String generateReduceGrammarInlined(String type, MyWhileExt ext) {
		String generator = "";
			
		/******** Generate terminal options *******/
		Map<String,List<String>> terminals = new HashMap<String,List<String>>();
		terminals.put(type, new ArrayList());
		
		terminals.get(type).add("val1");
		terminals.get(type).add("val2");
		
		
		for(Variable var : ext.inputVars){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				terminals.get(type).add(var.varName);
			}
			else if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 0){
				for(String globalType : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalType.equals(var.getOriginalType())){
						// Add an option for each field that matches type
	        			for(Variable field : ext.globalDataTypesFields.get(globalType)){
	        				if(casper.Util.compatibleTypes(type,field.getSketchType()) == 1){
	        					terminals.get(type).add(var.varName + "." + field.varName);
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
			terminalNames.put(ttype, new ArrayList<String>());
			for(int i=0; i<terminals.get(ttype).size(); i++){
					terminalNames.get(ttype).add("_"+typeName+"_terminal"+i);
			}
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		Map<String,Integer> constants = new HashMap<String,Integer>();
		getConstantsRequired(	type,
								ext.binaryOperators,
								ext.unaryOperators,
								ext.methodOperators,
								constants,
								Configuration.recursionDepth
							);
		getMapExpressions(	type,
							ext.binaryOperators,
							ext.unaryOperators,
							ext.methodOperators,
							terminalNames,
							exprs,
							constants,
							Configuration.recursionDepth
						);
		
		/******** Generate args decl code *******/
		
		String argsDecl = type+" val1, "+type+" val2";
		
		/******** Generate terminals code *******/
		String terminalsCode = "";
		int termIndex = 0;
		for(termIndex=0; termIndex<terminals.get(type).size(); termIndex++){
			terminalsCode += sketchType+" _"+typeName+"_terminal"+(termIndex)+" = "+terminals.get(type).get(termIndex)+";\n\t";
		}
		for(String cType : constants.keySet()){
			for(int i=termIndex; i<termIndex+constants.get(cType); i++){
				String ctypeName = cType.toLowerCase();
				if(cType == "bit[32]")
					ctypeName = "bitInt";
				
				if(cType.equals(type)){
					switch(cType){
						case "int":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = ??;\n\t";
							break;
						case "bit":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = {| true | false |};\n\t";
							break;
						case "bit[32]":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = genRandBitVec();\n\t";
							break;
						case "String":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = ??;\n\t";
							break;
					}
				}
				else{
					String args = ext.inputDataSet.varName;
					for(Variable var : ext.loopCounters){
						argsDecl += ", " + var.varName;
					}
					switch(cType){
						case "int":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = intMapGenerator("+args+");\n\t";
							break;
						case "bit":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = bitMapGenerator("+args+");\n\t";
							break;
						case "bit[32]":
							terminalsCode += cType+" _"+ctypeName+"_terminal"+(i)+" = bitIntMapGenerator("+args+")\n\t";
							break;
						case "String":
							terminalsCode += "int _"+ctypeName+"_terminal"+(i)+" = stringMapGenerator("+args+");\n\t";
							break;
					}
				}
			}
		}
		
		/******** Generate expressions code *******/
		String expressions = "int c = ??("+(int)Math.ceil(Math.log(exprs.size())/Math.log(2))+");\n\t";
		int c = 0;
		for(String expr : exprs){
			for(String ttype : constants.keySet()){
				String ttypeName = ttype.toLowerCase();
				if(ttypeName == "bit[32]")
					ttypeName = "bitInt";			
					
				int i=terminals.get(ttype).size();
				while(expr.contains("<"+ttype+"-term>")){
					int st_ind = expr.indexOf("<"+ttype+"-term>");
					expr = expr.substring(0,st_ind) + "_"+ttypeName+"_terminal"+(i++) + expr.substring(st_ind+("<"+ttype+"-term>").length(),expr.length());
				}
			}
			
			
			expressions += "if(c=="+c+++"){ return " + expr + "; }\n\t";
		}
		
		/****** Generate final output code ******/
		generator += "generator "+sketchType+" " +typeName+"ReduceGenerator("+argsDecl+"){\n\t" + terminalsCode + expressions + "\n}";
		
		return generator;
	}
	
	
	// Generate reduce init functions
	private static String generateInitFunctions(String type, Set<Variable> sketchOutputVars) {
		String code = "";
		
		for(Variable var : sketchOutputVars){
			switch(var.getSketchType()){
				case "int":
					code += type + " init_" + var.varName + "(){\n\treturn {| 0 | 1 |};\n}";
					break;
				case "bit":
					code += type + " init_" + var.varName + "(){\n\treturn {| true | false |};\n}";
					break;
				case "bit[32]":
					code += type + " init_" + var.varName + "(){\n\treturn genRandBitVec();\n}";
					break;
				case "String":
					code += type + " init_" + var.varName + "(){\n\treturn 0;\n}";
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
					code += "casper_r["+index+++"] = init_" + var.varName + "();\n\t";
				}
			}
			else{
				code += "casper_r["+index+++"] = init_" + var.varName + "();\n\t";
			}
		}
		return code;
	}
	
	
	private static String generateDeclKeysVals(int keyCount, int valCount) {
		String code = "";
		for(int i=0; i<keyCount; i++){
			code += "int[CASPER_NUM_OUTVARS] keys"+i+";\n\t\t";
		}
		for(int i=0; i<keyCount; i++){
			code += "int[CASPER_NUM_OUTVARS] values"+i+";\n\t\t";
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
				code += "int key0 = keys0[casper_j];\n\t";
			}
			else{
				code += type + " key"+i+" = keys"+i+"[casper_j];\n\t";
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
					for(int j=0; j<valCount; j++) valargs += "values"+j+"[casper_j]";
					code += "else if (key0 == "+varID+" && key1 == "+i+"){ casper_r["+index+"] = reduce_"+var.varName+"(casper_r["+index+"], "+valargs+"); }";
					index++;
				}
			}
			else{
				String valargs = "";
				for(int j=0; j<valCount; j++) valargs += "values"+j+"[casper_j]";
				if(keyCount == 1)
					code += "else if (key0 == "+varID+"){ casper_r["+index+"] = reduce_"+var.varName+"(casper_r["+index+"], "+valargs+"); }";
				else
					code += "else if (key0 == "+varID+" && key1 == 0){ casper_r["+index+"] = reduce_"+var.varName+"(casper_r["+index+"], "+valargs+"); }";
				index++;
			}
			varID++;
		}
		return code;
		
		
	}
	
	private static String generateReduceFunctions(String type, Set<Variable> sketchOutputVars, int keyCount, int valCount) {
		String code = "";
		
		for(Variable var : sketchOutputVars){
			String valargs = "int val1";
			String valargs2 = "val1";
			for(int j=2; j<valCount+2; j++) { valargs += ", " + type + " val"+j; valargs2 += ", val"+j; }
			code += type + " reduce_"+var.varName+"("+valargs+"){\n\treturn "+type+"ReduceGenerator("+valargs2+");\n}";
		}
		
		return code;
	}
	
	private static String generateMergeFunctions(String type, Set<Variable> sketchOutputVars) {
		String code = "";
		
		for(Variable var : sketchOutputVars){
			switch(type){
				case "int":
					code += type + " merge_"+var.varName+"(int val1, int val2){\n\treturn {| val1 | val1+val2 |};\n}\n";
					break;
				case "String":
					code += type + " merge_"+var.varName+"(int val1, int val2){\n\treturn {| val1 |};\n}\n";
					break;
				case "bit":
					code += type + " merge_"+var.varName+"(int val1, int val2){\n\tbit option1 = val1 || val2; int option2 = val1 && val2; return {| val1 | option1 | option2 |};\n}\n";
					break;
				case "bit[32]":
					code += type + " merge_"+var.varName+"(int val1, int val2){\n\treturn {| val1 |};\n}\n";
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
}
