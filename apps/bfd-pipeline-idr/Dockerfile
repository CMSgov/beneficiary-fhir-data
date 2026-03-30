ARG base_version=latest

# Image is expected to exist locally prior to build. Operators will need to pull the image from ECR
# first before building
FROM bfd-platform-base-python:${base_version} AS groupadd

# "shadow-utils" provides groupadd/useradd
RUN dnf -y install shadow-utils && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM groupadd AS deps

# Setup a non-root user
RUN groupadd --system --gid 999 nonroot \
  && useradd --system --gid 999 --uid 999 --create-home nonroot

# Install the project into `/app`
WORKDIR /app

# Use the non-root user to run our application
RUN chown -R nonroot:nonroot /app
USER nonroot

# Enable bytecode compilation
ENV UV_COMPILE_BYTECODE=1

# Copy from the cache instead of linking since it's a mounted volume
ENV UV_LINK_MODE=copy

# Omit development dependencies
ENV UV_NO_DEV=1

# Ensure installed tools can be executed out of the box
ENV UV_TOOL_BIN_DIR=/usr/local/bin

# Install the project's dependencies using the lockfile and settings
RUN --mount=type=cache,target=/root/.cache/uv \
  --mount=type=bind,source=uv.lock,target=uv.lock,z \
  --mount=type=bind,source=pyproject.toml,target=pyproject.toml,z \
  uv sync --locked --no-install-project

FROM deps

# Then, add the rest of the project source code and install it
# Installing separately from its dependencies allows optimal layer caching
COPY . /app
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --locked

# Place executables in the environment at the front of the path
ENV PATH="/app/.venv/bin:$PATH"

# Set the entrypoint to allow for command-line modesetting
ENTRYPOINT ["uv", "run", "--no-sync", "pipeline.py"]

# Run the pipeline in IDR mode by default
CMD []
