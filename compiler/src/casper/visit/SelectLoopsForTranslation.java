/* 
 * This class implements a single compiler pass. The pass is executed 
 * after the code has been loop-normalized and expression-flattened.
 * The goal of the pass is to identify all loops that we should attempt 
 * to parallelize.
 * 
 * Currently all loops are considered 'interesting' unless they contain
 * either an unrecognized function call (from an external library we 
 * do not support) or have branching points.
 *    
 * - Maaz
 */

package casper.visit;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import casper.JavaLibModel;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import polyglot.ast.Block;
import polyglot.ast.Branch;
import polyglot.ast.Call;
import polyglot.ast.If;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class SelectLoopsForTranslation extends NodeVisitor{
	ArrayList<MyWhileExt> extensions;
	Stack<Integer> branchCounts;
	Stack<Block> parents;
	Stack<Boolean> ignore;
	int currBranchCount;
   	boolean debug;
   	
   
   	@SuppressWarnings("deprecation")
	public SelectLoopsForTranslation(){
   		this.currBranchCount = 0;
	   	this.debug = false;
	   	this.extensions = new ArrayList<MyWhileExt>();
	   	this.branchCounts = new Stack<Integer>();
	   	this.parents = new Stack<Block>();
	   	this.ignore = new Stack<Boolean>();
	   	ignore.add(true);
   	}
   
   	@Override
	public NodeVisitor enter(Node parent, Node n){
   		
   		// If the node is a block of method
   		if(n instanceof Block && parent instanceof MethodDecl){
   			this.parents.push((Block)n);
   		}
		// If the node is a while loop
   		else if(n instanceof While){
			// Get extension of loop node
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			// Save parent
   			ext.parent = this.parents.peek();
			
			// Set this loop's body as parent for all inner loops
			Stmt loopBody = ((While) n).body();
			if(loopBody instanceof Block){
				this.parents.push((Block)loopBody);
			}
			
			// Mark loop as interesting
			ext.interesting = true;
			
			// Save current branch count for outer loop
			branchCounts.push(this.currBranchCount);
			
			// Reset current branch count to 0
			this.currBranchCount = 0;
		
			// Add to the set of loops being traversed.
			this.extensions.add(ext);
			
			// Mark loop body to be checked for conditionals
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
   		else if(n instanceof ExtendedFor){
			// Get extension of loop node
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			// Save parent
			ext.parent = this.parents.peek();
			
			// Set this loop's body as parent for all inner loops
			Stmt loopBody = ((ExtendedFor) n).body();
			if(loopBody instanceof Block){
				this.parents.push((Block)loopBody);
			}
			
			// Mark loop as interesting
			ext.interesting = true;
			
			// Save current branch count for outer loop
			branchCounts.push(this.currBranchCount);
			
			// Reset current branch count to 0
			this.currBranchCount = 0;
		
			// Add to the set of loops being traversed.
			this.extensions.add(ext);
			
			// Mark loop body to be checked for conditionals
			if(loopBody instanceof Block){
				MyStmtExt bodyExt = (MyStmtExt)JavaExt.ext(loopBody);
				bodyExt.process = true;
			}
		}
   		// If the node is a function call
		else if(n instanceof Call){
			// Do we recognize this function call?
			if(!JavaLibModel.recognizes((Call)n)){
				// We don't. So these loops cannot be optimized
				for(MyWhileExt ext : this.extensions){
					ext.interesting = false;
				}
			}
		}
		else if(n instanceof Block){
			// If statement
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			
			if(stmtext.process)
				this.ignore.push(false);
		}
   		// If the node is a conditional
		else if(n instanceof If && !this.ignore.peek()){
			for(MyWhileExt ext : this.extensions)
				ext.foundConditionals = true;
		}
   		// If the node is a branching statement
		else if(n instanceof Branch){
			// Increase count of branch statements
			this.currBranchCount++;
		}
     
		return this;
	}
  
  
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			// Get extension of loop node
			MyWhileExt ext = ((MyWhileExt)JavaExt.ext(n));
			
			// Were there more than one branches in the loop?
			if(this.currBranchCount > 1){
				ext.interesting = false;
			}
			else{
				if(debug)
				{
					System.err.println(n);
					System.err.println(((While) n).body());
				}
			}
			
			this.parents.pop();
			
			// Reset currBranchCount to outer loop branch count
			this.currBranchCount = this.branchCounts.pop();
			
			// We have exited the loop
			this.extensions.remove(ext);
		}
		else if(n instanceof Block){
			MyStmtExt stmtext = (MyStmtExt)JavaExt.ext(n);
			if(stmtext.process){
				this.ignore.pop();
				stmtext.process = false;
			}
		}
      
		return n;
	}
  
	@Override
	public void finish(){
		if(debug){
			System.err.println("\n************* Finished loop extraction complier pass *************");
		}
	}
}
