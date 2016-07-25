package casper.visit;

import java.util.ArrayList;

import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import polyglot.ast.Binary;
import polyglot.ast.If;
import polyglot.ast.Node;
import polyglot.ast.Unary;
import polyglot.ast.While;
import polyglot.visit.NodeVisitor;

public class ExtractOperators  extends NodeVisitor {
	boolean debug;
	boolean ignore;
	ArrayList<MyWhileExt> extensions;
   
	public ExtractOperators(){
		this.debug = false;
		this.ignore = true;
		this.extensions = new ArrayList<MyWhileExt>();
	}
   	
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				// begin extraction
				this.extensions.add((MyWhileExt)JavaExt.ext(n));
			}
		}
		else if(n instanceof If){
			// If statement
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			if(stmtext.process)
				this.ignore = false;
		}
		
		// If we are not extracting, then do nothing
		if(this.extensions.size() == 0 || this.ignore) return this;
		
		if(n instanceof Binary){
			for(MyWhileExt ext : extensions){
				ext.binaryOperators.add(((Binary) n).operator().toString());
			}
		}
		else if(n instanceof Unary){
			for(MyWhileExt ext : extensions){
				ext.binaryOperators.add(((Unary) n).operator().toString());
			}
		}
		
		return this;
	}
   
   
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				
				if(debug){
					System.err.println("Binary Operators:\n"+((MyWhileExt)JavaExt.ext(n)).binaryOperators.toString());
					System.err.println("Unary Operators:\n"+((MyWhileExt)JavaExt.ext(n)).unaryOperators.toString());
				}
				
				this.extensions.remove(((MyWhileExt)JavaExt.ext(n)));
			}
			else if(n instanceof If){
				MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
				if(stmtext.process)
					this.ignore = true;
			}
		}
       
		return n;
	}
   
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished operator extraction complier pass *************");
	}
}