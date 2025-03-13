ARG base_version
ARG registry_uri

# ECR_REPOSITORY_NAMESPACE and BFD_VERSION are both exported as part of the "Build Release" GHA
# Workflow, so we take them as defaults if the arguments are unspecified
FROM ${registry_uri:-${ECR_REPOSITORY_NAMESPACE}}/bfd-mgmt-base-python:${base_version:-${BFD_VERSION}} as deps
# Enable copy mode to support bind mount caching.
ENV UV_LINK_MODE=copy
# Change the working directory to the `app` directory
WORKDIR /app
# Install dependencies
RUN --mount=type=cache,target=/root/.cache/uv,z \
  --mount=type=bind,source=uv.lock,target=uv.lock,z \
  --mount=type=bind,source=pyproject.toml,target=pyproject.toml,z \
  uv sync --frozen --no-install-project --compile-bytecode

FROM deps
# Copy the application code to the `app` directory
ADD . /app
# Sync the project
RUN --mount=type=cache,target=/root/.cache/uv,z \
  uv sync --frozen
RUN mkdir -p /data
VOLUME ["/data"]
ENTRYPOINT ["uv", "run", "main.py"]
