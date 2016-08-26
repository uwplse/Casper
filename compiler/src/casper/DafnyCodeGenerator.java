package casper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.extension.MyWhileExt.Variable;
import casper.types.ArrayAccessNode;
import casper.types.ArrayUpdateNode;
import casper.types.ConditionalNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.visit.GenerateScaffold;
import casper.visit.GenerateScaffold.KvPair;
import casper.visit.GenerateScaffold.SketchVariable;

public class DafnyCodeGenerator {
	
	private static Map<String,List<CustomASTNode>> indexes = new HashMap<String,List<CustomASTNode>>();
	
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
		
		return code;
	}

	public static String generateLoopInv(MyWhileExt ext, String outputType, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, String reduceValue) {
		String code = "";
		
		code += "0 <= " + sketchLoopCounters.get(0).name + " <= |" + ext.inputDataCollections.get(0).name + "| &&\n\t";
		
		String args = ext.inputDataCollections.get(0).name+","+sketchLoopCounters.get(0).name+"0,"+sketchLoopCounters.get(0).name;
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ","+var.name;
			}
		}
		
		if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					String operator = casper.Util.getOperatorFromExp(reduceValue);
					code += "(" + var.name + " == (doreduce(mapper("+args+"),"+index+"<reducer-args-call>) " + operator + " " + var.name + "0)) &&\n\t";
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
	
	public static String generatePostCond(MyWhileExt ext, String outputType, Set<SketchVariable> sketchOutputVars, List<SketchVariable> sketchLoopCounters, Set<SketchVariable> sketchInputVars, String reduceValue) {
		String code = "";
		
		code += sketchLoopCounters.get(0).name+" == |" + ext.inputDataCollections.get(0).name + "| &&\n\t";
		
		String args = ext.inputDataCollections.get(0).name+","+sketchLoopCounters.get(0).name+"0,"+sketchLoopCounters.get(0).name;
		for(SketchVariable var : sketchInputVars){
			if(!sketchOutputVars.contains(var) && !sketchLoopCounters.contains(var) && !ext.inputDataCollections.contains(var)){
				args += ","+var.name;
			}
		}
		
		if(casper.Util.getTypeClass(outputType) == casper.Util.PRIMITIVE){
			int index = 0;
			for(SketchVariable var : sketchOutputVars){
				if(var.type.equals(outputType)){
					String operator = casper.Util.getOperatorFromExp(reduceValue);
					code += "(" + var.name + " == (doreduce(mapper("+args+"),"+index+"<reducer-args-call>) " + operator + " " + var.name + "0)) &&\n\t";
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
		
		return args;
	}

	public static String generateDomapPreCond(MyWhileExt ext, List<SketchVariable> sketchLoopCounters) {
		return "requires 0 <= " + sketchLoopCounters.get(0).name + " < |" + ext.inputDataCollections.get(0).name + "|";
	}
	
	public static String generateMapPreCond(MyWhileExt ext, List<SketchVariable> sketchLoopCounters) {
		return "requires 0 <= " + sketchLoopCounters.get(0).name + " <= |" + ext.inputDataCollections.get(0).name + "|";
	}

	public static String generateMapEmits(MyWhileExt ext, List<GenerateScaffold.KvPair> mapEmits) {
		String code = "[";
		for(GenerateScaffold.KvPair kvp : mapEmits){
			if(kvp.key2 == "")
				code += "("+kvp.key+","+kvp.value+"),";
			else
				code += "(("+kvp.key+","+kvp.key2+"),"+kvp.value+"),";
		}
		return code.substring(0,code.length()-1) + "]";
	}

	public static String generateReduceExp(String reduceValue, String reduceInitValue) { 
		String foldexp = reduceValue.replace("val1", "doreduce(input[1..], key<reducer-args-call>)");
		foldexp = foldexp.replaceAll("val2", "input[0].1");
		return foldexp;
	}

	public static String generateEmitLemmas(String type, List<GenerateScaffold.KvPair> mapEmits, String mapperArgsCall, String mapperArgsCallInd2, String reduceValue) {
		String code = "";

		if(type.contains("["+Configuration.arraySizeBound+"]")){
			for(GenerateScaffold.KvPair kvp : mapEmits){
				if(kvp.key2 == ""){
					code += "assert doreduce(domap("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>) == ("+kvp.value+");\n\t";
					code += "Lemma2(domap("+mapperArgsCall+"),mapper("+mapperArgsCall+"),"+kvp.key+"<reducer-args-call>);\n\t";
				}
				else{
					code += "assert doreduce(domap("+mapperArgsCall+"),("+kvp.key+","+kvp.key2+")<reducer-args-call>) == ("+kvp.value+");\n\t";
					code += "assert doreduce(domap("+mapperArgsCall+"),("+kvp.key+","+kvp.key2+")<reducer-args-call>) == ("+kvp.value+");\n\t";
				}
				
				//code += "assert forall k :: (0 <= k < |" + var.name  + "| && k != " + indexes.get(var.name).get(0) + ") ==> " + var.name + "[k] == doreduce(mapper("+mapperArgsCallInd2+"),k);\n\t";
				//code += "assert forall k :: (0 <= k < |" + var.name  + "| && k == " + indexes.get(var.name).get(0) + ") ==> " + var.name + "[k] + doreduce(domap("+mapperArgsCall+"),k) == doreduce(mapper("+mapperArgsCallInd2+"),k);\n\n\t";
			}
		}
		else{
			for(GenerateScaffold.KvPair kvp : mapEmits){
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

	public static String generateDomapEmitType(String mapKeyType, String outputType, List<KvPair> mapEmits) {
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

	public static String generateDoreduceKeyType(String mapKeyType, List<KvPair> mapEmits) {
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
		pcond = pcond.substring(0,pcond.length()-1) + ")";
		
		return pcond;
	}

	public static String generateReduceExpLemma(String reduceValue) {
		String foldexp = reduceValue.replace("val1", "doreduce(a, key<reducer-args-call>)");
		foldexp = foldexp.replaceAll("val2", "doreduce(b, key<reducer-args-call>)");
		return "("+foldexp+")";
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
}
