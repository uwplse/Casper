/* 
 * This class holds information about common java library functions.
 * It is used to identify how a library function affects an argument
 * passed to it i.e Read or Write.
 *    
 * - Maaz
 */

package casper;

import java.util.ArrayList;
import java.util.List;

import casper.extension.MyWhileExt;
import casper.types.ArrayAccessNode;
import casper.types.ArrayUpdateNode;
import casper.types.BinaryOperatorNode;
import casper.types.CallNode;
import casper.types.ConstantNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.types.SequenceNode;
import casper.types.Variable;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.Node;

public class JavaLibModel {
	
	private static boolean debug = false;
	
	// Determines if the provided library method is supported
	static public boolean recognizes(Call exp){
		String targetType = exp.target().type().toString();
		int end = targetType.indexOf('<');
		if(end != -1)
			targetType = targetType.substring(0, end);
		
		switch(targetType){
			case "java.lang.String":
				switch(exp.id().toString()){
					case "equals":
						return true;
					default:
						return false;
				}
			case "java.util.List":
				switch(exp.id().toString()){
					case "size":
					case "get":
					case "set":
						return true;
					default:
						return false;
				}
			case "java.util.Set":
				switch(exp.id().toString()){
					case "size":	
					case "add":
						return true;
					default:
						return false;
				}
			case "java.util.Map":
				switch(exp.id().toString()){
					case "get":
					case "put":
					case "containsKey":
						return true;
					default:
						return false;
				}
			case "java.lang.Math":
				switch(exp.id().toString()){
					case "pow":
					case "sqrt":
					case "max":
					case "min":
					case "abs":
					case "ceil":
						return true;
					default:
						return false;
				}
			default:
				if(debug){
					System.err.println("Unsupported library funcntion: "+ exp);
				}
				return false;
		}
	}
	
	// Extract all loop counters from the function call
	public static Node extractLoopCounters(Call exp) {
		// Get the container class
		String targetType = exp.target().type().toString();
		
		// Is the container class a generic?
		int end = targetType.indexOf('<');
		if(end != -1)
			// It is, drop the generic part
			targetType = targetType.substring(0, end);
		
		switch(targetType){
			case "java.util.List":
				switch(exp.id().toString()){
					case "get":
						return exp.arguments().get(0);
					default:
						return null;
				}
			default:
				return null;
		}
	}
	
	// Extract all expressions being read from function call
	static public List<Node> extractReads(Call exp, MyWhileExt ext){
		// Get the container class
		String targetType = exp.target().type().toString();
		
		// Is the container class a generic?
		int end = targetType.indexOf('<');
		if(end != -1)
			// It is, drop the generic part
			targetType = targetType.substring(0, end);
		
		List<Node> reads = new ArrayList<Node>();
		
		switch(targetType){
			case "java.lang.String":
				switch(exp.id().toString()){
					case "equals":
						reads.add(exp.target());
						reads.addAll(exp.arguments());
						break;
					default:
						break;
				}
				break;
			case "java.lang.Math":
				switch(exp.id().toString()){
					case "pow":
						reads.addAll(exp.arguments());
						break;
					case "sqrt":
						reads.addAll(exp.arguments());
						break;
					case "max":
						reads.addAll(exp.arguments());
						break;
					case "min":
						reads.addAll(exp.arguments());
						break;
					case "abs":
						reads.addAll(exp.arguments());
						break;
					case "ceil":
						reads.addAll(exp.arguments());
						break;
					default:
						break;
				}
			case "java.util.List":
				switch(exp.id().toString()){
					case "size":
						ext.saveInputVariable(exp.target().toString(), exp.target().type().toString(), Variable.CONST_ARRAY_ACCESS);
						break;
					case "get":
						for(Expr arg : exp.arguments()){
							if(arg instanceof Local){
								reads.add(arg);
								ext.saveInputVariable(exp.target().toString(), exp.target().type().toString(), Variable.ARRAY_ACCESS);
							}
							else if(arg instanceof Lit){
								ext.saveInputVariable(exp.target().toString(), exp.target().type().toString(), Variable.CONST_ARRAY_ACCESS);
							}
						}
						break;
					case "set":
						reads.addAll(exp.arguments());
						break;
					default:
						break;
				}
				break;
			case "java.util.Set":
					switch(exp.id().toString()){
						case "size":
							ext.saveInputVariable(exp.target().toString(), exp.target().type().toString(), Variable.CONST_ARRAY_ACCESS);
							break;
						case "add":
							reads.addAll(exp.arguments());
							break;
						default:
							break;
					}
					break;
			case "java.util.Map":
				switch(exp.id().toString()){
					case "get":
						reads.add(exp.target());
						reads.addAll(exp.arguments());
						break;
					case "put":
						reads.addAll(exp.arguments());
						break;
					case "containsKey":
						reads.add(exp.target());
						reads.addAll(exp.arguments());
						break;
					default:
						break;
				}
				break;
			default:
				break;
		}
		
		return reads;
	}
	
