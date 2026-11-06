FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20261102.0-minimal@sha256:d4495d25da60ef3f2d44bc7f6a2dd2ec1646c14414e0dfaaa44dc3ff653e1c2b AS base

# shadow-utils provides groupadd and useradd.
RUN dnf -y update && \
    dnf -y install shadow-utils && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.13.6@sha256:af22a52f5eafef3d78e551599d405c02df61919d18028f0cea8eda93bc9986d0 /uv /uvx /bin/

ENV USER=nonroot
ENV UID=1001
ENV GID=1001
RUN groupadd --system --gid ${GID} ${USER} \
  && useradd --system --gid ${GID} --uid ${UID} --create-home ${USER} \
  && mkdir -p /app \
  && chown -R nonroot:nonroot /app

ENV UV_CACHE_DIR=/app/.cache
ENV UV_COMPILE_BYTECODE=1
ENV UV_LINK_MODE=copy
# Hard links do not span filesystems. uv copies files instead of linking when the cache is on a
# mounted volume.
ENV UV_NO_DEV=1
# Tool executables go to a directory that is already on the PATH.
ENV UV_TOOL_BIN_DIR=/usr/local/bin
USER nonroot

# Build context is the repository root (the uv workspace root).
WORKDIR /app
