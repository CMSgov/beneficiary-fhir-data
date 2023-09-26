#!/bin/bash

set -e

sudo yum update-minimal --security -y && amazon-linux-extras enable docker && amazon-linux-extras install docker
