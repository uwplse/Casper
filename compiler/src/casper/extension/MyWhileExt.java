/*
 * Most of our analysis is only executed on loops. We flatten all loops
 * into while loops for simplicity. This while loop extension is custom
 * class. An object of this class is saved for every while loop in the 
 * program. We use this object to pass information forward from one
 * pass to the other until we reach the final pass for output scaffold 
 * generation.
 * 
 * - Maaz
 */

package casper.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import casper.JavaLibModel.SketchCall;
import casper.SketchParser.KvPair;
import casper.types.CustomASTNode;
import casper.types.Expression;
import casper.types.Variable;
import polyglot.ast.Node;

public class MyWhileExt extends MyStmtExt {
	private static final long serialVersionUID = 1L;
	
	// Used to mark promising loops that we will attempt to serialize
	public boolean interesting = false;
	
	// Save the parent node of this loop for analysis
	public Node parent = null;
	
	// Does the loop contain conditionals
	public boolean useConditionals = false;
	
    // All variables that are used as indexes when accessing large
	// data structures like arrays and collections. Must change with
	// loop iterations.
    public Set<Variable> loopCounters = new HashSet<Variable>();
	
	// Input variables are variables that were declared outside of the
	// loop body, but were read within the loop. They are thus inputs
	// to the loop code fragment.
    public Set<Variable> inputVars = new HashSet<Variable>();
    
    // These variables are fields of other variables and thus not added
    // immediately. We wait to see if the container variable is also
    // an input variable or not.
    public Set<Variable> pendingInputVars = new HashSet<Variable>();
    
    // Output variables are variables that were declared outside of the
    // loop body, but were written/modified within the loop. They thus
    // capture the effect/output of the loop on the program.
    public Set<Variable> outputVars = new HashSet<Variable>();
 
    // All variables that are declared within the loop body. They are
    // not accessible outside the loop body and thus are not input or
    // output but used merely for internal computation. Currently, we
    // are not using these but could be useful in the future.
    public Set<Variable> localVars = new HashSet<Variable>();
    
    // Expressions that occur inside the loop body. Currently these are
    // not used but they could be useful. These expressions may hint
    // to how the variables are used within the loop.
    // Note: Not used anywhere currently
    public Set<Expression> expUsed = new HashSet<Expression>();
    
    // Operators found in the loop body
 	public Set<String> binaryOperators = new HashSet<String>();
 	public Set<String> unaryOperators = new HashSet<String>();
 	public Set<SketchCall> methodOperators = new HashSet<SketchCall>();
    
 	// Does the loop terminate when the initCond is true or does it terminate
    // when it is false. If condInv is true, it means the loop terminates when
    // the condition is false.
    public boolean condInv = false;
    
    // The condition that causes the loop to terminate
    public CustomASTNode terminationCondition;
 	
 	// User defined data types that may or may not appear in the loop
   	public ArrayList<String> globalDataTypes;

   	// Use to save the fields of the user defined data types
	public Map<String, Set<Variable>> globalDataTypesFields;
 	
	// Save the initial values of input / output variables
	public Map<String, CustomASTNode> initVals = new HashMap<String,CustomASTNode>();
	
	// Used to save mappings of constants to variables (required at times due to bounded domain)
	public int constCount = 0;
	public Map<String, String> constMapping = new HashMap<String, String>();
	
	// The ordering of post condition args
	public Map<String,List<String>> postConditionArgsOrder = new HashMap<String,List<String>>();
	
	// The ordering of loop invariant args
	public Map<String,List<String>> loopInvariantArgsOrder = new HashMap<String,List<String>>();
		
	// The loop invariant (expressed using function loopInvariant(..)
	public Map<String,CustomASTNode> invariants = new HashMap<String,CustomASTNode>();	
		
	// Save how values change in loop body
	public Map<String,CustomASTNode> wpcValues;
	
	// Input data collections
	public boolean hasInputData = false;
	public List<Variable> inputDataCollections = new ArrayList<Variable>();
	
	// Input data set. Generated from inputDataCollections
	public Variable inputDataSet = null;
	
	// Flags to enable disable code generation pass
	public Map<String,Boolean> generateCode = new HashMap<String,Boolean>();
    
    // We perform an alias analysis. If two variables are aliases, then
    // modifying one should change the other too. Our generated programs
    // need to be able to capture this effect.
    // Note: Not used anywhere currently
    public Map<Variable, Set<Variable>> aliases = new HashMap<Variable, Set<Variable>>();
    
