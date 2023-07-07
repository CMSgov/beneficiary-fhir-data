# `bfd-mgmt-server-regression` Docker Image

## Building the Image Locally

As the `Dockerfile` for this image is not at the root of this `lambda/server-regression` project, we need to
ensure the [Docker build
context](https://docs.docker.com/engine/reference/commandline/build/#description) is properly set to
the root.

Ensure your current working directory is `/apps/utils/locust_tests` and run:

```bash
docker build -f "/ops/jenkins/bfd-server-regression-build/Dockerfile" -t "<your-tag>" .
```
