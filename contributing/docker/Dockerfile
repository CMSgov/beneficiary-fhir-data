FROM amazonlinux:2
# You may install a specific maven version using `docker build --build-arg MVN_VER="3.9.1" ...`
# See https://maven.apache.org/download.cgi to find the latest version and available mirrors.
ARG MVN_VER="3.9.1"
ARG MVN_MIRROR="https://dlcdn.apache.org/"
ENV LANG en_US.UTF-8

COPY . /config

# Install some common dev utils
# - net-tools includes ifconfig, netstat, route, etc
# - iputils includes ping and friends
# - systat includes top, uptime, ps, etc
# - procps-ng includes iostat
RUN yum update -y --security && \
    yum groupinstall -y "Development Tools" && \
    yum install -y \
      sudo \
      net-tools \
      iputils \
      sysstat \
      procps-ng \
      wget \
      which \
      python3 \
      python3-pip \
      traceroute \
      mtr 
      
# Install Java 17 and Maven
RUN yum install -y java-17-amazon-corretto-devel && \
    curl -O ${MVN_MIRROR}/maven/maven-3/${MVN_VER}/binaries/apache-maven-${MVN_VER}-bin.tar.gz && \
    tar xvzf apache-maven-${MVN_VER}-bin.tar.gz && \
    mv apache-maven-${MVN_VER} /opt/maven && \
    rm -f apache-maven-*-bin.tar.gz

# Add dev user
RUN useradd dev && \
    usermod -a -G wheel dev && \
    echo '%wheel ALL=(ALL) NOPASSWD: ALL' > /etc/sudoers.d/dev

# Prepare dev's environment
USER dev
ENV BFD_V2_ENABLED true
ENV BFD_PAC_ENABLED true
ENV MAVEN_HOME /opt/maven
ENV PATH="${PATH}:${MAVEN_HOME}/bin"
RUN mkdir -p $HOME/.m2 && \
    cp /config/toolchains.xml $HOME/.m2/toolchains.xml
