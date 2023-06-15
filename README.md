### Note: This project is a few years old and is not currently maintained. If you're interested in exploring the techniques behind Casper or in building your own Verified Lifting compiler, check out [Metalift](https://github.com/metalift/metalift)!

# Casper
Casper is a compiler for automatically re-targeting sequential Java code fragments to Apache Spark. 
Casper works by synthesizing high level MapReduce program specifications from raw un-annotated 
sequential Java source code and using the synthesized specificiations to generate Apache Spark code. 
The most recent release version for Casper is [v0.1.1](https://github.com/uwplse/Casper/releases/tag/v0.1.1).

To learn more about Casper, visit our [homepage](http://casper.uwplse.org), read our 
[paper](http://homes.cs.washington.edu/~maazsaf/synt16.pdf) or 
[email us](https://mailman.cs.washington.edu/mailman/listinfo/casper-users), and if you like
our tool, please star our repo!

Casper has been implemented as an extension of 
[Polyglot 2.6.1](https://www.cs.cornell.edu/projects/polyglot/).

### Online Demo
The quickest way to try out Casper is by using our [online demo](http://demo.casper.uwplse.org/). To 
download and install Casper on your own machine, follow the instructions below.

### Getting Started
The easiest way to install Casper is by using our [Docker image](#running-in-a-docker-container). 
It comes with all the dependencies installed and Casper already configured to run. Alternatively, 
use [these instructions](#build-casper) to configure and build Casper on your own.

### Running in a Docker Container
You can run Casper inside a docker container using our pre-configured image of Ubuntu 16.04 LTS.
Once you have Docker set up on your computer ([instructions here](https://docs.docker.com/engine/installation/)),
pull our ubuntu image using the following command:

    
    $ docker pull maazsaf/casper-ubuntu

Once the image is downloaded, use it to run a Docker container:

    $ docker run -t -i maazsaf/casper-ubuntu /bin/bash
    
Once inside the Docker container, you will find a clone of this repository in the root directory.
You can optionally pull the latest changes from git to bring the repository up to date by running `git pull`. 
After that, build casper using ant as follows:
    
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
memory.

### Contact
Casper is written by [Maaz Ahmad](http://homes.cs.washington.edu/~maazsaf/) at the [University of Washington](http://www.washington.edu/).
