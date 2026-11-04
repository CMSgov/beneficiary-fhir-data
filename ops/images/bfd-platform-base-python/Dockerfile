FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20261102.0-minimal@sha256:d4495d25da60ef3f2d44bc7f6a2dd2ec1646c14414e0dfaaa44dc3ff653e1c2b AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.13.5@sha256:32a7bb39b496babbd997b0a39a4d66c126bdaf5661b302bf6f06abda8deca6ff /uv /uvx /bin/
