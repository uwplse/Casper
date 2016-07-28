package casper.types;

import java.util.List;
import java.util.Map;

public class ConstantNode extends CustomASTNode {
	
	public static final int INTLIT = 0;
	public static final int STRINGLIT = 1;
	public static final int BOOLEANLIT = 2;
	public static final int UNKNOWNLIT = 3;

	public int type;
	
	public ConstantNode(String n, int t) {
		super(n);
		type = t;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		// Return clone of self
		return new ConstantNode(name,type);
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