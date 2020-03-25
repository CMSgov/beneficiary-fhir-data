FROM ubuntu:18.04
COPY . /config
RUN apt-get update
RUN apt install -y maven openjdk-8-jdk make
RUN useradd -ms /bin/bash dev
USER dev
RUN mkdir -p $HOME/.m2
RUN cp /config/toolchains.xml $HOME/.m2/toolchains.xml
