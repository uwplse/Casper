/* 
 * This class implements a data flow analysis. The compiler pass is executed 
 * after the loop termination conditions have been extracted.
 * 
 * When we use the expression flattener and loop normalizer to simplify
 * the source code, it often introduces new local variables to hold intermediary
 * results. The extracted loop conditions can therefore consist of such
 * generated variables instead of using variables only from the input variable
 * set. The goal for this analysis is to determine what intermediate results
 * these variables hold so that we can translate the condition back to using
 * only input variables.
 *    
 * We perform a backward flowing taint analysis. The loop condition after
 * flattening always contains a single variable. That variable forms our
 * initial tainted set.
 *    
 * - Maaz
 */

package casper.visit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.CustomASTNode;
import polyglot.ast.ArrayAccess;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Cast;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.If;
import polyglot.ast.Lit;
import polyglot.ast.Local;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.Unary;
import polyglot.ast.While;
import polyglot.visit.NodeVisitor;

public class ExtractLoopTerminateConditions extends NodeVisitor {
	
	boolean debug;
	CustomASTNode terminationCondition;
	
	@SuppressWarnings("deprecation")
	public ExtractLoopTerminateConditions ()
	{
		this.debug = false;
		this.terminationCondition = null;
	}
	
	@Override
	public NodeVisitor enter(Node parent, Node n){
   		
		// If the node is a loop
		if(n instanceof While){
			// Get extension of loop node
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			// If loop is marked
			if((ext.interesting)){
				Stmt loopBody = ((While) n).body();
				updateTerminateConditionBlock(loopBody, ext, false);
				ext.terminationCondition = this.terminationCondition;
				this.terminationCondition = null;
				
				if(debug){
					System.err.println(ext.terminationCondition);
				}
			}
		}
		
		return this;
	}
  
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		return n;
	}
	
	public void updateTerminateConditionBlock(Stmt block, MyWhileExt ext, boolean nested){
		
		if(block instanceof Block){
			List<Stmt> statements = ((Block) block).statements();
			
			int st_index = statements.size() - 1;
			
			// Extract initial condition
			if(!nested){
				for(int i=0; i<statements.size(); i++){
					Stmt currStatement = statements.get(i);
					
					// Satement is a conditional
					if(currStatement instanceof If){
						Stmt cons = ((If) currStatement).consequent();
						Stmt alt = ((If) currStatement).alternative(); 
						
						// And consequent contains a break
						if(casper.Util.containsBreak(cons)){
							ext.condInv = false;
							this.terminationCondition = CustomASTNode.convertToAST(((If) currStatement).cond());
							st_index = i-1;
							break;
						}
						// And alternative contains a break
						else if(casper.Util.containsBreak(alt)){
							ext.condInv = true;
							this.terminationCondition = CustomASTNode.convertToAST(((If) currStatement).cond());
							st_index = i-1;
							break;
						}
					}
				}
			}
			
			// Convert expression to not have local variables
			// Only consider statements before the if(%tc){..}
			for(int i = st_index; i>=0; i--){
				Stmt currStatement = statements.get(i);
				
				// Assignment etc may be wrapped in Eval -- unwrap them
				if(currStatement instanceof Eval){
					Expr expr = ((Eval) currStatement).expr();
					
					// If its an assignment statement
					if(expr instanceof Assign){
						if(debug){
							System.err.println("Statement: "+ expr);
							System.err.println("Before: " + this.terminationCondition);
						}
						
						// Process the expression.
						updateTerminateCondition(expr,ext);
						
						if(debug){
							System.err.println("After: " + this.terminationCondition + "\n");
						}
					}
				}
				// If it's a local declaration + assignment statement
				else if(currStatement instanceof LocalDecl){
					if(debug){
						System.err.println("Statement: "+ currStatement);
						System.err.println("Before: " + this.terminationCondition);
					}
					
					// Process the expression.
					updateTerminateCondition(currStatement,ext);
					
					if(debug){
						System.err.println("After: " + this.terminationCondition + "\n");
					}
				}
				else {
					// System.err.println("Unexpected statement type: " + currStatement.getClass() + " ::::: " + currStatement);
				}
			}
		}
		else{
			// This should never be the case
			System.err.println("Error: block was not an instance of Block. ("+ block.getClass() +")");
		}
	}
	
	// Taint an assignment expression.
	public void updateTerminateCondition(Node n, MyWhileExt ext){
		String lhs = null;
		Expr rhs = null;
		
		if(n instanceof Assign){
			lhs = ((Assign)n).left().toString();
			rhs = ((Assign)n).right();
		}
		else if(n instanceof LocalDecl){
			lhs = ((LocalDecl)n).name();
			rhs = ((LocalDecl)n).init();
		}
		
		// Is rhs a local
		if(	rhs instanceof Local		|| 
			rhs instanceof Lit			|| 
			rhs instanceof Field 		|| 
			rhs instanceof Call			||
			rhs instanceof Cast			||
			rhs instanceof ArrayAccess)	{
			
			// Replace all instances of lhs with rhs of the expression.
			this.terminationCondition = this.terminationCondition.replaceAll(lhs,CustomASTNode.convertToAST(rhs));
		}
		else if(rhs instanceof Unary){
			// Only replace if operator is "!"
			String opp = ((Unary) rhs).operator().toString();
			if(opp.equals("!")){
				// Replace all instances of lhs with rhs of the expression.
				this.terminationCondition = this.terminationCondition.replaceAll(lhs,CustomASTNode.convertToAST(rhs));
			}
		}
		else if(rhs instanceof Binary){
			Set<String> relOps = new HashSet<String>();
			relOps.add("==");
		   	relOps.add("!=");
		   	relOps.add("<");
		   	relOps.add("<=");
		   	relOps.add(">");
		   	relOps.add(">=");
		   	relOps.add("&&");
		   	relOps.add("||");
		   	relOps.add("!");
		   	
			// Only replace if operator is relational
		   	String opp = ((Binary) rhs).operator().toString();
			if(relOps.contains(opp)){
				// Replace all instances of lhs with rhs of the expression.
				this.terminationCondition = this.terminationCondition.replaceAll(lhs,CustomASTNode.convertToAST(rhs));
			}
		}
	}	
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished formulate loop terminate conditions complier pass *************");
	}
}