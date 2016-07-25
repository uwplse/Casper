package casper.types;

import java.util.List;
import java.util.Map;

import polyglot.ast.Expr;

public class ArrayUpdateNode  extends CustomASTNode{

	CustomASTNode array;
	CustomASTNode index;
	CustomASTNode value;
	
	public ArrayUpdateNode(CustomASTNode a, CustomASTNode i, CustomASTNode v) {
		super("");
		array = a;
		index = i;
		value = v;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		CustomASTNode newArray = array.replaceAll(lhs, rhs);
		CustomASTNode newIndex = index.replaceAll(lhs, rhs);
		CustomASTNode newValue = value.replaceAll(lhs, rhs);
		return new ArrayUpdateNode(newArray,newIndex,newValue);
	}
	 
	public String toString(){
		return "ind_" + array + "["+ index +"] = " + value;
	}

	@Override
	public boolean contains(String exp) {
		return array.contains(exp) || index.contains(exp) || value.contains(exp);
	}

	public String toStringDafny() {
		return "ind_" + array + "["+ index +" := " + value + "]";
	}

	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
		if(arrname.equals(array.toString())){
			if(!indexes.get(arrname).contains(index)){
				indexes.get(arrname).add(index);
			}
		}
		array.getIndexes(arrname, indexes);
		index.getIndexes(arrname, indexes);
		value.getIndexes(arrname, indexes);
	}
	
}