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

import casper.types.CustomASTNode;
import casper.visit.GenerateScaffold;
import casper.visit.GenerateScaffold.SketchVariable;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;

public class MyWhileExt extends MyStmtExt {
	private static final long serialVersionUID = 1L;
	
	// Used to mark promising loops that we will attempt to serialize
	public boolean interesting = false;
	
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
    
    // All variables that are used to count loop iterations.
    public List<Variable> loopCounters = new ArrayList<Variable>();
    
    // Increment expressions for loop counters
    public List<CustomASTNode> incrementExps = new ArrayList<CustomASTNode>();
    
    // We perform an alias analysis. If two variables are aliases, then
    // modifying one should change the other too. Our generated programs
    // need to be able to capture this effect.
    // Note: Not used anywhere currently
    public Map<Variable, Set<Variable>> aliases = new HashMap<Variable, Set<Variable>>();
    
    // Does the loop terminate when the initCond is true or does it terminate
    // when it is false. If condInv is true, it means the loop terminates when
    // the condition is false.
    public boolean condInv = false;
    
    // The condition that causes the loop to terminate
    public CustomASTNode terminationCondition;
   	
   	// User defined data types that may or may not appear in the loop
   	public ArrayList<String> globalDataTypes;

   	// Use to save the fields of the user defined data types
	public Map<String, Set<FieldDecl>> globalDataTypesFields;
	
	// The ordering of post condition args
	public Map<String,List<String>> postConditionArgsOrder = new HashMap<String,List<String>>();
	
	// The ordering of loop invariant args
	public Map<String,List<String>> loopInvariantArgsOrder = new HashMap<String,List<String>>();
	
	// The loop invariant (expressed using function loopInvariant(..)
	public Map<String,CustomASTNode> invariants = new HashMap<String,CustomASTNode>();

	// Save the parent node of this loop for analysis
	public Node parent = null;

	// Save the initial values of input / output variables
	public Map<String, CustomASTNode> initVals = new HashMap<String,CustomASTNode>();

	// Input data collections
	public boolean hasInputData = false;
	public List<SketchVariable> inputDataCollections = new ArrayList<SketchVariable>();

	// Operators found in the loop body
	public Set<String> binaryOperators = new HashSet<String>();
	public Set<String> unaryOperators = new HashSet<String>();
	
	// Map Emits
	public List<GenerateScaffold.KvPair> mapEmits = null;
	
	// Reduce Expression
	public String reduceExp = null;

	public String mapKeyType;

	// Save how values change in loop body
	public Map<String,CustomASTNode> wpcValues;	
	
   	// Custom class to represent an expression.
    public class Expression{
    	String exp;
    	String expType;
    	
    	Expression(String e, String t){
    		exp = e;
    		expType = t;
    	}
    	
    	@Override
    	public String toString(){
    		return "{" + exp + " : " + expType + "}";
    	}
    	
    	@Override
    	public boolean equals(Object obj){
    		if(obj != null && obj instanceof Expression){
    			Expression inp = (Expression)obj;
    			return this.exp.equals(inp.exp) && this.expType.equals(inp.expType);
    		}
    		
    		return false;
    	}
    	
    	@Override
    	public int hashCode(){
    		return 0;
    	}
    }
    
    // Custom class to represent a variable
    public class Variable{
    	public String varName;
    	public String varType;
    	public String containerType;
    	public int category;
    	
    	public static final int VAR = 0;
    	public static final int FIELD_ACCESS = 1;
    	public static final int ARRAY_ACCESS = 2;
    	public static final int CONST_ARRAY_ACCESS = 3;
    	
    	public Variable(String n, String t, String c, int cat){
    		varName = n;
    		varType = t;
    		containerType = c;
    		category = cat;
    	}
    	
