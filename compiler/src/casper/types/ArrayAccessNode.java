package casper.types;

import java.util.List;
import java.util.Map;

public class ArrayAccessNode extends CustomASTNode{

	public CustomASTNode array;
	CustomASTNode index;
	
	public ArrayAccessNode(String n, CustomASTNode a, CustomASTNode i) {
		super(n);
		array = a;
		index = i;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		if(name.equals(lhs)){
			return rhs;
		}
		CustomASTNode newIndex = index.replaceAll(lhs, rhs);
		CustomASTNode newArray = array.replaceAll(lhs, rhs);
		String newName = newArray.toString() + "[" + newIndex.toString() + "]";
		return new ArrayAccessNode(newName,newArray,newIndex);
	}
	
	public String toString(){
		return name;
	}

	@Override
	public boolean contains(String exp) {
		return name.equals(exp) || array.contains(exp) || index.contains(exp);
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
	}

	@Override
	public CustomASTNode fixArrays() {
		return new FieldNode("casper_data_set["+index.name+"]."+array.toString(),new ArrayAccessNode("casper_data_set["+index.name+"]",new IdentifierNode("casper_data_set"),index));
	}

}