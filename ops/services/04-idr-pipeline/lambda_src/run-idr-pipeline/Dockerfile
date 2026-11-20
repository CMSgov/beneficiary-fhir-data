FROM ghcr.io/astral-sh/uv:0.13.8@sha256:5cc1805e2426484abca4ad46ea783bab281fdb225fd82d8ce6d66a81254bac85 AS uv

FROM public.ecr.aws/lambda/python:3.15.2026.11.13.18-arm64@sha256:b6f57a390b8448f0dc235de68ad86d12a0b54f92c379250e11232f517fe3acf5 AS lambda

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
# Scope the export to this workspace member with --project. Skip development dependencies
# with --no-dev. This layer changes only when the workspace `uv.lock` or this member's
# `pyproject.toml` changes.
COPY pyproject.toml uv.lock .python-version ./
COPY ops/services/04-idr-pipeline/lambda_src/run-idr-pipeline/pyproject.toml ops/services/04-idr-pipeline/lambda_src/run-idr-pipeline/pyproject.toml
RUN --mount=from=uv,source=/uv,target=/bin/uv \
  --mount=type=cache,target=/root/.cache/uv \
  uv export --frozen --no-dev --no-editable \
    --project ops/services/04-idr-pipeline/lambda_src/run-idr-pipeline -o requirements.txt && \
  uv pip install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

FROM lambda
# Copy the runtime dependencies from the builder stage.
COPY --from=builder ${LAMBDA_TASK_ROOT} ${LAMBDA_TASK_ROOT}
# Copy the application code.
COPY ops/services/04-idr-pipeline/lambda_src/run-idr-pipeline/app ${LAMBDA_TASK_ROOT}/app
CMD [ "app.main.handler" ]
