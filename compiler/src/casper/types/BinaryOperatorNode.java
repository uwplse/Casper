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

	@Override
	public int convertConstToIDs(Map<String,String> constMapping, int constID){
		if(casper.Util.operatorType(name) == casper.Util.RELATIONAL_OP){
			if(operandLeft instanceof ConstantNode){
				if(((ConstantNode) operandLeft).type == ConstantNode.INTLIT){
					if(!constMapping.containsKey(operandLeft.toString())){
						constMapping.put(operandLeft.toString(), "casperConst"+constID);
						constID++;
					}
					operandLeft = new IdentifierNode(constMapping.get(operandLeft.toString()));
				}
			}
			if(operandRight instanceof ConstantNode){
				if(((ConstantNode) operandRight).type == ConstantNode.INTLIT){
					if(!constMapping.containsKey(operandRight.toString())){
						constMapping.put(operandRight.toString(), "casperConst"+constID);
						constID++;
					}
					operandRight = new IdentifierNode(constMapping.get(operandRight.toString()));
				}
			}
		}
		return constID;
	}

	@Override
	public CustomASTNode fixArrays() {
		operandLeft = operandLeft.fixArrays();
		operandRight = operandRight.fixArrays();
		return this;
	}
	
}