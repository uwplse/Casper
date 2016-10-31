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
import casper.ast.JavaExt;
import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import casper.types.BinaryOperatorNode;
import casper.types.CallNode;
import casper.types.ConstantNode;
import casper.types.CustomASTNode;
import casper.types.IdentifierNode;
import casper.types.Variable;
import polyglot.ast.Block;
import polyglot.ast.If;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class GenerateVerification extends NodeVisitor {
	
	boolean debug;
	NodeFactory nf;
	
	@SuppressWarnings("deprecation")
	public GenerateVerification (NodeFactory nf) {
		this.debug = false;
		this.nf = nf;
	}
	
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			// Get extension of loop node
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			// Extract Initial Values of all input, output and lc variables
			extractInitialValues(n, ext);
			if(n instanceof ExtendedFor)
				ext.initVals.put("casper_i", new ConstantNode("0",ConstantNode.INTLIT));
			
			// If the loop was marked as interesting
			if(ext.interesting){
				// Generate verification conditions for each output type
				Set<String> handledTypes = new HashSet<String>();
				for(Variable var : ext.outputVars){
					// Get sketch type
					String reduceType = var.getReduceType();
					
					// Have we already handled this case?
					if(handledTypes.contains(reduceType)){
						continue;
					}
					handledTypes.add(reduceType);
					
					// Add to handled types
					handledTypes.add(reduceType);
					
					// Add post condition args order for this type
					ext.postConditionArgsOrder.put(reduceType,generatePostConditionArgsOrder(casper.Util.reducerType(var.getSketchType()),ext.outputVars,ext.loopCounters));
					
					// Add loop invariant args order for this type
					ext.loopInvariantArgsOrder.put(reduceType,generateLoopInvariantArgsOrder(casper.Util.reducerType(var.getSketchType()),ext.outputVars,ext.loopCounters));
					
					// Construct post condition statement
					CustomASTNode postCond = generatePostCondition(reduceType,ext.postConditionArgsOrder,ext.initVals);
					ext.postConditions.put(reduceType,postCond);
					
					// Construct loop invariant statement
					CustomASTNode loopInv = generateLoopInvariant(reduceType,ext.parent,ext.loopInvariantArgsOrder,ext.terminationCondition,ext.initVals);
					ext.invariants.put(reduceType,loopInv);
					
					// Construct pre condition of loop
					CustomASTNode preCond =  generatePreCondition(reduceType,ext.parent,ext.loopInvariantArgsOrder,loopInv,ext.initVals);
					ext.preConditions.put(reduceType,preCond);
					
					// Construct pre condition statement of I for loop Body
					Map<String,CustomASTNode> wpcValues = new HashMap<String,CustomASTNode>();
					Stmt loopBody = null;
					if(n instanceof While)
						loopBody = ((While) n).body();
					else
						loopBody = ((ExtendedFor)n).body();
					CustomASTNode preCondBlock = generateWPC(reduceType,ext.parent,ext.loopInvariantArgsOrder,ext.terminationCondition,ext.initVals,wpcValues);
					MyStmtExt blockExt = (MyStmtExt) JavaExt.ext(loopBody);
					blockExt.preConditions.put(reduceType, preCondBlock);
					ext.wpcValues = generateWPCValues(reduceType,wpcValues,loopBody,ext);
					
					if(ext.inputDataCollections.size()>1){
						fixArrayAccesses(ext.wpcValues);
					}
					
					if(debug){
						System.err.println(ext.preConditions);
						System.err.println(ext.invariants);
						System.err.println(blockExt.preConditions);
						System.err.println(ext.wpcValues);
						System.err.println(ext.postConditions);
					}
				}
			}
		}
		
		return this;
	}

	// Extract Initial Values of all input output variables
	private void extractInitialValues(Node n, MyWhileExt ext) {
		List<Stmt> filteredStmts = new ArrayList<Stmt>();
		addStatementsBeforeWhile(n, ext.parent,filteredStmts);
		Stmt block = nf.Block(ext.parent.position(),filteredStmts);
		
		// Mapping of string literals to integers
		Map<String,Integer> strToIntMap = new HashMap<String,Integer>();
		int intVal = 0;
		
		for(Variable var : ext.inputVars){
			CustomASTNode n1 = new IdentifierNode(var.varNameOrig);
			CustomASTNode n2 = casper.Util.generatePreCondition("initValExt",block,n1,ext,debug);
			if(!n1.equals(n2) && n2 instanceof ConstantNode){
				if(casper.Util.getSketchTypeFromRaw(var.varType) == "int" && ((ConstantNode)n2).type == ConstantNode.STRINGLIT){
					if(!strToIntMap.containsKey(n2.toString())){
						strToIntMap.put(n2.toString(), intVal);
						intVal++;
					}
					ext.initVals.put(var.varName,new ConstantNode(strToIntMap.get(n2.toString()).toString(),ConstantNode.STRINGLIT));
				}
				else{
					ext.initVals.put(var.varName,n2);
				}
			}
		}
		
		for(Variable var : ext.outputVars){
			if(!ext.inputVars.contains(var)){
				CustomASTNode n1 = new IdentifierNode(var.varNameOrig);
				CustomASTNode n2 = casper.Util.generatePreCondition("initValExt",block,n1,ext,debug);
				if(!n1.equals(n2) && n2 instanceof ConstantNode){
					ext.initVals.put(var.varName,n2);
				}
			}
		}
		
		for(Variable var : ext.loopCounters){
			CustomASTNode n1 = new IdentifierNode(var.varNameOrig);
			CustomASTNode n2 = casper.Util.generatePreCondition("initValExt",block,n1,ext,debug);
			if(!n1.equals(n2) && n2 instanceof ConstantNode){
				ext.initVals.put(var.varName,n2);
			}
		}
		
		if(debug)
			System.err.println(ext.initVals);
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
						// till the previous loop or beginning of block -- whichever comes first)
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
	
	private List<String> generatePostConditionArgsOrder(String outputType, Set<Variable> outputVars, Set<Variable> loopCounters) {
		List<String> order = new ArrayList<String>();
		// Add input data as arg
		order.add("casper_data_set");
		
		// Add output variables
		for(Variable var : outputVars){
			// If desired type, add as option
			if(casper.Util.reducerType(var.getSketchType()).equals(outputType)){
				order.add(var.varName);
				order.add(var.varName+"0");
			}
		}
		
		// Add loop counters
		for(Variable var : loopCounters){
			order.add(var.varName);
			order.add(var.varName+"0");
		}
		return order;
	}
	
	private List<String> generateLoopInvariantArgsOrder(String outputType, Set<Variable> outputVars, Set<Variable> loopCounters) {
		List<String> order = new ArrayList<String>();
		// Add input data as arg
		order.add("casper_data_set");
		
		// Add output variables
		for(Variable var : outputVars){
			// If desired type, add as option
			if(casper.Util.reducerType(var.getSketchType()).equals(outputType)){
				String varname = var.varName;
				order.add(varname);
				order.add(varname+"0");
			}
		}
		
		// Add loop counters
		for(Variable var : loopCounters){
			String varname = var.varName;
			order.add(varname);
			order.add(varname+"0");
		}
		return order;
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
				if(initVals.containsKey(varname.substring(0,varname.length()-1))){
					CustomASTNode val = initVals.get(varname.substring(0,varname.length()-1));
					if(val instanceof ConstantNode && !(((ConstantNode) val).type == ConstantNode.ARRAYLIT)){
						n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
					}
					else{
						n1 = new IdentifierNode(varname.toString());
					}
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
				if(initVals.containsKey(varname.substring(0,varname.length()-1))){
					CustomASTNode val = initVals.get(varname.substring(0,varname.length()-1));
					if(val instanceof ConstantNode && !(((ConstantNode) val).type == ConstantNode.ARRAYLIT)){
						n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
					}
					else{
						n1 = new IdentifierNode(varname.toString());
					}
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
					pCond = pCond.replaceAll(varname, new IdentifierNode(varname+"0"));
				}
				else{
					pCond = pCond.replaceAll(varname, new IdentifierNode(varname+"0"));
				}
			}
			alt = !alt;
		}
		return pCond;
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
					CustomASTNode val = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
					if(val instanceof ConstantNode && !(((ConstantNode) val).type == ConstantNode.ARRAYLIT)){
						n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString());
					}
					else{
						n1 = new IdentifierNode(varname.toString());
					}
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
	
	private Map<String, CustomASTNode> generateWPCValues(String type, Map<String, CustomASTNode> wpcValues, Stmt body, MyWhileExt ext) {
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
						// Must be the increment if, process it firs
						for(String varname : wpcValues.keySet()){
							wpcValues.put(varname, casper.Util.generatePreCondition(type,((If) currStatement).consequent(),wpcValues.get(varname),ext,debug));
							if(varname.equals("ind_casper_i"))
								wpcValues.put(varname, new BinaryOperatorNode("+",new IdentifierNode(varname),new ConstantNode("1",ConstantNode.INTLIT)));
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
							wpcValues.put(varname, casper.Util.generatePreCondition(type,((If) currStatement).alternative(),wpcValues.get(varname),ext,debug));
							if(varname.equals("ind_casper_i"))
								wpcValues.put(varname, new BinaryOperatorNode("+",new IdentifierNode(varname),new ConstantNode("1",ConstantNode.INTLIT)));
						}
					}
					// And alternative contains a break
					else if(casper.Util.containsBreak(alt)){
						// Must be the increment if, process it first
						for(String varname : wpcValues.keySet()){
							wpcValues.put(varname, casper.Util.generatePreCondition(type,((If) currStatement).consequent(),wpcValues.get(varname),ext,debug));
							if(varname.equals("ind_casper_i"))
								wpcValues.put(varname, new BinaryOperatorNode("+",new IdentifierNode(varname),new ConstantNode("1",ConstantNode.INTLIT)));
						}
					}
				}
			}
		}
		return wpcValues;
	}
	
	private void fixArrayAccesses(Map<String, CustomASTNode> wpcValues) {
		for(String var : wpcValues.keySet()){
			CustomASTNode node = wpcValues.get(var);
			node = node.fixArrays();
			wpcValues.put(var, node);
		}
	}
	
	@Override
	public void finish(){
		if(debug)
			System.err.println("\n************* Finished generate verification complier pass *************");
	}
}