	// Extract all expressions being modified by this function call
	static public List<Node> extractWrites(Call exp, MyWhileExt ext){
		String targetType = exp.target().type().toString();
		int end = targetType.indexOf('<');
		if(end != -1)
			targetType = targetType.substring(0, end);
		
		List<Node> writes = new ArrayList<Node>();
		
		switch(targetType){
			case "java.lang.String":
				switch(exp.id().toString()){
					case "equals":
						break;
					default:
						break;
				}
				break;
			case "java.lang.Math":
				switch(exp.id().toString()){
					case "pow":
					case "sqrt":
					case "max":
					case "min":
					case "abs":
					case "ceil":
						break;
					default:
						break;
				}
			case "java.util.Map":
				switch(exp.id().toString()){
					case "get":
					case "containsKey":
						break;
					case "put":
						if(exp.arguments().get(0) instanceof Local || exp.arguments().get(0) instanceof Lit){
							writes.add(exp.target());
						}
						/*else if(exp.arguments().get(0) instanceof Lit){
							System.err.println(exp.arguments().get(0) instanceof Lit);
							ext.saveOutputVariable(exp.target().toString(), exp.target().type().toString(), Variable.CONST_ARRAY_ACCESS);
						}*/
						break;
					default:
						break;
				}
				break;
			case "java.util.List":
				switch(exp.id().toString()){
					case "size":
						break;
					case "get":
						break;
					case "set":
						if(exp.arguments().get(0) instanceof Local){
							writes.add(exp.target());
						}
						else if(exp.arguments().get(0) instanceof Lit){
							ext.saveOutputVariable(exp.target().toString(), exp.target().type().toString(), Variable.CONST_ARRAY_ACCESS);
						}
						break;
					default:
						break;
				}
				break;
			case "java.util.Set":
				switch(exp.id().toString()){
					case "size":
						break;
					case "add":
						writes.add(exp.target());
						break;
					default:
						break;
				}
				break;
			default:
				break;
		}
		
		return writes;
	}
	
	// A class used to encapsulate a function call in Sketch.
	// SketchCall objects are used to generate sketch code.
	public static class SketchCall{
		public String nameOrig;
		public String target;
		public String name;
		public String returnType;
		public List<String> args = new ArrayList<String>();
		public String toString(){ return "["+returnType+", "+name+", "+args+"]"; }
		public boolean equals(Object o){ return name.equals(((SketchCall)o).name) && returnType.equals(((SketchCall)o).returnType) && args.equals(((SketchCall)o).args); }
		public int hashCode(){ return 0; }
		public String resolve(String exp, List<String> argsR) {
			if(argsR.indexOf(exp) >= args.size()){
				return name+"("+casper.Util.join(argsR.subList(0, argsR.size()-1),",")+")";
			}
			return exp;
		}
	}
	
	// This method will translate a Java Call expression
	// in SketchCall.
	static public SketchCall translate(Call exp){
		SketchCall res = new SketchCall();
		
		// Get container type and template type
		String targetType = exp.target().type().toString();
		String templateType = exp.target().type().toString();
		
		// Is it a generic?
		int end = targetType.indexOf('<');
		if(end != -1){
			// Yes, extract container type and generic type
			targetType = targetType.substring(0, end);
			templateType = translateToSketchType(templateType);
		}
			
		switch(targetType){
			case "java.lang.String":
				switch(exp.id().toString()){
					case "equals":
						res.target = "first-arg";
						res.name = "casper_str_equal";
						res.nameOrig = "equals";
						res.args.add(targetType);
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					case "split":
						return res;
					default:
						return res;
				}
			case "java.util.List":
				switch(exp.id().toString()){
					case "size":
					case "get":
					default:
						return res;
				}
			case "java.util.Set":
				switch(exp.id().toString()){
					case "size":
					case "add":
					default:
						return res;
				}
			case "java.util.Map":
				switch(exp.id().toString()){
					case "get":
					case "put":
					case "containsKey":
					default:
						return res;
				}
			case "java.lang.Math":
				switch(exp.id().toString()){
					case "pow":
						res.target = "Math";
						res.name = "casper_math_pow";
						res.nameOrig = "pow";
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					case "sqrt":
						res.target = "Math";
						res.name = "casper_math_sqrt";
						res.nameOrig = "sqrt";
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					case "max":
						res.target = "Math";
						res.name = "casper_math_max";
						res.nameOrig = "max";
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					case "min":
						res.target = "Math";
						res.name = "casper_math_min";
						res.nameOrig = "min";
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					case "abs":
						res.target = "Math";
						res.name = "casper_math_abs";
						res.nameOrig = "abs";
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					case "ceil":
						res.target = "Math";
						res.name = "casper_math_ceil";
						res.nameOrig = "ceil";
						for(Expr arg : exp.arguments()){
							res.args.add(arg.type().toString());
						}
						res.returnType = exp.type().toString();
						return res;
					default:
						return res;
				}
			default:
				return res;
		}
	}
	
