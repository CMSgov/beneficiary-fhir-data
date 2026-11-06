FROM ghcr.io/astral-sh/uv:0.13.6@sha256:af22a52f5eafef3d78e551599d405c02df61919d18028f0cea8eda93bc9986d0 AS uv

FROM public.ecr.aws/lambda/python:3.14.2026.10.31.17-arm64@sha256:36a56dd455d445a23e9f45e4959cdcaf6422068bd3390996c0f9544e6372d986 AS lambda

FROM lambda as builder
# First, bundle the dependencies into the task root.
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
COPY apps/utils/locust_tests/pyproject.toml apps/utils/locust_tests/pyproject.toml
RUN --mount=from=uv,source=/uv,target=/bin/uv \
  --mount=type=cache,target=/root/.cache/uv \
  uv export --frozen --no-dev --no-editable --group lambda-run-locust \
    --project apps/utils/locust_tests -o requirements.txt && \
  uv pip install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

FROM lambda
# Copy the runtime dependencies from the builder stage.
COPY --from=builder ${LAMBDA_TASK_ROOT} ${LAMBDA_TASK_ROOT}
# Copy the application code.
COPY apps/utils/locust_tests ${LAMBDA_TASK_ROOT}/app
RUN mv "${LAMBDA_TASK_ROOT}/app/lambda/run-locust/app.py" "${LAMBDA_TASK_ROOT}/app"
RUN rm -rf "${LAMBDA_TASK_ROOT}/app/lambda"
# Ensure that all packages installed are available on the Python path (necessary to invoke locust
# CLI)
ENV PYTHONPATH="${LAMBDA_TASK_ROOT}:${LAMBDA_TASK_ROOT}/app"
# Ensure that binaries (such as locust) are in the PATH
ENV PATH="${LAMBDA_TASK_ROOT}/bin:${PATH}"
CMD [ "app.app.handler" ]
