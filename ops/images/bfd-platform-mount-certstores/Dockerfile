ARG base_version=latest

# Image is expected to exist locally prior to build. Operators will need to pull the image from ECR
# first before building
FROM bfd-platform-base-python:${base_version} AS deps
# Enable copy mode to support bind mount caching.
ENV UV_LINK_MODE=copy
# Build context is the repository root (the uv workspace root).
WORKDIR /app
# Install dependencies
COPY pyproject.toml uv.lock .python-version ./
COPY ops/images/bfd-platform-mount-certstores/pyproject.toml ops/images/bfd-platform-mount-certstores/pyproject.toml
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --frozen --no-install-project --compile-bytecode --no-dev \
    --project ops/images/bfd-platform-mount-certstores

FROM deps
# Copy the application code
COPY ops/images/bfd-platform-mount-certstores /app/ops/images/bfd-platform-mount-certstores
# Sync the project
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --frozen --no-dev --project ops/images/bfd-platform-mount-certstores

WORKDIR /app/ops/images/bfd-platform-mount-certstores
RUN mkdir -p /data
VOLUME ["/data"]
ENTRYPOINT ["uv", "run", "--no-dev", "--no-cache", "--no-sync", "main.py"]
