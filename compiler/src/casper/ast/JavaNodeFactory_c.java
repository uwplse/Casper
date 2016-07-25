package casper.ast;

import polyglot.ext.jl7.ast.JL7NodeFactory_c;

/**
 * NodeFactory for javatosketch extension.
 */
public class JavaNodeFactory_c extends JL7NodeFactory_c implements JavaNodeFactory {
    public JavaNodeFactory_c(JavaLang lang, JavaExtFactory extFactory) {
        super(lang, extFactory);
    }

    @Override
    public JavaExtFactory extFactory() {
        return (JavaExtFactory) super.extFactory();
    }

    // TODO:  Implement factory methods for new AST nodes.
    // TODO:  Override factory methods for overridden AST nodes.
    // TODO:  Override factory methods for AST nodes with new extension nodes.
}
