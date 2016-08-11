package casper;

import java.util.ArrayList;
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
				if(!ext.initVals.containsKey(var.name) || !(ext.initVals.get(var.name) instanceof ConstantNode)){
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
				inputInit += varname + " = " + ext.initVals.get(varname) +";\n\t";
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
			if(tCond.contains(var.name)){
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
				emits += 	"if(condMapGenerator(input, i)){\n\t\t" +
								"if(mapKeyType == 0){\n\t\t\t" +
									"Pair kvp = new Pair();\n\t\t\t" +
									"kvp.intkey = intMapGenerator("+args+");\n\t\t\t" +
									intkey2decl +
									"kvp.value = "+valType+"MapGenerator("+args+");\n\t\t\t" +
									"intlist_put(result, kvp);\n\t\t" +
								"}\n\t\t" +
								"else if(mapKeyType == 1){\n\t\t\t" +
									"Pair kvp = new Pair();\n\t\t\t" +
									stringkey2decl +
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
	public static String generateIntGrammar(List<SketchVariable> sketchLoopCounters, MyWhileExt ext) {
		String intGenerator = "";
		
		// Generate args decl
		String argsDecl = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			argsDecl += ", " + var.type + " " + var.name;
		}
		
		// Generate args call
		String argscall = ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			argscall += ", " + var.name;
		}
		
		// Generate int map terminal args
		String mapIntTermArgs = "";
		for(SketchVariable var : sketchLoopCounters){
			mapIntTermArgs += var.name + ", ";
		}
		mapIntTermArgs = mapIntTermArgs.substring(0, mapIntTermArgs.length()-2);
		
		// Generate grammar options
		String opts = "";
		if(ext.inputDataCollections.get(0).type.equals("int["+Configuration.arraySizeBound+"]")){
			opts = " | " + ext.inputDataCollections.get(0).name+"[terminal2]"; 
		}
		else if(casper.Util.getTypeClass(ext.inputDataCollections.get(0).type) == casper.Util.OBJECT_ARRAY){
			String objectType = ext.inputDataCollections.get(0).type.replace("["+Configuration.arraySizeBound+"]", "");
			for(FieldDecl field : ext.globalDataTypesFields.get(objectType)){
				if(field.type().toString().equals("int")){
					opts += " | " + ext.inputDataCollections.get(0).name+"[terminal2]."+field.name();
				}
			}
		}
		
		for(String op : ext.binaryOperators){
			if(casper.Util.operatorType(op) == casper.Util.ARITHMETIC_OP){
				opts += " | terminal2 " + op + " terminal3";
			}
		}
		for(String op : ext.unaryOperators){
			if(casper.Util.operatorType(op) == casper.Util.ARITHMETIC_OP){
				opts += " | " + op + "terminal3";
			}
		}
		
		intGenerator +=	"generator int intMapGenerator("+argsDecl+", int depth){\n\t" +
							"if(depth == 1){\n\t\t" +
								"return intMapTerminals("+mapIntTermArgs+");\n\t" +
							"}\n\t" +
							"else{\n\t\t" +
								"int terminal1 = intMapTerminals("+mapIntTermArgs+");\n\t\t" +
								"int terminal2 = intMapGenerator("+argscall+", depth-1);\n\t\t" +
								"int terminal3 = intMapGenerator("+argscall+", depth-1);\n\t\t" +
								"return {| terminal1" + opts + " |};\n\t" +
							"}\n" +
						"}";
		
		return intGenerator;
	}

	// Generate do map grammars
	public static String generateMapGrammarInlined(String type, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, MyWhileExt ext) {
		String generator = "";
		
		// Generate args decl
		String argsDecl = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			argsDecl += ", " + var.type + " " + var.name;
		}
		
		// Terminal options
		String terminalOpts = "";
		
		for(Variable var : ext.loopCounters){
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				terminalOpts += " | " + var.varName;
			}
		}
		for(Variable var : ext.inputVars){
			if(ext.outputVars.contains(var))
				continue;
			
			if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 1){
				terminalOpts += " | " + var.varName;
			}
			else if(casper.Util.compatibleTypes(type,var.getOriginalType()) == 2){
				if(!ext.inputDataCollections.contains(var) || true){
					for(SketchVariable lc : sketchLoopCounters){
						terminalOpts += " | " + var.varName + "["+lc.name+"]";
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
	        					terminalOpts += " | " + var.varName + "." + field.id().toString();
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
		        						terminalOpts += " | " + var.varName + "["+lc.name+"]." + field.id().toString();
		        					}
		        				}
		        			}
	        			}
	        		}
				}
			}
		}
		
		switch(type){
			case "int":
				terminalOpts = "??" + terminalOpts;
				break;
			case "bit":
				terminalOpts = "true | false" + terminalOpts;
				break;
			case "String":
				if(terminalOpts == "")
					terminalOpts = "-1";
				else
					terminalOpts = terminalOpts.substring(3,terminalOpts.length());
				break;
		}
		
		String sketchType = type;
		if(type == "String")
			sketchType = "int";
		
		// Terminals
		String terminals = "";
		for(int i=0; i<Configuration.recursionDepth; i++){
			terminals += sketchType+" _terminal"+(i+1)+" = {| " + terminalOpts + " |};\n\t";
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,"<-expr->",Configuration.recursionDepth,1);
		String expressions = "";
		String opts = "";
		int id = 0;
		for(String expr : exprs){
			expressions += sketchType+" _option"+id+" = " + expr + ";\n\t";
			opts += "_option"+id+ " | ";
			id++;
		}
		opts = opts.substring(0, opts.length()-3);
		
		// Generator code		
		generator +=	"generator "+sketchType+" "+type.toLowerCase()+"MapGenerator("+argsDecl+"){\n\t" + terminals + expressions + "return {| " + opts + " |};\n}";
		
		return generator;
	}
	
	private static void getMapExpressions(String type, MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> exprs, String exprString, int depth, int terminalid) {
		if(depth == 0){
			return;
		}
		if(depth == 1){
			exprs.add(exprString.replace("<-expr->","_terminal"+terminalid));
			return;
		}
		else{
			exprs.add(exprString.replace("<-expr->","_terminal"+terminalid));
			
			/*if(ext.inputDataCollections.get(0).type.equals(type+"["+Configuration.arraySizeBound+"]")){
				String newExprString = exprString.replace("<-expr->", ext.inputDataCollections.get(0).name+"[<-expr->]");
				getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid);
				
				for(String op : ext.binaryOperators){
					if(javatosketch.Util.operatorType(op) == javatosketch.Util.getOpClassForType(type)){
						List<String> subexprs = new ArrayList<String>();
						getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subexprs,"<-expr->",depth/2,terminalid);
						List<String> handled = new ArrayList<String>();
						for(String expr1 : subexprs){
							if(!handled.contains(expr1)){
								handled.add(expr1);
								newExprString = exprString.replace("<-expr->", ext.inputDataCollections.get(0).name+"["+expr1+"]" + " " + op + " <-expr->");
								getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid+(depth/2));
							}
						}
					}
				}
				for(String op : ext.unaryOperators){
					if(javatosketch.Util.operatorType(op) == javatosketch.Util.getOpClassForType(type)){
						newExprString = exprString.replace("<-expr->", op + "("+ext.inputDataCollections.get(0).name+"[<-expr->])");
						getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid);
					}
				}
			}
			else if(javatosketch.Util.getTypeClass(ext.inputDataCollections.get(0).type) == javatosketch.Util.OBJECT_ARRAY){
				String objectType = ext.inputDataCollections.get(0).type.replace("["+Configuration.arraySizeBound+"]", "");
				for(FieldDecl field : ext.globalDataTypesFields.get(objectType)){
					if(field.type().toString().equals(type)){
						String newExprString = exprString.replace("<-expr->", ext.inputDataCollections.get(0).name+"[<-expr->]."+field.name());
						getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid);
						
						for(String op : ext.binaryOperators){
							if(javatosketch.Util.operatorType(op) == javatosketch.Util.getOpClassForType(type)){
								List<String> subexprs = new ArrayList<String>();
								getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,subexprs,"<-expr->",depth/2,terminalid);
								List<String> handled = new ArrayList<String>();
								for(String expr1 : subexprs){
									if(!handled.contains(expr1)){
										handled.add(expr1);
										newExprString = exprString.replace("<-expr->", ext.inputDataCollections.get(0).name+"["+expr1+"]."+field.name() + " " + op + " <-expr->");
										getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid+(depth/2));
									}
								}
							}
						}
						for(String op : ext.unaryOperators){
							if(javatosketch.Util.operatorType(op) == javatosketch.Util.getOpClassForType(type)){
								newExprString = exprString.replace("<-expr->", op + "("+ext.inputDataCollections.get(0).name+"[<-expr->]."+field.name()+")");
								getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid);
							}
						}
					}
				}
			}*/
			
			for(String op : ext.binaryOperators){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					String newExprString = exprString.replace("<-expr->", "_terminal" + terminalid + " " + op + " <-expr->");
					getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid+1);
				}
			}
			for(String op : ext.unaryOperators){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					String newExprString = exprString.replace("<-expr->", op + "(<-expr->)");
					getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid);
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
					getMapExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1,terminalid);
				}
			}
			
			return;
		}
	}

	public static String generateIntTerminals(Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, MyWhileExt ext){
		String intTerms = "";
		
		// Generate args decl
		String argsDecl = "";
		for(SketchVariable var : sketchLoopCounters){
			argsDecl += var.type + " " + var.name + ", ";
		}
		argsDecl = argsDecl.substring(0, argsDecl.length()-2);
		
		intTerms +=	"generator int intMapTerminals("+argsDecl+"){\n\t";
		intTerms += "int option0 = ??;\n\t"; 
		
		int varid = 1;
		for(SketchVariable var : sketchInputVars){
			if(sketchOutputVars.contains(var))
				continue;
			
			if(var.type.equals("int")){
				intTerms += "int option" + varid++ + " = " + var.name + ";\n\t";
			}
			else if(var.type.equals("int["+Configuration.arraySizeBound+"]")){
				if(!ext.inputDataCollections.contains(var)){
					for(SketchVariable lc : sketchLoopCounters){
						intTerms += "int option" + varid++ + " = " + var.name + "["+lc.name+"]" + ";\n\t";
					}
				}
			}
			else{
				for(String globalVar : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalVar.equals(var.type)){
						// Add an option for each field that matches type
	        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
	        				if(field.type().toString().equals("int")){
	        					intTerms += "int option" + varid++ + " = " + var.name + "." + field.id().toString() + ";\n\t";
	        				}
	        			}
	        		}
					// If it is an array of one of the global data types
	        		else if(globalVar.equals(var.type.replace("["+Configuration.arraySizeBound+"]", ""))){
	        			if(!ext.inputDataCollections.contains(var)){
		        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
		        				// Add an option for each field (of an arbitrary array index) that matches type
		        				if(field.type().toString().equals("int")){
		        					for(SketchVariable lc : sketchLoopCounters){
		        						intTerms += "int option" + varid++ + " = " + var.name + "["+lc.name+"]." + field.id().toString() + ";\n\t";
		        					}
		        				}
		        			}
	        			}
	        		}
				}
			}
		}
		for(SketchVariable var : sketchLoopCounters){
			if(var.type.equals("int")){
				intTerms += "int option" + varid++ + " = " + var.name + ";\n\t";
			}
		}
		
		String opts = "";
		for(int i=0; i<varid; i++){
			opts += "option" + i + " | ";
		}
		opts = opts.substring(0,opts.length()-3);
		
		intTerms += "return {| "+opts+" |};\n}\n";
		
		return intTerms;
	}

	public static String generateBitGrammar(List<SketchVariable> sketchLoopCounters, MyWhileExt ext) {
		String bitGenerator = "";
		
		// Generate args decl
		String argsDecl = ext.inputDataCollections.get(0).type.replace(""+Configuration.arraySizeBound, ""+(Configuration.arraySizeBound-1)) + " " + ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			argsDecl += ", " + var.type + " " + var.name;
		}
		
		// Generate args call
		String argscall = ext.inputDataCollections.get(0).name;
		for(SketchVariable var : sketchLoopCounters){
			argscall += ", " + var.name;
		}
		
		// Generate bit map terminal args
		String mapBitTermArgs = "";
		for(SketchVariable var : sketchLoopCounters){
			mapBitTermArgs += var.name + ", ";
		}
		mapBitTermArgs = mapBitTermArgs.substring(0, mapBitTermArgs.length()-2);
		
		// Generate grammar options
		String opts = "";
		if(ext.inputDataCollections.get(0).type.equals("bit["+Configuration.arraySizeBound+"]")){
			opts = " | " + ext.inputDataCollections.get(0).name+"[terminal2]"; 
		}
		for(String op : ext.binaryOperators){
			if(casper.Util.operatorType(op) == casper.Util.RELATIONAL_OP){
				opts += " | terminal2 " + op + " terminal3";
			}
		}
		for(String op : ext.unaryOperators){
			if(casper.Util.operatorType(op) == casper.Util.RELATIONAL_OP){
				opts += " | " + op + "terminal3";
			}
		}
		
		bitGenerator +=	"generator bit bitMapGenerator("+argsDecl+", int depth){\n\t" +
							"if(depth == 1){\n\t\t" +
								"return bitMapTerminals("+mapBitTermArgs+");\n\t" +
							"}\n\t" +
							"else{\n\t\t" +
								"bit terminal1 = bitMapTerminals("+mapBitTermArgs+");\n\t\t" +
								"bit terminal2 = bitMapGenerator("+argscall+", depth-1);\n\t\t" +
								"bit terminal3 = bitMapGenerator("+argscall+", depth-1);\n\t\t" +
								"return {| terminal1" + opts + " |};\n\t" +
							"}\n" +
						"}";
		
		return bitGenerator;
	}
	
	public static String generateBitTerminals(Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, MyWhileExt ext){
		String bitTerms = "";
		
		// Generate args decl
		String argsDecl = "";
		for(SketchVariable var : sketchLoopCounters){
			argsDecl += var.type + " " + var.name + ", ";
		}
		argsDecl = argsDecl.substring(0, argsDecl.length()-2);
		
		bitTerms +=	"generator bit bitMapTerminals("+argsDecl+"){\n\t";
		bitTerms += "bit option0 = true;\n\t";
		bitTerms += "bit option1 = false;\n\t";
		
		int varid = 2;
		for(SketchVariable var : sketchInputVars){
			if(sketchOutputVars.contains(var))
				continue;
			
			if(var.type.equals("bit")){
				bitTerms += "bit option" + varid++ + " = " + var.name + ";\n\t";
			}
			else if(var.type.equals("bit["+Configuration.arraySizeBound+"]")){
				if(!ext.inputDataCollections.contains(var)){
					for(SketchVariable lc : sketchLoopCounters){
						bitTerms += "bit option" + varid++ + " = " + var.name + "["+lc.name+"]" + ";\n\t";
					}
				}
			}
			else{
				for(String globalVar : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalVar.equals(var.type)){
						// Add an option for each field that matches type
	        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
	        				if(field.type().toString().equals("bit")){
	        					bitTerms += "bit option" + varid++ + " = " + var.name + "." + field.id().toString() + ";\n\t";
	        				}
	        			}
	        		}
					// If it is an array of one of the global data types
	        		else if(globalVar.equals(var.type.replace("["+Configuration.arraySizeBound+"]", ""))){
	        			if(!ext.inputDataCollections.contains(var)){
		        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
		        				// Add an option for each field (of an arbitrary array index) that matches type
		        				if(field.type().toString().equals("bit")){
		        					for(SketchVariable lc : sketchLoopCounters){
		        						bitTerms += "bit option" + varid++ + " = " + var.name + "["+lc.name+"]." + field.id().toString() + ";\n\t";
		        					}
		        				}
		        			}
	        			}
	        		}
				}
			}
		}
		for(SketchVariable var : sketchLoopCounters){
			if(var.type.equals("bit")){
				bitTerms += "bit option" + varid++ + " = " + var.name + ";\n\t";
			}
		}
		
		String opts = "";
		for(int i=0; i<varid; i++){
			opts += "option" + i + " | ";
		}
		opts = opts.substring(0,opts.length()-3);
		
		bitTerms += "return {| "+opts+" |};\n}\n";
		
		return bitTerms;
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
	public static String generateReduceGrammar(MyWhileExt ext, Set<SketchVariable> sketchInputVars, String outputType) {
		String reduceExpGen = "";
		
		// Generate grammar options
		String opts = "";
		for(String op : ext.binaryOperators){
			if(casper.Util.operatorType(op) == casper.Util.ARITHMETIC_OP){
				opts += " | terminal2 " + op + " terminal3";
			}
		}
		for(String op : ext.unaryOperators){
			if(casper.Util.operatorType(op) == casper.Util.ARITHMETIC_OP){
				opts += " | " + op + "terminal3";
			}
		}
		
		reduceExpGen +=	"generator "+outputType+" "+outputType+"ReduceGenerator("+outputType + " val1, " + outputType + " val2, int depth){\n\t" +
							"if(depth == 1){\n\t\t" +
								"return "+outputType+"ReduceTerminals(val1, val2);\n\t" +
							"}\n\t" +
							"else{\n\t\t" +
								"int terminal1 = "+outputType+"ReduceTerminals(val1, val2);\n\t\t" +
								"int terminal2 = "+outputType+"ReduceGenerator(val1, val2, depth-1);\n\t\t" +
								"int terminal3 = "+outputType+"ReduceGenerator(val1, val2, depth-1);\n\t\t" +
								"return {| terminal1" + opts + " |};\n\t" +
							"}\n" +
						"}";
		
		return reduceExpGen;
	}
	
	// Generate do reduce grammars
	public static String generateReduceGrammarInlined(MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, String outputType) {
		String generator = "";
		
		String type = outputType.replace("["+Configuration.arraySizeBound+"]", "");
		
		// Terminal options
		String terminalOpts = "val1 | val2";
		switch(outputType){
			case "int":
				terminalOpts += " | ??";
				break;
			case "bit":
				terminalOpts += " | true | false";
				break;
		}
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
		
		// Terminals
		String terminals = "";
		for(int i=0; i<Configuration.recursionDepth; i++){
			terminals += type+" terminal"+(i+1)+" = {| " + terminalOpts + " |};\n\t";
		}
		
		// Grammar options
		List<String> exprs = new ArrayList<String>();
		getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,"<-expr->",Configuration.recursionDepth);
		String expressions = "";
		String opts = "";
		int id = 0;
		for(String expr : exprs){
			expressions += type+" _option"+id+" = " + expr + ";\n\t";
			opts += "_option"+id+ " | ";
			id++;
		}
		if(!opts.equals("")) opts = opts.substring(0, opts.length()-3);
		else opts = "";
		
		// Generator code		
		generator += "generator "+type+" "+type+"ReduceGenerator("+type+" val1, "+type+" val2){\n\t" + terminals + expressions + "return {| " + opts + " |};\n}";
		
		return generator;
	}
	
	private static void getReduceExpressions(String type, MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, List<String> exprs, String exprString, int depth) {
		if(depth == 0){
			return;
		}
		if(depth == 1){
			exprs.add(exprString.replace("<-expr->","terminal"+depth));
			return;
		}
		else{
			//exprs.add(exprString.replace("<-expr->","terminal"+depth));
			
			for(String op : ext.binaryOperators){
				if(casper.Util.operatorType(op) == casper.Util.getOpClassForType(type)){
					String newExprString = exprString.replace("<-expr->", "terminal" + depth + " " + op + " <-expr->");
					getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1);
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
					boolean ignore = false;
					for(String argType : op.args){
						if(casper.Util.getSketchTypeFromRaw(argType).equals(type)){
							call += "<-expr->,";
						}
						else{
							ignore = true;
							break;
						}
						 
					}
					if(!ignore){
						call = call.substring(0,call.length()-1) + ")";
						String newExprString = exprString.replace("<-expr->", call);
						getReduceExpressions(type,ext,sketchInputVars,sketchOutputVars,sketchLoopCounters,exprs,newExprString,depth-1);
					}
				}
			}
			
			return;
		}
	}

	public static String generateReduceTerminals(MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, String outputType){
		String reduceTerms = "";
		
		reduceTerms +=	"generator " + outputType + " " + outputType + "ReduceTerminals("+outputType + " val1, " + outputType + " val2){\n\t";
		reduceTerms += outputType + " option0 = val1;\n\t";
		reduceTerms += outputType + " option1 = val2;\n\t";
		
		int varid = 2;
		for(SketchVariable var : sketchInputVars){
			if(sketchOutputVars.contains(var))
				continue;
			
			if(var.type.equals(outputType)){
				reduceTerms += outputType + " option" + varid++ + " = " + var.name + ";\n\t";
			}
			else{
				for(String globalVar : ext.globalDataTypes){
					// If it is one of the global data types
					if(globalVar.equals(var.type)){
						// Add an option for each field that matches type
	        			for(FieldDecl field : ext.globalDataTypesFields.get(globalVar)){
	        				if(field.type().toString().equals(outputType)){
	        					reduceTerms += outputType + " option" + varid++ + " = " + var.name + "." + field.id().toString() + ";\n\t";
	        				}
	        			}
	        		}
				}
			}
		}
		
		String opts = "";
		for(int i=0; i<varid; i++){
			opts += "option" + i + " | ";
		}
		opts = opts.substring(0,opts.length()-3);
		
		reduceTerms += "return {| "+opts+" |};\n}\n";
		
		return reduceTerms;
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
