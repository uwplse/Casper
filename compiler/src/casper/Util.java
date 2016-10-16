package casper;

import java.util.List;

import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.ArrayUpdateNode;
import casper.types.ConditionalNode;
import casper.types.CustomASTNode;
import polyglot.ast.ArrayAccess;
import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Branch;
import polyglot.ast.Call;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.If;
import polyglot.ast.LocalDecl;
import polyglot.ast.Stmt;

public class Util {
	public static String getSketchTypeFromRaw(String original){
		switch(original){
			case "boolean":
				return "bit";
			case "char":
				return "char";
			case "short":
			case "byte":
			case "int":
			case "long":
			case "float":
			case "double":
			case "String":
			case "Integer":
			case "Double":
			case "Float":
				return "int";
			case "boolean[]":
				return "bit[" + Configuration.arraySizeBound + "]";
			case "char[]":
				return "char[" + Configuration.arraySizeBound + "]";
			case "short[]":
			case "byte[]":
			case "int[]":
			case "long[]":
			case "double[]":
			case "float[]":
			case "String[]":
			case "Integer[]":
			case "Double[]":
			case "Float[]":
				return "int[" + Configuration.arraySizeBound + "]";
			default:
				String[] components = original.split("\\.");
				String dataType = components[components.length-1];
				if(dataType.contains("[]")){
					dataType.replace("[]", "["+Configuration.arraySizeBound+"]");
				}
				return dataType;
		}
	}
	
	public static String getPrimitiveTypeFromRaw(String original){
		switch(original){
			case "boolean":
			case "char":
			case "short":
			case "byte":
			case "int":
			case "long":
			case "float":
			case "double":
			case "String":
				return original;
			case "boolean[]":
			case "char[]":
			case "short[]":
			case "byte[]":
			case "int[]":
			case "long[]":
			case "double[]":
			case "float[]":
			case "String[]":
				return original.replace("[]", "");
			case "Integer":
				return "int";
			case "Double":
				return "double";
			case "Float":
				return "float";
			case "Integer[]":
				return "int";
			case "Double[]":
				return "ddouble";
			case "Float[]":
				return "float";
			default:
				String[] components = original.split("\\.");
				String dataType = components[components.length-1];
				if(dataType.contains("[]")){
					dataType.replace("[]", "");
				}
				return dataType;
		}
	}
	
	// Returns the type with the higher rank from the two
	// Used for proper casting
	public static String getHigherType(String type1, String type2) {
		type1 = getSketchTypeFromRaw(type1);
		type2 = getSketchTypeFromRaw(type2);
		
		int t1r = getTypeRank(type1);
		int t2r = getTypeRank(type2);
		
		return (t1r > t2r? type1 : type2);
	}

	// Cast ranks, to know how to cast without loss of data
	public static int getTypeRank(String type) {
		switch(type){
		case "bit":
			return 0;
		case "char":
			return 1;
		case "int":
			return 2;
		case "float":
		case "double":
			return 3;
		default:
			// should not happen for now
			return -1;
		}
	}
	
	public static final int OBJECT = 0;
	public static final int PRIMITIVE = 1;
	public static final int ARRAY = 2;
	public static final int OBJECT_ARRAY = 3;

	public static int getTypeClass(String type) {
		switch(type){
		case "bit":
		case "char":
		case "int":
		case "float":
		case "double":
		case "String":
		case "Integer":
		case "Double":
			return PRIMITIVE;
		default:
			if(type.endsWith("["+Configuration.arraySizeBound+"]")){
				if(getTypeClass(type.substring(0, type.lastIndexOf("["+Configuration.arraySizeBound+"]"))) == PRIMITIVE ||
				   getTypeClass(type.substring(0, type.lastIndexOf("["+Configuration.arraySizeBound+"]"))) == ARRAY	){
					return ARRAY;
				}
				else {
					return OBJECT_ARRAY;
				}
			}
			else{
				return OBJECT;
			}
		}
	}
	
