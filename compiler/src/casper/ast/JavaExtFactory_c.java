package casper.ast;

import casper.extension.MyStmtExt;
import casper.extension.MyWhileExt;
import polyglot.ast.Ext;
import polyglot.ast.ExtFactory;

public final class JavaExtFactory_c extends JavaAbstractExtFactory_c {

    public JavaExtFactory_c() {
        super();
    }

    public JavaExtFactory_c(ExtFactory nextExtFactory) {
        super(nextExtFactory);
    }

    @Override
    protected Ext extNodeImpl() {
        return new JavaExt();
    }

    @Override
    protected Ext extWhileImpl(){
		return new MyWhileExt();
    }
    
    @Override
    protected Ext extExtendedForImpl(){
		return new MyWhileExt();
    }
    
    @Override
    protected Ext extStmtImpl(){
		return new MyStmtExt();
    }
}
