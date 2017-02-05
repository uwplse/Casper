package casper.visit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.JavaLibModel.SketchCall;
import casper.SketchParser.KvPair;
import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.Formal;
import polyglot.ast.Import;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class GenerateSparkCode extends NodeVisitor{
	boolean debug;
	boolean demo;
	NodeFactory nf;
	int id;
	boolean first;
	MyWhileExt loopExt;
	
	public GenerateSparkCode(NodeFactory nodeFactory){
		this.debug = false;
		this.demo = true;
		this.nf = nodeFactory;
		this.id = 0;
		this.first = true;
		this.loopExt = null;
	}
	
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// Change parameters to RDD type
		if(n instanceof MethodDecl){
			if(demo){
				List<Formal> fixedFormals = new ArrayList<Formal>();
				for(Formal arg : ((MethodDecl) n).formals()){
					if(arg.id().toString().equals(loopExt.inputDataSet.varName)){
						String newType = "JavaRDD<" + loopExt.inputDataSet.getRDDType() + ">";
						fixedFormals.add(nf.Formal(arg.position(), arg.flags(), nf.TypeNodeFromQualifiedName(arg.type().position(), newType), arg.id()));
					}
					else{
						fixedFormals.add(arg);
					}
				}
				n = nf.MethodDecl(n.position(), ((MethodDecl) n).flags(), ((MethodDecl) n).returnType(), ((MethodDecl) n).id(), fixedFormals, ((MethodDecl) n).throwTypes(), ((MethodDecl) n).body());
			}
		}
		// If the node is a loop
		else if(n instanceof While || n instanceof ExtendedFor){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
				
				Set<String> handledTypes = new HashSet<String>();
				
				int typeid = 0;
				for(Variable var : ext.outputVars){
					if(!handledTypes.contains(var.varType)){
						handledTypes.add(var.varType);
						
						if(!ext.generateCode.get(var.getReduceType())) continue;
						
						if(demo) loopExt = ext;
						
						String template = "";
						
						try {
							template = new String(Files.readAllBytes(Paths.get("templates/spark_skeleton_demo.txt")), StandardCharsets.UTF_8);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						String createRDD = generateCreateRDD(ext);
						String rddName = "rdd_"+typeid+"_"+id;
						if(demo) rddName = ext.inputDataSet.varName;
						String dupInputVars = generateDuplicateVarInit(ext);
						String mapKeyType = generateMapKeyType(ext);
						String inputDataType = generateInputDataType(ext);
						String outputType = generateOutputDataType(ext,var.varType);
						String inputDataName = ext.inputDataSet.varName;
						String lcName = ext.mainLoopCounter.varName;
						String mapEmits = generateMapEmits(ext);
						String reduceExps = generateReduceExps(ext);
						String reconOutput = generateOutputReconstruction(ext,var.varType);
					
						template = template.replace("<create-rdd>", createRDD);
						template = template.replace("<input-name>", inputDataName+"_"+lcName);
						template = template.replace("<map-emits>", mapEmits);
						template = template.replace("<reduce-exp>", reduceExps);
						template = template.replace("<reconstruct-output>", reconOutput);
						template = template.replace("<rdd-name>", rddName);
						template = template.replace("<map-key-type>", mapKeyType);
						template = template.replace("<input-type>", inputDataType);
						template = template.replace("<output-type>", outputType);
						template = template.replace(inputDataName+"["+lcName+"]", inputDataName+"_"+lcName);
						
						
						for(String constVar : ext.constMapping.keySet()){
							template = template.replace(ext.constMapping.get(constVar), constVar);
						}
						
						for(Variable inVar : ext.inputVars){
							if(!ext.outputVars.contains(inVar) && inVar.category != Variable.ARRAY_ACCESS)
								template = template.replaceAll("\\b"+inVar.varName+"\\b",inVar.varName+"_final");
						}
						
						template = template.replace("<duplicate-input-vars>", dupInputVars);
						
						n = nf.Eval(n.position(), nf.ExprFromQualifiedName(n.position(), template.substring(0,template.length())));
						
						typeid++;
					}
				}
				
				id++;
			}
		}
		else if(n instanceof Import){
			if(first){
				String imports = 	"org.apache.spark.SparkConf;\n"
									+ "import org.apache.spark.api.java.JavaSparkContext;\n"
									+ "import org.apache.spark.api.java.JavaPairRDD;\n"
									+ "import org.apache.spark.api.java.JavaRDD;\n"
									+ "import org.apache.spark.api.java.function.Function2;\n"
									+ "import org.apache.spark.api.java.function.PairFlatMapFunction;\n"
									+ "import scala.Tuple2;\n"
									+ "import java.util.ArrayList;\n"
									+ "import java.util.Map;\n"
									+ "import java.util.Iterator;\n";
				
				n = nf.Import(n.position(), ((Import) n).kind(), imports+n.toString());
				first = !first;
			}
		}
       
		return n;
	}

	private String generateCreateRDD(MyWhileExt ext) {
		String code = "";
		
		code += "JavaRDD<<input-type>> <rdd-name> = sc.parallelize("+ext.inputDataSet.varName+");";
		
		return code;
	}
   
	private String generateDuplicateVarInit(MyWhileExt ext) {
		String code = "";
		for(Variable var : ext.inputVars){
			if(!ext.outputVars.contains(var) && var.category != Variable.ARRAY_ACCESS)
				code += "final " + var.varType + " " + var.varName + "_final = "+var.varNameOrig+";\n";
		}
		return code;
	}
	
	private String generateInputDataType(MyWhileExt ext) {
		return ext.inputDataSet.getRDDType();
	}
	
	private String generateOutputDataType(MyWhileExt ext, String type) {
		if(ext.outVarCount > 1)
			return "Tuple2<Integer," + casper.Util.getSparkType(type) + ">";
		else
			return casper.Util.getSparkType(type);
	}
	
	private String generateMapKeyType(MyWhileExt ext) {
		String keyType = "";
		
		Map<String, List<KvPair>> mapEmits = ext.verifiedMapEmits.get(ext.selectedSolutionIndex);
		for(String conditional : mapEmits.keySet()){
			if(mapEmits.get(conditional).size() > 0){
				KvPair kvp = mapEmits.get(conditional).get(0);
				if(kvp.keys.size()>1 && ext.outVarCount > 1) keyType += "Tuple2<";
				for(Integer i : kvp.keys.keySet()){
					if(i==0)
						if(ext.outVarCount > 1)
							keyType += "Integer,";
					else
						keyType += casper.Util.getSparkType(ext.verifiedSolKeyTypes.get(ext.selectedSolutionIndex)) +",";
				}
				keyType = keyType.substring(0,keyType.length()-1);
				if(kvp.keys.size()>1 && ext.outVarCount > 1) keyType = keyType + ">";
				break;
			}
		}
		
		return keyType;
	}
	
	private String generateMapEmits(MyWhileExt ext) {
		String emits = "";
		
		Map<String, List<KvPair>> mapEmits = ext.verifiedMapEmits.get(ext.selectedSolutionIndex);
		
		for(String cond : mapEmits.keySet()){
			String fixedCond = cond;
			
			for(KvPair kvp : mapEmits.get(cond)){
				// Fix function calls
				for(SketchCall call : ext.methodOperators){
					Pattern r = Pattern.compile("^("+call.name+")\\((..*)\\)$");
					Matcher m;
					
					// Fix condition
					m = r.matcher(fixedCond);
					if(m.find()){
						if(call.target.equals("first-arg")){
							String target = m.group(2).substring(0, m.group(2).indexOf(","));
							String args = m.group(2).substring(m.group(2).indexOf(",")+1, m.group(2).length());
							fixedCond = fixedCond.replace(m.group(0), target+"."+call.nameOrig+"("+args+")");
							
						}
						else{
							String args = m.group(2);
							fixedCond = fixedCond.replace(m.group(0), call.nameOrig+"("+args+")");
						}
					}
					
					// Fix keys
					/*m = r.matcher(kvp.key);
					if(m.find()){
					}
					
					m = r.matcher(kvp.key2);
					if(m.find()){
					}*/
					
					// Fix Values
					Map<Integer, String> valuesFixed = new HashMap<Integer, String>();
					for(Integer key : kvp.values.keySet()){
						String kvpValue = kvp.values.get(key);
						m = r.matcher(kvpValue);
						if(m.find()){
							if(call.target.equals("first-arg")){
								String target = m.group(2).substring(0, m.group(2).indexOf(","));
								String args = m.group(2).substring(m.group(2).indexOf(",")+1, m.group(2).length());
								kvpValue = kvpValue.replace(m.group(0), target+"."+call.nameOrig+"("+args+")");
								
							}
							else{
								String args = m.group(2);
								kvpValue = kvpValue.replace(m.group(0), call.nameOrig+"("+args+")");
							}
						}
						valuesFixed.put(key, kvpValue);
					}
					
					kvp.values = valuesFixed;
				}
				
				if(kvp.keys.size() < 2){
					if(cond.equals("noCondition")){
						if(ext.outVarCount > 1)
							emits += "emits.add(new Tuple2("+kvp.keys.get(0)+",new Tuple2("+kvp.keys.get(0)+","+kvp.values.get(0)+")));\n";
						else
							emits += "emits.add(new Tuple2("+kvp.keys.get(0)+","+kvp.values.get(0)+"));\n";
					}
					else{
						if(ext.outVarCount > 1)
							emits += "if("+fixedCond+") emits.add(new Tuple2("+kvp.keys.get(0)+",new Tuple2("+kvp.keys.get(0)+","+kvp.values.get(0)+")));\n";
						else
							emits += "if("+fixedCond+") emits.add(new Tuple2("+kvp.keys.get(0)+","+kvp.values.get(0)+"));\n";
					}
				}
				else{
					if(cond.equals("noCondition")){
						if(ext.outVarCount > 1)
							emits += "emits.add(new Tuple2(new Tuple2("+kvp.keys.get(0)+","+kvp.keys.get(1)+"), new Tuple2("+kvp.keys.get(0)+","+kvp.values.get(0)+")));\n";
						else
							emits += "emits.add(new Tuple2("+kvp.keys.get(1)+", "+kvp.values.get(0)+"));\n";
					}
					else{
						if(ext.outVarCount > 1)
							emits += "if("+fixedCond+") emits.add(new Tuple2(new Tuple2("+kvp.keys.get(0)+","+kvp.keys.get(1)+"), new Tuple2("+kvp.keys.get(0)+","+kvp.values.get(0)+")));\n";
						else
							emits += "if("+fixedCond+") emits.add(new Tuple2("+kvp.keys.get(1)+", "+kvp.values.get(0)+"));\n";
					}
				}
			}
		}
		return emits;
	}

	private String generateOutputReconstruction(MyWhileExt ext, String type) {
		String code = "Map<<map-key-type>, <output-type>> output_<rdd-name> = reduceEmits.collectAsMap();\n";
		
		int id = 1;
		for(Variable var : ext.outputVars){
			if(var.varType == type){
				if(type.startsWith("java.util.Map")){
					if(ext.outVarCount > 1)
						code += "for(<map-key-type> output_<rdd-name>_k : output_<rdd-name>.keySet()){\n\t" +
									"if(output_<rdd-name>_k._1 == " + id + ") " +
									var.varName+".put(output_<rdd-name>_k._2, output_<rdd-name>.get(output_<rdd-name>_k)._2);\n" + 
								"}\n";
					else
						code += var.varName+" = output_<rdd-name>;\n";
				}
				else if(type.startsWith("java.util.List")){
					code += "for(int "+var.varName+"_ind = 0; "+var.varName+"_ind < "+var.varName+".size(); "+var.varName+"_ind++){\n\t"
							+ var.varName+".set("+var.varName+"_ind, output_<rdd-name>.get(new Tuple2("+id+","+var.varName+"_ind);\n"
									+ "};\n";
				}
				else{
					code += var.varName + " = output_<rdd-name>.get("+id+")._2;\n";
				}
					
				id++;
			}
		}
		
		return code.substring(0,code.length()-1);
	}
	
	private String generateReduceExps(MyWhileExt ext) {
		int id = 1;
		String code = "";
		Map<String, String> reduceExps = ext.verifiedReduceExps.get(ext.selectedSolutionIndex);
		for(Variable var : ext.outputVars){
			if(ext.outVarCount > 1)
				code += "if(val1._1 == "+id+"){\n\t\treturn new Tuple2(val1._1,"+ reduceExps.get(var.varName).replaceAll("val1", "val1._2").replaceAll("val2", "val2._2") + ");\n\t}\n\t";
			else
				code += "return "+ reduceExps.get(var.varName) + ";";
			id++;
		}
		if(ext.outVarCount > 1)
			return code + "return null;";
		else
			return code;
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished generate code complier pass *************");
	}
}
