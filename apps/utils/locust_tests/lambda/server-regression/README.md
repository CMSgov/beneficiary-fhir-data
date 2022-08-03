# `bfd-mgmt-server-regression` Docker Image

## Building the Image Locally

As the `Dockerfile` for this image is not at the root of this `locust_tests` project, we need to ensure the [Docker build context](https://docs.docker.com/engine/reference/commandline/build/#description) is properly set to the root. This can be achieved in two ways:

### Option 1 -- Specifying the `lambda/server-regression/Dockerfile` from the Root

This option is _preferred_ to option 2, as it is generally better form to run `docker build` from the directory of the build context itslef.

Ensure you current working directory is `/apps/utils/locust_tests` and run:

```
docker build -f lambda/bfd-server-regression/Dockerfile -t "<your-tag>" --platform linux/amd64 .
```

### Option 2 -- Specifying the Context Relative to This Directory

This option simply tells Docker that the build context is the grandparent directory, via `../../`. Ensure your current working directory is `lambda/server-regression` and run:

```
docker build -t "<your-tag>" --platform linux/amd64 ../../.
```
