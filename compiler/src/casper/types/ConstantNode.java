package casper.types;

import java.util.List;
import java.util.Map;

public class ConstantNode extends CustomASTNode {
	
	public static final int INTLIT = 0;
	public static final int STRINGLIT = 1;
	public static final int BOOLEANLIT = 2;
	public static final int NULLLIT = 3;
	public static final int ARRAYLIT = 4;
	public static final int UNKNOWNLIT = 5;

	public int type_code;
	
	public ConstantNode(String n, String t, int tc) {
		super(n);
		type = t;
		type_code = tc;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		// Return clone of self
		return new ConstantNode(name,type,type_code);
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

	@Override
	public CustomASTNode fixArrays() {
		return this;
	}
	
	@Override
	public void replaceIndexesWith(String k) {
		return;
	}

	@Override
	public boolean containsArrayAccess() {
		return false;
	}
	
}