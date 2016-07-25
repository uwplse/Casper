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

import casper.types.ArrayAccessNode;
import casper.types.ArrayUpdateNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Receiver;

public class JavaLibModel {
	
	private static boolean debug = false;
	
	// A class used to encapsulate a function call in Sketch.
	// SketchCall objects are used to generate sketch code.
	public static class SketchCall{
		public String name;
		public String target;
		public List<Expr> args = new ArrayList<Expr>();
	}
	
	// Extract all expressions being read by this function call
	static public List<Node> extractReads(Call exp){
		// Get the container class
		String targetType = exp.target().type().toString();
		
		// Is the container class a generic?
		int end = targetType.indexOf('<');
		if(end != -1)
			// It is, drop the generic part
			targetType = targetType.substring(0, end);
		
		List<Node> reads = new ArrayList<Node>();
		
		// Add reads based on container and method name
		// This 'knowledge' is meant to be input manually
		if(targetType.equals("java.lang.String")){
			if(exp.id().toString().equals("equals")){
				reads.addAll(exp.arguments());
				reads.add(exp.target());
			}
			else if(exp.id().toString().equals("split")){
				reads.add(exp.target());
			}
		}
		else if(targetType.equals("java.util.List")){
			if(exp.id().toString().equals("size")){
				reads.add(exp.target());
			}
			else if(exp.id().toString().equals("get")){
				reads.add(exp.target());
				reads.addAll(exp.arguments());
			}
			else if(exp.id().toString().equals("add")){
				reads.addAll(exp.arguments());
			}
		}
		else if(targetType.equals("java.util.Map")){
			if(exp.id().toString().equals("get")){
				reads.add(exp.target());
				reads.addAll(exp.arguments());
			}
			else if(exp.id().toString().equals("put")){
				reads.addAll(exp.arguments());
			}
		}
		
		return reads;
	}
	
	// Extract all expressions being modified by this function call
	static public List<Node> extractWrites(Call exp){
		String targetType = exp.target().type().toString();
		int end = targetType.indexOf('<');
		if(end != -1)
			targetType = targetType.substring(0, end);
		
		List<Node> writes = new ArrayList<Node>();
		
		// Add writes based on container and method name
		// This 'knowledge' is meant to be input manually
		if(targetType.equals("java.lang.String")){
			if(exp.id().toString().equals("equals")){
				return writes;
			}
			else if(exp.id().toString().equals("split")){
				return writes;
			}
		}
		else if(targetType.equals("java.util.List")){
			if(exp.id().toString().equals("size")){
				return writes;
			}
			else if(exp.id().toString().equals("get")){
				return writes;
			}
			else if(exp.id().toString().equals("add")){
				writes.add(exp.target());
			}
		}
		else if(targetType.equals("java.util.Map")){
			if(exp.id().toString().equals("get")){
				return writes;
			}
			else if(exp.id().toString().equals("put")){
				writes.add(exp.target());
			}
		}
		
		return writes;
	}
	
	// This method is used to judge whether we
	// 'handle' a certain method or not
	static public boolean recognizes(Call exp){
		String targetType = exp.target().type().toString();
		int end = targetType.indexOf('<');
		if(end != -1)
			targetType = targetType.substring(0, end);
		
		if(targetType.equals("java.lang.String")){
			if(exp.id().toString().equals("equals"))
				return true;
			else if(exp.id().toString().equals("split"))
				// return true
				return false;
		}
		else if(targetType.equals("java.util.List")){
			if(exp.id().toString().equals("size"))
				return true;
			else if(exp.id().toString().equals("get"))
				return true;
			else if(exp.id().toString().equals("add"))
				return true;
		}
		else if(targetType.equals("java.util.Map")){
			if(exp.id().toString().equals("get"))
				return true;
			else if(exp.id().toString().equals("put"))
				return true;
		}
		
		return false;
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
			
		// Translate based on container type and method
		// name. Translation is hardcoded by the user.
		if(targetType.equals("java.lang.String")){
			if(exp.id().toString().equals("equals")){
				res.name = "str_equal";
				res.target = exp.target().toString();
				res.args.addAll(exp.arguments());
				return res;
			}
			else if(exp.id().toString().equals("split")){
				return res;
			}
		}
		else if(targetType.equals("java.util.List")){
			if(exp.id().toString().equals("size")){
				res.name = templateType + "_list_size";
				res.target = exp.target().toString();
				return res;
			}
			else if(exp.id().toString().equals("get")){
				res.name = templateType + "_list_get";
				res.target = exp.target().toString();
				res.args.addAll(exp.arguments());
				return res;
			}
			else if(exp.id().toString().equals("add"));{
				res.name = templateType + "_list_add";
				res.target = exp.target().toString();
				res.args.addAll(exp.arguments());
				return res;
			}
		}
		else if(targetType.equals("java.util.Map")){
			if(exp.id().toString().equals("get")){
				res.name = templateType + "_map_get";
				res.target = exp.target().toString();
				res.args.addAll(exp.arguments());
				return res;
			}
			else if(exp.id().toString().equals("put")){
				res.name = templateType + "_map_put";
				res.target = exp.target().toString();
				res.args.addAll(exp.arguments());
				return res;
			}
		}
		
		return res;
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
		    					return currVerifCondition.replaceAll(target,upd);
		    				default:
		    					if(debug){
		    						System.out.println("Currently not handling Map of custom types");
		    					}
		    						
		    					break;
	    				}
						break;
					default:
						if(debug){
							System.out.println("Method " + id + " of java.util.Map not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.util.List":
				switch(id){
					case "get":
						List<Expr> args = c.arguments();
						CustomASTNode acc = new ArrayAccessNode(target+"["+args.get(0)+"]",new IdentifierNode(target),CustomASTNode.convertToAST(args.get(0)));
						return currVerifCondition.replaceAll(target,acc);
					default:
						if(debug){
							System.out.println("Method " + id + " of java.util.List not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			default:
				if(debug){
					System.out.println("Container type " + targetTypeMain + " not currently supported. Please extend the JavaLibModel.");
				}
		}
		
		return null;
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
		    						System.out.println("Currently not handling Map of type: " + targetSubTypes[0]);
		    					}
		    					break;
	    				}
						break;
					default:
						if(debug){
							System.out.println("Method " + id + " of java.util.Map not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			case "java.util.List":
				switch(id){
					case "get":
						List<Expr> args = c.arguments();
						return new ArrayAccessNode(target+"["+args.get(0)+"]",new IdentifierNode(target),CustomASTNode.convertToAST(args.get(0)));
					default:
						if(debug){
							System.out.println("Method " + id + " of java.util.List not currently supported. Please extend the JavaLibModel.");
						}
						break;
				}
				break;
			default:
				if(debug){
					System.out.println("Container type " + targetTypeMain + " not currently supported. Please extend the JavaLibModel.");
				}
		}
		
		return new IdentifierNode("");
	}
}