    // Save an loop counter variable.
    public void saveLoopCounterVariable(String varName, String varType, int category){
    	if(category == Variable.VAR){
    		Variable var = new Variable(varName,varType,"",category);
        	loopCounters.add(var);
    	}
    	else if(category == Variable.FIELD_ACCESS){
    		Variable var = new Variable(varName,varType,"",category);
    		loopCounters.add(var);
    	}
    	else if(category == Variable.ARRAY_ACCESS){
    		Variable var = new Variable(varName,varType,"",category);
    		loopCounters.add(var);
    	}
    }
    
    // Save an input variable
    public void saveInputVariable(String varName, String varType, String containerType, int category){
    	Variable var = new Variable(varName,varType, containerType,category);
    	
    	// Ignore loop counters - already extracted
    	if(loopCounters.contains(var))
    		return;
    	
    	if(category == Variable.VAR){
    		if(!localVars.contains(var))
        		inputVars.add(var);
    	}
    	else if(category == Variable.FIELD_ACCESS){
    		String rootContainerName = varName.split("\\.")[0]; 
    		Variable rootContainer = new Variable(rootContainerName,"", "",category);
    		if(!localVars.contains(rootContainer)){
    			pendingInputVars.add(var);
    		}
    	}
    	else if(category == Variable.ARRAY_ACCESS){
    		if(!localVars.contains(var)){
        		inputVars.add(var);
    		}
    		else{
    			for(Variable v : localVars){
    				if(v.varName.equals(var.varName)){
    					v.category = category;
    				}
    			}
    		}
    	}
    }
    
    // Save an input variable.
    public void saveInputVariable(String varName, String varType, int category){
    	saveInputVariable(varName,varType,"",category);
    }
    
    // Save an expression.
    public void saveExpression(String exp, String expType){
    	expUsed.add(new Expression(exp,expType));
    }
    
    // Add pending input variables
    public void savePendingInputVariables(){
    	for(Variable var : pendingInputVars){
    		String[] components = var.varName.split("\\.");
    		String varName = "";
    		boolean save = true;
    		for(int i=0; i<components.length; i++){
    			if(i==0){
    				varName += components[i]; 
    			}
    			else{
    				varName += "." + components[i]; 
    			}
    			
    			Variable tempVar = new Variable(varName,"","",0);
    			if(inputVars.contains(tempVar)){
    				save = false;
    				break;
    			}
    		}
    		if(save){
    			inputVars.add(var);
    		}
    	}
    }
    
    // Save a local variable.
    public void saveLocalVariable(String varName, String varType){
    	Variable var = new Variable(varName,varType,"",Variable.VAR);
    	localVars.add(var);
    	loopCounters.remove(var);
    }
    
    // Save an output variable which is a field of a class.
    public void saveOutputVariable(String varName, String varType, String containerType, int category){
    	Variable var = new Variable(varName,varType,containerType,category);
    	
    	// Ignore loop counters - already extracted
    	if(loopCounters.contains(var))
    		return;
    	
    	if(category == Variable.VAR){
        	if(!localVars.contains(var)){
        		inputVars.remove(var);
        		outputVars.add(var);
        	}
    	}
    	else if(category == Variable.FIELD_ACCESS){
    		String rootContainerName = varName.split("\\.")[0]; 
    		Variable rootContainer = new Variable(rootContainerName,"", "",category);
    		if(!localVars.contains(rootContainer)){
    			inputVars.remove(var);
    			outputVars.add(var);
    		}
    	}
    	else if(category == Variable.ARRAY_ACCESS){
        	if(!localVars.contains(var)){
        		inputVars.remove(var);
        		outputVars.add(var);
        	}
    	}
    }
    
    // Save an output variable.
    public void saveOutputVariable(String varName, String varType, int category){
    	saveOutputVariable(varName,varType,"",category);
    }
    
    // Alias analysis
    public void extractAliases(){
    	HashSet<Variable> allVars = new HashSet<Variable>();
    	allVars.addAll(inputVars);
    	allVars.addAll(outputVars);
    	
    	for(Variable x : allVars){
    		aliases.put(x, new HashSet<Variable>());
    		switch(x.varType){
				// Primitive data types are immutable
	    		case "byte":
	    		case "short":
	    		case "int":
	    		case "long":
	    		case "float":
	    		case "double":
	    		case "char":
	    		case "boolean":
	    		case "java.lang.String":
	    		case "java.lang.Integer":
	    			break;
    			// Unknown data type
	    		default:
	    			for(Variable y : allVars){
	    				if(y.equals(x)) continue;
	    				
	    				if(y.varType.equals(x.varType)){// && y.containerType.equals(x.containerType)){
	    					aliases.get(x).add(y);
	    				}
	    			}
    		}
    	}
    }
}
