FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20260817.0-minimal@sha256:66e9e022070c9eaf1ef9cdbd3f56ca9f9b3dee008621e7aaf55195dff1ef324e AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.12.5@sha256:e85be844203885286c60ffad8a858d48afb6c5a5c237ca0e67f12e74b8f174b1 /uv /uvx /bin/
