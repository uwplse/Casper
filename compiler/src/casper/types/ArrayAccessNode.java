package casper.types;

import java.util.List;
import java.util.Map;

import casper.Configuration;
import casper.Util;

public class ArrayAccessNode extends CustomASTNode{

	public CustomASTNode array;
	public CustomASTNode index;
	
	private static String fixType(String t) {
		return (Util.getSketchTypeFromRaw(t).endsWith("["+Configuration.arraySizeBound+"]") ? 
					Util.getSketchTypeFromRaw(t).replace("["+Configuration.arraySizeBound+"]","") : 
					Util.getSketchTypeFromRaw(t));
	}
	
	public ArrayAccessNode(String t, CustomASTNode a, CustomASTNode i) {		
		super(fixType(t)+"_getter("+a+","+i+")");
		type = t;
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
		return new ArrayAccessNode(type,newArray,newIndex);
	}
	
	public String toString(){
		return name;
	}

	@Override
	public boolean contains(String exp) {
		return array.contains(exp) || index.contains(exp);
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
		return new FieldNode("CasperDataRecord_getter(casper_data_set,"+index.name+")."+array.toString(),"CasperDataRecord",new ArrayAccessNode("CasperDataRecord",new IdentifierNode("casper_data_set","CasperDataRecord"),index));
	}
	
	@Override
	public void replaceIndexesWith(String k) {
		if(index instanceof IdentifierNode)
			index = new IdentifierNode(k,index.type);
		else
			index.replaceIndexesWith(k);
		array.replaceIndexesWith(k);
	}

	@Override
	public boolean containsArrayAccess() {
		return index.containsArrayAccess() || array.containsArrayAccess() || index instanceof IdentifierNode;
	}

}