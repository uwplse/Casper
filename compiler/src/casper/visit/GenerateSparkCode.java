package casper.visit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.JavaLibModel.SketchCall;
import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.extension.MyWhileExt.Variable;
import polyglot.ast.Import;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.While;
import polyglot.visit.NodeVisitor;

public class GenerateSparkCode extends NodeVisitor{
	boolean debug;
	NodeFactory nf;
	int id;
	boolean first;
	
	public GenerateSparkCode(NodeFactory nodeFactory){
		this.debug = false;
		this.nf = nodeFactory;
		this.id = 0;
		this.first = true;
	}
	
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
				
				Set<String> handledTypes = new HashSet<String>();
				
				int typeid = 0;
				for(Variable var : ext.outputVars){
					if(!handledTypes.contains(var.varType)){
						handledTypes.add(var.varType);
						
						String template = "";
						
						try {
							template = new String(Files.readAllBytes(Paths.get("templates/spark_skeleton.txt")), StandardCharsets.UTF_8);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						String mapEmits = generateMapEmits(ext);
						String createRDD = generateCreateRDD(ext);
						String inputDataType = generateInputDataType(ext);
						String inputDataName = ext.inputDataCollections.get(0).name;
						String lcName = ext.loopCounters.get(0).varName;
						String rddName = "rdd_"+typeid+"_"+id;
						String reconOutput = generateOutputReconstruction(ext,var.varType);
						String dupInputVars = generateDuplicateVarInit(ext);
						
						template = template.replace("<reconstruct-output>", reconOutput);
						template = template.replace("<create-rdd>", createRDD);
						template = template.replace("<rdd-name>", rddName);
						template = template.replace("<map-key-type>", getSparkType(ext.mapKeyType));
						template = template.replace("<output-type>", getSparkType(var.varType));
						template = template.replace("<reduce-exp>", ext.reduceExp);
						template = template.replace("<map-emits>", mapEmits);
						template = template.replace("<input-type>", inputDataType);
						template = template.replace("<input-name>", inputDataName+"_"+lcName);
						template = template.replace(inputDataName+"["+lcName+"]", inputDataName+"_"+lcName);
						
						for(Variable inVar : ext.inputVars){
							if(!ext.outputVars.contains(inVar) && inVar.category != Variable.ARRAY_ACCESS)
								template = template.replaceAll("\\b"+inVar.varName+"\\b",inVar.varName+"_final");
						}
						
						template = template.replace("<duplicate-input-vars>", dupInputVars);
						
						n = nf.Eval(n.position(), nf.ExprFromQualifiedName(n.position(), template.substring(0,template.length()-2)));
						
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
									+ "import java.util.Map;\n";
				
				n = nf.Import(n.position(), ((Import) n).kind(), imports+n.toString());
				first = !first;
			}
		}
       
		return n;
	}
   
	private String generateDuplicateVarInit(MyWhileExt ext) {
		String code = "";
		for(Variable var : ext.inputVars){
			if(!ext.outputVars.contains(var) && var.category != Variable.ARRAY_ACCESS)
				code += "final " + var.varType + " " + var.varName + "_final = "+var.varNameOrig+";\n";
		}
		return code;
	}

	private String generateOutputReconstruction(MyWhileExt ext, String type) {
		String code = "Map<<map-key-type>, <output-type>> output_<rdd-name> = reduceEmits.collectAsMap();\n";
		
		int id = 0;
		for(Variable var : ext.outputVars){
			if(var.varType == type){
				if(type.startsWith("java.util.Map")){
					code += "for(<map-key-type> output_<rdd-name>_k : output_<rdd-name>.keySet()){\n\t" +
								var.varName+".put(output_<rdd-name>_k._2, output_<rdd-name>.get(output_<rdd-name>_k));\n" + 
							"};\n";
				}
				else if(type.startsWith("java.util.List")){
					code += "for(int "+var.varName+"_ind = 0; "+var.varName+"_ind < "+var.varName+".size(); "+var.varName+"_ind++){\n\t"
							+ var.varName+".set("+var.varName+"_ind, output_<rdd-name>.get(new Tuple2("+id+","+var.varName+"_ind);\n"
									+ "};\n";
				}
				else{
					code += var.varName + " = output_<rdd-name>.get("+id+");\n";
				}
					
				id++;
			}
		}
		
		return code.substring(0,code.length()-1);
	}

