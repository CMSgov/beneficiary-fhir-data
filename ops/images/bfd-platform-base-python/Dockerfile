FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20260817.0-minimal@sha256:66e9e022070c9eaf1ef9cdbd3f56ca9f9b3dee008621e7aaf55195dff1ef324e AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.13.4@sha256:9f130421e930bfbdc563c00740466603f8723158e90297d07957c2294640262b /uv /uvx /bin/
