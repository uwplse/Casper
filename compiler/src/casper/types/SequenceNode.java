package casper.types;

import java.util.List;
import java.util.Map;

public class SequenceNode  extends CustomASTNode{

	public CustomASTNode inst1;
	public CustomASTNode inst2;
	
	public SequenceNode(CustomASTNode s1, CustomASTNode s2) {
		super("");
		inst1 = s1;
		inst2 = s2;
	}

	@Override
	public boolean contains(String exp) {
		return inst1.contains(exp) || inst2.contains(exp);
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs) {
		CustomASTNode inst3 = inst1.replaceAll(lhs, rhs);
		CustomASTNode inst4 = inst2.replaceAll(lhs, rhs);
		return new SequenceNode(inst3, inst4);
	}

	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
		inst1.getIndexes(arrname, indexes);
		inst2.getIndexes(arrname, indexes);
	}

	@Override
	public boolean containsArrayAccess() {
		return inst1.containsArrayAccess() || inst2.containsArrayAccess();
	}

	@Override
	public void replaceIndexesWith(String k) {
		inst1.replaceIndexesWith(k);
		inst2.replaceIndexesWith(k);
	}

	@Override
	public CustomASTNode fixArrays() {
		inst1.fixArrays();
		inst2.fixArrays();
		return this;
	}
	
	public String toString(){
		return "Instr 1: " + inst1 + "\n" + "Instr 2: " + inst2;
	}
	
	public String inst1ToString(String vardecl){
		String code = "";
		
		if(inst1 instanceof ConditionalNode){
			code += ((ConditionalNode)inst1).toString(vardecl);
		}
		else if(inst1 instanceof ArrayUpdateNode){
			code += inst1.toString();
		}
		else if(inst1 instanceof SequenceNode){
			code += ((SequenceNode) inst1).inst1ToString(vardecl) + ";\n\t\t" + ((SequenceNode) inst1).inst2ToString(vardecl);
		}
		else{
			code += vardecl + inst1;
		}
		
		return code;
	}
	
	public String inst2ToString(String vardecl){
		String code = "";
		
		if(inst2 instanceof ConditionalNode){
			code += ((ConditionalNode)inst2).toString(vardecl);
		}
		else if(inst2 instanceof ArrayUpdateNode){
			code += inst2.toString();
		}
		else if(inst2 instanceof SequenceNode){
			code += ((SequenceNode) inst2).inst1ToString(vardecl) + ";\n\t\t" + ((SequenceNode) inst2).inst2ToString(vardecl);
		}
		else{
			code += vardecl + inst2;
		}
		
		return code;
	}

	public String inst1ToStringDafny(String vardecl) {
		String code = "";
		
		if(inst1 instanceof ConditionalNode){
			code += ((ConditionalNode)inst1).toStringDafny(vardecl);
		}
		else if(inst1 instanceof ArrayUpdateNode){
			code += vardecl + ((ArrayUpdateNode) inst1).toStringDafny();
		}
		else if(inst1 instanceof SequenceNode){
			code += ((SequenceNode) inst1).inst1ToStringDafny(vardecl) + ";\n\t\t" + ((SequenceNode) inst1).inst2ToStringDafny(vardecl);
		}
		else{
			code += vardecl + inst1;
		}

		return code;
	}

	public String inst2ToStringDafny(String vardecl) {
		String code = "";
		
		if(inst2 instanceof ConditionalNode){
			code += ((ConditionalNode)inst2).toStringDafny(vardecl);
		}
		else if(inst2 instanceof ArrayUpdateNode){
			code += vardecl + ((ArrayUpdateNode) inst2).toStringDafny();
		}
		else if(inst2 instanceof SequenceNode){
			code += ((SequenceNode) inst2).inst1ToStringDafny(vardecl) + ";\n\t\t" + ((SequenceNode) inst2).inst2ToStringDafny(vardecl);
		}
		else{
			code += vardecl + inst2;
		}
		
		return code;
	}

}