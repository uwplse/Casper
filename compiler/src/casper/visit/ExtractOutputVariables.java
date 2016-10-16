/* 
 * This class implements a single compiler pass. The pass is executed 
 * after the input variables have been extracted for marked fragments. 
 * The goal for this compiler pass is to extract the set of output 
 * variables for marked fragments.
 * 
 * Output variables are variables that were declared outside of the
 * loop body, but were written/modified within the loop. They thus
 * capture the effect/output of the loop on the program.
 *    
 * - Maaz
 */

package casper.visit;

import java.util.ArrayList;
import java.util.List;

import casper.JavaLibModel;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.ArrayAccess;
import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.If;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class ExtractOutputVariables extends NodeVisitor  {
	boolean debug;
	boolean ignore;
	ArrayList<MyWhileExt> extensions;
	
	@SuppressWarnings("deprecation")
	public ExtractOutputVariables(){
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
						// Statement is a conditional
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
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				// begin extraction
				this.extensions.add((MyWhileExt)JavaExt.ext(n));
				
				// Mark the first if condition to be ignored. It contains the loop increment
				Stmt loopBody = ((ExtendedFor) n).body();
				if(loopBody instanceof Block){
					MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(loopBody);
					stmtext.process = true;
				}
			}
		}
		else if(n instanceof Block){
			// If statement
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			if(stmtext.process)
				this.ignore = false;
		}
		
		// If we are not extracting, then do nothing
		if(this.extensions.size() == 0 || this.ignore) return this;
		
		if(n instanceof Assign){
			// Assignment statement
			Expr left = ((Assign) n).left();
			
			if(left instanceof ArrayAccess){
				for(MyWhileExt ext : this.extensions){
					// Save the array
					ext.saveOutputVariable(((ArrayAccess)left).array().toString(), ((ArrayAccess)left).array().type().toString(),Variable.ARRAY_ACCESS);
					
					// Save the index (unless it is a constant)
					Expr index = ((ArrayAccess)left).index();
					if(index instanceof Local)
						ext.saveInputVariable(index.toString(), index.type().toString(),Variable.VAR);
				}
			}
			else if(left instanceof Field){
				for(MyWhileExt ext : this.extensions){
					// Save the variable
					ext.saveOutputVariable(left.toString(), left.type().toString(), ((Field) left).target().type().toString(),Variable.FIELD_ACCESS);
				}
			}
			else{
				for(MyWhileExt ext : this.extensions){
					// Save the variable
					ext.saveOutputVariable(left.toString(), left.type().toString(),Variable.VAR);
				}
			}
		}
		else if(n instanceof Call){
			// Function call
			for(MyWhileExt ext : extensions){
				List<Node> writes = JavaLibModel.extractWrites((Call)n,ext);
				for(Node node : writes){
					if(node instanceof Receiver){
						ext.saveOutputVariable(node.toString(),((Receiver)node).type().toString(),Variable.FIELD_ACCESS);
					}
					else if(node instanceof Local){
						ext.saveOutputVariable(ext.toString(),node.toString(),((Local) node).type().toString(),Variable.VAR);
					}
				}
			}
		}
		
		return this;
	}
	
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				if(debug){
					System.err.println("Input:\n"+((MyWhileExt)JavaExt.ext(n)).inputVars.toString());
					System.err.println("Output:\n"+((MyWhileExt)JavaExt.ext(n)).outputVars.toString());
					System.err.println("Local:\n"+((MyWhileExt)JavaExt.ext(n)).localVars.toString());
				}
				this.extensions.remove(((MyWhileExt)JavaExt.ext(n)));
			}
		}
		else if(n instanceof Block){
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			if(stmtext.process){
				this.ignore = true;
			}
		}
       
		return n;
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished output var extraction complier pass *************");
	}
}
