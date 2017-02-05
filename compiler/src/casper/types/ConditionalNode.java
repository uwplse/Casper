package casper.types;

import java.util.List;
import java.util.Map;

import polyglot.ast.Expr;

public class ConditionalNode extends CustomASTNode{

	CustomASTNode cond;
	CustomASTNode cons;
	CustomASTNode alt;
	
	public ConditionalNode(CustomASTNode co, CustomASTNode c, CustomASTNode a) {
		super("");
		cond = co;
		cons = c;
		alt = a;
	}

	@Override
	public CustomASTNode replaceAll(String lhs, CustomASTNode rhs){
		CustomASTNode newCond = cond.replaceAll(lhs, rhs);
		CustomASTNode newCons = cons.replaceAll(lhs, rhs);
		CustomASTNode newAlt = alt.replaceAll(lhs, rhs);
		return new ConditionalNode(newCond,newCons,newAlt);
	}
	
	public String toString(String vardecl){
		String output = "if("+cond.toString()+"){\n\t\t\t";
		if(cons instanceof ArrayUpdateNode){
			output += cons.toString() + ";\n\t\t} else {\n\t\t\t";
		}
		else if(cons instanceof ConditionalNode){
			output += ((ConditionalNode) cons).toString(vardecl) + "\n\t\t} else {\n\t\t\t";
		}
		else if(cons instanceof SequenceNode){
			output += ((SequenceNode) cons).inst1ToString(vardecl) + ";\n\t\t" + ((SequenceNode) cons).inst2ToString(vardecl) + ";\n\t\t} else {\n\t\t\t";
		}
		else{
			//output += vardecl + "ind_" + cons.toString() + ";\n\t\t} else {\n\t\t\t";
			output += vardecl + cons.toString() + ";\n\t\t} else {\n\t\t\t";
		}
		
		if(alt instanceof ArrayUpdateNode){
			output += alt.toString() + ";\n\t\t}\n\t\t";
		}
		else if(alt instanceof ConditionalNode){
			output += ((ConditionalNode) alt).toString(vardecl) + "}\n\t\t";
		}
		else if(cons instanceof SequenceNode){
			output += ((SequenceNode) alt).inst1ToString(vardecl) + ";\n\t\t" + ((SequenceNode) alt).inst2ToString(vardecl) + ";\n\t\t} else {\n\t\t\t";
		}
		else{
			output += vardecl + alt.toString() + ";\n\t\t}\n\t\t";
		}
		return output;
	}

	@Override
	public boolean contains(String exp) {
		return cond.contains(exp) || cons.contains(exp) || alt.contains(exp);
	}

	public String toStringDafny(String vardecl) {
		String output = "if("+cond.toString()+")\n\t\t{\n\t\t\t";
		if(cons instanceof ArrayUpdateNode){
			output += vardecl + ((ArrayUpdateNode) cons).toStringDafny() + ";\n\t\t} \n\t\telse \n\t\t{\n\t\t\t";
		}
		else if(cons instanceof ConditionalNode){
			output += ((ConditionalNode) cons).toStringDafny(vardecl) + "\n\t\t} else {\n\t\t\t";
		}

		else if(cons instanceof SequenceNode){
			if(((SequenceNode) cons).inst1 instanceof ConditionalNode){
				output += ((SequenceNode) cons).inst1ToStringDafny(vardecl) + "\n\t\t"; 
			}
			else{
				output += ((SequenceNode) cons).inst1ToStringDafny(vardecl) + ";\n\t\t";
			}
			
			if(((SequenceNode) cons).inst2 instanceof ConditionalNode){
				output += ((SequenceNode) cons).inst2ToStringDafny(vardecl) + "\n\t\t} else {\n\t\t\t"; 
			}
			else{
				output += ((SequenceNode) cons).inst2ToStringDafny(vardecl) + ";\n\t\t} else {\n\t\t\t";
			}
		}
		else{
			output += vardecl + cons.toString() + ";\n\t\t} else \n\t\t{\n\t\t\t";
		}
		
		if(alt instanceof ArrayUpdateNode){
			output += vardecl + ((ArrayUpdateNode) alt).toStringDafny() + ";\n\t\t}\n\t\t";
		}
		else if(alt instanceof ConditionalNode){
			output += ((ConditionalNode) alt).toStringDafny(vardecl) + "}\n\t\t";
		}
		else if(alt instanceof SequenceNode){
			if(((SequenceNode) alt).inst1 instanceof ConditionalNode){
				output += ((SequenceNode) alt).inst1ToStringDafny(vardecl) + "\n\t\t"; 
			}
			else{
				output += ((SequenceNode) alt).inst1ToStringDafny(vardecl) + ";\n\t\t";
			}
			
			if(((SequenceNode) alt).inst2 instanceof ConditionalNode){
				output += ((SequenceNode) alt).inst2ToStringDafny(vardecl) + "\n\t\t} else {\n\t\t\t"; 
			}
			else{
				output += ((SequenceNode) alt).inst2ToStringDafny(vardecl) + ";\n\t\t} else {\n\t\t\t";
			}
		}
		else{
			output += vardecl + alt.toString() + ";\n\t\t}\n\t\t";
		}
		return output;
	}
	
	public String toString(){
		return "Cond: " + cond + "\n" + "Cons: " + cons + "\n" + "Alt: " + alt;
	}

	@Override
	public void getIndexes(String arrname, Map<String, List<CustomASTNode>> indexes) {
		cond.getIndexes(arrname, indexes);
		cons.getIndexes(arrname, indexes);
		alt.getIndexes(arrname, indexes);		
	}

	@Override
	public CustomASTNode fixArrays() {
		cond = cond.fixArrays();
		cons = cons.fixArrays();
		alt = alt.fixArrays();
		return this;
	}
	
	@Override
	public void replaceIndexesWith(String k) {
		cond.replaceIndexesWith(k);
		cons.replaceIndexesWith(k);
		alt.replaceIndexesWith(k);
	}

	@Override
	public boolean containsArrayAccess() {
		return cond.containsArrayAccess() || cons.containsArrayAccess() ||  alt.containsArrayAccess();
	}

}