# Ubuntu 14.04 as base OS
FROM ubuntu:14.04
MAINTAINER Maaz Ahmad

# Install dependencies
RUN apt-get update && apt-get -y install curl
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF
RUN echo "deb http://download.mono-project.com/repo/debian wheezy main" | tee /etc/apt/sources.list.d/mono-xamarin.list
RUN curl -sL https://deb.nodesource.com/setup_6.x | bash -
RUN apt-get update && apt-get -y install \
	bash \ 
	g++ \
	flex \
	bison \
	make \
	ant \
	openjdk-7-jdk \
	nodejs \
	unzip \
	mono-complete \
	git \
	wget

# Install SKETCH
RUN wget "http://people.csail.mit.edu/asolar/sketch-1.7.2.tar.gz"
RUN tar -xvzf sketch-1.7.2.tar.gz
RUN rm sketch-1.7.2.tar.gz
WORKDIR sketch-1.7.2/sketch-backend/
RUN chmod +x ./configure
RUN ./configure
RUN make
WORKDIR ../../
RUN chmod a+rwx sketch-1.7.2/*
ENV PATH $PATH:/sketch-1.7.2/sketch-frontend/
ENV SKETCH_HOME /sketch-1.7.2/sketch-frontend/runtime

# Install Dafny
RUN wget -O dafny-1.9.9.zip "https://github.com/Microsoft/dafny/releases/download/v1.9.9/dafny-1.9.9.40414-x64-ubuntu-14.04.zip"
RUN unzip dafny-1.9.9.zip -d .
RUN rm dafny-1.9.9.zip
RUN /bin/bash -c "sed $'s/\r$//' ./dafny/dafny > ./dafny/dafny.Unix"
RUN mv /dafny/dafny.Unix /dafny/dafny
RUN chmod -R a+rwx dafny/*
ENV PATH $PATH:/dafny/

# Clone CASPER
RUN git clone https://github.com/uwplse/Casper.git
WORKDIR Casper/
RUN git reset --hard 834d980
RUN ant
