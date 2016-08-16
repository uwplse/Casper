# Casper
Casper is a compiler for automatically re-targeting sequential Java code fragments
to Apache Spark. Casper works by synthesizing high level MapReduce program specifications
from raw un-annotated sequential Java source code and using the synthesized specificiations 
to generate Apache Spark code.

To learn more about Casper, visit our [homepage](http://casper.uwplse.org), read our [paper](http://homes.cs.washington.edu/~maazsaf/synt16.pdf) or [email us](https://mailman.cs.washington.edu/mailman/listinfo/casper-users)!

Casper has been implemented as an extension of [Polyglot 2.6.1](https://www.cs.cornell.edu/projects/polyglot/).

### Getting Started
    The most recent release version for Casper is v0.0.1.
    
    Dependencies:
        JDK 7 or greater            - http://www.oracle.com/technetwork/java/javase/overview/index.html
        SKETCH                      - https://bitbucket.org/gatoatigrado/sketch-frontend/wiki/Home        
        Dafny                       - https://dafny.codeplex.com/
        ant							- http://ant.apache.org/
        Nodejs*                     - https://nodejs.org/en/
        * optional, for generated code formatting 
        
    You must set environment variables for Sketch and Dafny for Casper to run successfully. To
    allow Casper to use your sketch installation, run the following commands under your
    sketch-frontend directory:
        $ export PATH="$PATH:`pwd`"
        $ export SKETCH_HOME="`pwd`/runtime"
        
    Similarly, to allow Casper to use your Dafny installation, run the following command:
        $ export PATH=$PATH:/path/to/dafny
    
    To check whether you have all the necessary dependencies installed and properly configured, 
    you can run:
        $ ./bin/check.sh

   	You can compile the project by running 'ant' in the project's root directory. Once compiled, 
   	you run the tool as follows:
        $ ./bin/run.sh [input-file] [output-file]

To help you get started, check out the benchmarks under `/bin/benchmarks`. In
some cases the synthesizer may run for a very long time or require a significant amount of
memory. Casper is currently under development and we are rolling out new features and bug
fixes frequently. If you experience any difficulties, contact us through our [mailing-list](https://mailman.cs.washington.edu/mailman/listinfo/casper-users).

### Contact
Casper is written by [Maaz Ahmad](http://homes.cs.washington.edu/~maazsaf/) at the [University of Washington](http://www.washington.edu/).