	// Convert abbreviation to proper sketch type
	public static String convertAbbrToType(String abbr) {
		switch(abbr){
		case "int":
			return "int";
		case "str":
			return "int";
		default:
			return "bug";
		}
	}
	
	// Does the statement contain a "break"?
   	public static boolean containsBreak(Stmt st){
   		if(st == null)
   			return false;
   		if(st instanceof Branch)
   			return true;
   		else if(st instanceof Block){
   			List<Stmt> stmts = ((Block) st).statements();
   			for(Stmt stmt : stmts){
   				if(containsBreak(stmt))
   					return true;
   			}
   			return false;
   		}
   		else
   			return false;
   	}

   	public static final int ARITHMETIC_OP = 1;
   	public static final int RELATIONAL_OP = 2;
   	public static final int BITVECTOR_OP = 3;
   	public static final int UNKNOWN_OP = 4;
   	
	public static int operatorType(String op) {
		switch(op){
		case "+":
		case "-":
		case "*":
		case "/":
		case "%":
			return ARITHMETIC_OP;
		case ">>":
		case "<<":
		case ">>>":
		case "&":
		case "^":
		case "|":
			return BITVECTOR_OP;
		case "<":
		case ">":
		case "<=":
		case ">=":
		case "instanceof":
		case "==":
		case "!=":
		case "&&":
		case "||":
		case "!":
			return RELATIONAL_OP;
		default:
			return UNKNOWN_OP;
		}
	}

	public static boolean isAggOperator(String op) {
		switch(op){
		case "+":
		case "-":
		case "*":
		case "/":
		case "%":
		case ">>":
		case "<<":
		case ">>>":
		case "<":
		case ">":
		case "<=":
		case ">=":
		case "&":
		case "^":
		case "|":
		case "&&":
		case "||":
			return true;
		default:
			return false;
		}
	}
	
	public static final int INT_ONLY = 1;
	public static final int BIT_ONLY = 2;
	public static final int VEC_INT = 3;
	public static final int VEC_ONLY = 4;
	public static final int ALL_TYPES = 5;
	
	public static int operandTypes(String op) {
		switch(op){
		case "&&":
		case "||":
		case "!":
			return BIT_ONLY;
		case "<":
		case ">":
		case "<=":
		case ">=":
		case "==":
		case "!=":
			return INT_ONLY;
		case ">>":
		case ">>>":
		case "<<":
			return VEC_INT;
		case "&":
		case "^":
		case "|":
			return VEC_ONLY;
		default:
			return ALL_TYPES;
		}
	}
	
	public static String getDafnyType(String original){
		final int arrBnd = Configuration.arraySizeBound;
		if(original.equals("bit")){
			return "bool";
		}
		else if(original.equals("char")){
			return "char";
		}
		else if(original.equals("int")){
			return "int";
		}
		else if(original.equals("String")){
			return "int";
		}
		else if(original.equals("float")){
			return "real";
		}
		else if(original.equals("double")){
			return "real";
		}
		else if(original.equals("bit["+Configuration.arraySizeBound+"]")){
			return "seq<bool>";
		}
		else if(original.equals("char["+Configuration.arraySizeBound+"]")){
			return "int<int>";
		}
		else if(original.equals("float["+Configuration.arraySizeBound+"]")){
			return "seq<real>";
		}
		else if(original.equals("duble["+Configuration.arraySizeBound+"]")){
			return "seq<real>";
		}
		else if(original.equals("String["+Configuration.arraySizeBound+"]")){
			return "seq<int>";
		}
		else{
			return original;
		}
	}

	public static int getOpClassForType(String type) {
		switch(type){
		case "int":
			return ARITHMETIC_OP;
		case "bit":
			return RELATIONAL_OP;
		case "bit[32]":
			return BITVECTOR_OP;
		default:
			return UNKNOWN_OP;
		}
	}

