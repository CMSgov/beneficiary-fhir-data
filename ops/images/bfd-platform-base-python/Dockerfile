FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20261021.0-minimal@sha256:5a0fa28a0ef6511c7e01e8b96c676a345ccdd03877cf1f24708784b86ee40832 AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.13.5@sha256:32a7bb39b496babbd997b0a39a4d66c126bdaf5661b302bf6f06abda8deca6ff /uv /uvx /bin/
