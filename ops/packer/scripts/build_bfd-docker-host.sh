#!/bin/bash

set -e

sudo yum update-minimal --security -y
sudo amazon-linux-extras enable docker
sudo yum clean metadata
sudo yum install docker -y
