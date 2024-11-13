#!/usr/bin/env sh

# Import Corretto RPM key
sudo rpm --import https://yum.corretto.aws/corretto.key

# Download Corretto repository configuration
sudo curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo

# Install dependencies
sudo yum install -y \
    "@Development Tools" \
    amazon-cloudwatch-agent \
    ansible \
    gcc \
    git \
    java-21-amazon-corretto-devel \
    libffi-devel \
    openssl-devel \
    selinux-policy \
    systemd \
    tar \
    unzip \
    yum-utils \
    yum-plugin-versionlock
