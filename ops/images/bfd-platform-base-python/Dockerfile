FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20261102.0-minimal@sha256:d4495d25da60ef3f2d44bc7f6a2dd2ec1646c14414e0dfaaa44dc3ff653e1c2b AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.13.6@sha256:af22a52f5eafef3d78e551599d405c02df61919d18028f0cea8eda93bc9986d0 /uv /uvx /bin/