    	public String translateToSketchType(String templateType) {
    		templateType = templateType.substring(templateType.indexOf("<")+1,templateType.lastIndexOf(">"));
    		
    		int depth = 0;
    		int i=0; 
    		while(i<templateType.length()){
    			if(templateType.charAt(i) == ',' && depth == 0){
    				break;
    			}
    			else if(templateType.charAt(i) == '<'){
    				depth++;
    			}
    			else if(templateType.charAt(i) == '>'){
    				depth--;
    			}
    			i++;
    		}
    		
    		String sketchType = "";
    		
    		if(i < templateType.length()){
    			String p1 = templateType.substring(0, i);
    			String p2 = templateType.substring(i+1, templateType.length());
    			
    			if(p1.equals("java.lang.String")){
    				sketchType = "Int";
    			}
    			else if(p1.equals("java.lang.Integer")){
    				sketchType = "Int";
    			}
    			
    			if(p2.equals("java.lang.String")){
    				sketchType += "Int";
    			}
    			else if(p2.equals("java.lang.Integer")){
    				sketchType += "Int";
    			}
    		}
    		else{
    			if(templateType.equals("java.lang.String")){
    				sketchType = "Int";
    			}
    			else if(templateType.equals("java.lang.Integer")){
    				sketchType = "Int";
    			}
    		}
    		
    		return sketchType;
    	}
    	
    	public String getType(){		
    		String targetType = varType;
    		String templateType = varType;
    		int end = targetType.indexOf('<');
    		if(end != -1){
    			targetType = targetType.substring(0, end);
    			
    			switch(targetType){
    				case "java.util.List":
    				case "java.util.ArrayList":
    					templateType = templateType.substring(end+1,templateType.length()-1);
    					this.category = Variable.ARRAY_ACCESS;
    					return casper.Util.getSketchTypeFromRaw(templateType)+"[]";
    				case "java.util.Map":
    					templateType = templateType.substring(end+1,templateType.length()-1);
        				String[] subTypes = templateType.split(",");
        				switch(subTypes[0]){
	        				case "java.lang.Integer":
	        				case "java.lang.String":
	        				case "java.lang.Double":
	        				case "java.lang.Float":
	        				case "java.lang.Long":
	        				case "java.lang.Short":
	        				case "java.lang.Byte":
	        				case "java.lang.BigInteger":
	        					this.category = Variable.ARRAY_ACCESS;
	        					return casper.Util.getSketchTypeFromRaw(subTypes[1])+"[]";
	        				default:
	        					templateType = translateToSketchType(templateType);
	        					return templateType + "Map";
        				}
    				default:
    					String[] components = varType.split("\\.");
    	        		return components[components.length-1];
    			}
    		}
    		
    		String[] components = varType.split("\\.");
    		return components[components.length-1];
    	}
    	
    	public String getOriginalType(){		
    		String targetType = varType;
    		String templateType = varType;
    		int end = targetType.indexOf('<');
    		if(end != -1){
    			targetType = targetType.substring(0, end);
    			
    			switch(targetType){
    				case "java.util.List":
    				case "java.util.ArrayList":
    					templateType = templateType.substring(end+1,templateType.length()-1);
    					this.category = Variable.ARRAY_ACCESS;
    					return templateType+"[]";
    				case "java.util.Map":
    					templateType = templateType.substring(end+1,templateType.length()-1);
        				String[] subTypes = templateType.split(",");
        				switch(subTypes[0]){
	        				case "java.lang.Integer":
	        				case "java.lang.String":
	        				case "java.lang.Double":
	        				case "java.lang.Float":
	        				case "java.lang.Long":
	        				case "java.lang.Short":
	        				case "java.lang.Byte":
	        				case "java.lang.BigInteger":
	        					this.category = Variable.ARRAY_ACCESS;
	        					return subTypes[1]+"[]";
	        				default:
	        					templateType = translateToSketchType(templateType);
	        					return templateType + "Map";
        				}
    				default:
    					String[] components = varType.split("\\.");
    	        		return components[components.length-1];
    			}
    		}
    		
    		return varType;
    	}
    	
