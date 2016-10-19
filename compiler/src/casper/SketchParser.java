package casper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.JavaLibModel.SketchCall;
import casper.extension.MyWhileExt;
import casper.types.Variable;

public class SketchParser {
	
	static boolean debug = false;
	
	public static class KvPair{
		int index  = -1;
		public Map<Integer,String> keys;
		public Map<Integer,String> values;
		
		KvPair(Map<Integer,String> k, Map<Integer,String> v, int i) 
		{ 
			keys = k; 
			values = v; 
			index = i; 
		}
		
		public String toString(){ 
			return "["+keys+","+values+"]"; 
		}
		
		@Override
		public boolean equals(Object obj){
			if(obj != null && obj instanceof KvPair){
				KvPair inp = (KvPair)obj;
				return this.index == inp.index;
			}
			
			return false;
		}
		
		@Override
		public int hashCode(){
			return 0;
		}
	}
	
	private static String resolve(String exp, List<String> mapLines, int i, MyWhileExt ext) {
		String[] binaryOps = {"\\+","\\-","\\*","\\/","\\%","\\&\\&","\\|\\|","\\=\\=","\\!\\=","\\>","\\>\\=","\\<","\\<\\=","\\^","\\&","\\|","\\>\\>\\>","\\>\\>","\\<\\<","instanceof"};
		String[] unaryOps = {"!"};
		
		// Remove ";" at the end
		if(exp.charAt(exp.length()-1) == ';'){
			exp = exp.substring(0,exp.length()-1);
		}
		
		// Remove brackets
		if(exp.charAt(0) == '('){
			exp = exp.substring(1,exp.length());
		}
		if(exp.charAt(exp.length()-1) == ')'){
			exp = exp.substring(0,exp.length()-1);
		}
			
				
		// If binary expression
		for(String op_esc : binaryOps){
			String op = op_esc.replace("\\", "");
			if(exp.contains(op)){
				String[] expComponents = exp.split(op_esc,2);
				return resolve(expComponents[0].trim(),mapLines,i,ext) + " " + op + " " + resolve(expComponents[1].trim(),mapLines,i,ext);
			}
		}
		// If unary expression
		for(String op : unaryOps){
			if(exp.contains(op)){
				String[] expComponents = exp.split(op);
				return op + " " + resolve(expComponents[0].trim(),mapLines,i,ext);
			}
		}
		// If generated variable
		Pattern r = Pattern.compile("^[_][a-zA-Z_$0-9]*$");
		Matcher m = r.matcher(exp);
		if(m.matches()){
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
				}
				// Function call
				r = Pattern.compile("^([a-zA-Z_$][a-zA-Z_$0-9]*)\\((..*)\\);$");
				m = r.matcher(mapLines.get(i));
				if(m.matches()){
					String funcName = m.group(1);
					List<String> args = new ArrayList<String>();
					for(String arg : m.group(2).split(",")){
						args.add(arg.trim());
					}
					List<String> argsR = new ArrayList<String>();
					for(String arg : args){
						if(!arg.equals(exp)){
							argsR.add(resolve(arg,mapLines,i,ext));
						}
						else {
							argsR.add(exp);
						}
					}
					for(SketchCall op : ext.methodOperators){
						if(funcName.equals(op.name)){
							String expR = op.resolve(exp,argsR);
							if(!expR.equals(exp)){
								return expR;
							}
							break;
						}
					}
				}
				
				i = i - 1;
			}
		}
		// If original variable
		r = Pattern.compile("^[a-zA-Z$][a-zA-Z_$0-9]*$");
		m = r.matcher(exp);
		if(m.matches()){
			// Sketch appends this to global variables
			if(exp.contains("__ANONYMOUS")){
				exp = exp.substring(0,exp.indexOf("__ANONYMOUS"));
			}
			return exp;
		}
		// If object field, with generated container
		r = Pattern.compile("^([_][a-zA-Z_$0-9]*).([a-zA-Z_$][a-zA-Z_$0-9]*)$");
		m = r.matcher(exp);
		if(m.matches()){
			String container = m.group(1);
			String field = m.group(2);
			
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){					
					String[] stmt = mapLines.get(i).split("=");
					
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+container+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim() + "." + field,mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		// If object field with original container
		r = Pattern.compile("^([a-zA-Z$][a-zA-Z_$0-9]*).([a-zA-Z_$][a-zA-Z_$0-9]*)$");
		m = r.matcher(exp);
		if(m.matches()){
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		// If array access with generated container
		r = Pattern.compile("^([_][a-zA-Z_$0-9]*)\\[(.*?)\\]$");
		m = r.matcher(exp);
		if(m.matches()){
			String container = m.group(1);
			String index = m.group(2);
			
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+container+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim()+"["+index+"]",mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+index+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(container+"["+stmt[1].trim()+"]",mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		// If array access with original container
		r = Pattern.compile("^([a-zA-Z$][a-zA-Z_$0-9]*)\\[(.*?)\\]$");
		m = r.matcher(exp);
		if(m.matches()){
			String container = m.group(1);
			String index = m.group(2);
			
			i = i - 1;
			while(i >= 0){
				// Assignment
				if(mapLines.get(i).contains("=")){
					String[] stmt = mapLines.get(i).split("=");
					
					r = Pattern.compile("\\b"+exp+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim(),mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+container+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(stmt[1].trim()+"["+index+"]",mapLines,i,ext);
					}
					
					r = Pattern.compile("\\b"+index+"\\b");
					m = r.matcher(stmt[0]);
					if(m.find()){
						return resolve(container+"["+stmt[1].trim()+"]",mapLines,i,ext);
					}
				}
				// Function call
				
				i = i - 1;
			}
		}
		
		return exp;
	}
	
	private static List<KvPair> extractMapEmits(String body, MyWhileExt ext, int emitCount) {
		List<KvPair> emits = new ArrayList<KvPair>();
		
		// Extract map emits
		for(int i=0; i<emitCount; i++){
			Map<Integer,String> keys = new HashMap<Integer,String>();
			Map<Integer,String> values = new HashMap<Integer,String>();
			Pattern r = Pattern.compile("keys(.*?)\\["+i+"\\] = (.*?);");
			Matcher m = r.matcher(body);
			while(m.find()){
				keys.put(Integer.parseInt(m.group(1)), m.group(2));
			}
			r = Pattern.compile("values(.*?)\\["+i+"\\] = (.*?);");
			m = r.matcher(body);
			while(m.find()){
				values.put(Integer.parseInt(m.group(1)), m.group(2));
			}
			if(keys.size()==0 && values.size() == 0)
				continue;
			KvPair kvp = new KvPair(keys,values,i);
			if(!emits.contains(kvp))
				emits.add(kvp);
		}
		
		List<String> mapLines = new ArrayList<String>();
		for(String line : body.split("\n")){
			if(!line.trim().equals(""))
				mapLines.add(line.trim());
		}

		// Resolve emits
		for(KvPair kvp : emits){
			int index = kvp.index;
			for(Integer i : kvp.keys.keySet()){
				String raw_key = kvp.keys.get(i);
				String new_key = kvp.keys.get(i);
				for(int j=0; j<mapLines.size(); j++){
					Pattern r = Pattern.compile("keys"+i+"\\["+index+"\\] = "+Pattern.quote(raw_key)+";");
					Matcher m = r.matcher(mapLines.get(j));
					if(m.find()){
						new_key = resolve(raw_key,mapLines,j,ext);
						break;
					}
				}
				kvp.keys.put(i, new_key);
			}
			for(Integer i : kvp.values.keySet()){
				String raw_value = kvp.values.get(i);
				String new_value = kvp.values.get(i);
				for(int j=0; j<mapLines.size(); j++){
					Pattern r = Pattern.compile("values"+i+"\\["+index+"\\] = "+Pattern.quote(raw_value)+";");
					Matcher m = r.matcher(mapLines.get(j));
					if(m.find()){
						new_value = resolve(raw_value,mapLines,j,ext);
						break;
					}
				}
				kvp.values.put(i, new_value);
			}
		}
		return emits;
	}
	
	public static void parseSolution(String filename, Set<Variable> outputVars, MyWhileExt ext, int emitCount) throws IOException {
		// Read sketch output
		BufferedReader br = new BufferedReader(new FileReader(filename));
		
		String text;
		StringBuilder sb = new StringBuilder();
	    String line = br.readLine();

	    while (line != null) {
	        sb.append(line);
	        sb.append(System.lineSeparator());
	        line = br.readLine();
	    }
	    text = sb.toString();
	    br.close();
		
		// Extract map function 
		Pattern r = Pattern.compile("void map (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
		Matcher m = r.matcher(text);
		m.find();
		String map = m.group(2); 
		
		List<String> mapLines = new ArrayList<String>();
		for(String mapLine : map.split("\n")){
			if(!mapLine.trim().equals(""))
				mapLines.add(mapLine.trim());
		}
		
		// Extract map emits
		// First look for emits wrapped in if conditions
		r = Pattern.compile("if(.*?)\\{(.*?)\\}",Pattern.DOTALL);
		m = r.matcher(map);
		Map<String,String> conditionals = new HashMap<String,String>();
		while(m.find()){
			conditionals.put(m.group(1).substring(1, m.group(1).lastIndexOf(")")),m.group(2));
		}
		
		Map<String,List<KvPair>> mapEmits = new HashMap<String,List<KvPair>>();
		
		for(String conditional : conditionals.keySet()){
			String conditional_res = conditional;
			for(int i=0; i<mapLines.size(); i++){
				if(mapLines.get(i).contains("if("+conditional+")")){
					conditional_res = resolve(conditional,mapLines,i,ext);
					break;
				}
			}
			mapEmits.put(conditional_res,extractMapEmits(conditionals.get(conditional),ext,emitCount));
		}
		
		// Remaining emits
		List<KvPair> allEmits = extractMapEmits(map,ext,emitCount);
		List<KvPair> filteredEmits = new ArrayList<KvPair>();
		for(KvPair emit : allEmits){
			boolean keep = true;
			for(String conditional : mapEmits.keySet()){
				if(mapEmits.get(conditional).contains(emit)){
					keep = false;
					break;
				}
			}
			if(keep)
				filteredEmits.add(emit);
		}
		
		mapEmits.put("noCondition",filteredEmits);
		
		
		// Extract reduce functions
		Map<String,String> reduceExps = new HashMap<String,String>();
		
		for(Variable var : outputVars){
			r = Pattern.compile("void "+Pattern.quote("reduce_"+var.varName)+" (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
			m = r.matcher(text);
			if(m.find()){
				// Get reduce function for this output variable
				String reduce = m.group(2);
				
				// Split code to lines
				List<String> reduceLines = new ArrayList<String>();
				for(String reduceLine : reduce.split("\n")){
					if(!reduceLine.trim().equals(""))
						reduceLines.add(reduceLine.trim());
				}
				
				for(int i=reduceLines.size()-1; i>=0; i--){
					r = Pattern.compile(Pattern.quote("_out = ")+"(.*?);",Pattern.DOTALL);
					m = r.matcher(reduceLines.get(i));
					if(m.find()){
						reduceExps.put(var.varName, resolve(m.group(1),reduceLines,i,ext));
						break;
					}
				}
			}
			else {
				if(debug)
					System.err.println("Something unexpected happened in the parser.");
			}
			 
		}
		
		// Extract init functions
		Map<String,String> initExps = new HashMap<String,String>();
		
		for(Variable var : outputVars){
			r = Pattern.compile("void "+Pattern.quote("init_"+var.varName)+" (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
			m = r.matcher(text);
			if(m.find()){
				// Get init function for this output variable
				String init = m.group(2);
				
				// Split code to lines
				List<String> initLines = new ArrayList<String>();
				for(String initLine : init.split("\n")){
					if(!initLine.trim().equals(""))
						initLines.add(initLine.trim());
				}
				
				for(int i=initLines.size()-1; i>=0; i--){
					r = Pattern.compile(Pattern.quote("_out = ")+"(.*?);",Pattern.DOTALL);
					m = r.matcher(initLines.get(i));
					if(m.find()){
						initExps.put(var.varName, resolve(m.group(1),initLines,i,ext));
						break;
					}
				}
			}
			else {
				if(debug)
					System.err.println("Something unexpected happened in the parser.");
			}
			 
		}
		
		// Extract merge functions
		Map<String,String> mergeExps = new HashMap<String,String>();
		
		for(Variable var : outputVars){
			r = Pattern.compile("void "+Pattern.quote("merge_"+var.varName)+" (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
			m = r.matcher(text);
			if(m.find()){
				// Get init function for this output variable
				String merge = m.group(2);
				
				// Split code to lines
				List<String> mergeLines = new ArrayList<String>();
				for(String mergeLine : merge.split("\n")){
					if(!mergeLine.trim().equals(""))
						mergeLines.add(mergeLine.trim());
				}
				
				for(int i=mergeLines.size()-1; i>=0; i--){
					r = Pattern.compile(Pattern.quote("_out = ")+"(.*?);",Pattern.DOTALL);
					m = r.matcher(mergeLines.get(i));
					if(m.find()){
						mergeExps.put(var.varName, resolve(m.group(1),mergeLines,i,ext));
						break;
					}
				}
			}
			else {
				if(debug)
					System.err.println("Something unexpected happened in the parser.");
			}
			 
		}
		
		ext.mapEmits = mapEmits;
		ext.initExps = initExps;
		ext.reduceExps = reduceExps;
		ext.mergeExps = mergeExps;
		
		if(debug){
			System.err.println(mapEmits);
			System.err.println(initExps);
			System.err.println(reduceExps);
			System.err.println(mergeExps);
		}
	}	
}