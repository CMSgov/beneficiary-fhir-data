<!-- markdownlint-disable MD013 -->

# `log-retention-checker` Lambda Source

This subdirectory is the `log-retention-checker` workspace member. It is the Python source of the `log-retention-checker` Lambda.

## Environment Setup

This project is a member of the repository-wide `uv` workspace. Its dependencies are resolved from the root `pyproject.toml` and the shared `uv.lock`.

1. Install [`uv`](https://docs.astral.sh/uv/).

2. From the repository root, create the shared virtual environment:

   ```bash
   uv sync
   ```

3. Run this member's code with `uv run` from any directory in the repository. The shared virtual environment is `.venv` in the repository root. VS Code can use it through `Python: Select Interpreter`.

Linting and type checking (`ruff`, `basedpyright`) are configured at the repository root. The `prek` hooks run them.

## Updating/managing dependencies

See [`Managing dependencies`](https://docs.astral.sh/uv/concepts/projects/dependencies/) and [`Locking and syncing`](https://docs.astral.sh/uv/concepts/projects/sync/) in the `uv` docs for more information.
