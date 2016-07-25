package casper;

import java.io.Reader;

import casper.ast.JavaExtFactory_c;
import casper.ast.JavaLang_c;
import casper.ast.JavaNodeFactory_c;
import casper.parse.Grm;
import casper.parse.Lexer_c;
import casper.types.JavaTypeSystem_c;
import polyglot.ast.NodeFactory;
import polyglot.ext.jl5.ast.JL5ExtFactory_c;
import polyglot.ext.jl7.ast.JL7ExtFactory_c;
import polyglot.frontend.CupParser;
import polyglot.frontend.FileSource;
import polyglot.frontend.Parser;
import polyglot.frontend.Scheduler;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorQueue;

/**
 * Extension information for javatosketch extension.
 */
public class ExtensionInfo extends polyglot.ext.jl7.JL7ExtensionInfo {
    static {
        // force Topics to load
        @SuppressWarnings("unused")
        Topics t = new Topics();
    }

    @Override
    public String defaultFileExtension() {
        return "java";
    }

    @Override
    public String compilerName() {
        return "casper";
    }

    @Override
    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {
        reader = new polyglot.lex.EscapedUnicodeReader(reader);

        polyglot.lex.Lexer lexer = new Lexer_c(reader, source, eq);
        polyglot.parse.BaseParser parser = new Grm(lexer, ts, nf, eq);

        return new CupParser(parser, source, eq);
    }

    @Override
    protected NodeFactory createNodeFactory() {
        return new JavaNodeFactory_c(JavaLang_c.instance, new JavaExtFactory_c(new JL7ExtFactory_c(new JL5ExtFactory_c())));
    }

    @Override
    protected TypeSystem createTypeSystem() {
        return new JavaTypeSystem_c();
    }
    
    @Override
    public Scheduler createScheduler() {
        return new CasperScheduler(this);
    }

}
