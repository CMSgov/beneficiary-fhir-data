#!/bin/bash

set -e

sudo yum update-minimal --security -y
sudo amazon-linux-extras enable docker
yum clean metadata
yum install docker
echo "done"
