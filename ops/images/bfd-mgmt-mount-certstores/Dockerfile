ARG base_version
ARG registry_uri

FROM ${registry_uri}/bfd-mgmt-base-python:${base_version} AS deps
# Enable copy mode to support bind mount caching.
ENV UV_LINK_MODE=copy
# Change the working directory to the `app` directory
WORKDIR /app
# Install dependencies
COPY pyproject.toml uv.lock ./
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --frozen --no-install-project --compile-bytecode

FROM deps
# Copy the application code to the `app` directory
COPY . /app
# Sync the project
RUN --mount=type=cache,target=/root/.cache/uv \
  uv sync --frozen
RUN mkdir -p /data
VOLUME ["/data"]
ENTRYPOINT ["uv", "run", "main.py"]
