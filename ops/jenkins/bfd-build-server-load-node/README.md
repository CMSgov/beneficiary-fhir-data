# `bfd-server-load` Node

## Overview

The files within this directory consist of the Docker files used to build the node Lambda Docker image, 
and the Jenkinsfile and related build script for the `bfd-build-server-load-node` Jenkins pipeline.

## Building the `bfd-mgmt-server-load-node` Docker Image

### Building the Image Locally

As the `Dockerfile` for this image is not at the root of this `locust_tests` project, we need to
ensure the [Docker build
context](https://docs.docker.com/engine/reference/commandline/build/#description) is properly set to
the root. 

Ensure your current working directory is `/apps/utils/locust_tests` and run:

```bash
docker build -f "/ops/jenkins/bfd-build-server-load-node/Dockerfile" -t "<your-tag>" --platform linux/amd64 .
```
