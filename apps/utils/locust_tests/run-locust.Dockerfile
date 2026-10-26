FROM ghcr.io/astral-sh/uv:0.13.5@sha256:32a7bb39b496babbd997b0a39a4d66c126bdaf5661b302bf6f06abda8deca6ff AS uv

FROM public.ecr.aws/lambda/python:3.14.2026.10.22.03-arm64@sha256:9b353a9c5d361d322f58ce176f1322deb983ecd8da367a52256a20e1ccc7ad06 AS lambda

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
# Omit any local packages (`--no-emit-workspace`) and development dependencies (`--no-dev`).
# This ensures that the Docker layer cache is only invalidated when the `pyproject.toml` or `uv.lock`
# files change, but remains robust to changes in the application code.
COPY pyproject.toml .
COPY uv.lock .
RUN --mount=from=uv,source=/uv,target=/bin/uv \
  --mount=type=cache,target=/root/.cache/uv \
  uv export --frozen --no-emit-workspace --no-dev --no-editable --group lambda-run-locust -o requirements.txt && \
  uv pip install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

FROM lambda
# Copy the runtime dependencies from the builder stage.
COPY --from=builder ${LAMBDA_TASK_ROOT} ${LAMBDA_TASK_ROOT}
# Copy the application code.
COPY . ${LAMBDA_TASK_ROOT}/app
RUN mv "${LAMBDA_TASK_ROOT}/app/lambda/run-locust/app.py" "${LAMBDA_TASK_ROOT}/app"
RUN rm -rf "${LAMBDA_TASK_ROOT}/app/lambda"
# Ensure that all packages installed are available on the Python path (necessary to invoke locust
# CLI)
ENV PYTHONPATH="${LAMBDA_TASK_ROOT}:${LAMBDA_TASK_ROOT}/app"
# Ensure that binaries (such as locust) are in the PATH
ENV PATH="${LAMBDA_TASK_ROOT}/bin:${PATH}"
CMD [ "app.app.handler" ]
