# Casper
Casper is a compiler for automatically re-targeting sequential Java code fragments
to Apache Spark. Casper works by synthesizing high level MapReduce program specifications
from raw un-annotated sequential Java source code and using the synthesized specificiations 
to generate Apache Spark code.

To learn more about Casper, visit our [homepage](http://casper.uwplse.org), read our [paper](http://homes.cs.washington.edu/~maazsaf/synt16.pdf) or [email us](https://mailman.cs.washington.edu/mailman/listinfo/casper-users)!

### Getting Started
    Dependencies:
        Java 7+                 - http://www.oracle.com/technetwork/java/javase/overview/index.html
        SKETCH                  - https://bitbucket.org/gatoatigrado/sketch-frontend/wiki/Home        
        Dafny                   - https://dafny.codeplex.com/
        Nodejs                  - https://nodejs.org/en/
        
    To check for the necessary dependencies, you can run:
        $ ./bin/check.sh

    To run the tool:
        $ ./bin/run.sh [input-file] [output-file]

To help you get started, we have added two simple benchmarks under /bin/benchmarks. In
some cases the synthesizer may run for a very long time or require a significant amount of
memory. Casper is currently under development and we are rolling out new features and bug
fixes frequently. If you experience any difficulties, contact us through our [mailing-list](https://mailman.cs.washington.edu/mailman/listinfo/casper-users).