package casper.types;

import casper.Configuration;

//Custom class to represent a variable
public class Variable{
	public String varNameOrig;
	public String varName;
	public String varType;
	public String containerType;
	public int category;
	public boolean bitVec = false;
	
	public static final int VAR = 0;
	public static final int FIELD_ACCESS = 1;
	public static final int ARRAY_ACCESS = 2;
	public static final int CONST_ARRAY_ACCESS = 3;
	
	public Variable(String n, String t, String c, int cat){
		varNameOrig = n;
		varName = n.replace("$", "");
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
	
	private String getSketchType(String vtype){
		String targetType = vtype;
		String templateType = vtype;
		int end = targetType.indexOf('<');
		if(end != -1){
			targetType = targetType.substring(0, end);
			
			switch(targetType){
				case "java.util.List":
				case "java.util.ArrayList":
				case "java.util.Set":
					templateType = templateType.substring(end+1,templateType.length()-1);
					this.category = Variable.ARRAY_ACCESS;
					return casper.Util.getSketchTypeFromRaw(this.getSketchType(templateType))+"["+Configuration.arraySizeBound+"]";
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
        					return casper.Util.getSketchTypeFromRaw(this.getSketchType(subTypes[1]))+"["+Configuration.arraySizeBound+"]";
        				default:
        					templateType = translateToSketchType(templateType);
        					return templateType + "Map";
    				}
				default:
					String[] components = vtype.split("\\.");
	        		return casper.Util.getSketchTypeFromRaw(components[components.length-1]);
			}
		}
		
		String[] components = vtype.split("\\.");
		return casper.Util.getSketchTypeFromRaw(components[components.length-1]);
	}
	
	public String getSketchType(){		
		return this.getSketchType(varType);
	}
	
	private String getReduceType(String vtype){
		String targetType = vtype;
		String templateType = vtype;
		int end = targetType.indexOf('<');
		if(end != -1){
			targetType = targetType.substring(0, end);
			
			switch(targetType){
				case "java.util.List":
				case "java.util.ArrayList":
				case "java.util.Set":
					templateType = templateType.substring(end+1,templateType.length()-1);
					this.category = Variable.ARRAY_ACCESS;
					return casper.Util.getPrimitiveTypeFromRaw(this.getReduceType(templateType))+"[]";
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
        					return casper.Util.getPrimitiveTypeFromRaw(this.getReduceType(subTypes[1]))+"[]";
        				default:
        					templateType = translateToSketchType(templateType);
        					return templateType + "Map";
    				}
				default:
					String[] components = vtype.split("\\.");
	        		return casper.Util.getPrimitiveTypeFromRaw(components[components.length-1]);
			}
		}
		
		String[] components = vtype.split("\\.");
		return casper.Util.getPrimitiveTypeFromRaw(components[components.length-1]);
	}
	
	public String getReduceType() {
		return this.getReduceType(varType);
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
					String[] components = templateType.split("\\.");
					this.category = Variable.ARRAY_ACCESS;
					return components[components.length-1]+"[]";
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
					String[] components2 = varType.split("\\.");
	        		return components2[components2.length-1];
			}
		}
		
		String[] components = varType.split("\\.");
		return components[components.length-1];
	}
	
	// Should only be called for input data set variable
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
		
		System.err.println("RDD cannot be created from: "+targetType+" (input data type)");
		switch(targetType){
			case "double[]":
				return "Double";
			default:
				return varType;
		}
	}
	
	public String getDafnyType(){
		return casper.Util.getDafnyType(this.getSketchType());
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