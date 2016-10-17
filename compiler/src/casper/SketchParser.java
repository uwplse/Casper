package casper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.JavaLibModel.SketchCall;
import casper.extension.MyWhileExt;

public class SketchParser {
	
	static boolean debug = false;
	
	public static class KvPair{
		int index  = -1;
		public Map<Integer,String> keys;
		public Map<Integer,String> values;
		KvPair(Map<Integer,String> k, Map<Integer,String> v, int i) { keys = k; values = v; index = i; }
		public String toString(){ return "["+keys+","+values+"]"; }
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
			emits.add(new KvPair(keys,values,i));
		}
		
		List<String> mapLines = new ArrayList<String>();
		for(String line : body.split("\n")){
			if(!line.trim().equals(""))
				mapLines.add(line.trim());
		}
		
		System.err.println(emits);

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
		
		System.err.println(emits);
		
		return emits;
	}
	
	public static void parseSolution(String filename, MyWhileExt ext, int emitCount) throws IOException {
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
		mapEmits.put("noCondition",extractMapEmits(map,ext,emitCount));
	}
	
	
}
		
		
		
		
		
		/** Extract reduce emits **//*
		r = Pattern.compile("void do_reduce(.*?)\\{(.*?)return;\n\\}",Pattern.DOTALL);
		m = r.matcher(text);
		m.find();
		String reduce = m.group(0);
		
		List<String> reduceLines = new ArrayList<String>();
		for(String line : reduce.split("\n")){
			reduceLines.add(line.trim());
		}
		
		r = Pattern.compile("while\\(values != \\(null\\)\\)\\s*\\{(.*?)\\}",Pattern.DOTALL);
		m = r.matcher(text);
		m.find();
		String agg = m.group(0);
		
		List<String> aggLines = new ArrayList<String>();
		for(String line : agg.split("\n")){
			aggLines.add(line.trim());
		}
		
		String reduceInitValue = "";
		for(int i=0; i<reduceLines.size(); i++){
			r = Pattern.compile("_out.(.*?)value = (.*);");
			m = r.matcher(reduceLines.get(i));
			if(m.find()){
				reduceInitValue = m.group(2).trim();
				if(outputType.equals("bit")){
					switch(reduceInitValue){
						case "0":
							reduceInitValue = "false";
							break;
						case "1":
							reduceInitValue = "true";
							break;
					}
				}
				break;
			}
		}
		String reduceValue = "";
		for(int i=0; i<aggLines.size(); i++){
			r = Pattern.compile("_out.(.*?)value = (.*)");
			m = r.matcher(aggLines.get(i));
			if(m.find()){
				reduceValue = m.group(2).trim();
				reduceValue = resolve(reduceValue,aggLines,i,ext);
				if(outputType.equals("bit")){
					switch(reduceValue){
						case "0":
							reduceValue = "false";
							break;
						case "1":
							reduceValue = "true";
							break;
					}
				}
				break;
			}
		}
		
		if(debug)
			System.err.println(reduceInitValue + ", " + reduceValue);
		
		
		/** Extract merge operator **//*
		r = Pattern.compile("void int_get (.*?)\\{(.*?)\\}",Pattern.DOTALL);
		m = r.matcher(text);
		String mergeOp = "";
		if(!m.find()){
			r = Pattern.compile("void string_get (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
			m = r.matcher(text);
			if(!m.find()){
				r = Pattern.compile("void int_get_tuple (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
				m = r.matcher(text);
				if(!m.find()){
					r = Pattern.compile("void string_get_tuple (.*?)\\{(.*?)\n\\}",Pattern.DOTALL);
					m = r.matcher(text);
					m.find();
				}
			}
		}
		mergeOp = m.group(2);
		r = Pattern.compile(outputType.replace("["+Configuration.arraySizeBound+"]", "") + " option(.*?) = (.*?);",Pattern.DOTALL);
		m = r.matcher(mergeOp);
		m.find();
		mergeOp = casper.Util.getOperatorFromExp(m.group(2));
	}
	
}*/