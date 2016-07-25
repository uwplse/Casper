package casper.types;

import java.util.List;
import java.util.Map;

public class BinaryOperatorNode extends CustomASTNode{
	
	CustomASTNode operandLeft;
	CustomASTNode operandRight;
	
	public BinaryOperatorNode(String n, CustomASTNode oleft, CustomASTNode oright) {
		super(n);
		operandLeft = oleft;
		operandRight = oright;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		CustomASTNode newOperandLeft = operandLeft.replaceAll(lhs, rhs);
		CustomASTNode newOperandRight = operandRight.replaceAll(lhs, rhs);
		return new BinaryOperatorNode(name,newOperandLeft,newOperandRight);
	}
	
	public String toString(){
		return "(" + operandLeft.toString() + name + operandRight.toString() + ")";
	}

	@Override
	public boolean contains(String exp) {
		return name.equals(exp) || operandLeft.contains(exp) || operandRight.contains(exp);
	}

	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
		operandLeft.getIndexes(arrname, indexes);
		operandRight.getIndexes(arrname, indexes);
	}
	
}