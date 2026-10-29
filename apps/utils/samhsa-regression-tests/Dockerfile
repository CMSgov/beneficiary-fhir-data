# HACK: `server-ng-resources` is a named build context supplied by the release pipeline
# (namedContexts in .github/workflows/build_container_images_matrix.json), and
# pindock requires every `--from` reference to resolve to a stage in this file or
# to a digest-pinned image. This empty stage is declared to satisfy it. BuildKit
# always prefers a named context over a stage of the same name, so the stage is
# never used. Tested with podman and it behaves as expected.
FROM scratch AS server-ng-resources

FROM ghcr.io/astral-sh/uv:0.13.5@sha256:32a7bb39b496babbd997b0a39a4d66c126bdaf5661b302bf6f06abda8deca6ff AS uv

FROM public.ecr.aws/lambda/python:3.14.2026.10.20.00-arm64@sha256:25860f9a052c84ab3354e7616bf6461d6f6cb45d92f754fadfbc9b87c68dcce9 AS lambda

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
