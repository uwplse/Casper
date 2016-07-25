/*
 * Generate the scaffold for sketch. Some work has already been done
 * in the previous compiler passes.
 * 
 * - Maaz
 */

package casper.visit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import casper.Configuration;
import casper.JavaLibModel;
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.extension.MyWhileExt.Variable;
import casper.types.CallNode;
import casper.types.ConditionalNode;
import casper.types.ConstantNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Eval;
import polyglot.ast.Expr;
import polyglot.ast.If;
import polyglot.ast.LocalDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.visit.NodeVisitor;

public class GenerateVerification extends NodeVisitor {
	
	boolean debug;
	NodeFactory nf;
	
	public GenerateVerification (NodeFactory nf) {
		this.debug = false;
		this.nf = nf;
	}
	
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While){
			// Get extension of loop node
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			// Extract Initial Values of all input output variables
			extractInitialValues(n, ext);
			
			// Extract Loop Increment
			extractLoopIncrement((While)n, ext);
			
			// If the loop was marked as interesting
			if(ext.interesting){
				// Generate the order for postcondition function arguments
				Set<String> handledTypes = new HashSet<String>();
				for(Variable var : ext.outputVars){
					// Get sketch type
					String sketchType = casper.Util.getSketchType(var.getType());
					
					// Have we already handled this case?
					if(handledTypes.contains(sketchType)){
						continue;
					}
					
					// Add to handled types
					handledTypes.add(sketchType);
					
					// Add post condition args order for this type
					ext.postConditionArgsOrder.put(sketchType,generatePostConditionArgsOrder(sketchType,ext.outputVars,ext.loopCounters));
					
					// Add loop invariant args order for this type
					ext.loopInvariantArgsOrder.put(sketchType,generateLoopInvariantArgsOrder(sketchType,ext.outputVars,ext.loopCounters));
					
					// Construct post condition statement
					CustomASTNode postCond = generatePostCondition(sketchType,ext.postConditionArgsOrder,ext.initVals);
					ext.postConditions.put(sketchType,postCond);
					
					// Construct loop invariant statement
					CustomASTNode loopInv = generateLoopInvariant(sketchType,ext.parent,ext.loopInvariantArgsOrder,ext.terminationCondition,ext.initVals);
					ext.invariants.put(sketchType,loopInv);
					
					// Construct pre condition of loop
					CustomASTNode preCond =  generatePreCondition(sketchType,ext.parent,ext.loopInvariantArgsOrder,loopInv,ext.initVals);
					ext.preConditions.put(sketchType,preCond);
					
					// Construct pre condition statement of I for loop Body
					Map<String,CustomASTNode> wpcValues = new HashMap<String,CustomASTNode>();
					Stmt loopBody = ((While) n).body();
					CustomASTNode preCondBlock = generateWPC(sketchType,ext.parent,ext.loopInvariantArgsOrder,ext.terminationCondition,ext.initVals,wpcValues);
					MyStmtExt blockExt = (MyStmtExt) JavaExt.ext(loopBody);
					blockExt.preConditions.put(sketchType, preCondBlock);
					ext.wpcValues = generateWPCValues(sketchType,wpcValues,loopBody);
				}
			}
		}
		
		return this;
	}

	private Map<String, CustomASTNode> generateWPCValues(String type, Map<String, CustomASTNode> wpcValues, Stmt body) {
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
						for(String varname : wpcValues.keySet()){
							wpcValues.put(varname, generatePreCondition(type,((If) currStatement).consequent(),wpcValues.get(varname)));
						}
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
						// Must be the increment if, process it first
						for(String varname : wpcValues.keySet()){
							wpcValues.put(varname, generatePreCondition(type,((If) currStatement).alternative(),wpcValues.get(varname)));
						}
					}
					// And alternative contains a break
					else if(casper.Util.containsBreak(alt)){
						// Must be the increment if, process it first
						for(String varname : wpcValues.keySet()){
							wpcValues.put(varname, generatePreCondition(type,((If) currStatement).consequent(),wpcValues.get(varname)));
						}
					}
				}
			}
		}
		return wpcValues;
	}

	private CustomASTNode generateWPC(String type, Node parent, Map<String, List<String>> loopInvArgs, CustomASTNode tcond, Map<String, CustomASTNode> initVals, Map<String, CustomASTNode> wpcValues) {
		String fname = "loopInvariant";
		
		boolean alt = false;
		ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
		args.add(new IdentifierNode(loopInvArgs.get(type).get(0)));
		for(int i=1; i<loopInvArgs.get(type).size(); i++){
			String varname = loopInvArgs.get(type).get(i);
			CustomASTNode arg;
			if(alt){
				CustomASTNode n1;
				if(initVals.containsKey(varname.substring(0,varname.length()-1)) && initVals.get(varname.substring(0,varname.length()-1)) instanceof ConstantNode){
					n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
				}
				else{
					n1 = new IdentifierNode(varname.toString());
					wpcValues.put(varname, n1);
				}
				arg = n1;
			}
			else{
				arg = new IdentifierNode("ind_"+varname);
				wpcValues.put(varname, new IdentifierNode(varname));
			}
			args.add(arg);
			alt = !alt;
		}
		return new CallNode(fname,args);
	}

	private void extractInitialValues(Node n, MyWhileExt ext) {
		List<Stmt> filteredStmts = new ArrayList<Stmt>();
		addStatementsBeforeWhile(n, ext.parent,filteredStmts);
		Stmt block = nf.Block(ext.parent.position(),filteredStmts);
		
		for(Variable var : ext.inputVars){
			CustomASTNode n1 = new IdentifierNode(var.varName);
			CustomASTNode n2 = generatePreCondition("initValExt",block,n1);
			if(!n1.equals(n2))
				ext.initVals.put(var.varName.replace("$", ""),n2);
		}
		
		for(Variable var : ext.outputVars){
			if(!ext.inputVars.contains(var)){
				CustomASTNode n1 = new IdentifierNode(var.varName);
				CustomASTNode n2 = generatePreCondition("initValExt",block,n1);
				if(!n1.equals(n2))
					ext.initVals.put(var.varName.replace("$", ""),n2);
			}
		}
		
		for(Variable var : ext.loopCounters){
			CustomASTNode n1 = new IdentifierNode(var.varName);
			CustomASTNode n2 = generatePreCondition("initValExt",block,n1);
			if(!n1.equals(n2))
				ext.initVals.put(var.varName.replace("$", ""),n2);
		}
		
		if(debug)
			System.err.println(ext.initVals);
	}
	
	private void extractLoopIncrement(While n, MyWhileExt ext) {
		Stmt block = null;
		
		// Extract the increment block
		Stmt body = n.body();
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
						block = cons;
						break;
					}
				}
			}
		}
		
		for(Variable lc : ext.loopCounters){
			CustomASTNode n1 = new IdentifierNode(lc.varName);
			CustomASTNode n2 = generatePreCondition("incExpExt",block,n1);
			ext.incrementExps.add(n2);
		}
	}

	private CustomASTNode generateVerificationConditions(String type, Stmt body, CustomASTNode postCondition) {
		// TODO: Some pre-processing to remove extra breaks and returns and continues.
		
		CustomASTNode currVerifCondition = postCondition;
		
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
						currVerifCondition = generatePreCondition(type,((If) currStatement).consequent(),currVerifCondition);
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
						return generatePreCondition(type, ((If) currStatement).alternative(),currVerifCondition);
					}
					// And alternative contains a break
					else if(casper.Util.containsBreak(alt)){
						return generatePreCondition(type, ((If) currStatement).consequent(),currVerifCondition);
					}
				}
			}
		}
		
		// Should not be reached
		System.err.println("Error: Unexpected loop body format!");
		return currVerifCondition;
	}

	private CustomASTNode generatePreCondition(String type, Stmt body, CustomASTNode postCondition){
		CustomASTNode currVerifCondition = postCondition;
		
		if(body instanceof Block){
			List<Stmt> statements = ((Block) body).statements();
			
			for(int i=statements.size()-1; i>=0; i--){
				// Get the statement
				Stmt currStatement = statements.get(i);
				
				if(debug){
					System.err.println(currVerifCondition);
					System.err.println(currStatement);
				}
				
				// Get extension of statement
				MyStmtExt ext = (MyStmtExt) JavaExt.ext(currStatement);
					
				if(currStatement instanceof Eval){
					Expr expr = ((Eval) currStatement).expr();
					
					if(expr instanceof Assign){
						// Save post-condition
						ext.postConditions.put(type,currVerifCondition);
						
						// Derive pre-condition 
						String lhs = ((Assign)expr).left().toString();
						Expr rhs = ((Assign)expr).right();
						
						currVerifCondition = currVerifCondition.replaceAll(lhs, CustomASTNode.convertToAST(rhs));
						
						// Save pre-condition
						ext.preConditions.put(type,currVerifCondition);
					}
					else if(expr instanceof Call){
						// Save post-condition
						ext.postConditions.put(type,currVerifCondition);
						
						currVerifCondition = JavaLibModel.updatePreCondition((Call)expr,currVerifCondition);
						
						// Save pre-condition
						ext.preConditions.put(type,currVerifCondition);
					}
					else{
						System.err.println("Unexpected Eval type: " + expr.getClass() + " :::: " + expr);
					}
				}
				else if(currStatement instanceof LocalDecl){
					// Save post-condition
					ext.postConditions.put(type,currVerifCondition);

					// Derive pre-condition
					String lhs = ((LocalDecl)currStatement).name();
					Expr rhs = ((LocalDecl)currStatement).init();
 
					currVerifCondition = currVerifCondition.replaceAll(lhs, CustomASTNode.convertToAST(rhs));
					
					// Save pre-condition
					ext.preConditions.put(type,currVerifCondition);
				}
				else if(currStatement instanceof If){
					// Save post-condition
					ext.postConditions.put(type,currVerifCondition);
					
					// Derive pre-condition
					Stmt cons = ((If) currStatement).consequent();
					Stmt alt = ((If) currStatement).alternative();
					Expr cond = ((If) currStatement).cond();
					
					CustomASTNode loopCond = CustomASTNode.convertToAST(cond);
					
					CustomASTNode verifCondCons = generatePreCondition(type,cons,currVerifCondition);
					
					CustomASTNode verifCondAlt;
					if(alt != null)
						verifCondAlt = generatePreCondition(type,alt,currVerifCondition);
					else
						verifCondAlt = currVerifCondition;
					
					if(!verifCondCons.toString().equals(verifCondAlt.toString()))
						currVerifCondition = new ConditionalNode(loopCond,verifCondCons,verifCondAlt);
					
					// Save pre-condition
					ext.preConditions.put(type, currVerifCondition);
				}
				else if(currStatement instanceof Block){
					// Save post-condition
					ext.postConditions.put(type,currVerifCondition);
					
					// Derive pre-condition
					currVerifCondition = generatePreCondition(type,currStatement,currVerifCondition);
					
					// Save pre-condition
					ext.preConditions.put(type, currVerifCondition);
				}
			}
		}
		else{
			// This should never be the case
			System.err.println("Error: body was not an instance of Block. ("+ body.getClass() +")");
		}
		
		return currVerifCondition;
	}
	
	private CustomASTNode generatePostCondition(String type, Map<String, List<String>> postCondArgs, Map<String, CustomASTNode> initVals) {
		String fname = "postCondition";
		boolean alt = false;
		boolean first = true;
		ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
		for(String varname : postCondArgs.get(type)){
			CustomASTNode arg;
			if(alt){
				CustomASTNode n1;
				if(initVals.containsKey(varname.substring(0,varname.length()-1)) && initVals.get(varname.substring(0,varname.length()-1)) instanceof ConstantNode){
					n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
				}
				else{
					n1 = new IdentifierNode(varname.toString());
				}
				arg = n1;
			}
			else{
				arg = new IdentifierNode(varname);
			}
			args.add(arg);
			if(!first){
				alt = !alt;
			}
			else{
				first = !first;
			}
		}
		return new CallNode(fname,args);
	}
	
	private CustomASTNode generateLoopInvariant(String type, Node parent, Map<String, List<String>> loopInvArgs, CustomASTNode tCond, Map<String, CustomASTNode> initVals) {
		String fname = "loopInvariant";
		
		boolean alt = false;
		ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
		args.add(new IdentifierNode(loopInvArgs.get(type).get(0)));
		for(int i=1; i<loopInvArgs.get(type).size(); i++){
			String varname = loopInvArgs.get(type).get(i);
			CustomASTNode arg;
			if(alt){
				CustomASTNode n1;
				if(initVals.containsKey(varname.substring(0,varname.length()-1)) && initVals.get(varname.substring(0,varname.length()-1)) instanceof ConstantNode){
					n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
				}
				else{
					n1 = new IdentifierNode(varname.toString());
				}
				arg = n1;
			}
			else{
				arg = new IdentifierNode(varname);
			}
			args.add(arg);
			alt = !alt;
		}
		return new CallNode(fname,args);
	}
	
	private CustomASTNode generatePreCondition(String type, Node parent, Map<String, List<String>> loopInvArgs, CustomASTNode loopInv, Map<String, CustomASTNode> initVals) {
		CustomASTNode pCond = loopInv;
		boolean alt = false;
		for(String varname : loopInvArgs.get(type)){
			if(alt){
				if(initVals.containsKey(varname)){
					pCond = pCond.replaceAll(varname, new IdentifierNode(initVals.get(varname).toString()));
				}
				else{
					pCond = pCond.replaceAll(varname, new IdentifierNode(varname+"0"));
				}
			}
			alt = !alt;
		}
		return pCond;
	}

	private boolean addStatementsBeforeWhile(Node loop, Node parent, List<Stmt> filteredStmts) {
		if(parent instanceof Block){
			List<Stmt> stmts = ((Block) parent).statements();
			
			for(Stmt s : stmts){
				// We've reached a loop
				if(s instanceof While){
					// Is it the loop we are currently processing?
					if(s.position() == loop.position()){
						// Yes, we're done.
						return true;
					}
					else{
						// Some other loop, so clear filtered Stmts (since currently, we only go back
						// till the previous loop or beginning of function -- whichever comes first)
						filteredStmts.clear();
					}
				}
				else if(s instanceof Block){
					if(addStatementsBeforeWhile(loop,s,filteredStmts))
						return true;
				}
				else{
					filteredStmts.add(s);
				}
			}
		}
		return false;
	}

	private List<String> generatePostConditionArgsOrder(String outputType, Set<Variable> outputVars, List<Variable> loopCounters) {
		List<String> order = new ArrayList<String>();
		// Add input data as arg
		order.add("input_data");
		
		// Add output variables
		for(Variable var : outputVars){
			// If desired type, add as option
			if(casper.Util.getSketchType(var.getType()).equals(outputType) || casper.Util.getSketchType(var.getType()).equals(outputType.replace("["+Configuration.arraySizeBound+"]", ""))){
				String varname = var.varName.replace("$", "");
				order.add(varname);
				order.add(varname+"0");
			}
		}
		
		// Add loop counters
		for(Variable var : loopCounters){
			// If desired type, add as option
			if(casper.Util.getSketchType(var.getType()).equals(outputType) || casper.Util.getSketchType(var.getType()).equals(outputType.replace("["+Configuration.arraySizeBound+"]", ""))){
				String varname = var.varName.replace("$", "");
				order.add(varname);
				order.add(varname+"0");
			}
		}
		
		return order;
	}

	private List<String> generateLoopInvariantArgsOrder(String outputType, Set<Variable> outputVars, List<Variable> loopCounters) {
		List<String> order = new ArrayList<String>();
		// Add input data as arg
		order.add("input_data");
		
		// Add output variables
		for(Variable var : outputVars){
			// If desired type, add as option
			if(casper.Util.getSketchType(var.getType()).equals(outputType) || casper.Util.getSketchType(var.getType()).equals(outputType.replace("["+Configuration.arraySizeBound+"]", ""))){
				String varname = var.varName.replace("$", "");
				order.add(varname);
				order.add(varname+"0");
			}
		}
		
		// Add loop counters
		for(Variable var : loopCounters){
			// If desired type, add as option
			if(casper.Util.getSketchType(var.getType()).equals(outputType) || casper.Util.getSketchType(var.getType()).equals(outputType.replace("["+Configuration.arraySizeBound+"]", ""))){
				String varname = var.varName.replace("$", "");
				order.add(varname);
				order.add(varname+"0");
			}
		}
		return order;
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished generate verification complier pass *************");
	}
}