ARG base_version=latest

# Image is expected to exist locally prior to build. Operators will need to pull the image from ECR
# first before building
FROM bfd-platform-base-python:${base_version} AS deps
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
# Amazon ECS creates the task volume mounted at /data from this directory, taking its
# ownership and permissions. The image runs as uid 1001. The directory is owned by that user.
# A root-owned directory would stop this container from writing the certstores. The mode is
# 0777 in case the volume is created with a different owner.
USER root
RUN mkdir -p /data && chown nonroot:nonroot /data && chmod 0777 /data
USER nonroot
VOLUME ["/data"]
ENTRYPOINT ["uv", "run", "--no-dev", "--no-cache", "--no-sync", "main.py"]
