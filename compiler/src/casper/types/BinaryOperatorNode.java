package casper.types;

import java.util.List;
import java.util.Map;

public class BinaryOperatorNode extends CustomASTNode{
	
	public CustomASTNode operandLeft;
	public CustomASTNode operandRight;
	
	public BinaryOperatorNode(String n, String t, CustomASTNode oleft, CustomASTNode oright) {
		super(n);
		operandLeft = oleft;
		operandRight = oright;
		type = t;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		CustomASTNode newOperandLeft = operandLeft.replaceAll(lhs, rhs);
		CustomASTNode newOperandRight = operandRight.replaceAll(lhs, rhs);
		return new BinaryOperatorNode(name,type,newOperandLeft,newOperandRight);
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
				if(((ConstantNode) operandLeft).type_code == ConstantNode.INTLIT){
					if(!constMapping.containsKey(operandLeft.toString())){
						constMapping.put(operandLeft.toString(), "casperConst"+constID);
						constID++;
					}
					operandLeft = new IdentifierNode(constMapping.get(operandLeft.toString()),operandLeft.type);
				}
			}
			if(operandRight instanceof ConstantNode){
				if(((ConstantNode) operandRight).type_code == ConstantNode.INTLIT){
					if(!constMapping.containsKey(operandRight.toString())){
						constMapping.put(operandRight.toString(), "casperConst"+constID);
						constID++;
					}
					operandRight = new IdentifierNode(constMapping.get(operandRight.toString()),operandRight.type);
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
	
	@Override
	public void replaceIndexesWith(String k) {
		operandLeft.replaceIndexesWith(k);
		operandRight.replaceIndexesWith(k);
	}

	@Override
	public boolean containsArrayAccess() {
		return operandLeft.containsArrayAccess() || operandRight.containsArrayAccess();
	}
}