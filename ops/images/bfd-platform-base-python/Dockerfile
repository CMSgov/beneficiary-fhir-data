FROM public.ecr.aws/amazonlinux/amazonlinux:2023.12.20261026.1-minimal@sha256:c25add18b6c35620a6e096ef03ecbafbcaed993281ad227792f67c324980011b AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base AS uv

COPY --from=ghcr.io/astral-sh/uv:0.13.5@sha256:32a7bb39b496babbd997b0a39a4d66c126bdaf5661b302bf6f06abda8deca6ff /uv /uvx /bin/