	public static int compatibleTypes(String type, String type2) {
		switch(type){
			case "int":
				switch(type2){
					case "short":
					case "byte":
					case "int":
					case "long":
					case "float":
					case "double":
					case "Integer":
					case "Double":
					case "Float":
						return 1;
					case "short[]":
					case "byte[]":
					case "int[]":
					case "long[]":
					case "float[]":
					case "double[]":
					case "Integer[]":
					case "Double[]":
					case "Float[]":
						return 2;
					default:
						return 0;
				}
			case "bit":
				switch(type2){
					default:
						return 0; // TODO: Implement this.
				}
			case "String":
				switch(type2){
					case "String":
						return 1;
					case "String[]":
						return 2;
					default:
						return 0; // TODO: Implement this.
				}
			default:
				return 0;
		}
	}
	
	static public String join(List<String> list, String conjunction)
	{
	   StringBuilder sb = new StringBuilder();
	   boolean first = true;
	   for (String item : list)
	   {
	      if (first)
	         first = false;
	      else
	         sb.append(conjunction);
	      sb.append(item);
	   }
	   return sb.toString();
	}

	public static String getOperatorFromExp(String reduceValue) {
		if(reduceValue.contains("+")){
			return "+";
		}
		else if(reduceValue.contains("-")){
			return "-";
		}
		else if(reduceValue.contains("*")){
			return "*";
		}
		else if(reduceValue.contains("/")){
			return "/";
		}
		else if(reduceValue.contains("%")){
			return "%";
		}
		else if(reduceValue.contains(">>>")){
			return ">>>";
		}
		else if(reduceValue.contains(">>")){
			return ">>";
		}
		else if(reduceValue.contains("<<")){
			return "<<";
		}
		else if(reduceValue.contains("<=")){
			return "<=";
		}
		else if(reduceValue.contains(">=")){
			return ">=";
		}
		else if(reduceValue.contains(">")){
			return ">";
		}
		else if(reduceValue.contains("<")){
			return "<";
		}
		else if(reduceValue.contains("==")){
			return "==";
		}
		else if(reduceValue.contains("!=")){
			return "!=";
		}
		else if(reduceValue.contains("!")){
			return "!";
		}
		else if(reduceValue.contains("||")){
			return "||";
		}
		else if(reduceValue.contains("&&")){
			return "&&";
		}
		else if(reduceValue.contains("&")){
			return "&";
		}
		else if(reduceValue.contains("|")){
			return "|";
		}
		else if(reduceValue.contains("^")){
			return "^";
		}
		else{
			return "unknownOP";
		}
	}

	public static boolean isAssociative(String op) {
		switch(op){
		case "+":
		case "*":
		case "&&":
		case "||":
		case "&":
		case "|":
		case "==":
		case "^":
			return true;
		default:
			return false;
		}
	}

	public static String getInitVal(String type) {
		switch(type){
			case "double":
				return "0";
			case "java.lang.Integer":
				
			default:
				return "0";
		}
	}
	
