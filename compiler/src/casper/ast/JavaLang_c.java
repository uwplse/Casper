package casper.ast;

import polyglot.ast.Ext;
import polyglot.ast.Lang;
import polyglot.ast.Node;
import polyglot.ast.NodeOps;
import polyglot.ext.jl7.ast.J7Lang_c;
import polyglot.util.InternalCompilerError;

public class JavaLang_c extends J7Lang_c implements JavaLang {
    public static final JavaLang_c instance = new JavaLang_c();

    public static JavaLang lang(NodeOps n) {
        while (n != null) {
            Lang lang = n.lang();
            if (lang instanceof JavaLang) return (JavaLang) lang;
            if (n instanceof Ext)
                n = ((Ext) n).pred();
            else return null;
        }
        throw new InternalCompilerError("Impossible to reach");
    }

    protected JavaLang_c() {
    }

    protected static JavaExt javatosketchExt(Node n) {
        return JavaExt.ext(n);
    }

    @Override
    protected NodeOps NodeOps(Node n) {
        return javatosketchExt(n);
    }

    // TODO:  Implement dispatch methods for new AST operations.
    // TODO:  Override *Ops methods for AST nodes with new extension nodes.
}
