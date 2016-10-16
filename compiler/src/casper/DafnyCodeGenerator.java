package casper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import polyglot.ast.While;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.ArrayAccessNode;
import casper.types.ArrayUpdateNode;
import casper.types.ConditionalNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.visit.GenerateScaffold;

public class DafnyCodeGenerator {
	
	/*private static Map<String,List<CustomASTNode>> indexes = new HashMap<String,List<CustomASTNode>>();
	
	public static String generateDafnyHarnessArgs(MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String args = "";
		
		SketchVariable inputData = ext.inputDataCollections.get(0);
		args += inputData.name + ": " + casper.Util.getDafnyType(inputData.type);
		
		for(SketchVariable var : sketchOutputVars){
			args += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
			if(!ext.initVals.containsKey(var.name)){
				args += ", " + var.name + "0: " + casper.Util.getDafnyType(var.type);
			}
		}
		for(SketchVariable var : sketchInputVars){
			if(sketchOutputVars.contains(var))
				continue;
			if(ext.inputDataCollections.contains(var))
				continue;
			
			if(!ext.initVals.containsKey(var.name)){
				args += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
			}
		}
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
			if(!ext.initVals.containsKey(var.name)){
				args += ", " + var.name + "0: " + casper.Util.getDafnyType(var.type);
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += ", " + "casperConst" + i + ": int";
		}
		
		return args;
	}
	
	public static String generateVarInit(MyWhileExt ext, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String code = "";
		
		for(SketchVariable var : sketchOutputVars){
			if(ext.initVals.containsKey(var.name)){
				code += "var " + var.name + "0 := " + ext.initVals.get(var.name) + ";\n\t";
			}
		}
		for(SketchVariable var : sketchInputVars){
			if(sketchOutputVars.contains(var))
				continue;
			if(ext.inputDataCollections.contains(var))
				continue;
			
			if(ext.initVals.containsKey(var.name)){
				code += "var " + var.name + " := " + ext.initVals.get(var.name) + ";\n\t";
			}
		}
		for(SketchVariable var : sketchLoopCounters){
			if(ext.initVals.containsKey(var.name)){
				code += "var " + var.name + "0 := " + ext.initVals.get(var.name) + ";\n\t";
			}
		}
		
		return code;
	}

	public static String generateInvPcAargs(MyWhileExt ext, List<String> loopInvariantArgsOrder, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars) {
		String code = "";
		
		SketchVariable inputData = ext.inputDataCollections.get(0);
		code += inputData.name + ": " + casper.Util.getDafnyType(inputData.type);
		
		for(String nextVarName : loopInvariantArgsOrder){
			for(SketchVariable var : sketchOutputVars){
				if(nextVarName.equals(var.name)){
					code += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
					code += ", " + var.name + "0: " + casper.Util.getDafnyType(var.type);
				}
			}
			for(SketchVariable var : sketchLoopCounters){
				if(nextVarName.equals(var.name)){
					code += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
					code += ", " + var.name + "0: " + casper.Util.getDafnyType(var.type);
				}
			}
		}
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				code += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
			}
		}
		for(int i=0; i<ext.constCount; i++){
			code += ", " + "casperConst" + i + ": int";
		}
		
		return code;
	}

	public static String generateLoopInv(MyWhileExt ext, String outputType, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, String mergeOp, String reduceValue) {
		String code = "";
		
		code += "0 <= " + sketchLoopCounters.get(0).name + " <= |" + ext.inputDataCollections.get(0).name + "| &&\n\t";
		
		String args = ext.inputDataCollections.get(0).name+","+sketchLoopCounters.get(0).name+"0,"+sketchLoopCounters.get(0).name;
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ","+var.name;
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += "," + "casperConst" + i;
		}
		
		if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					if(reduceValue.contains("val1") && reduceValue.contains("val2")){
						code += "(" + var.name + " == (doreduce(mapper("+args+"),"+index+"<reducer-args-call>) " + mergeOp + " " + var.name + "0)) &&\n\t";
					}
					else{
						code += "("
								+ "(contains(mapper("+args+"),"+index+") && (" + var.name + " == (doreduce(mapper("+args+"),"+index+"<reducer-args-call>) " + mergeOp + " " + var.name + "0))) || "
								+ "((!contains(mapper("+args+"),"+index+")) && (" + var.name + " == " + var.name + "0))"
							  + ") &&\n\t";
					}
					index++;
				}
			}
		}
		else if(casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					code += "(forall k :: 0 <= k < |" + var.name  + "| ==> " + var.name + "[k] == doreduce(mapper("+args+"),("+index+",k)<reducer-args-call>) " + mergeOp + " " + var.name + "0[k]) &&\n\t";
					index++;
				}
			}
		} 
		
		code = code.substring(0,code.length()-5);
		
		return code;
	}
	
	public static String generatePostCond(MyWhileExt ext, String outputType, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, String mergeOp, String reduceValue) {
		String code = "";
		
		code += sketchLoopCounters.get(0).name+" == |" + ext.inputDataCollections.get(0).name + "| &&\n\t";
		
		String args = ext.inputDataCollections.get(0).name+","+sketchLoopCounters.get(0).name+"0,"+sketchLoopCounters.get(0).name;
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ","+var.name;
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += "," + "casperConst" + i;
		}
		
		if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					if(reduceValue.contains("val1") && reduceValue.contains("val2")){
						code += "(" + var.name + " == (doreduce(mapper("+args+"),"+index+"<reducer-args-call>) " + mergeOp + " " + var.name + "0)) &&\n\t";
					}
					else{
						code += "("
								+ "(contains(mapper("+args+"),"+index+") && (" + var.name + " == (doreduce(mapper("+args+"),"+index+"<reducer-args-call>) " + mergeOp + " " + var.name + "0))) || "
								+ "((!contains(mapper("+args+"),"+index+")) && (" + var.name + " == " + var.name + "0))"
							  + ") &&\n\t";
					}
					index++;
				}
			}
		}
		else if(casper.Util.getTypeClass(outputType) == casper.Util.ARRAY){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					String operator = casper.Util.getOperatorFromExp(reduceValue);
					code += "(forall k :: 0 <= k < |" + var.name  + "| ==> " + var.name + "[k] == doreduce(mapper("+args+"),("+index+",k)<reducer-args-call>) " + operator + " " + var.name + "0[k]) &&\n\t";
					index++;
				}
			}
		}
		
		code = code.substring(0,code.length()-5);
		
		return code;
	}

	public static String generateMapperArgsDecl(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		
		SketchVariable inputData = ext.inputDataCollections.get(0);
		args += inputData.name + ": " + casper.Util.getDafnyType(inputData.type);
		
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.name + "0: " + casper.Util.getDafnyType(var.type);
			args += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
		}
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += ", " + "casperConst" + i + ": int";
		}
		
		return args;
	}
	
	public static String generateMapperArgsCall(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		
		args += ext.inputDataCollections.get(0).name;
		
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.name + "0";
			args += ", " + var.name;
		}
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ", " + var.name;
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += ", " + "casperConst" + i;
		}
		
		return args;
	}
	
	public static String generateMapperArgsCallInd(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		
		args += ext.inputDataCollections.get(0).name;
		
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.name + "0";
			args += ", " + var.name + "-1";
		}
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ", " + var.name;
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += ", " + "casperConst" + i;
		}
		
		return args;
	}
	
	public static String generateMapperArgsCallInd2(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		
		args += ext.inputDataCollections.get(0).name;
		
		for(SketchVariable var : sketchLoopCounters){
			args += ", " + var.name + "0";
			args += ", " + var.name + "+1";
		}
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ", " + var.name;
			}
		}
		for(int i=0; i<ext.constCount; i++){
			args += ", " + "casperConst" + i;
		}
		
		return args;
	}

	public static String generateDomapPreCond(MyWhileExt ext, List<SketchVariable> sketchLoopCounters) {
		return "requires 0 <= " + sketchLoopCounters.get(0).name + " < |" + ext.inputDataCollections.get(0).name + "|";
	}
	
	public static String generateMapPreCond(MyWhileExt ext, List<SketchVariable> sketchLoopCounters) {
		return "requires 0 <= " + sketchLoopCounters.get(0).name + " <= |" + ext.inputDataCollections.get(0).name + "|";
	}

	public static String generateMapEmits(Map<String, List<KvPair>> mapEmits, String mapperArgsCall) {
		String code = "";
		int index = 0;
		for(String cond : mapEmits.keySet()){
			for(KvPair kvp : mapEmits.get(cond)){
				code += "emit" + index + "(" + mapperArgsCall + ") + ";
				index++;
			}
		}
		code = code.substring(0,code.length()-3);
		return code;
	}
	
	public static String generateEmitFuncs(Map<String, List<KvPair>> mapEmits) {
		String code = "";
		int index = 0;
		for(String cond : mapEmits.keySet()){
			for(KvPair kvp : mapEmits.get(cond)){
				String emit = "[";
				if(kvp.key2 == "")
					emit += "("+kvp.key+","+kvp.value+"),";
				else
					emit += "(("+kvp.key+","+kvp.key2+"),"+kvp.value+"),";
				emit = emit.substring(0,emit.length()-1) + "]";
				
				if(cond.equals("noCondition")){
					code += "function emit" + index + "(<mapper-args-decl>) : <domap-emit-type>\n\t" + 
							"<loop-counter-range-domap>\n\t" +
							"ensures emit"+index+"(<mapper-args-call>) == "+emit+"\n" +
						"{\n\t" +
						  emit + "\n" +
						"}\n\n";
				}
				else{
					code += "function emit" + index + "(<mapper-args-decl>) : <domap-emit-type>\n\t" + 
							"<loop-counter-range-domap>\n\t" +
							"ensures ("+cond+") ==> (emit"+index+"(<mapper-args-call>) == "+emit+")\n" +
						"{\n\t" +
						  "if "+cond+" then" + emit + "\n" +
						  "else []\n" +
						"}\n\n";
				}
				index++;
			}
		}
		
		return code;
	}

	public static String generateReduceExp(String reduceValue, String reduceInitValue) { 
		String foldexp = reduceValue.replace("val1", "doreduce(input[1..], key<reducer-args-call>)");
		foldexp = foldexp.replaceAll("val2", "input[0].1");
		return foldexp;
	}

	public static String generateEmitLemmas(String type, Map<String, List<KvPair>> mapEmits, String mapperArgsCall, String mapperArgsCallInd2, String reduceValue) {
		String code = "";

		if(type.contains("["+Configuration.arraySizeBound+"]")){
			for(GenerateScaffold.KvPair kvp : mapEmits.get("noCondition")){
				if(kvp.key2 == ""){
					code += "assert doreduce(domap("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>) == ("+kvp.value+");\n\t";
					code += "Lemma2(domap("+mapperArgsCall+"),mapper("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>);\n\t";
				}
				else{
					code += "assert doreduce(domap("+mapperArgsCall+"),("+kvp.key+","+kvp.key2+")<reducer-args-call>) == ("+kvp.value+");\n\t";
				}
				
				//code += "assert forall k :: (0 <= k < |" + var.name  + "| && k != " + indexes.get(var.name).get(0) + ") ==> " + var.name + "[k] == doreduce(mapper("+mapperArgsCallInd2+"),k);\n\t";
				//code += "assert forall k :: (0 <= k < |" + var.name  + "| && k == " + indexes.get(var.name).get(0) + ") ==> " + var.name + "[k] + doreduce(domap("+mapperArgsCall+"),k) == doreduce(mapper("+mapperArgsCallInd2+"),k);\n\n\t";
			}
		}
		else{
			for(GenerateScaffold.KvPair kvp : mapEmits.get("noCondition")){
				String exp = reduceValue;
				exp = exp.replace("val2", "doreduce(domap("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>)");
				exp = exp.replace("val1", "doreduce(mapper("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>)");
				code += "assert doreduce(domap("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>) == ("+kvp.value+");\n\t";
				code += "Lemma2(domap("+mapperArgsCall+"),mapper("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>);\n\t";
				code += "assert doreduce(mapper("+mapperArgsCallInd2+"),"+kvp.key+"<reducer-args-call>) == ("+exp+");\n\n\t";
			}
		}
		
		return code;
	}

	public static String generateWPCInits(Map<String, CustomASTNode> wpcValues, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String code = "";
		for(String varname : wpcValues.keySet())
		{
			boolean found = false;
			for(SketchVariable var : sketchOutputVars){
				if(var.name.equals(varname)){
					code += "var ind_" + varname + " := " + varname + ";\n\t\t";
					CustomASTNode value = wpcValues.get(varname);
					if(value instanceof ConditionalNode){
						code += ((ConditionalNode)value).toStringDafny("ind_" + varname + " := ");
					}
					else if(value instanceof ArrayUpdateNode){
						code += "ind_" + varname + " := " + ((ArrayUpdateNode)value).toStringDafny() + ";\n\t\t";
					}
					else{
						code += "ind_" + varname + " := " + value + ";\n\t\t";
					}
					found = true;
					break;
				}
			}
			if(found) continue;
			for(SketchVariable var : sketchLoopCounters){
				if(var.name.equals(varname)){
					code += "var ind_" + varname + " := " + varname + ";\n\t\t";
					CustomASTNode value = wpcValues.get(varname);
					if(value instanceof ConditionalNode){
						code += ((ConditionalNode)value).toString("ind_" + varname + " := ");
					}
					else if(value instanceof ArrayUpdateNode){
						code += "ind_" + varname + " := " + ((ArrayUpdateNode)value).toStringDafny() + ";\n\t\t";
					}
					else{
						code += "ind_" + varname + " := " + value + ";\n\t\t";
					}
					break;
				}
			}
		}
		return code;
	}

	public static String generateMapTerminateCondition(List<SketchVariable> sketchLoopCounters) {
		String code = "";
		for(SketchVariable var : sketchLoopCounters){
			code += var.name + " == 0 && "; 
		}
		code = code.substring(0,code.length()-4);
		return code;
	}

	public static String generateRequireStatements(Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Map<String, CustomASTNode> wpcValues) {
		String code = "";
		indexes.clear();
		for(SketchVariable var : sketchOutputVars){
			if(wpcValues.containsKey(var.name)){
				CustomASTNode wpcValue = wpcValues.get(var.name);
				if(var.category == Variable.ARRAY_ACCESS){
					// Length requirement
					code += "requires |"+var.name+"| == |"+var.name+"0|\n\t";
					
					// Indexes requirement
					indexes.put(var.name, new ArrayList<CustomASTNode>());
					wpcValue.getIndexes(var.name,indexes);
					for(CustomASTNode index : indexes.get(var.name)){
						if(index instanceof ArrayAccessNode){
							code += "requires forall k :: 0 <= k < |" + ((ArrayAccessNode)index).array + "| ==> 0 <= " + ((ArrayAccessNode)index).array + "[k] < |" + var.name + "|\n\t";
						}
						else{
							code += "requires 0 <= " + index + " < |" + var.name + "|\n\t";
						}
					}
				}
			}
		}
		if(code.length()>0) code = code.substring(0,code.length()-2);
		return code;
	}

	public static String generateDomapEmitType(String mapKeyType, String outputType) {
		String code = "";

		// Convert mapKeyType to dafny types
		String dafnyMapKeyType = "";
		
		if(mapKeyType.contains(",")){
			dafnyMapKeyType += "("+casper.Util.getDafnyType(mapKeyType.substring(1,mapKeyType.indexOf(",")))+",";
			dafnyMapKeyType += casper.Util.getDafnyType(mapKeyType.substring(mapKeyType.indexOf(",")+1,mapKeyType.length()-1))+")";
		}
		else{
			dafnyMapKeyType = casper.Util.getDafnyType(mapKeyType);
		}
		
		code += "seq<("+dafnyMapKeyType+", "+casper.Util.getDafnyType(outputType.replace("["+Configuration.arraySizeBound+"]",""))+")>";
		return code;
	}

	public static String generateDoreduceKeyType(String mapKeyType) {
		String code = "";
		//if(mapEmits.get(0).key2 == "")
			code += mapKeyType.replace("string", "int");
		//else
			//code += "("+mapKeyType+","+mapKeyType+")";
		return code;
	}

	public static String generatePreCondition(MyWhileExt ext, String outputType, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String code = "";
		
		code = ext.preConditions.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		code = code.substring(0,code.length()-1) + ",";
		for(SketchVariable v : sketchInputVars){
			if(!sketchOutputVars.contains(v) && !sketchLoopCounters.contains(v) && !ext.inputDataCollections.contains(v)){
				code += v.name + ",";
			}
		}
		for(int i=0; i<ext.constCount; i++){
			code += "casperConst" + i + ",";
		}
		code = code.substring(0,code.length()-1) + ")";
		
		return code;
	}

	public static String generateInvariant(MyWhileExt ext, String outputType, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String invariant = "";
		
		invariant = ext.invariants.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		invariant = invariant.substring(0,invariant.length()-1) + ",";
		for(SketchVariable v : sketchInputVars){
			if(!sketchOutputVars.contains(v) && !sketchLoopCounters.contains(v) && !ext.inputDataCollections.contains(v)){
				invariant += v.name + ",";
			}
		}
		for(int i=0; i<ext.constCount; i++){
			invariant += "casperConst" + i + ",";
		}
		invariant = invariant.substring(0,invariant.length()-1) + ")";
		
		return invariant;
	}

	public static String generateWPC(MyWhileExt ext, String outputType, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, MyStmtExt bodyExt) {
		String wpc = "";
		
		wpc = bodyExt.preConditions.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		wpc = wpc.substring(0,wpc.length()-1) + ",";
		for(SketchVariable v : sketchInputVars){
			if(!sketchOutputVars.contains(v) && !sketchLoopCounters.contains(v) && !ext.inputDataCollections.contains(v)){
				wpc += v.name + ",";
			}
		}
		for(int i=0; i<ext.constCount; i++){
			wpc += "casperConst" + i + ",";
		}
		wpc = wpc.substring(0,wpc.length()-1) + ")";
		
		return wpc;
	}

	public static String generatePostCondStmt(MyWhileExt ext, String outputType, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters) {
		String pcond = "";
		
		pcond = ext.postConditions.get(outputType).replaceAll("input_data", new IdentifierNode(ext.inputDataCollections.get(0).name)).toString();
		pcond = pcond.substring(0,pcond.length()-1) + ",";
		for(SketchVariable v : sketchInputVars){
			if(!sketchOutputVars.contains(v) && !sketchLoopCounters.contains(v) && !ext.inputDataCollections.contains(v)){
				pcond += v.name + ",";
			}
		}
		for(int i=0; i<ext.constCount; i++){
			pcond += "casperConst" + i + ",";
		}
		pcond = pcond.substring(0,pcond.length()-1) + ")";
		
		return pcond;
	}

	public static String generateReduceLemma(String reduceValue, String reduceInitValue) {
		if(reduceValue.contains("val1") && reduceValue.contains("val2")){
			String foldexp = reduceValue.replace("val1", "doreduce(a, key<reducer-args-call>)");
			foldexp = foldexp.replaceAll("val2", "doreduce(b, key<reducer-args-call>)");
			
			String lemma = "lemma Lemma2 (a: <domap-emit-type>, b: <domap-emit-type>, key: <doreduce-key-type><reducer-args-decl>)\n\t" +
							  "ensures doreduce(a+b, key<reducer-args-call>) == ("+foldexp+")\n" +
							"{\n\t" +
							    "if a != []\n\t" +
							    "{\n\t\t" +
							    	"Lemma2(a[1..], b, key<reducer-args-call>);\n\t\t" +
							    	"assert a + b == [a[0]] + (a[1..] + b);\n\t" +
								"}\n" +
							"}";
			return lemma;	
		}
		else{
			return "";
		}
	}

	public static String generatedReducerArgsDecl(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ", " + var.name + ": " + casper.Util.getDafnyType(var.type);
			}
		}
		return args;
	}
	
	public static String generatedReducerArgsCall(MyWhileExt ext, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, Set<SketchVariable> sketchOutputVars) {
		String args = "";
		
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ", " + var.name;
			}
		}
		return args;
	}

	public static void generateSummary(int id, MyWhileExt ext, List<Variable> sketchFilteredOutputVars, String reducerType) {
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
		
		/** Extract map function **//*
		Pattern r = Pattern.compile("void do_map(.*?)\\{(.*?)return;\n\\}",Pattern.DOTALL);
		Matcher m = r.matcher(text);
		m.find();
		String map = m.group(0); 
		
		List<String> mapLines = new ArrayList<String>();
		for(String line : map.split("\n")){
			mapLines.add(line.trim());
		}
		
		/** Extract map emits **//*
		// First look for emits wrapped in if conditions
		r = Pattern.compile("if(.*?)\\{(.*?)\\}",Pattern.DOTALL);
		m = r.matcher(map);
		Map<String,String> conditionals = new HashMap<String,String>();
		while(m.find()){
			conditionals.put(m.group(1).substring(1, m.group(1).lastIndexOf(")")),m.group(2));
		}
		
		Map<String,List<KvPair>> mapEmits = new HashMap<String,List<KvPair>>();
		
		for(String conditional : conditionals.keySet()){
			String conditional_res = conditional;
			for(int i=0; i<mapLines.size(); i++){
				if(mapLines.get(i).contains("if("+conditional+")")){
					conditional_res = resolve(conditional,mapLines,i,ext);
					break;
				}
			}
			mapEmits.put(conditional_res,extractMapEmits(conditionals.get(conditional),ext,outputType,mapEmits));
		}
		
		// Remaining emits
		mapEmits.put("noCondition",extractMapEmits(map,ext,outputType,mapEmits));
		
		ext.mapEmits = mapEmits;
		
		/** Extract reduce emits **//*
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
		
		
		/** Extract merge operator **//*
		r = Pattern.compile("void int_get (.*?)\\{(.*?)\\}",Pattern.DOTALL);
		m = r.matcher(text);
		String mergeOp = "";
		if(!m.find()){
			r = Pattern.compile("void string_get (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
			m = r.matcher(text);
			if(!m.find()){
				r = Pattern.compile("void int_get_tuple (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
				m = r.matcher(text);
				if(!m.find()){
					r = Pattern.compile("void string_get_tuple (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
					m = r.matcher(text);
					m.find();
				}
			}
		}
		mergeOp = m.group(2);
		r = Pattern.compile(outputType.replace("["+Configuration.arraySizeBound+"]", "") + " option(.*?) = (.*?);",Pattern.DOTALL);
		m = r.matcher(mergeOp);
		m.find();
		mergeOp = casper.Util.getOperatorFromExp(m.group(2));
		
		/****** Generate dafny verification file ******//*
		
		// Generate main harness args
		String harnessArgs = DafnyCodeGenerator.generateDafnyHarnessArgs(ext,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		
		// Generate require statements for dafny main function
		String mainReqStmts = DafnyCodeGenerator.generateRequireStatements(sketchOutputVars,sketchLoopCounters,ext.wpcValues);
		
		// Init variables in main
		String initVars = DafnyCodeGenerator.generateVarInit(ext,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		
		// Generate verification code
		String preC = DafnyCodeGenerator.generatePreCondition(ext,outputType,sketchInputVars,sketchOutputVars,sketchLoopCounters);
		MyStmtExt bodyExt = ((MyStmtExt) JavaExt.ext(((While)n).body()));
		String loopCond = "("+sketchLoopCounters.get(0).name+"<|"+ext.inputDataCollections.get(0).varName+"|)";
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
		String loopInv = DafnyCodeGenerator.generateLoopInv(ext, outputType, sketchOutputVars, sketchLoopCounters, sketchInputVars, mergeOp, reduceValue);
		
		// Generate post condition
		String postCond = DafnyCodeGenerator.generatePostCond(ext, outputType, sketchOutputVars, sketchLoopCounters, sketchInputVars, mergeOp, reduceValue);
		
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
		String domapEmits = DafnyCodeGenerator.generateMapEmits(mapEmits, mapperArgsCall);
		String emitFuncs = DafnyCodeGenerator.generateEmitFuncs(mapEmits);
		
		// Do reduce args declaration
		String reducerArgsDecl = DafnyCodeGenerator.generatedReducerArgsDecl(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		
		// Do reduce args call
		String reducerArgsCall = DafnyCodeGenerator.generatedReducerArgsCall(ext, sketchLoopCounters, sketchInputVars, sketchOutputVars);
		
		// Generate map outputType
		String domapEmitType = DafnyCodeGenerator.generateDomapEmitType(ext.mapKeyType,outputType);

		// Generate do reduce key type
		String doreduceKeyType = DafnyCodeGenerator.generateDoreduceKeyType(ext.mapKeyType);
		
		// Generate reduce expression
		String reduceExp = DafnyCodeGenerator.generateReduceExp(reduceValue,reduceInitValue);
		
		// Generate reduce expression for lemma
		String reduceExpLemma = DafnyCodeGenerator.generateReduceLemma(reduceValue,reduceInitValue);
		
		// Generate lemma proof for map emits
		String emitLemmas = DafnyCodeGenerator.generateEmitLemmas(outputType,mapEmits,mapperArgsCall,mapperArgsCallInd2,reduceValue);
		
		// Generate terminate condition for map recursion
		String tCond = DafnyCodeGenerator.generateMapTerminateCondition(sketchLoopCounters);
		
		// Plug all the generated code into the template
		String template = new String(Files.readAllBytes(Paths.get("templates/dafny_skeleton.dfy")), StandardCharsets.UTF_8);
		PrintWriter writer = new PrintWriter(filename, "UTF-8");
		
		template = template.replace("<reduce-exp-lemma>",reduceExpLemma);
		template = template.replace("<emit-funcs>", emitFuncs);
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
	}*/

}
