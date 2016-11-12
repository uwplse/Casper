package casper.types;

import java.util.List;
import java.util.Map;

import casper.JavaLibModel;
import polyglot.ast.ArrayAccess;
import polyglot.ast.Binary;
import polyglot.ast.BooleanLit;
import polyglot.ast.Call;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FloatLit;
import polyglot.ast.IntLit;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.StringLit;
import polyglot.ast.Unary;

abstract public class CustomASTNode {
	
	public enum Operator {
	    Add, 
	    Subtract, 
	    Multiply, 
	    Divide, 
	    Modulus, 
	    LessThan, 
	    GreaterThan, 
	    Equal, 
	    LessThanEqual, 
	    GreaterThanEqual, 
	    NotEqual
	}
	
	String name;
	
	public CustomASTNode(String n){
		name = n;
	}
	
	@Override
	public boolean equals(Object o){
		return this.toString().equals(o.toString());
	}
	
	abstract public boolean contains(String exp);
	
	abstract public CustomASTNode replaceAll(String lhs, CustomASTNode rhs);
	
	abstract public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes);
	
	public int convertConstToIDs(Map<String,String> constMapping, int constID){
		return constID;
	}
	
	public static CustomASTNode convertToAST(Expr exp){
		CustomASTNode node = null;
		
		if(exp instanceof New){
			String objType = ((New) exp).objectType().toString();
			if(objType.startsWith("java.util.ArrayList<")){
				String subType = objType.substring("java.util.ArrayList<".length(), objType.length()-1);
				node = new ConstantNode(casper.Util.getInitVal(subType),ConstantNode.ARRAYLIT);
			}
			else if(objType.startsWith("java.util.HashMap<")){
				String subType = objType.substring("java.util.HashMap<".length(), objType.length()-1);
				String[] subTypes = subType.split(",");
				node = new ConstantNode(casper.Util.getInitVal(subTypes[1]),ConstantNode.ARRAYLIT);
			}
		}
		else  if(exp instanceof NewArray){
			node = new ConstantNode(casper.Util.getInitVal(((NewArray) exp).baseType().toString()),ConstantNode.ARRAYLIT);
		}
		else if(exp instanceof Cast){
			node = convertToAST(((Cast) exp).expr());
		}
		else if(exp instanceof Local){
			node = new IdentifierNode(exp.toString());
		}
		else if(exp instanceof Lit){
			if(exp instanceof IntLit){
				node = new ConstantNode(exp.toString(),ConstantNode.INTLIT);
			}
			else if(exp instanceof StringLit){
				node = new ConstantNode(exp.toString(),ConstantNode.STRINGLIT);
			}
			else if(exp instanceof BooleanLit){
				node = new ConstantNode(exp.toString(),ConstantNode.BOOLEANLIT);
			}
			else if(exp instanceof FloatLit){
				String exp_p = Integer.toString((int)Math.ceil(Double.parseDouble(exp.toString()))); 
				node = new ConstantNode(exp_p,ConstantNode.INTLIT);
			}
			else{
				node = new ConstantNode(exp.toString(),ConstantNode.UNKNOWNLIT);
			}
		}
		else if(exp instanceof Field){
			CustomASTNode container = new IdentifierNode(((Field) exp).target().toString());
			node = new FieldNode(exp.toString(), container);
		}
		else if(exp instanceof ArrayAccess){
			Expr arrayExpr = ((ArrayAccess) exp).array();
			Expr indexExpr = ((ArrayAccess) exp).index();
			CustomASTNode array = convertToAST(arrayExpr);
			CustomASTNode index = convertToAST(indexExpr);
			node = new ArrayAccessNode(exp.toString(), array, index);
		}
		else if(exp instanceof Unary){
			String operator = ((Unary) exp).operator().toString();
			CustomASTNode operand = convertToAST(((Unary) exp).expr());
			node = new UnaryOperatorNode(operator,operand);
		}
		else if(exp instanceof Binary){
			String operator = ((Binary) exp).operator().toString();
			CustomASTNode operandLeft = convertToAST(((Binary) exp).left());
			CustomASTNode operandRight = convertToAST(((Binary) exp).right());
			
			// Fix nulls
			if(operator == "==" && operandRight.toString().equals("null")){
				switch(((Binary) exp).left().type().toString()){
					case "java.lang.String":
					case "java.lang.Integer":
					case "java.lang.Float":
					case "java.lang.Double":
						operandRight = new ConstantNode("0",ConstantNode.NULLLIT);
					default:
						break;
				}
			}
			if(operator == "==" && operandLeft.toString().equals("null")){
				switch(((Binary) exp).right().type().toString()){
					case "String":
					case "Integer":
					case "Float":
					case "Double":
						operandLeft = new ConstantNode("0",ConstantNode.NULLLIT);
					default:
						break;
				}
			}
			
			node = new BinaryOperatorNode(operator,operandLeft,operandRight);
		}
		else if(exp instanceof Call){
			node = JavaLibModel.convertToAST((Call)exp);
		}
		else {
			System.out.println("Unrecognized AST Node: " + exp.toString());
		}
		
		return node;
	}
	
	abstract public boolean containsArrayAccess();
	
	abstract public void replaceIndexesWith(String k);
	
	abstract public CustomASTNode fixArrays();
	
}
