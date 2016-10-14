/*
 * TODO: Brief documentation.
 */

package casper.visit;

import java.util.ArrayList;
import java.util.List;
import casper.JavaLibModel;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.extension.MyWhileExt.Variable;
import polyglot.ast.ArrayAccess;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.If;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class ExtractLoopCounters extends NodeVisitor {
	boolean debug;
	boolean ignore;
	ArrayList<MyWhileExt> extensions;
   
	@SuppressWarnings("deprecation")
	public ExtractLoopCounters(){
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
				
				// Mark the first if condition to be ignored. It contains the loop increment
				Stmt loopBody = ((While) n).body();
				if(loopBody instanceof Block){
					List<Stmt> stmts = ((Block) loopBody).statements();
					for(Stmt stmt : stmts){
						// Satement is a conditional
						if(stmt instanceof If){
							Stmt cons = ((If) stmt).consequent();
							Stmt alt = ((If) stmt).alternative(); 
							
							// And consequent or alternative contains a break
							if(casper.Util.containsBreak(cons)){
								MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(((If) stmt).alternative());
								stmtext.process = true;
							}
							else if(casper.Util.containsBreak(alt)){
								MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(((If) stmt).consequent());
								stmtext.process = true;
							}
						}
					}
				}
			}
		}
		else if(n instanceof ExtendedFor){
			((MyWhileExt)JavaExt.ext(n)).saveLoopCounterVariable("i", "int", Variable.VAR);
		}
		else if(n instanceof Block){
			// If statement
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			if(stmtext.process){
				this.ignore = false;
			}
		}
		
		// If we are not extracting, then do nothing
		if(this.extensions.size() == 0 || this.ignore) return this;
		
		
		if(n instanceof ArrayAccess){
			Expr index = ((ArrayAccess) n).index();
			for(MyWhileExt ext : this.extensions){
				if(index instanceof Local){
					ext.saveLoopCounterVariable(index.toString(), index.type().toString(), Variable.VAR);
				}
			}
		}
		else if(n instanceof Call){
			// Function call
			Node lc = JavaLibModel.extractLoopCounters((Call) n);
			if(lc instanceof Local){
				for(MyWhileExt ext : this.extensions){
					ext.saveLoopCounterVariable(lc.toString(),lc.toString(),MyWhileExt.Variable.VAR);
				}
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
					System.err.println("Loop Counters:\n"+((MyWhileExt)JavaExt.ext(n)).loopCounters.toString());
				}
				
				this.extensions.remove(((MyWhileExt)JavaExt.ext(n)));
			}
		}
		// If the node is a Block
		else if(n instanceof Block){
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			// and was marked as the incremental block of the loop
			if(stmtext.process){
				this.ignore = true;
				stmtext.process = false;
			}
		}
       
		return n;
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished loop counter extraction complier pass *************");
	}
}