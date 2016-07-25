package casper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.visit.GenerateScaffold.SketchVariable;
import polyglot.ast.Block;
import polyglot.ast.Branch;
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
			case "java.lang.String":
			case "java.lang.Integer":
				return "int";
			case "float":
				return "float";
			case "double":
				return "double";
			case "boolean[]":
				return "bit[" + Configuration.arraySizeBound + "]";
				// return "BitArray";
			case "char[]":
				return "char[" + Configuration.arraySizeBound + "]";
				// return "CharArray";
			case "short[]":
			case "byte[]":
			case "int[]":
			case "long[]":
			case "java.lang.String[]":
			case "java.lang.Integer[]":
				return "int[" + Configuration.arraySizeBound + "]";
				// return "IntArray";
			case "float[]":
				return "float[" + Configuration.arraySizeBound + "]";
				// return "FloatArray";
			case "double[]":
				return "double[" + Configuration.arraySizeBound + "]";
				// return "DoubleArray";	
			default:
				String[] components = original.split("\\.");
				String dataType = components[components.length-1];
				if(dataType.contains("[]")){
					dataType.replace("[]", "["+Configuration.arraySizeBound+"]");
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
	
	public static String getSketchType(String original){
		switch(original){
			case "boolean":
				return "bit";
			case "char":
				return "char";
			case "short":
			case "byte":
			case "int":
			case "long":
			case "String":
			case "Integer":
				return "int";
			case "float":
				return "float";
			case "double":
				return "double";
			case "boolean[]":
				// return "BitArray";
				return "bit["+Configuration.arraySizeBound+"]";
			case "char[]":
				// return "CharArray";
				return "char["+Configuration.arraySizeBound+"]";
			case "short[]":
			case "byte[]":
			case "int[]":
			case "long[]":
			case "String[]":
			case "Integer[]":
				// return "IntArray";
				return "int["+Configuration.arraySizeBound+"]";
			case "float[]":
				// return "FloatArray";
				return "float["+Configuration.arraySizeBound+"]";
			case "double[]":
				// return "DoubleArray";
				return "double["+Configuration.arraySizeBound+"]";	
			default:
				return original;
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
			return PRIMITIVE;
		case "bit["+Configuration.arraySizeBound+"]":
		case "char["+Configuration.arraySizeBound+"]":
		case "int["+Configuration.arraySizeBound+"]":
		case "float["+Configuration.arraySizeBound+"]":
		case "double["+Configuration.arraySizeBound+"]":
		case "String["+Configuration.arraySizeBound+"]":
		case "Integer["+Configuration.arraySizeBound+"]":
			return ARRAY;
		default:
			if(type.endsWith("["+Configuration.arraySizeBound+"]")){
				return OBJECT_ARRAY;
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
   	public static final int UNKNOWN_OP = 3;
   	
	public static int operatorType(String op) {
		switch(op){
		case "+":
		case "-":
		case "*":
		case "/":
		case "%":
		case ">>":
		case "<<":
		case ">>>":
			return ARITHMETIC_OP;
		case "<":
		case ">":
		case "<=":
		case ">=":
		case "instanceof":
		case "==":
		case "!=":
		case "&":
		case "^":
		case "|":
		case "&&":
		case "||":
		case "!":
			return RELATIONAL_OP;
		default:
			return UNKNOWN_OP;
		}
	}
	
	public static String getDafnyType(String original){
		switch(original){
		case "bit":
			return "bool";
		case "char":
			return "char";
		case "int":
			return "int";
		case "float":
		case "double":
			return "real";
		case "bit["+Configuration.arraySizeBound+"]":
			return "seq<bool>";
		case "char["+Configuration.arraySizeBound+"]":
			return "seq<char>";
		case "int["+Configuration.arraySizeBound+"]":
			return "seq<int>";
		case "float["+Configuration.arraySizeBound+"]":
		case "real["+Configuration.arraySizeBound+"]":
			return "seq<real>";
		default:
			return original;
		}
	}

	public static int getOpClassForType(String type) {
		switch(type){
		case "int":
			return ARITHMETIC_OP;
		case "bit":
			return RELATIONAL_OP;
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
					case "java.lang.Integer":
						return 1;
					case "short[]":
					case "byte[]":
					case "int[]":
					case "long[]":
					case "float[]":
					case "double[]":
					case "java.lang.Integer[]":
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
					case "java.lang.String":
						return 1;
					case "java.lang.String[]":
						return 2;
					default:
						return 0; // TODO: Implement this.
				}
			default:
				return 0;
		}
	}
}
