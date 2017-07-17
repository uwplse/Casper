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

import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.ArrayAccessNode;
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
import polyglot.util.Pair;
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
			
			// If the loop was marked as interesting
			if(ext.interesting){
				// Extract Initial Values of all input, output and lc variables
				extractInitialValues(n, ext);
				if(n instanceof ExtendedFor)
					ext.initVals.put("casper_index", new ConstantNode("0","int",ConstantNode.INTLIT));
				
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
					CustomASTNode preCondBlock = generateWPC(n,reduceType,ext);
					ext.wpcs.put(reduceType, preCondBlock);
					
					if(debug){
						System.err.println(ext.preConditions);
						System.err.println(ext.invariants);
						System.err.println(ext.wpcs);
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
			CustomASTNode n1 = new IdentifierNode(var.varNameOrig,var.getSketchType());
			CustomASTNode n2 = casper.Util.generatePreCondition("initValExt",block,n1,ext,debug);
			if(!n1.equals(n2) && n2 instanceof ConstantNode){
				if(casper.Util.getSketchTypeFromRaw(var.varType) == "int" && ((ConstantNode)n2).type_code == ConstantNode.STRINGLIT){
					if(!strToIntMap.containsKey(n2.toString())){
						strToIntMap.put(n2.toString(), intVal);
						intVal++;
					}
					ext.initVals.put(var.varName,new ConstantNode(strToIntMap.get(n2.toString()).toString(),"String",ConstantNode.STRINGLIT));
				}
				else{
					ext.initVals.put(var.varName,n2);
				}
			}
		}
		
		for(Variable var : ext.outputVars){
			if(!ext.inputVars.contains(var)){
				CustomASTNode n1 = new IdentifierNode(var.varNameOrig,var.getSketchType());
				CustomASTNode n2 = casper.Util.generatePreCondition("initValExt",block,n1,ext,debug);
				if(!n1.equals(n2) && n2 instanceof ConstantNode){
					ext.initVals.put(var.varName,n2);
				}
			}
		}
		
		for(Variable var : ext.loopCounters){
			CustomASTNode n1 = new IdentifierNode(var.varNameOrig,var.getSketchType());
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
	
	private List<Pair<String, String>> generateLoopInvariantArgsOrder(String outputType, Set<Variable> outputVars, Set<Variable> loopCounters) {
		List<Pair<String, String>> order = new ArrayList<Pair<String,String>>();
		// Add input data as arg
		order.add(new Pair("casper_data_set","CasperDataRecord[]"));
		
		// Add output variables
		for(Variable var : outputVars){
			// If desired type, add as option
			if(casper.Util.reducerType(var.getSketchType()).equals(outputType)){
				String varname = var.varName;
				order.add(new Pair(varname,var.getSketchType()));
				order.add(new Pair(varname+"0",var.getSketchType()));
			}
		}
		
		// Add loop counters
		for(Variable var : loopCounters){
			String varname = var.varName;
			order.add(new Pair(varname,var.getSketchType()));
			order.add(new Pair(varname+"0",var.getSketchType()));
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
					if(val instanceof ConstantNode && !(((ConstantNode) val).type_code == ConstantNode.ARRAYLIT)){
						n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString(),"");
					}
					else{
						n1 = new IdentifierNode(varname,"");
					}
				}
				else{
					n1 = new IdentifierNode(varname,"");
				}
				arg = n1;
			}
			else{
				arg = new IdentifierNode(varname,"");
			}
			args.add(arg);
			if(!first){
				alt = !alt;
			}
			else{
				first = !first;
			}
		}
		return new CallNode(fname,"bit",args);
	}
	
	private CustomASTNode generateLoopInvariant(String type, Node parent, Map<String, List<Pair<String, String>>> loopInvariantArgsOrder, CustomASTNode tCond, Map<String, CustomASTNode> initVals) {
		String fname = "loopInvariant";
		
		boolean alt = false;
		ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
		args.add(new IdentifierNode(loopInvariantArgsOrder.get(type).get(0).part1(),loopInvariantArgsOrder.get(type).get(0).part2()));
		for(int i=1; i<loopInvariantArgsOrder.get(type).size(); i++){
			Pair<String,String> var = loopInvariantArgsOrder.get(type).get(i);
			String varname = var.part1();
			String vartype = var.part2();
			CustomASTNode arg;
			if(alt){
				CustomASTNode n1;
				if(initVals.containsKey(varname.substring(0,varname.length()-1))){
					CustomASTNode val = initVals.get(varname.substring(0,varname.length()-1));
					if(false && val instanceof ConstantNode && !(((ConstantNode) val).type_code == ConstantNode.ARRAYLIT)){
						n1 = new IdentifierNode(initVals.get(varname.substring(0,varname.length()-1)).toString(),vartype);
					}
					else{
						n1 = new IdentifierNode(varname.toString(),vartype);
					}
				}
				else{
					n1 = new IdentifierNode(varname.toString(),vartype);
				}
				arg = n1;
			}
			else{
				arg = new IdentifierNode(varname,vartype);
			}
			args.add(arg);
			alt = !alt;
		}
		return new CallNode(fname,"bit",args);
	}
	
	private CustomASTNode generatePreCondition(String type, Node parent, Map<String, List<Pair<String, String>>> loopInvariantArgsOrder, CustomASTNode loopInv, Map<String, CustomASTNode> initVals) {
		CustomASTNode pCond = loopInv;
		boolean alt = false;
		for(Pair<String,String> var : loopInvariantArgsOrder.get(type)){
			if(alt){
				if(initVals.containsKey(var.part1())){
					pCond = pCond.replaceAll(var.part1(), new IdentifierNode(var.part1()+"0",var.part2()));
				}
				else{
					pCond = pCond.replaceAll(var.part1(), new IdentifierNode(var.part1()+"0",var.part2()));
				}
			}
			alt = !alt;
		}
		return pCond;
	}
	
	private CustomASTNode generateWPC(Node n, String type, MyWhileExt ext) {	
		Stmt loopBodyInc = null;
		Stmt loopBodyMain = null;
		if(n instanceof While) {
			Stmt loopBody = ((While) n).body();
			
			List<Stmt> statements = ((Block) loopBody).statements();
			
			// Extract initial condition
			for(int i=0; i<statements.size(); i++){
				Stmt currStatement = statements.get(i);
				
				// Satement is a conditional
				if(currStatement instanceof If){
					Stmt cons = ((If) currStatement).consequent();
					Stmt alt = ((If) currStatement).alternative(); 
					
					// And consequent or alternative does not contain a break
					if(!casper.Util.containsBreak(cons) && !casper.Util.containsBreak(alt)){
						// Must be the increment if
						loopBodyInc = cons;
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
						loopBodyMain = alt;
					}
					// And alternative contains a break
					else if(casper.Util.containsBreak(alt)){
						loopBodyMain = cons;
					}
				}
			}
		}
		else {
			loopBodyMain = ((ExtendedFor)n).body();
		}
			
		
		String fname = "loopInvariant";
		
		boolean alt = false;
		ArrayList<CustomASTNode> args = new ArrayList<CustomASTNode>();
		args.add(new IdentifierNode(ext.loopInvariantArgsOrder.get(type).get(0).part1(),ext.loopInvariantArgsOrder.get(type).get(0).part2()));
		for(int i=1; i<ext.loopInvariantArgsOrder.get(type).size(); i++){
			String varname = ext.loopInvariantArgsOrder.get(type).get(i).part1();
			String vartype = ext.loopInvariantArgsOrder.get(type).get(i).part2();
			
			if(n instanceof ExtendedFor && varname.equals("casper_index")) {
				args.add(new BinaryOperatorNode("+", "int", new IdentifierNode("casper_index","int"),new ConstantNode("1","int",ConstantNode.INTLIT)));
				continue;
			}
			
			CustomASTNode arg = null;
			if(alt){
				if(ext.initVals.containsKey(varname.substring(0,varname.length()-1)) && ext.initVals.get(varname.substring(0,varname.length()-1)) instanceof ConstantNode){
					CustomASTNode val = new IdentifierNode(ext.initVals.get(varname.substring(0,varname.length()-1)).toString(),vartype);
					if(val instanceof ConstantNode && !(((ConstantNode) val).type_code == ConstantNode.ARRAYLIT)){
						arg = new IdentifierNode(ext.initVals.get(varname.substring(0,varname.length()-1)).toString(),vartype);
					}
					else{
						arg = new IdentifierNode(varname.toString(),vartype);
					}
				}
				else{
					if(n instanceof While) {
						arg = casper.Util.generatePreCondition(type,loopBodyInc,new IdentifierNode(varname,vartype),ext,false);
						arg = casper.Util.generatePreCondition(type,loopBodyMain,arg,ext,false);
					}
					else if(n instanceof ExtendedFor) {
						arg = casper.Util.generatePreCondition(type,loopBodyMain,new IdentifierNode(varname,vartype),ext,false);
						arg = arg.replaceAll(((ExtendedFor) n).decl().id().toString(),new ArrayAccessNode(casper.Util.getSketchTypeFromRaw(((ExtendedFor) n).decl().type().toString()),new IdentifierNode(ext.inputDataSet.varName,ext.inputDataSet.getSketchType()),new IdentifierNode("casper_index","int")));
					}
					
				}
			}
			else{
				if(n instanceof While) {
					arg = casper.Util.generatePreCondition(type,loopBodyInc,new IdentifierNode(varname,vartype),ext,false);
					arg = casper.Util.generatePreCondition(type,loopBodyMain,arg,ext,false);
				}
				else if(n instanceof ExtendedFor) {
					arg = casper.Util.generatePreCondition(type,loopBodyMain,new IdentifierNode(varname,vartype),ext,false);
					arg = arg.replaceAll(((ExtendedFor) n).decl().id().toString(),new ArrayAccessNode(casper.Util.getSketchTypeFromRaw(((ExtendedFor) n).decl().type().toString()),new IdentifierNode(ext.inputDataSet.varName,ext.inputDataSet.getSketchType()),new IdentifierNode("casper_index","int")));
				}
			}
			args.add(arg);
			alt = !alt;
		}
		return new CallNode(fname,"bit",args);
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