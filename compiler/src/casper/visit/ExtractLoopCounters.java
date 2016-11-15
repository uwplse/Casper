/*
 * TODO: Brief documentation.
 */

package casper.visit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import casper.JavaLibModel;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.types.Variable;
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
			((MyWhileExt)JavaExt.ext(n)).saveLoopCounterVariable("casper_index", "int", Variable.VAR);
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
					ext.saveLoopCounterVariable(lc.toString(),((Local) lc).type().toString(),Variable.VAR);
				}
			}
		}
		
		return this;
	}
	
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While){
			MyWhileExt ext = (MyWhileExt)JavaExt.ext(n);
			
			// If the loop was marked as interesting
			if(ext.interesting){
				Stmt body = ((While) n).body();
				
				// Filter out loop counters
				Set<Variable> lcCopy = new HashSet<Variable>();
				lcCopy.addAll(ext.loopCounters);
				for(Variable var : lcCopy){
					CustomASTNode lcExp = new IdentifierNode(var.varNameOrig);
					
					if(body instanceof Block){
						List<Stmt> statements = ((Block) body).statements();
						
						// Extract initial condition
						for(int i=0; i<statements.size(); i++){
							Stmt currStatement = statements.get(i);
							
							// Satement is a conditional
							if(currStatement instanceof If){
								Stmt cons = ((If) currStatement).consequent();
								Stmt alt = ((If) currStatement).alternative(); 
								
								// And consequent or alternative does not contain a break
								if(!casper.Util.containsBreak(cons) && !casper.Util.containsBreak(alt)){
									// Must be the increment if, process it first
									lcExp = casper.Util.generatePreCondition(var.getSketchType(), cons, lcExp, ext, debug);
									break;
								}
							}
						}
						
						for(int i=0; i<statements.size(); i++){
							Stmt currStatement = statements.get(i);
							
							// Satement is a conditional
							if(currStatement instanceof If){
								Stmt cons = ((If) currStatement).consequent();
								Stmt alt = ((If) currStatement).alternative(); 
								
								// And consequent contains a break
								if(casper.Util.containsBreak(cons)){
									lcExp = casper.Util.generatePreCondition(var.getSketchType(), alt, lcExp, ext, debug);
									break;
								}
								// And alternative contains a break
								else if(casper.Util.containsBreak(alt)){
									lcExp = casper.Util.generatePreCondition(var.getSketchType(), cons, lcExp, ext, debug);
									break;
								}
							}
						}
					}
					
					if(lcExp.toString().equals(var.varName)){
						ext.loopCounters.remove(var);
					}
				}

				if(ext.loopCounters.size() == 0){
					ext.interesting = false;
				}
				
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