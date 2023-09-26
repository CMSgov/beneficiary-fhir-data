#!/bin/bash

set -e

sudo yum update-minimal --security -y
sudo amazon-linux-extras enable docker
sudo amazon-linux-extras install docker
