# `rds-enhanced-monitoring-enabler` Lambda Source

This subdirectory contains the Python source code for the `rds-enhanced-monitoring-enabler` Lambda.

## Environment Setup

It is assumed you are using `pyright`/`pylance` for type-checking, `uv` for virtual environment and dependency management, and `ruff` for linting, formatting, and import sorting.

1. Install `uv`:

   ```bash
   brew install uv
   ```

2. Setup Python 3.11 virtual environment:

   ```bash
   uv venv --python 3.11
   source .venv/bin/activate
   ```

3. Install all dependencies:

   ```bash
   uv pip sync requirements.txt dev-requirements.txt
   ```

4. Your virtual environment is now setup!

## Updating/managing dependencies

1. Add the dependency (with version constraint, if necessary) to `dev-requirements.in` if it is a development-only tool/library (like for testing, linting, etc.) or `requirements.in` if it is a runtime dependency
2. Source the virtual environment (`source .venv/bin/activate`)
3. Run:

   ```bash
   uv pip compile --upgrade --output-file requirements.txt --generate-hashes requirements.in
   uv pip compile --upgrade --constraint requirements.txt --output-file dev-requirements.txt --generate-hashes dev-requirements.in
   ```

4. Install requirements:

   ```bash
   uv pip sync requirements.txt dev-requirements.txt
   ```

<!-- BEGIN_TF_DOCS -->
