# HACK: `server-ng-resources` is a named build context supplied by the release pipeline
# (namedContexts in .github/workflows/build_container_images_matrix.json), and
# pindock requires every `--from` reference to resolve to a stage in this file or
# to a digest-pinned image. This empty stage is declared to satisfy it. BuildKit
# always prefers a named context over a stage of the same name, so the stage is
# never used. Tested with podman and it behaves as expected.
FROM scratch AS server-ng-resources

FROM ghcr.io/astral-sh/uv:0.13.7@sha256:96f40d3904aad8d0bd010539001d36c8e2188b981e1334471a90c4a1d93bb490 AS uv

FROM public.ecr.aws/lambda/python:3.15.2026.11.05.19-arm64@sha256:505a1d4216c7e32169b0b801ebcb6c9e75c4e89bd25b09eb24a1a7ffbac7f3cf AS lambda

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
COPY pyproject.toml uv.lock ./
RUN --mount=from=uv,source=/uv,target=/bin/uv \
  --mount=type=cache,target=/root/.cache/uv \
  uv export --frozen --no-emit-workspace --no-dev --no-editable -o requirements.txt && \
  uv pip install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"

FROM lambda
# Copy the runtime dependencies from the builder stage.
COPY --from=builder ${LAMBDA_TASK_ROOT} ${LAMBDA_TASK_ROOT}
# Copy the application code.
COPY ./app ${LAMBDA_TASK_ROOT}/app
# Bundle the security labels used by the server-ng SAMHSA filter alongside the application code.
COPY --from=server-ng-resources security_labels.yml ${LAMBDA_TASK_ROOT}/app/security_labels.yml
CMD [ "app.handler.handler" ]