	public static CustomASTNode generatePreCondition(String type, Stmt body, CustomASTNode postCondition, MyWhileExt loopExt, boolean debug){
		CustomASTNode currVerifCondition = postCondition;
		
		if(body instanceof Block){
			List<Stmt> statements = ((Block) body).statements();
			
			for(int i=statements.size()-1; i>=0; i--){
				// Get the statement
				Stmt currStatement = statements.get(i);
				
				if(debug){
					System.err.println("---------------------------");
					System.err.println(currVerifCondition);
					System.err.println(currStatement);
					System.err.println("---------------------------");
				}
				
				// Get extension of statement
				MyStmtExt ext = (MyStmtExt) JavaExt.ext(currStatement);
					
				if(currStatement instanceof Eval){
					Expr expr = ((Eval) currStatement).expr();
					
					if(expr instanceof Assign){
						// Save post-condition
						ext.postConditions.put(type,currVerifCondition);
						
						// Derive pre-condition
						Expr lhs = ((Assign)expr).left();
						Expr rhs = ((Assign)expr).right();
						
						CustomASTNode rhsAST = CustomASTNode.convertToAST(rhs);
						loopExt.constCount = rhsAST.convertConstToIDs(loopExt.constMapping,loopExt.constCount);
						
						if(lhs instanceof ArrayAccess){
							currVerifCondition = currVerifCondition.replaceAll(((ArrayAccess) lhs).array().toString(), new ArrayUpdateNode(CustomASTNode.convertToAST(((ArrayAccess) lhs).array()),CustomASTNode.convertToAST(((ArrayAccess) lhs).index()),rhsAST));
						}
						else {
							currVerifCondition = currVerifCondition.replaceAll(lhs.toString(), rhsAST);
						}
						
						// Save pre-condition
						ext.preConditions.put(type,currVerifCondition);
					}
					else if(expr instanceof Call){
						// Save post-condition
						ext.postConditions.put(type,currVerifCondition);
						
						currVerifCondition = JavaLibModel.updatePreCondition((Call)expr,currVerifCondition);
						
						// Save pre-condition
						ext.preConditions.put(type,currVerifCondition);
					}
					else{
						System.err.println("Unexpected Eval type: " + expr.getClass() + " :::: " + expr);
					}
				}
				else if(currStatement instanceof LocalDecl){
					// Save post-condition
					ext.postConditions.put(type,currVerifCondition);

					// Derive pre-condition
					String lhs = ((LocalDecl)currStatement).name();
					Expr rhs = ((LocalDecl)currStatement).init();
					
					CustomASTNode rhsAST = CustomASTNode.convertToAST(rhs);

					loopExt.constCount = rhsAST.convertConstToIDs(loopExt.constMapping,loopExt.constCount);
 
					currVerifCondition = currVerifCondition.replaceAll(lhs, rhsAST);
					
					// Save pre-condition
					ext.preConditions.put(type,currVerifCondition);
				}
				else if(currStatement instanceof If){
					// Save post-condition
					ext.postConditions.put(type,currVerifCondition);
					
					// Derive pre-condition
					Stmt cons = ((If) currStatement).consequent();
					Stmt alt = ((If) currStatement).alternative();
					Expr cond = ((If) currStatement).cond();
					
					CustomASTNode loopCond = CustomASTNode.convertToAST(cond);
					loopExt.constCount = loopCond.convertConstToIDs(loopExt.constMapping,loopExt.constCount);
					
					CustomASTNode verifCondCons = generatePreCondition(type,cons,currVerifCondition,loopExt,debug);
					
					CustomASTNode verifCondAlt;
					if(alt != null)
						verifCondAlt = generatePreCondition(type,alt,currVerifCondition,loopExt,debug);
					else
						verifCondAlt = currVerifCondition;
					
					if(!verifCondCons.toString().equals(verifCondAlt.toString()))
						currVerifCondition = new ConditionalNode(loopCond,verifCondCons,verifCondAlt);
					
					// Save pre-condition
					ext.preConditions.put(type, currVerifCondition);
				}
				else if(currStatement instanceof Block){
					// Save post-condition
					ext.postConditions.put(type,currVerifCondition);
					
					// Derive pre-condition
					currVerifCondition = generatePreCondition(type,currStatement,currVerifCondition,loopExt,debug);
					
					// Save pre-condition
					ext.preConditions.put(type, currVerifCondition);
				}
			}
		}
		else{
			// This should never be the case
			System.err.println("Error: body was not an instance of Block. ("+ body.getClass() +")");
		}
		
		return currVerifCondition;
	}

	public static String reducerType(String outputType) {
		return outputType.replace("["+Configuration.arraySizeBound+"]", "");
	}
}