	public static CustomASTNode convertToAST(Call c) {
		String target = c.target().toString();
		String targetType = c.target().type().toString();
		String targetTypeMain = targetType;
		if(targetType.contains("<"))
			targetTypeMain = targetType.substring(0,targetType.indexOf("<"));
		
		String id = c.id().toString();
		
		switch(targetTypeMain){
			case "java.util.Map":
				String[] targetSubTypes = targetType.substring(targetType.indexOf("<")+1,targetType.lastIndexOf(">")).split(",");
				switch(id){
					case "get":
						List<Expr> args = c.arguments();
						switch(targetSubTypes[0]){
		    				case "java.lang.Integer":
		    				case "java.lang.String":
		    				case "java.lang.Double":
		    				case "java.lang.Float":
		    				case "java.lang.Long":
		    				case "java.lang.Short":
		    				case "java.lang.Byte":
		    				case "java.lang.BigInteger":
		    					return new ArrayAccessNode("",new IdentifierNode(target),CustomASTNode.convertToAST(args.get(0)));
		    				default:
		    					if(debug){
		    						System.err.println("Currently not handling Map of type: " + targetSubTypes[0]);
		    					}
		    					break;
	    				}
						break;
					case "containsKey":
						List<Expr> args2 = c.arguments();
						switch(targetSubTypes[0]){
		    				case "java.lang.Integer":
		    				case "java.lang.String":
		    				case "java.lang.Double":
		    				case "java.lang.Float":
		    				case "java.lang.Long":
		    				case "java.lang.Short":
		    				case "java.lang.Byte":
		    				case "java.lang.BigInteger":
		    					return new BinaryOperatorNode("!=",new ArrayAccessNode("",new IdentifierNode(target),CustomASTNode.convertToAST(args2.get(0))),new ConstantNode("0",ConstantNode.NULLLIT));
		    				default:
		    					if(debug){
		    						System.err.println("Currently not handling Map of type: " + targetSubTypes[0]);
		    					}
		    					break;
	    				}
						break;
					default:
						if(debug){
							System.err.println("Method " + id + " of java.util.Map not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.util.List":
				switch(id){
					case "get":
						List<Expr> args = c.arguments();
						return new ArrayAccessNode(target+"["+args.get(0)+"]",new IdentifierNode(target),CustomASTNode.convertToAST(args.get(0)));
					case "size":
						return new CallNode(target+".size",new ArrayList<CustomASTNode>());
					default:
						if(debug){
							System.err.println("Method " + id + " of java.util.List not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.util.Set":
				switch(id){
					case "add":
						return new ArrayUpdateNode(new IdentifierNode(target), new IdentifierNode("temp_index_casper"), CustomASTNode.convertToAST(c.arguments().get(0)));
					default:
						if(debug){
							System.err.println("Method " + id + " of java.util.Set not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.lang.String":
				switch(id){
					case "equals":
						ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
						args.add(new IdentifierNode(target));
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						return new CallNode("casper_str_equal",args);
					default:
						if(debug){
							System.err.println("Method " + id + " of java.lang.String not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.lang.Math":
				ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
				switch(id){
					case "pow":
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						args.add(CustomASTNode.convertToAST(c.arguments().get(1)));
						return new CallNode("casper_math_pow",args);
					case "sqrt":
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						return new CallNode("casper_math_sqrt",args);
					case "max":
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						args.add(CustomASTNode.convertToAST(c.arguments().get(1)));
						return new CallNode("casper_math_max",args);
					case "min":
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						args.add(CustomASTNode.convertToAST(c.arguments().get(1)));
						return new CallNode("casper_math_min",args);
					case "abs":
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						return new CallNode("casper_math_abs",args);
					case "ceil":
						args.add(CustomASTNode.convertToAST(c.arguments().get(0)));
						return new CallNode("casper_math_ceil",args);
					default:
						if(debug){
							System.err.println("Method " + id + " of java.lang.Math not currently supported. Please extend the JavaLibModel.");
						}
						break;	
				}
			default:
				if(debug || true){
					System.err.println("Container type " + targetTypeMain + " not currently supported. Please extend the JavaLibModel.");
				}
		}
		
		return new IdentifierNode("");
	}

	// Currently cannot handle data structures containing non primitive types
	public static String translateToSketchType(String templateType) {
		// Get the substring "<...>"
		templateType = templateType.substring(templateType.indexOf("<")+1,templateType.lastIndexOf(">"));
		
		// Extract first generic type i.e first "," occurrence at depth 0
		int depth = 0;
		int i=0; 
		while(i<templateType.length()){
			if(templateType.charAt(i) == ',' && depth == 0){
				break;
			}
			else if(templateType.charAt(i) == '<'){
				depth++;
			}
			else if(templateType.charAt(i) == '>'){
				depth--;
			}
			i++;
		}
		
		// Create a sketch type...format is containertype concat with generic
		// types in original order
		String sketchType = "";
		
		if(i < templateType.length()){
			String p1 = templateType.substring(0, i);
			String p2 = templateType.substring(i+1, templateType.length());
			
			if(p1.equals("java.lang.String")){
				sketchType = "int";
			}
			else if(p1.equals("java.lang.Integer")){
				sketchType = "int";
			}
			
			if(p2.equals("java.lang.String")){
				sketchType += "int";
			}
			else if(p2.equals("java.lang.Integer")){
				sketchType += "int";
			}
		}
		else{
			if(templateType.equals("java.lang.String")){
				sketchType = "int";
			}
			else if(templateType.equals("java.lang.Integer")){
				sketchType = "int";
			}
		}
		
		return sketchType;
	}
	
	public static CustomASTNode updatePreCondition(Call c, CustomASTNode currVerifCondition){
		String target = c.target().toString();
		String targetType = c.target().type().toString();
		String targetTypeMain = targetType;
		if(targetType.contains("<"))
			targetTypeMain = targetType.substring(0,targetType.indexOf("<"));
		
		String id = c.id().toString();
		
		switch(targetTypeMain){
			case "java.util.Map":
				String[] targetSubTypes = targetType.substring(targetType.indexOf("<")+1,targetType.lastIndexOf(">")).split(",");
				switch(id){
					case "put":
						List<Expr> args = c.arguments();
						switch(targetSubTypes[0]){
		    				case "java.lang.Integer":
		    				case "java.lang.String":
		    				case "java.lang.Double":
		    				case "java.lang.Float":
		    				case "java.lang.Long":
		    				case "java.lang.Short":
		    				case "java.lang.Byte":
		    				case "java.lang.BigInteger":
		    					CustomASTNode upd = new ArrayUpdateNode(new IdentifierNode(target),CustomASTNode.convertToAST(args.get(0)),CustomASTNode.convertToAST(args.get(1)));
		    					if(currVerifCondition.toString().equals(target)){
		    						return upd;
		    					}
		    					if(currVerifCondition.contains(target)){
		    						return new SequenceNode(upd,currVerifCondition);
		    					}
		    					else{
		    						return currVerifCondition;
		    					}
		    				default:
		    					if(debug){
		    						System.err.println("Currently not handling Map of custom types");
		    					}
		    						
		    					break;
	    				}
						break;
					default:
						if(debug || true){
							System.err.println("Method " + id + " of java.util.Map not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.util.List":
				switch(id){
					case "set":
						List<Expr> args = c.arguments();
						CustomASTNode acc = new ArrayUpdateNode(new IdentifierNode(target),CustomASTNode.convertToAST(args.get(0)),CustomASTNode.convertToAST(args.get(1)));
						return currVerifCondition.replaceAll(target,acc);
					default:
						if(debug){
							System.err.println("Method " + id + " of java.util.List not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.util.Set":
				switch(id){
					case "add":
						List<Expr> args = c.arguments();
						CustomASTNode acc = new ArrayUpdateNode(new IdentifierNode(target),new IdentifierNode("temp_index_casper"),CustomASTNode.convertToAST(args.get(0)));
						return currVerifCondition.replaceAll(target,acc);
					default:
						if(debug){
							System.err.println("Method " + id + " of java.util.List not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			default:
				if(debug || true){
					System.err.println("Container type " + targetTypeMain + " not currently supported. Please extend the JavaLibModel.");
					System.exit(1);
				}
		}
		
		return null;
	}
}
