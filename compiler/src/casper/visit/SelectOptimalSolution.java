package casper.visit;

import java.util.HashSet;
import java.util.Set;

import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.Node;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class SelectOptimalSolution extends NodeVisitor{
	boolean debug;
	
	public SelectOptimalSolution(){
		this.debug = false;
	}
	
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
				
				Set<String> handledTypes = new HashSet<String>();
				
				int typeid = 0;
				for(Variable var : ext.outputVars){
					if(!handledTypes.contains(var.varType)){
						handledTypes.add(var.varType);
						
						if(!ext.generateCode.get(var.getReduceType())) continue;
						
						// Just select the first solution right now
						ext.selectedSolutionIndex = 0;
					}
				}
			}
		}
		
		return n;
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished generate code complier pass *************");
	}
}