	private String generateInputDataType(MyWhileExt ext) {
		String inputDataName = ext.inputDataCollections.get(0).name;
		for(Variable var : ext.inputVars){
			if(var.varName.equals(inputDataName)){
				return var.getRDDType();
			}
		}
		return ext.inputDataCollections.get(0).type;
	}

	private String generateCreateRDD(MyWhileExt ext) {
		String code = "";
		
		code += "JavaRDD<<input-type>> <rdd-name> = sc.parallelize("+ext.inputDataCollections.get(0).name+");";
		
		return code;
	}

	private String generateMapEmits(MyWhileExt ext) {
		String emits = "";
		for(GenerateScaffold.KvPair kvp : ext.mapEmits){
			// Fix function calls
			for(SketchCall call : ext.methodOperators){
				Pattern r = Pattern.compile("^("+call.name+")\\((..*)\\)$");
				Matcher m;
				
				m = r.matcher(kvp.key);
				if(m.find()){
					System.err.println("key:" + m);
				}
				
				m = r.matcher(kvp.key2);
				if(m.find()){
					System.err.println("key2:" + m);
				}
				
				m = r.matcher(kvp.value);
				if(m.find()){
					if(call.target.equals("first-arg")){
						String target = m.group(2).substring(0, m.group(2).indexOf(","));
						String args = m.group(2).substring(m.group(2).indexOf(",")+1, m.group(2).length());
						kvp.value = kvp.value.replace(m.group(0), target+"."+call.nameOrig+"("+args+")");
					}
					else{
						String args = m.group(2);
						kvp.value = kvp.value.replace(m.group(0), call.nameOrig+"("+args+")");
					}
				}
			}
			if(kvp.key2 == ""){
				emits += "emits.add(new Tuple2("+kvp.key+","+kvp.value+"));\n";
			}
			else{
				emits += "emits.add(new Tuple2(new Tuple2("+kvp.key+","+kvp.key2+"), "+kvp.value+"));\n";
			}
		}
		return emits;
	}

	private CharSequence getSparkType(String varType) {
		switch(varType){
		case "int":
			return "Integer";
		case "boolean":
			return "Boolean";
		case "(int,int)":
			return "Tuple2<Integer,Integer>";
		case "(int,string)":
			return "Tuple2<Integer,String>";
		case "int[]":
			return "Integer";
		default:
			String targetType = varType;
			String templateType = varType;
			int end = targetType.indexOf('<');
			if(end != -1){
				targetType = targetType.substring(0, end);
				
				switch(targetType){
					case "java.util.List":
					case "java.util.ArrayList":
						templateType = templateType.substring(end+1,templateType.length()-1);
						return templateType;
					case "java.util.Map":
						templateType = templateType.substring(end+1,templateType.length()-1);
	    				String[] subTypes = templateType.split(",");
	    				switch(subTypes[0]){
	        				case "java.lang.Integer":
	        				case "java.lang.String":
	        				case "java.lang.Double":
	        				case "java.lang.Float":
	        				case "java.lang.Long":
	        				case "java.lang.Short":
	        				case "java.lang.Byte":
	        				case "java.lang.BigInteger":
	        					return subTypes[1];
	        				default:
	        					return varType;
	    				}
					default:
						String[] components = varType.split("\\.");
		        		return components[components.length-1];
				}
			}
			
			return varType;
		}
	}

	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished generate code complier pass *************");
	}
}
