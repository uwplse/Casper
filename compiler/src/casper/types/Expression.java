package casper.types;

//Custom class to represent an expression.
public class Expression{
	String exp;
	String expType;
	
	public Expression(String e, String t){
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