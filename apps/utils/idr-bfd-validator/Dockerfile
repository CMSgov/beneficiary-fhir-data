ARG base_version=latest

# Image is expected to exist locally prior to build. Operators will need to pull the image from ECR
# first before building
FROM bfd-platform-base-python:${base_version} AS deps

# The base image leaves the build as the non-root user. Package installation here needs root.
# TODO: Remove git when loguru is no longer installed from GitHub directly
USER root
RUN dnf -y install git && \
  dnf -y clean all && \
  rm -rf /var/cache
USER nonroot

# Install the project's dependencies using the lockfile and settings
COPY pyproject.toml uv.lock .python-version ./
COPY apps/utils/idr-bfd-validator/pyproject.toml apps/utils/idr-bfd-validator/pyproject.toml
COPY apps/bfd-pipeline-idr/pyproject.toml apps/bfd-pipeline-idr/pyproject.toml
COPY apps/bfd-pipeline-idr/src apps/bfd-pipeline-idr/src
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --frozen --no-install-project --project apps/utils/idr-bfd-validator

FROM deps

# Then, add the rest of the project source code and install it
# Installing separately from its dependencies allows optimal layer caching
COPY apps/utils/idr-bfd-validator /app/apps/utils/idr-bfd-validator
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --frozen --project apps/utils/idr-bfd-validator

WORKDIR /app/apps/utils/idr-bfd-validator

# Place executables in the environment at the front of the path
ENV PATH="/app/.venv/bin:$PATH"

# Stop Python from generating bytecode at runtime
ENV PYTHONDONTWRITEBYTECODE=1

# Run the validator
CMD ["uv", "run", "--no-sync", "main.py"]
