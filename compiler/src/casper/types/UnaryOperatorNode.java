package casper.types;

import java.util.List;
import java.util.Map;

import polyglot.ast.Expr;

public class UnaryOperatorNode extends CustomASTNode{

	CustomASTNode operand;
	
	public UnaryOperatorNode(String n, CustomASTNode o) {
		super(n);
		operand = o;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		CustomASTNode newOperand = operand.replaceAll(lhs, rhs);
		return new UnaryOperatorNode(name,newOperand);
	}
	
	public String toString(){
		return "(" + name + operand.toString() + ")";
	}

	@Override
	public boolean contains(String exp) {
		return name.equals(exp) || operand.contains(exp);
	}

	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
		operand.getIndexes(arrname, indexes);
	}

	@Override
	public CustomASTNode fixArrays() {
		operand = operand.fixArrays();
		return this;
	}

	@Override
	public void replaceIndexesWith(String k) {
		operand.replaceIndexesWith(k);
	}

	@Override
	public boolean containsArrayAccess() {
		return operand.containsArrayAccess();
	}
	
}