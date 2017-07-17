package casper.types;

import java.util.List;
import java.util.Map;

public class IdentifierNode extends CustomASTNode {

	public IdentifierNode(String n, String t) {
		super(n);
		type = t;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		if(name.equals(lhs)){
			return rhs;
		}
		// Return clone of self
		return new IdentifierNode(name, type);
	}
	
	public boolean equals(Object o){
		if(o instanceof IdentifierNode){
			return ((IdentifierNode) o).name.equals(name); 
		}
		return false;
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