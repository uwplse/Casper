package casper.types;

import java.util.List;
import java.util.Map;

import polyglot.ast.Expr;

public class FieldNode extends CustomASTNode{

	public CustomASTNode container;
	
	public FieldNode(String n, CustomASTNode c) {
		super(n);
		container = c;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		if(name.equals(lhs)){
			return rhs;
		}
		CustomASTNode newContainer = container.replaceAll(lhs,rhs);
		String[] components = name.split("\\.");
		String newName = newContainer.toString() + "." + components[components.length-1];
		return new FieldNode(newName,newContainer);
	}
	
	public String toString(){
		return name;
	}

	@Override
	public boolean contains(String exp) {
		return name.equals(exp) || container.contains(exp);
	}

	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
		container.getIndexes(arrname, indexes);
	}

	@Override
	public CustomASTNode fixArrays() {
		container = container.fixArrays();
		return this;
	}

	@Override
	public void replaceIndexesWith(String k) {
		container.replaceIndexesWith(k);
	}

	@Override
	public boolean containsArrayAccess() {
		return container.containsArrayAccess();
	}
	
}