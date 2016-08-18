# Casper
Casper is a compiler for automatically re-targeting sequential Java code fragments to Apache Spark. 
Casper works by synthesizing high level MapReduce program specifications from raw un-annotated 
sequential Java source code and using the synthesized specificiations to generate Apache Spark code. 
The most recent release version for Casper is v0.0.1.

To learn more about Casper, visit our [homepage](http://casper.uwplse.org), read our 
[paper](http://homes.cs.washington.edu/~maazsaf/synt16.pdf) or 
[email us](https://mailman.cs.washington.edu/mailman/listinfo/casper-users)!

Casper has been implemented as an extension of 
[Polyglot 2.6.1](https://www.cs.cornell.edu/projects/polyglot/).

### Getting Started
The quickest way to try out Casper is by using our [Docker image](#running-in-a-docker-container). 
It comes with all the dependencies installed and Casper already configured to run. Alternatively, 
use [these instructions](#build-casper) to configure and build Casper on your machine.

### Running in a Docker Container
You can run Casper inside a docker container using our pre-configured image of Ubuntu 16.04 LTS.
Once you have Docker set up on your computer ([instructions here](https://docs.docker.com/engine/installation/)),
pull our ubuntu image using the following command:

    
    $ docker pull maazsaf/casper-ubuntu

Once the image is downloaded, use it to run a Docker container:

    $ docker run -t -i maazsaf/casper-ubuntu /bin/bash
    
Once inside the Docker container, you will find a clone of this repository in the root directory.
Pull the latest changes from git to bring the repository up to date. Build casper using ant as follows:
    
    $ cd Casper/
    $ ant
    
Optionally, you may want to reset to one of the stable release commits before you build:

    $ git reset --hard <commit hash or release tag>
    
### Build Casper from source code 

Note: You only need to do this if you are building Casper from scratch. The Docker image already includes all software prerequisites.

Dependencies:
        
- [JDK 7 or greater](http://www.oracle.com/technetwork/java/javase/overview/index.html)
- [SKETCH](https://bitbucket.org/gatoatigrado/sketch-frontend/wiki/Home)
- [Dafny](https://dafny.codeplex.com/)
- [Apache ant](http://ant.apache.org/)
- [Nodejs](https://nodejs.org/en/) (this is optional for formatting the generated code)
        
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

### Benchmarks
To help you get started, we have added some example programs under `/bin/benchmarks`. In
some cases the synthesizer may run for a very long time or require a significant amount of
memory. Casper is currently under development and we are rolling out new features and bug
fixes frequently. If you experience any difficulties, contact us through our [mailing-list](https://mailman.cs.washington.edu/mailman/listinfo/casper-users).

### Contact
Casper is written by [Maaz Ahmad](http://homes.cs.washington.edu/~maazsaf/) at the [University of Washington](http://www.washington.edu/).
