#!/bin/bash

set -e

yum update-minimal --security -y && amazon-linux-extras enable docker && amazon-linux-extras install docker
