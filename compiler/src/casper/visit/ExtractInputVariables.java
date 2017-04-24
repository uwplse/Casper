/* 
 * This class implements a single compiler pass. The pass is executed 
 * after the promising loop fragments have been marked. The goal for
 * this compiler pass is to extract the set of input variables for
 * all of the marked code fragments.
 * 
 * Input variables are variables that were declared outside of the loop 
 * body, but were read within the loop. They are thus inputs to the 
 * loop code fragment.
 *    
 * - Maaz
 */

package casper.visit;

import java.util.ArrayList;
import java.util.List;

import casper.JavaLibModel;
import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.ArrayAccess;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Call;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.If;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ast.Return;
import polyglot.ast.Switch;
import polyglot.ast.Unary;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class ExtractInputVariables extends NodeVisitor {
	boolean debug;
	boolean ignore;
	ArrayList<MyWhileExt> extensions;
   
	@SuppressWarnings("deprecation")
	public ExtractInputVariables(){
		this.debug = false;
		this.extensions = new ArrayList<MyWhileExt>();
	}
	
	private void extractReadsFromExpr(Expr exp){
		if(exp == null){
			return;
		}
		else if(exp instanceof Local){
			// If expression is a local variable
			for(MyWhileExt ext : extensions){
				ext.saveInputVariable(exp.toString(), exp.type().toString(),Variable.VAR);
			}
		}
		else if(exp instanceof Unary){
			// If expression is a unary operation
			Expr operand = ((Unary) exp).expr();
			if(operand instanceof Local){
				// If operand is a variable
				for(MyWhileExt ext : extensions){
					ext.saveInputVariable(operand.toString(),operand.type().toString(),Variable.VAR);
				}
			}

			for(MyWhileExt ext : extensions)
				ext.saveExpression(exp.toString(), exp.type().toString());
		}
		else if(exp instanceof Binary){
			// If expression is a binary expression
			Expr operandLeft = ((Binary) exp).left();
			Expr operandRight = ((Binary) exp).right();
			if(operandLeft instanceof Local){
				// If operand is a variable
				for(MyWhileExt ext : extensions){
					ext.saveInputVariable(operandLeft.toString(),operandLeft.type().toString(),Variable.VAR);	
				}
			}
			if(operandRight instanceof Local){
				// If operand is a variable
				for(MyWhileExt ext : extensions){
					ext.saveInputVariable(operandRight.toString(),operandRight.type().toString(),Variable.VAR);
				}
			}
			// For now, not saving unary or binary expressions - but their components instead
			for(MyWhileExt ext : extensions)
				ext.saveExpression(exp.toString(), exp.type().toString());
		}
		else if(exp instanceof Call){
			// If expression is a user defined function call
			// TODO
			
			// Else if the expression is a library function call
			for(MyWhileExt ext : extensions){
				List<Node> reads = JavaLibModel.extractReads((Call)exp,ext);
				for(Node node : reads){
					if(node instanceof Receiver && !(node instanceof Lit) ){
						ext.saveInputVariable(node.toString(),((Receiver)node).type().toString(),Variable.FIELD_ACCESS);
					}
					else if(node instanceof Expr){
						extractReadsFromExpr((Expr)node);
					}
				}
			}
		}
		else if(exp instanceof Field){
			// If right hand side is a field load
			for(MyWhileExt ext : extensions){
				ext.saveInputVariable(exp.toString(), exp.type().toString(), ((Field) exp).target().type().toString(),Variable.FIELD_ACCESS);
			}
		}
		else if(exp instanceof ArrayAccess){
			// If expression is an array access
			for(MyWhileExt ext : extensions){
				Expr index = ((ArrayAccess)exp).index();
				
				// Very naive. Should scan previous code. Locals may be holding constant values.
				if(index instanceof Local){
					// Save the array
					ext.saveInputVariable(((ArrayAccess)exp).array().toString(), ((ArrayAccess)exp).array().type().toString(),Variable.ARRAY_ACCESS);
				
					// Save the index (unless it is a constant)
					ext.saveInputVariable(index.toString(), index.type().toString(),Variable.VAR);
				}
				else if(index instanceof Lit){
					// Save the array
					ext.saveInputVariable(((ArrayAccess)exp).array().toString(), ((ArrayAccess)exp).array().type().toString(),Variable.CONST_ARRAY_ACCESS);
				}
			}
		}
		else if(exp instanceof Cast){
			// If expression is being casted
			extractReadsFromExpr(((Cast) exp).expr());
		}
		else if(exp instanceof Lit){
			// Ignore
		}
		else{
			if(debug){
				// Something weird happened
				System.err.print("NOT SURE! ");
				System.err.print(exp.getClass().getName());
		        System.err.print(" : " );
		        System.err.println(exp.toString());
			}
		}
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
		if(n instanceof ExtendedFor){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				// begin extraction
				MyWhileExt ext = (MyWhileExt)JavaExt.ext(n);
				this.extensions.add(ext);
				ext.saveInputVariable(((ExtendedFor) n).expr().toString(), ((ExtendedFor) n).expr().type().toString(), Variable.ARRAY_ACCESS);
			}
		}
		
		// If we are not extracting, then do nothing
		if(this.extensions.size() == 0) return this;
		
		if(n instanceof Assign){
			// Assignment statement	
			extractReadsFromExpr((((Assign) n).right()));
		}
		else if(n instanceof If){
			// If statement
			extractReadsFromExpr(((If) n).cond());
		}
		else if(n instanceof While){
			// While statement
			extractReadsFromExpr(((While) n).cond());
		}
		else if(n instanceof Switch){
			// Switch statement
			extractReadsFromExpr(((Switch) n).expr());
		}
		else if(n instanceof LocalDecl){
			// Local declaration statement
			extractReadsFromExpr(((LocalDecl) n).init());
			
			// Save local variable in each ext
			for(MyWhileExt ext : extensions)
				ext.saveLocalVariable(((LocalDecl) n).id().toString(), ((LocalDecl) n).type().toString());
		}
		else if(n instanceof Call){
			// If expression is a user defined function call
			// TODO
			
			// Else if the expression is a library function call
			for(MyWhileExt ext : extensions){
				List<Node> reads = JavaLibModel.extractReads((Call)n,ext);
				for(Node node : reads){
					if(node instanceof Receiver && !(node instanceof Lit) ){
						ext.saveInputVariable(node.toString(),((Receiver)node).type().toString(),Variable.FIELD_ACCESS);
					}
					else if(node instanceof Expr){
						extractReadsFromExpr((Expr)node);
					}
				}
			}
		}
		else if(n instanceof Return){
			// Return statement
			extractReadsFromExpr(((Return) n).expr());
		}
		
		return this;
	}
   
   
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			// If the loop was marked as interesting
			if(((MyWhileExt)JavaExt.ext(n)).interesting){
				((MyWhileExt)JavaExt.ext(n)).savePendingInputVariables();
				
				if(debug){
					System.err.println("Input:\n"+((MyWhileExt)JavaExt.ext(n)).inputVars.toString());
					System.err.println("Local:\n"+((MyWhileExt)JavaExt.ext(n)).localVars.toString());
					System.err.println("Expressions:\n"+((MyWhileExt)JavaExt.ext(n)).expUsed.toString());
				}
				
				this.extensions.remove(((MyWhileExt)JavaExt.ext(n)));
			}
		}
       
		return n;
	}
   
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished input var extraction complier pass *************");
	}
}