    	// Should only be called for input data variable
    	public String getRDDType(){
    		String targetType = varType;
    		String templateType = varType;
    		int end = targetType.indexOf('<');
    		if(end != -1){
    			targetType = targetType.substring(0, end);
    			
    			switch(targetType){
    				case "java.util.List":
    				case "java.util.ArrayList":
    					return templateType.substring(end+1,templateType.length()-1);
    				default:
    					System.err.println("Currently not supported: "+targetType+" (For input data type)");
    					return varType;
    			}
    		}
    		
    		return varType;
    	}
    	
    	@Override
    	public String toString(){
    		return "{[" + category + "] " + varName + " : " + containerType + " -> " + varType + "}";
    	}
    	
    	@Override
    	public boolean equals(Object obj){
    		if(obj != null && obj instanceof Variable){
    			Variable inp = (Variable)obj;
    			return this.varName.equals(inp.varName);
    		}
    		
    		return false;
    	}
    	
    	@Override
    	public int hashCode(){
    		return 0;
    	}
    }
    
    // Save an input variable which is a field of a class.
    public void saveInputVariable(String varName, String varType, String containerType, int category){
    	Variable var = new Variable(varName,varType, containerType,category);
    	
    	// Ignore loop counters - already extracted
    	if(loopCounters.contains(var))
    		return;
    	
    	if(category == MyWhileExt.Variable.VAR){
    		if(!localVars.contains(var))
        		inputVars.add(var);
    	}
    	else if(category == MyWhileExt.Variable.FIELD_ACCESS){
    		String rootContainerName = varName.split("\\.")[0]; 
    		Variable rootContainer = new Variable(rootContainerName,"", "",category);
    		if(!localVars.contains(rootContainer)){
    			pendingInputVars.add(var);
    		}
    	}
    	else if(category == MyWhileExt.Variable.ARRAY_ACCESS){
    		if(!localVars.contains(var))
        		inputVars.add(var);
    	}
    }
    
    // Save an input variable.
    public void saveInputVariable(String varName, String varType, int category){
    	saveInputVariable(varName,varType,"",category);
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
    
    // Save an output variable which is a field of a class.
    public void saveOutputVariable(String varName, String varType, String containerType, int category){
    	Variable var = new Variable(varName,varType,containerType,category);
    	
    	// Ignore loop counters - already extracted
    	if(loopCounters.contains(var))
    		return;
    	
    	if(category == MyWhileExt.Variable.VAR){
        	if(!localVars.contains(var)){
        		inputVars.remove(var);
        		outputVars.add(var);
        	}
    	}
    	else if(category == MyWhileExt.Variable.FIELD_ACCESS){
    		String rootContainerName = varName.split("\\.")[0]; 
    		Variable rootContainer = new Variable(rootContainerName,"", "",category);
    		if(!localVars.contains(rootContainer)){
    			inputVars.remove(var);
    			outputVars.add(var);
    		}
    	}
    	else if(category == MyWhileExt.Variable.ARRAY_ACCESS){
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
    
    // Save an output variable.
    public void saveLoopCounterVariable(String varName, String varType, int category){
    	if(category == MyWhileExt.Variable.VAR){
    		Variable var = new Variable(varName,varType,"",category);
        	loopCounters.add(var);
    	}
    	else if(category == MyWhileExt.Variable.FIELD_ACCESS){
    		Variable var = new Variable(varName,varType,"",category);
    		loopCounters.add(var);
    	}
    	else if(category == MyWhileExt.Variable.ARRAY_ACCESS){
    		Variable var = new Variable(varName,varType,"",category);
    		loopCounters.add(var);
    	}
    }
    
    // Save an expression.
    public void saveExpression(String exp, String expType){
    	expUsed.add(new Expression(exp,expType));
    }
    
    // Save a local variable.
    public void saveLocalVariable(String varName, String varType){
    	localVars.add(new Variable(varName,varType,"",MyWhileExt.Variable.VAR));
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
