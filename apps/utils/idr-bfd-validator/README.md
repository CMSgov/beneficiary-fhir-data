# `idr-bfd-validator`

This subdirectory contains the Python source code for the `idr-bfd-validator` utility.

## Environment Setup

It is assumed you are using `pyright`/`pylance` for type-checking, `uv` for virtual environment and dependency management, and `ruff` for linting, formatting, and import sorting.

1. Install `uv`:

   ```bash
   brew install uv
   ```

2. Setup Python 3.13 virtual environment:

   ```bash
   uv sync
   ```

3. Your virtual environment is now setup! By default, it is available under `.venv`; using VS Code, this Virtual Environment can be chosen using the `Python: Select Interpreter` command

## Updating/managing dependencies

See [`Managing dependencies`](https://docs.astral.sh/uv/concepts/projects/dependencies/) and [`Locking and syncing`](https://docs.astral.sh/uv/concepts/projects/sync/) in the `uv` docs for more information.

## Building the Docker Image

This utility is particularly unique among most of BFD's Python in that it references the IDR Pipeline (`idr-pipeline`) as a direct dependency. Since we have no package repository for our Python and Docker/Podman does not follow symlinks for security reasons, we need a way for Docker to install `idr-pipeline` into a Docker Image.

We do this by using "[named contexts](https://docs.docker.com/reference/cli/docker/buildx/build/#build-context)" in addition to the default context (this `README`'s directory). That named context can then be referenced much like a layer in the `Dockerfile`, thus allowing the expected relative path handled by the `packages/idr-pipeline` directory symlink to resolve in a Docker Image build context.

### Using `dockerbuild.sh`

Typical local build:

```sh
./dockerbuild.sh
```

With arguments:

```sh
./dockerbuild.sh --build_arg base_version=2.999.0
```

### Using `podman`/`docker`

> [!NOTE]
> This is identical to what the `dockerbuild.sh` script does, so just use that. This is included for completeness.

```sh
DOCKER_BUILDKIT=1 podman buildx build "." \
  --file Dockerfile \
  --platform "linux/arm64" \
  --build-context idr-pipeline="./packages/$(readlink ./packages/idr-pipeline)" \
  --tag "bfd-platform-idr-bfd-validator"
```
