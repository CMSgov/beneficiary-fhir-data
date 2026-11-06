<!-- markdownlint-disable MD013 -->
# `server-mount-certstores` (`bfd-platform-mount-certstores` image)

This subfolder contains the source code for a small Python script that downloads the certstores for the BFD Server into a specified location and the corresponding `Dockerfile` for building it into a container image.

Used in `ops/services/server` ECS Task Definitions for the `server` ECS Service as `certstores` to ensure the BFD Server has the necessary certificates prior to startup.

## Runtime user

The image runs as uid `1001`, the `nonroot` user created in `ops/images/bfd-platform-base-python/Dockerfile`.

`main.py` writes the certstores into `/data`. The BFD Server container mounts the same task volume read-only at the same path. Amazon ECS builds that volume from the `/data` directory in this image and takes its ownership and permissions. The `Dockerfile` therefore creates the directory as that user and leaves it world-writable. The task definition in `ops/services/04-server/service.tf` mounts a tmpfs at `/tmp` with matching uid and gid mount options. A task volume at that path would be owned by root and unwritable to this container.

## Developer Environment Setup

### Prerequisites

- `uv` (`brew install uv`)

### Setup

This project is a member of the repository-wide `uv` workspace. Run `uv sync` from the repository root. The dependencies come from the root `pyproject.toml` and the shared `uv.lock`. The virtual environment is created at `.venv` in the repository root.
