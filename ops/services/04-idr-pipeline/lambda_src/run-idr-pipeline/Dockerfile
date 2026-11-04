FROM ghcr.io/astral-sh/uv:0.13.6@sha256:af22a52f5eafef3d78e551599d405c02df61919d18028f0cea8eda93bc9986d0 AS uv

FROM public.ecr.aws/lambda/python:3.14.2026.10.31.17-arm64@sha256:36a56dd455d445a23e9f45e4959cdcaf6422068bd3390996c0f9544e6372d986 AS lambda

# First, bundle the dependencies into the task root.
FROM lambda AS builder
# Enable bytecode compilation, to improve cold-start performance.
ENV UV_COMPILE_BYTECODE=1
# Disable installer metadata, to create a deterministic layer.
ENV UV_NO_INSTALLER_METADATA=1
# Enable copy mode to support bind mount caching.
ENV UV_LINK_MODE=copy
# Bundle the dependencies into the Lambda task root via `uv pip install --target`.
#
# Omit any local packages (`--no-emit-workspace`) and development dependencies (`--no-dev`).
# This ensures that the Docker layer cache is only invalidated when the `pyproject.toml` or `uv.lock`
# files change, but remains robust to changes in the application code.
COPY pyproject.toml uv.lock .python-version ./
RUN --mount=from=uv,source=/uv,target=/bin/uv \
  --mount=type=cache,target=/root/.cache/uv \
  uv export --frozen --no-emit-workspace --no-dev --no-editable -o requirements.txt && \
  uv pip install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

FROM lambda
# Copy the runtime dependencies from the builder stage.
COPY --from=builder ${LAMBDA_TASK_ROOT} ${LAMBDA_TASK_ROOT}
# Copy the application code.
COPY ./app ${LAMBDA_TASK_ROOT}/app
CMD [ "app.main.handler" ]
