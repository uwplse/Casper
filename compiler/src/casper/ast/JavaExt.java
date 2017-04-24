package casper.ast;

import polyglot.ast.Ext;
import polyglot.ast.Ext_c;
import polyglot.ast.Node;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;

public class JavaExt extends Ext_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public static JavaExt ext(Node n) {
        Ext e = n.ext();
        while (e != null && !(e instanceof JavaExt)) {
            e = e.ext();
        }
        if (e == null) {
            throw new InternalCompilerError("No Java extension object for node "
                    + n + " (" + n.getClass() + ")", n.position());
        }
        return (JavaExt) e;
    }

    @Override
    public final JavaLang lang() {
        return JavaLang_c.instance;
    }

    // TODO:  Override operation methods for overridden AST operations.
}
