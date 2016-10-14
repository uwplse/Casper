package casper.extension;

import java.util.HashMap;
import java.util.Map;

import casper.ast.JavaExt;
import casper.types.CustomASTNode;

public class MyStmtExt extends JavaExt {
	private static final long serialVersionUID = 1L;
	
	// For input/output/lc var extraction. Used to mark blocks that should be processed.
	public boolean process = false;
	
	// The post condition of the statement (expressed using function postCondition(..)
	public Map<String,CustomASTNode> postConditions = new HashMap<String,CustomASTNode>();
	
	// The pre condition of the loop (expressed using function postCondition(..)
	public Map<String,CustomASTNode> preConditions = new HashMap<String,CustomASTNode>();
}
