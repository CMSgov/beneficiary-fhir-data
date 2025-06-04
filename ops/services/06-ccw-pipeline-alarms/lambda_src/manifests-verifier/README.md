# `manifests-verifier` Lambda Source

This subdirectory contains the Python source code for the `manifests-verifier` Lambda.

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
