package casper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import casper.JavaLibModel.SketchCall;
import casper.extension.MyWhileExt;

public class SketchOutputParser {
	boolean debug;
	
	private String resolve(String exp, List<String> mapLines, int i, MyWhileExt ext) {
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
	
	public class KvPair{
		public String name;
		public String key;
		public String key2;
		public String value;
		KvPair(String n, String k, String k2, String v){ name = n; key = k; key2 = k2; value = v; }
		public String toString(){ return  name + "["+key+","+key2+""+value+"]"; }
	}
	
	

	private List<KvPair> extractMapEmits(String body, MyWhileExt ext, String outputType, Map<String, List<KvPair>> mapEmits) {
		List<String> mapLines = new ArrayList<String>();
		
		for(String line : body.split("\n")){
			mapLines.add(line.trim());
		}

		// Extract map emits
		Pattern r = Pattern.compile("Pair(.*?)= new Pair\\(\\);");
		Matcher m = r.matcher(body);
		List<String> mapKvpNames = new ArrayList<String>();
		while(m.find()){
			String kvpName = m.group(1).trim();
			boolean exists = false;
			for(String key : mapEmits.keySet()){
				List<KvPair> kvps = mapEmits.get(key);
				for(KvPair kvp : kvps){
					if(kvp.name.equals(kvpName)){
						exists = true;
						break;
					}
				}
				if(exists) break;
			}
			if(!exists)
				mapKvpNames.add(kvpName);
		}
		
		List<KvPair> emits = new ArrayList<KvPair>();
		
		String mapKeyType = ext.mapKeyType;
		for(String kvpName : mapKvpNames){
			String mapKey = "";
			String mapKey2 = "";
			String mapValue = "";
			mapKeyType = "";
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".intkey = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					mapKey = m.group(1).trim();
					mapKey = resolve(mapKey,mapLines,i,ext);
					mapKeyType = "int";
					break;
				}
			}
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".intkey2 = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					mapKey2 = m.group(1).trim();
					mapKey2 = resolve(mapKey2,mapLines,i,ext);
					mapKeyType = "("+mapKeyType+",int)";
					break;
				}
			}
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".stringkey = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					if(mapKeyType.equals("")){
						mapKey = m.group(1).trim();
						mapKey = resolve(mapKey,mapLines,i,ext);
						mapKeyType = "string";
					}
					else{
						mapKey2 = m.group(1).trim();
						mapKey2 = resolve(mapKey2,mapLines,i,ext);
						mapKeyType = "("+mapKeyType+",string)";
					}
					break;
				}
			}
			for(int i=0; i<mapLines.size(); i++){
				r = Pattern.compile(kvpName + ".(.*?)value = (.*)");
				m = r.matcher(mapLines.get(i));
				if(m.find()){
					mapValue = m.group(2).trim();
					mapValue = resolve(mapValue,mapLines,i,ext);
					if(outputType.equals("bit")){
						switch(mapValue){
							case "0":
								mapValue = "false";
								break;
							case "1":
								mapValue = "true";
								break;
						}
					}
					break;
				}
			}
			if(debug){
				if(mapKey2.equals("")){
					System.err.println(mapKey + ", " + mapValue);
					System.err.println(mapKeyType);
				}
				else{
					System.err.println("(" + mapKey + "," + mapKey2 + "), " + mapValue);
					System.err.println(mapKeyType);
				}
			}
			
			emits.add(new KvPair(kvpName,mapKey,mapKey2,mapValue));
		}
		
		ext.mapKeyType = mapKeyType;
		
		return emits;
	}
}