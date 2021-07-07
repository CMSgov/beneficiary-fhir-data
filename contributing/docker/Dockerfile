FROM centos:centos7
COPY . /config
RUN yum -y update
RUN yum -y install java-1.8.0-openjdk-devel make sudo
# Install Latest Maven Stable
ARG MVN_VER="3.6.3"
RUN curl -O http://mirrors.advancedhosters.com/apache/maven/maven-3/${MVN_VER}/binaries/apache-maven-${MVN_VER}-bin.tar.gz
RUN tar xvf apache-maven-${MVN_VER}-bin.tar.gz
RUN mv apache-maven-${MVN_VER} /opt/maven
ENV LANG en_US.UTF-8
RUN useradd -ms /bin/bash dev
ENV BFD_V2_ENABLED true
ENV BFD_PREADJ_ENABLED true
USER dev
ENV MAVEN_HOME /opt/maven
ENV PATH="${PATH}:${MAVEN_HOME}/bin"
RUN mkdir -p $HOME/.m2
RUN cp /config/toolchains.xml $HOME/.m2/toolchains.xml
