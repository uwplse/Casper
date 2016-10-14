package casper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import casper.JavaLibModel.SketchCall;
import casper.extension.MyWhileExt;
import casper.extension.MyWhileExt.Variable;
import casper.types.ArrayUpdateNode;
import casper.types.ConditionalNode;
import casper.types.ConstantNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.visit.GenerateScaffold.SketchVariable;
import polyglot.ast.FieldDecl;

public class SketchCodeGenerator {
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

			for(FieldDecl fdecl : ext.globalDataTypesFields.get(vartype)){
				String fieldType = fdecl.type().toString();
				handleVarArgs(fieldType, category, ext, argsList, increment, isInputVar);
			}
		}
		else if(casper.Util.getTypeClass(vartypeOrig) == casper.Util.OBJECT_ARRAY){
			String vartype = vartypeOrig.substring(0, vartypeOrig.length()-("["+Configuration.arraySizeBound+"]").length());
			
			if(category != Variable.CONST_ARRAY_ACCESS){
				increment = increment * (Configuration.arraySizeBound-1);
			}
			
			for(FieldDecl fdecl : ext.globalDataTypesFields.get(vartype)){
				String fieldType = fdecl.type().toString();
				handleVarArgs(fieldType, category, ext, argsList, increment, isInputVar);
			}
		}
	}
		
	// Generate code that initializes main function args
	public static String generateMainFuncArgs(MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Map<String, Integer> argsList) {
		String mainFuncArgs = "";
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var)){
				if(!ext.initVals.containsKey(var.name) || !(ext.initVals.get(var.name) instanceof ConstantNode) || ((ConstantNode)ext.initVals.get(var.name)).type == ConstantNode.STRINGLIT){
					handleVarArgs(var.type,var.category,ext,argsList,1,true);
				}
			}
		}
		for(SketchVariable var : sketchOutputVars){
			if(!ext.initVals.containsKey(var.name) || !(ext.initVals.get(var.name) instanceof ConstantNode)){
				handleVarArgs(var.type,var.category,ext,argsList,2,false);
			}
			else{
				handleVarArgs(var.type,var.category,ext,argsList,1,false);
			}
		}
		for(SketchVariable var : sketchLoopCounters){
			if(!ext.initVals.containsKey(var.name)){
				handleVarArgs(var.type,var.category,ext,argsList,2,false);
			}
			else{
				handleVarArgs(var.type,var.category,ext,argsList,1,false);
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
		
	// Create and Initialize output variables
	public static String initMainOutput(MyWhileExt ext, Map<String, Integer> argsList, Set<SketchVariable> sketchOutputVars) {
		String ret = "";
		for(SketchVariable var : sketchOutputVars){
			if(ext.initVals.containsKey(var.name) && ext.initVals.get(var.name) instanceof ConstantNode){
				if(casper.Util.getTypeClass(var.type) == casper.Util.PRIMITIVE){
					ret += var.type + " "+ var.name + "0 = " + ext.initVals.get(var.name) + ";\n\t";
				}
				else if(casper.Util.getTypeClass(var.type) == casper.Util.ARRAY){
					ret += var.type + " "+ var.name + "0;\n\t";
					for(int i=0; i<Configuration.arraySizeBound; i++)
						ret += var.name + "0["+i+"] = " + ext.initVals.get(var.name) + ";\n\t";
				}
			}
			else{
				if(casper.Util.getTypeClass(var.type) == casper.Util.PRIMITIVE){
					ret += var.type + " "+ var.name + "0 = " + var.type + "Set[" + (argsList.get(var.type)-1) + "];\n\t";
					argsList.put(var.type, argsList.get(var.type) - 1 );
				}
				else if(casper.Util.getTypeClass(var.type) == casper.Util.ARRAY){
					String vartype = var.type.replace("["+Configuration.arraySizeBound+"]", "");
					ret += var.type + " "+ var.name + "0;\n\t";
					for(int i=0; i<Configuration.arraySizeBound; i++){
						ret += var.name + "0["+i+"] = " + vartype + "Set[" + (argsList.get(vartype)-1) + "];\n\t";
						argsList.put(vartype, argsList.get(vartype) - 1 );
					}
				}
			}
			
			if(casper.Util.getTypeClass(var.type) == casper.Util.PRIMITIVE){
				ret += var.type + " "+ var.name + " = " + var.type + "Set[" + (argsList.get(var.type)-1) + "];\n\t";
				argsList.put(var.type, argsList.get(var.type) - 1 );
			}
			else if(casper.Util.getTypeClass(var.type) == casper.Util.ARRAY){
				String vartype = var.type.replace("["+Configuration.arraySizeBound+"]", "");
				ret += var.type + " "+ var.name + ";\n\t";
				for(int i=0; i<Configuration.arraySizeBound; i++){
					ret += var.name + "["+i+"] = " + vartype + "Set[" + (argsList.get(vartype)-1) + "];\n\t";
					argsList.put(vartype, argsList.get(vartype) - 1 );
				}
			}
		}
		return ret.substring(0, ret.length()-2);
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
					for(FieldDecl fdecl : ext.globalDataTypesFields.get(vartype)){
						String fieldname = fdecl.id().toString();
						String fieldtype = fdecl.type().toString();
						inputInit += handleInputVarInit(fieldtype,varname+"."+fieldname,ext,argsList);
					}
				}
			}
		}
		
		return inputInit;
	}
		
	// Generate code that Initializes the input class using main function args
	public static String initMainInputVars(MyWhileExt ext, Map<String, Integer> argsList, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String inputInit = "";
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var)){
				// If the input variable is a constant index of an array, broadcast it
				if(var.category == MyWhileExt.Variable.CONST_ARRAY_ACCESS){
					inputInit += handleInputVarInit(var.type.replace("["+Configuration.arraySizeBound+"]", ""),var.name,ext,argsList);
				}
				// If the input variable is a field of an object, broadcast it
				else if(var.category == MyWhileExt.Variable.FIELD_ACCESS){
					inputInit += handleInputVarInit(var.type,var.name,ext,argsList);
				}
				// If the input variable is a primitive variable, broadcast it
				else if(var.category == MyWhileExt.Variable.VAR){
					inputInit += handleInputVarInit(var.type,var.name,ext,argsList);
				}
			}
		}
		for(int i=0; i<ext.constCount; i++){
			inputInit += "casperConst"+i+" = intSet["+(argsList.get("int")-1)+"];\n\t";
			argsList.put("int", argsList.get("int") - 1);
			
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
			for(FieldDecl fdecl : ext.globalDataTypesFields.get(vartype)){
				String fieldType = fdecl.type().toString();
				String fieldName = fdecl.name().toString();
				ret += handleInputDataInit(fieldType, varname+"."+fieldName,ext,argsList);
			}
		}
		
		return ret;
	}
		
	// Generate code that Initializes the input class using main function args
	public static String initMainInputData(MyWhileExt ext, Map<String, Integer> argsList, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String inputInit = "";
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var)){
				// If the input variable is a constant index of an array, broadcast it
				if(var.category == MyWhileExt.Variable.ARRAY_ACCESS){
					ext.hasInputData = true;
					ext.inputDataCollections.add(var);
					inputInit += var.type.replace(Configuration.arraySizeBound+"", (Configuration.arraySizeBound-1)+"") + " " + var.name + ";\n\t";
					String vartype = var.type.substring(0, var.type.length()-("["+Configuration.arraySizeBound+"]").length());
					
					for(int i=0; i<Configuration.arraySizeBound-1; i++){
						inputInit += handleInputDataInit(vartype,var.name+"["+i+"]",ext,argsList);
					}
					
					break;
				}
			}
		}
		
		// No collection accessed. Generate collection.
		if(ext.inputDataCollections.size() == 0){
			
			ext.inputDataCollections.add(new SketchVariable("input_data","int["+Configuration.arraySizeBound+"]",0));
			ext.hasInputData = false;
			
			SketchVariable lc = sketchLoopCounters.get(0);
			
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
		}
		// More than one collection accessed. Not sure what to do yet.
		else if(ext.inputDataCollections.size() > 1){
			System.err.println(">>>>>>>>>>>>>>> MULTIPLE COLLECTIONS ACCESSED <<<<<<<<<<<<<<");
		}
		
		return inputInit;
	}

	public static String initLoopCounters(MyWhileExt ext, Map<String, Integer> argsList, List<SketchVariable> sketchLoopCounters) {
		String ret = "";

		for(SketchVariable var : sketchLoopCounters){
			if(ext.initVals.containsKey(var.name)){
				ret += var.type + " "+ var.name + "0 = " + ext.initVals.get(var.name) + ";\n\t";
			}
			else{
				ret += var.type + " "+ var.name + "0 = " + var.type + "Set[" + (argsList.get(var.type)-1) + "];\n\t";
				argsList.put(var.type, argsList.get(var.type) - 1 );
			}
			ret += var.type + " "+ var.name + " = " + var.type + "Set[" + (argsList.get(var.type)-1) + "];\n\t";
			argsList.put(var.type, argsList.get(var.type) - 1 );
		}
		
		if(ret == "")
			return ret;
		
		return ret.substring(0, ret.length()-2);
	}
	
	// Generate code that declares the input broadcast variables using main function args
	public static String declBroadcastVars(MyWhileExt ext, Map<String, Integer> argsList, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String declBrdcstVars = "";
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var)){
				if(casper.Util.getTypeClass(var.type) == casper.Util.PRIMITIVE){
					declBrdcstVars += var.type + " " + var.name + ";\n";
				}
				else if(casper.Util.getTypeClass(var.type) == casper.Util.ARRAY){
					if(var.category == Variable.CONST_ARRAY_ACCESS)
						declBrdcstVars += var.type.replace("["+Configuration.arraySizeBound+"]", "") + " " + var.name + ";\n";
				}
				else if(casper.Util.getTypeClass(var.type) == casper.Util.OBJECT){
					if(var.type.endsWith("["+Configuration.arraySizeBound+"]")){
						if(var.category == Variable.CONST_ARRAY_ACCESS)
							declBrdcstVars += var.type.replace("["+Configuration.arraySizeBound+"]", "") + " " + var.name + ";\n";
					}
					else {
						declBrdcstVars += var.type + " " + var.name + " = new " + var.type + "();\n";
					}
				}
			}
		}
		for(int i=0; i<ext.constCount; i++){
			declBrdcstVars += "int casperConst" + i+";\n";
		}
		
		return declBrdcstVars;
	}
	
	public static String generateMapLoopIncr(List<SketchVariable> sketchLoopCounters, List<CustomASTNode> incrementExps) {
		String incr = "";
		for(int i=0; i<sketchLoopCounters.size(); i++){
			SketchVariable var = sketchLoopCounters.get(i);
			incr += "_" + var.name + " = " + incrementExps.get(i).replaceAll(var.name, new IdentifierNode("_"+var.name)) + ";\n\t";
		}
		return incr;
	}

	public static String generateMapLoopCond(List<SketchVariable> sketchLoopCounters, CustomASTNode tCond, boolean hasInputData) {	
		if(!hasInputData || true){
			return "(_"+sketchLoopCounters.get(0).name+"<"+sketchLoopCounters.get(0).name+") && " + "(_"+sketchLoopCounters.get(0).name+"<"+(Configuration.arraySizeBound-1)+")";
		}	

		CustomASTNode mlcond = tCond;
		for(SketchVariable var : sketchLoopCounters){
			mlcond = mlcond.replaceAll(var.name, new IdentifierNode("_"+var.name));
		}
		return mlcond.toString();
	}

	public static String generateMapLoopInit(List<SketchVariable> sketchLoopCounters, CustomASTNode tCond) {
		String init = "";
		for(SketchVariable var : sketchLoopCounters){
			if(true || tCond.contains(var.name)){
				init += var.type + " _" + var.name + " = " + var.name + "0;\n\t";
			}
		}
		return init;
	}
	
	public static String generateDomapFuncArgsCall(MyWhileExt ext, List<SketchVariable> sketchLoopCounters) {
		String args = ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			args += ", _" + var.name + ",";
		}
		args = args.substring(0,args.length()-1);
		return args;
	}
	
	public static String generateDomapFuncArgsDecl(MyWhileExt ext, List<SketchVariable> sketchLoopCounters) {
		String args = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.type + " " + var.name + ",";
		}
		args = args.substring(0,args.length()-1);
		return args;
	}
	
	public static String generateDomapEmits(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, String outputType) {
		String emits = "";
		
		// Generate args for generator functions
		String args = ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.name + ", ";
		}
		args = args.substring(0, args.length()-2);
		
		// TODO: Determine type of key to be used.
		String intkey2decl = "";
		String stringkey2decl = "";
		String valType = outputType;
		if(valType.contains("["+Configuration.arraySizeBound+"]")){
			valType = valType.replace("["+Configuration.arraySizeBound+"]", "");
			intkey2decl = "kvp.intkey2 = intMapGenerator("+args+");\n\t\t";
			stringkey2decl = "kvp.intkey = intMapGenerator("+args+");\n\t\t";
		}
		
		// Include conditionals?
		if(Configuration.useConditionals){
			// Generate emit code
			for(int i=0; i<Configuration.emitCount; i++){
				emits += 	"if(bitMapGenerator("+args+")){\n\t\t" +
								"if(mapKeyType == 0){\n\t\t\t" +
									"Pair kvp = new Pair();\n\t\t\t" +
									"kvp.intkey = intMapGenerator("+args+");\n\t\t\t" +
									intkey2decl + "\t" +
									"kvp.value = "+valType+"MapGenerator("+args+");\n\t\t\t" +
									"intlist_put(result, kvp);\n\t\t" +
								"}\n\t\t" +
								"else if(mapKeyType == 1){\n\t\t\t" +
									"Pair kvp = new Pair();\n\t\t\t" +
									stringkey2decl + "\t" +
									"kvp.stringkey = stringMapGenerator("+args+");\n\t\t\t" +
									"kvp.value = "+valType+"MapGenerator("+args+");\n\t\t\t" +
									"stringlist_put(result, kvp);\n\t\t" +
								"}\n\t" +
							"}\n\t";
			}
		}
		else{
			// Generate emit code
			for(int i=0; i<Configuration.emitCount; i++){
				emits += 	"if(mapKeyType == 0){\n\t\t" +
								"Pair kvp = new Pair();\n\t\t" +
								"kvp.intkey = intMapGenerator("+args+");\n\t\t" +
								intkey2decl +
								"kvp.value = "+valType+"MapGenerator("+args+");\n\t\t" +
								"intlist_put(result, kvp);\n\t" +
							"}\n\t" +
							"else if(mapKeyType == 1){\n\t\t" +
								"Pair kvp = new Pair();\n\t\t" +
								stringkey2decl +
								"kvp.stringkey = stringMapGenerator("+args+");\n\t\t" +
								"kvp.value = "+valType+"MapGenerator("+args+");\n\t\t" +
								"stringlist_put(result, kvp);\n\t" +
							"}\n\t";
			}
		}
		
		return emits;
	}

	public static String generateCollectArgsDecl(String outputType, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		for(SketchVariable var : sketchOutputVars){
			if(var.type.equals(outputType))
				args += " " + var.type + " " + var.name + "0,";
		}
		args = args.substring(0,args.length()-1);
		return args;
	}

	public static String generateLoopInvariant(String outputType, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> paramList, boolean hasInputData) {
		String inv = "return ";
		boolean first = true;
		
		for(SketchVariable var : sketchOutputVars){
			String prefix = "";
			if(!first){
				prefix = " && ";
				first = false;
			}
			if(var.type.equals(outputType)){
				if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE || casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
					inv += prefix + "resMR." + var.name + " == " + var.name + " && ";
				}
				//else if(javatosketch.Util.getTypeClass(outputType) == 3){
					// inv += prefix + outputType.substring(0,6).toLowerCase() + "_map_equal(resMR." + var.name + ", " + var.name + ")";
				//}	
			}
			else if(var.type.equals(outputType + "["+Configuration.arraySizeBound+"]")){
				if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE || casper.Util.getTypeClass(outputType) == casper.Util.ARITHMETIC_OP){
					inv += prefix + "resMR." + var.name + " == " + var.name + " && ";
				}
				//else if(javatosketch.Util.isBasicType(outputType) == 3){
					// TODO: Lets not worry about this right now. Maybe later.
					// postCond += "for(int i=0; i<"+Configuration.arraySizeBound +"; i++) assert "+outputType.substring(0,6).toLowerCase()+"_map_equal(resSeq." + var.name + ", resMR." + var.name + ");\n\t";
				//}
			}
		}
		
		SketchVariable lc = sketchLoopCounters.get(0);
		inv += "0 <= " + lc.name + " && " + lc.name + " <= " + (Configuration.arraySizeBound-1);
		
		return inv += ";";
	}

	// Generate post condition function args
	public static String generatePostConditionArgsDecl(MyWhileExt ext, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> postConditionArgsOrder) {
		String pcArgs = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		
		for(String nextVarName : postConditionArgsOrder){
			for(SketchVariable var : sketchOutputVars){
				if(nextVarName.equals(var.name)){
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "") + "0";
				}
			}
			for(SketchVariable var : sketchLoopCounters){
				if(nextVarName.equals(var.name)){
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "") + "0";
				}
			}
		}
		
		return pcArgs;
	}
	
	public static String generateMapArgsCall(MyWhileExt ext, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> postConditionArgsOrder) {
		String pcArgs = ext.inputDataCollections.get(0).name;
		
		for(SketchVariable var : sketchLoopCounters){
			pcArgs += "," + var.name + "0";
			pcArgs += "," + var.name;
		}
		
		return pcArgs;
	}
	
	public static String generateMapArgsDecl(MyWhileExt ext, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> postConditionArgsOrder) {
		String pcArgs = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		
		for(SketchVariable var : sketchLoopCounters){
			pcArgs += ", " + var.type + " " + var.name + "0";
			pcArgs += ", " + var.type + " " + var.name;
		}
		
		return pcArgs;
	}
	
	// Generate loop invariant function args
	public static String generateLoopInvariantArgsDecl(MyWhileExt ext, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> loopInvariantArgsOrder) {
		String pcArgs = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		
		for(String nextVarName : loopInvariantArgsOrder){
			for(SketchVariable var : sketchOutputVars){
				if(nextVarName.equals(var.name)){
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "") + "0";
				}
			}
			for(SketchVariable var : sketchLoopCounters){
				if(nextVarName.equals(var.name)){
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "");
					pcArgs += ", " + var.type + " " + nextVarName.replace(".", "") + "0";
				}
			}
		}
		
		return pcArgs;
	}
	
	// Generate post condition function body
	public static String generatePostCondition(String outputType, Set<SketchVariable> sketchOutputVars) {
		String postCond = "return ";
		boolean first = true;
		
		for(SketchVariable var : sketchOutputVars){
			String prefix = "";
			if(!first){
				prefix = " && ";
				first = false;
			}
			if(var.type.equals(outputType)){
				if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE || casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
					postCond += prefix + "resMR." + var.name + " == " + var.name + " && ";
				}
			//	else if(javatosketch.Util.getTypeClass(outputType) == 3){
			//		postCond += prefix + outputType.substring(0,6).toLowerCase() + "_map_equal(resMR." + var.name + ", " + var.name + ")";
			//	}	
			}
			else if(var.type.equals(outputType + "["+Configuration.arraySizeBound+"]")){
				if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE || casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
					postCond += prefix + "resMR." + var.name + " == " + var.name + " && ";
				}
			//	else if(javatosketch.Util.isBasicType(outputType) == 3){
					// TODO: Lets not worry about this right now. Maybe later.
					// postCond += "for(int i=0; i<"+Configuration.arraySizeBound +"; i++) assert "+outputType.substring(0,6).toLowerCase()+"_map_equal(resSeq." + var.name + ", resMR." + var.name + ");\n\t";
			//	}
			}
		}
		
		postCond = postCond.substring(0, postCond.length()-4);
		
		postCond += ";";
		
		return postCond;
	}
	
	public static String reconstructOutput(String outputType, Set<SketchVariable> sketchOutputVars) {
		String outputRecon = "";
		
		outputRecon += "if(mapKeyType == 0){\n\t\t";
		if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					outputRecon += "output." + var.name + " = int_get(resMR,"+index+","+ var.name +"0);\n\t\t";
					index++;
				}
			}
		}
		else if(casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					for(int i=0; i<Configuration.arraySizeBound; i++){
						outputRecon += "output." + var.name + "["+i+"] = int_get_tuple(resMR,"+index+","+i+","+ var.name +"0["+i+"]);\n\t\t";
					}
					index++;
				}
			}
		}
		//else if(javatosketch.Util.isBasicType(outputType) == 3){
			// For now hardcoded for wordcount example
			// outputRecon += "ListNode ptr = resMR.handle;\n\twhile(ptr != null){\n\t\tintint_map_put(ptr.intkey, ptr.value, output.counts);\n\t}";
		//}
		outputRecon = outputRecon.substring(0,outputRecon.length()-1) + "}\n\telse if(mapKeyType == 1){\n\t\t";
		if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					outputRecon += "output." + var.name + " = string_get(resMR,"+index+","+ var.name +"0);\n\t\t";
					index++;
				}
			}
		}
		else if(casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					for(int i=0; i<Configuration.arraySizeBound; i++){
						outputRecon += "output." + var.name + "["+i+"] = string_get_tuple(resMR,"+index+","+i+","+ var.name +"0["+i+"]);\n\t\t";
					}
					index++;
				}
			}
		}
		//else if(javatosketch.Util.isBasicType(outputType) == 3){
			// For now hardcoded for wordcount example
			// outputRecon += "ListNode ptr = resMR.handle;\n\twhile(ptr != null){\n\t\tintint_map_put(ptr.intkey, ptr.value, output.counts);\n\t}";
		//}
		
		return outputRecon.substring(0,outputRecon.length()-1) + "}";
	}

	// Generate do map grammars
	public static String generateMapGrammarInlined(String type, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, MyWhileExt ext) {
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
			if(ext.outputVars.contains(var))
				continue;
			
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				terminals.get(type).add(var.varName);
			}
			else if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 2){
				if(!ext.inputDataCollections.contains(var) || true){
					for(SketchVariable lc : sketchLoopCounters){
						terminals.get(type).add(var.varName + "["+lc.name+"]");
					}
				}
			}
			else{
				for(String globalVar : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalVar.equals(var.getOriginalType())){
						// Add an option for each field that matches type
	        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
	        				if(casper.Util.compatibleTypes(type,field.type().toString()) == 1){
	        					terminals.get(type).add(var.varName + "." + field.id().toString());
	        				}
	        			}
	        		}
					// If it is an array of one of the global data types
	        		else if(globalVar.equals(var.getOriginalType().replace("[]", ""))){
	        			if(!ext.inputDataCollections.contains(var) || true){
		        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
		        				// Add an option for each field (of an arbitrary array index) that matches type
		        				if(casper.Util.compatibleTypes(type,field.type().toString()) == 1){
		        					for(SketchVariable lc : sketchLoopCounters){
		        						terminals.get(type).add(var.varName + "["+lc.name+"]." + field.id().toString());
		        					}
		        				}
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
		
		String argsDecl = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			argsDecl += ", " + var.type + " " + var.name;
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
					String args = ext.inputDataCollections.get(0).name;
					for(SketchVariable var : sketchLoopCounters){
						argsDecl += ", " + var.name;
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
		generator += "generator "+sketchType+" "+typeName+"MapGenerator("+argsDecl+"){\n\t" + terminalsCode + expressions + "\n}";
		
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
			
			/*if(constants.containsKey(type)){
				String typeName = type.toLowerCase();
				if(typeName == "bit[32]")
					typeName = "bitInt";
				
				
				for(int i=terminals.size(); i<terminals.size()+constants.get(type); i++){
					exprs.add("_"+typeName+"_terminal"+i);
				}
			}*/
			exprs.add("<"+type+"-term>");
			return;
		}
		else{
			/*if(terminals.containsKey(type)){
				for(String terminal : terminals.get(type))
					exprs.add(terminal);
			}
			
			if(constants.containsKey(type)){
				String typeName = type.toLowerCase();
				if(typeName == "bit[32]")
					typeName = "bitInt";
				
				for(int i=terminals.size(); i<terminals.size()+constants.get(type); i++){
					exprs.add("_"+typeName+"_terminal"+i);
				}
			}*/
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
							//for(SketchVariable var : sketchLoopCounters){
							//	args += ", " + var.name + ", ";
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
	
	// Generate do reduce emits
	public static String generateDoreduceEmits(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, int size, String outputType) {
		String type = outputType.replace("["+Configuration.arraySizeBound+"]", "");
		
		String initOpts = "";
		switch(type){
		case "bit":
			initOpts = "true | false";
			break;
		case "int":
		default:
			initOpts = "0 | 1";
			break;
		}
		
		// Generate emit code
		String emits = 	"p.value = {| " + initOpts + " |};\n\t" +
						"while(values != null){\n\t\t" +
							"p.value = "+type+"ReduceGenerator(p.value, values.value);\n\t\t" +
							"values = values.next;\n\t" +
						"}";
				
		return emits;
	}
	
	// Generate do reduce grammars
	public static String generateReduceGrammarInlined(MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, String outputType) {
		String generator = "";
		
		String type = outputType.replace("["+Configuration.arraySizeBound+"]", "");
		
		// Terminal options
		String terminalOpts = "val1 | val2";
		/*switch(outputType){
			case "int":
				terminalOpts += " | ??";
				break;
			case "bit":
				terminalOpts += " | true | false";
				break;
		}*/
		if(!outputType.equals("bit")){
			for(SketchVariable var : sketchInputVars){
				if(sketchOutputVars.contains(var))
					continue;
				
				if(var.type.equals(type)){
					terminalOpts += " | " + var.name;
				}
				else if(var.type.equals(type+"["+Configuration.arraySizeBound+"]")){
					if(!ext.inputDataCollections.contains(var)){
						for(SketchVariable lc : sketchLoopCounters){
							terminalOpts += " | " + var.name + "["+lc.name+"]";
						}
					}
				}
				else{
					for(String globalVar : ext.globalDataTypes){
						// If it is one of the global data types
						if(globalVar.equals(var.type)){
							// Add an option for each field that matches type
		        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
		        				if(field.type().toString().equals(type)){
		        					terminalOpts += " | " + var.name + "." + field.id().toString();
		        				}
		        			}
		        		}
					}
				}
			}
			for(int i=0; i<ext.constCount; i++){
				if(casper.Util.compatibleTypes(type,"int") == 1){
					terminalOpts += " | casperConst" + i;
				}
			}
		}
		
		// Terminals
		String terminals = "";
		int termC = Configuration.recursionDepth;
		if(termC > 2) Math.pow(termC-1,2);
		for(int i=0; i<termC; i++){
			terminals += type+" _terminal"+(i)+" = {| " + terminalOpts + " |};\n\t";
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,"<-expr->",Configuration.recursionDepth);
		String expressions = "";
		String opts = "";
		int id = 0;
		for(String expr : exprs){
			int index = 0;
			while(expr.contains("<-expr->")){
				int st_index = expr.indexOf("<-expr->");
				int end_index = st_index + 8;
				expr = expr.substring(0, st_index) + "_terminal"+ index++ + expr.substring(end_index, expr.length());
			}
			
			expressions += type+" _option"+id+" = " + expr + ";\n\t";
			opts += "_option"+id+ " | ";
			id++;
		}
		if(!opts.equals("")) opts = opts.substring(0, opts.length()-3);
		
		if(outputType.equals("bit")){
			opts = "true | false | " + opts;
		}
		
		// Generator code		
		generator += "generator "+type+" "+type+"ReduceGenerator("+type+" val1, "+type+" val2){\n\t" + terminals + expressions + "return {| " + opts + " |};\n}";
		
		return generator;
	}
	
	private static void getReduceExpressions(String type, MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> exprs, String exprString, int depth) {
		if(depth == 0){
			return;
		}
		if(depth == 1){
			exprs.add(exprString);
			return;
		}
		else{
			exprs.add(exprString);
			
			for(String op : ext.binaryOperators){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					if(type.equals("bit")){
						if(casper.Util.operandTypes(op) == casper.Util.BIT_ONLY){
							if(casper.Util.isAssociative(op)){
								List<String> subExprs = new ArrayList<String>();
								getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
											continue;
										if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
											continue;
										exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
									}
								}
							}
							else {
								List<String> subExprs = new ArrayList<String>();
								getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
									}
								}
							}
						}
						
					}
					else if(type.equals("bit[32]")){
						if(casper.Util.operandTypes(op) == casper.Util.VEC_ONLY){
							if(casper.Util.isAssociative(op)){
								List<String> subExprs = new ArrayList<String>();
								getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
											continue;
										if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
											continue;
										exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
									}
								}
							}
							else {
								List<String> subExprs = new ArrayList<String>();
								getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
								for(String exprLeft : subExprs){
									for(String exprRight : subExprs){
										exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
									}
								}
							}
						}
						else if(casper.Util.operandTypes(op) == casper.Util.VEC_INT){
							List<String> lhsSubExprs = new ArrayList<String>();
							List<String> rhsSubExprs = new ArrayList<String>();
							getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,lhsSubExprs,"<-expr->",depth-1);
							getReduceExpressions("int",ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,rhsSubExprs,"<-expr->",depth-1);
							for(String exprLeft : lhsSubExprs){
								for(String exprRight : rhsSubExprs){
									exprRight = exprRight.replace("<-expr->", "intTerminals()");
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
								}
							}
						}
					}
					else{
						if(casper.Util.isAssociative(op)){
							List<String> subExprs = new ArrayList<String>();
							getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									if(exprs.contains("("+exprLeft + " " + op + " " + exprRight + ")"))
										continue;
									if(exprs.contains("("+exprRight + " " + op + " " + exprLeft + ")"))
										continue;
									exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
								}
							}
						}
						else{
							List<String> subExprs = new ArrayList<String>();
							getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
							for(String exprLeft : subExprs){
								for(String exprRight : subExprs){
									exprs.add(exprString.replace("<-expr->", "("+exprLeft + " " + op + " " + exprRight + ")"));
								}
							}
						}
					}
				}
			}
			for(String op : ext.unaryOperators){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					String newExprString = exprString.replace("<-expr->", op + "(<-expr->)");
					getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1);
				}
			}
			for(SketchCall op : ext.methodOperators){
				if(casper.Util.getOpClassForType(casper.Util.getSketchTypeFromRaw(op.returnType)) == casper.Util.getOpClassForType(type)){
					String call = op.name+"(";
					for(String argType : op.args){
						if(casper.Util.getSketchTypeFromRaw(argType).equals(type)){
							call += "<-expr->,";
						}
						else{
							// Generate args for generator functions
							String args = ext.inputDataCollections.get(0).name;
							for(SketchVariable var : sketchLoopCounters){
								args += ", " + var.name + ", ";
							}
							args = args.substring(0, args.length()-2);
							if(argType.equals("java.lang.String"))
								call += "stringMapGenerator("+args+"),";
							else
								call += casper.Util.getSketchTypeFromRaw(argType).toLowerCase()+"MapGenerator("+args+"),";
						}
						 
					}
					call = call.substring(0,call.length()-1) + ")";
					String newExprString = exprString.replace("<-expr->", call);	
					List<String> subExprs = new ArrayList<String>();
					getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subExprs,"<-expr->",depth-1);
					List<String> newExprs = new ArrayList<String>();
					fillExpressions(exprs,subExprs,newExprs,newExprString,0);
					exprs.addAll(newExprs);
				}
			}
			
			return;
		}
	}
	
	// Generate code that includes all necessary files
	public static String generateIncludeList(MyWhileExt ext, String outputType, int id) {
		String includeList = "";
		
		for(String dataType : ext.globalDataTypes){
			includeList += "include \"output/" + dataType + ".sk\";\n";
		}
		includeList += "include \"output/output"+id+".sk\";\n";
		if(outputType.endsWith("["+Configuration.arraySizeBound+"]")){
			includeList += "include \"output/kvpair_list_"+outputType.replace("["+Configuration.arraySizeBound+"]", "")+id+".sk\";\n";
		}
		else{
			includeList += "include \"output/kvpair_list_"+outputType+id+".sk\";\n";
		}
		
		return includeList;
	}

	public static String generateWPCValuesInit(Map<String,CustomASTNode> wpcValues, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String code = "";
		for(String varname : wpcValues.keySet())
		{
			boolean found = false;
			for(SketchVariable var : sketchOutputVars){
				if(var.name.equals(varname)){
					code += var.type + " ind_" + varname + " = " + varname + ";\n\t\t";
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
			for(SketchVariable var : sketchLoopCounters){
				if(var.name.equals(varname)){
					code += var.type + " ind_" + varname + " = " + varname + ";\n\t\t";
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
}
