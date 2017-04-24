package casper.ast;

import polyglot.ast.ExtFactory;
import polyglot.ext.jl7.ast.JL7AbstractExtFactory_c;

public abstract class JavaAbstractExtFactory_c extends JL7AbstractExtFactory_c
        implements JavaExtFactory {

    public JavaAbstractExtFactory_c() {
        super();
    }

    public JavaAbstractExtFactory_c(ExtFactory nextExtFactory) {
        super(nextExtFactory);
    }

    // TODO: Implement factory methods for new extension nodes in future
    // extensions.  This entails calling the factory method for extension's
    // AST superclass.
}
