# `bfd-mgmt-mount-certstores` Image and Application

This subfolder contains the source code for a small Python script that downloads the certstores for the BFD Server into a specified location and the corresponding `Dockerfile` for building it into a container image.

Used in `ops/services/server` ECS Task Definitions for the `server` ECS Service as `certstores` to ensure the BFD Server has the necessary certificates prior to startup.

## Developer Environment Setup

### Prerequisites

- `uv` (`brew install uv`)

### Setup

To set up your development environment, just run `uv sync`. Assumning you have `uv` installed, this will create a `.venv` within this subfolder with all the necessary dependencies.
