/*
 * This compiler pass is responsible for extracting all user
 * defined data types in the input code and create their
 * model in Sketch.
 * 
 * - Maaz
 */

package casper.visit;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import casper.ast.JavaExt;
import casper.extension.MyWhileExt;
import casper.types.Variable;
import polyglot.ast.ClassDecl;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.ast.While;
import polyglot.ext.jl5.ast.ExtendedFor;
import polyglot.visit.NodeVisitor;

public class ExtractUserDefinedDataTypes extends NodeVisitor {
	boolean debug;
	ArrayList<String> dataTypes;
	ArrayList<MyWhileExt> extensions;
	Stack<String> classes;
	Map<String,Set<Variable>> fields;
   
	@SuppressWarnings("deprecation")
	public ExtractUserDefinedDataTypes(){
		this.debug = false;
		this.dataTypes = new ArrayList<String>();
		this.extensions = new ArrayList<MyWhileExt>();
		this.fields = new HashMap<String,Set<Variable>>();
		this.classes = new Stack<String>();
	}
	
	@Override
	public NodeVisitor enter(Node parent, Node n){
		// If the node is a loop
		if(n instanceof While || n instanceof ExtendedFor){
			// Get extension of loop node
			MyWhileExt ext = (MyWhileExt) JavaExt.ext(n);
			
			// If loop is interesting
			if(ext.interesting){
				this.extensions.add(ext);
			}			
		}
		// Class decleration found
		else if(n instanceof ClassDecl){
			// Add new data type
			this.dataTypes.add(((ClassDecl) n).id().toString());
			
			// Save name of class
			this.classes.push(((ClassDecl) n).id().toString());
			
			// Create empty field set
			Set<Variable> fieldSet = new HashSet<Variable>();
			this.fields.put(this.classes.peek(), fieldSet);
		}
		else if(n instanceof FieldDecl){
			// Class field definition found, save it
			this.fields.get(this.classes.peek()).add(new Variable(((FieldDecl) n).id().toString(),((FieldDecl) n).type().toString(),"",Variable.VAR));
		}
		
		if(debug){
			System.err.println(n.getClass() + "  ==>  " + n);
		}
		
		return this;
	}
	
	@Override
	public Node leave(Node old, Node n, NodeVisitor v){
		// Class definition ends, we already have all the
		// fields, generate sketch model
		if(n instanceof ClassDecl){
			try {
				// Get name of class
				String name = this.classes.pop();
				
				if(debug){
					System.err.println(fields + " :::: " + name);
				}
				
				// Generate sketch class file
				PrintWriter writer = new PrintWriter("output/"+name+".sk", "UTF-8");

				// Generate class with field variables
				String text = "struct " + name + "{\n\t";
				for(Variable field : fields.get(name)){
					text += field.getSketchType() + " " + field.varName + ";\n\t";
				}
				text += "}";
				
				// Write to file
				writer.print(text);
				writer.close();
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		return n;
	}
	
	@Override
	public void finish(){
		// Save data-types in all interesting loop's extensions
		for(MyWhileExt ext : extensions){
			ext.globalDataTypes = this.dataTypes;
			ext.globalDataTypesFields = this.fields;
		}
		
		if(debug){
			System.err.println("Datatypes extracted: " + this.dataTypes);
			System.err.println("\n************* Finished prepare sequential code complier pass *************");
		}
	}
}
