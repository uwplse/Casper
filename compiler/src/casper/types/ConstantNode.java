package casper.types;

import java.util.List;
import java.util.Map;

import polyglot.ast.Expr;

public class ConstantNode extends CustomASTNode {

	public ConstantNode(String n) {
		super(n);
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		// Return clone of self
		return new ConstantNode(name);
	}
	
	public String toString(){
		return name;
	}

	@Override
	public boolean contains(String exp) {
		return name.equals(exp);
	}
	
	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
	}
}