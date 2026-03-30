FROM public.ecr.aws/amazonlinux/amazonlinux@sha256:37c9056c6fc3492b333aff2836dfbd4013f5f63458a9e557907ccf9996da6e7f AS base

RUN dnf -y update && \
    dnf -y clean all && \
    rm -rf /var/cache

FROM base as uv

COPY --from=ghcr.io/astral-sh/uv:0.11.2@sha256:c4f5de312ee66d46810635ffc5df34a1973ba753e7241ce3a08ef979ddd7bea5 /uv /uvx /bin/
