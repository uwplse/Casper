package casper.visit;

import java.util.ArrayList;

import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.Node;
import polyglot.ast.While;
import polyglot.visit.NodeVisitor;

public class IdentifyDataSet extends NodeVisitor{
	boolean debug;
	ArrayList<MyWhileExt> extensions;
   
	@SuppressWarnings("deprecation")
	public IdentifyDataSet(){
		this.debug = false;
		this.extensions = new ArrayList<MyWhileExt>();
	}
	
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While){
			MyWhileExt ext = (MyWhileExt)JavaExt.ext(n);
			
			// If the loop was marked as interesting
			if(ext.interesting){
				for(Variable var : ext.inputVars){
					if(var.category == Variable.ARRAY_ACCESS){
						ext.inputDataCollections.add(var);
						ext.hasInputData = true;
					}
				}
				
				if(!ext.hasInputData)
					ext.interesting = false;
			}
			
			if(debug)
				System.err.println(ext.inputDataCollections);
		}
		return this;
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished identify dataset complier pass *************");
	}
}