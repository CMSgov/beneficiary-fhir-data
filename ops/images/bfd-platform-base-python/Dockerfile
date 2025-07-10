FROM amazonlinux:2023 AS base

RUN yum -y --security update && \
    yum -y clean all && \
    rm -rf /var/cache

FROM base as uv

COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

FROM uv

# Ensure that uv-managed Python installation takes precedence over existing Python installation(s)
# in the PATH. This is necessary to ensure that the correct Python version is used when running
# scripts or applications
ENV PATH="/root/.local/bin:$PATH"
# As of uv 0.6.3, the "--preview" flag is still necessary to set a specific Python version as the
# default. This flag will be removed when "--default" is supported as a release feature
RUN uv python install python3.13 --preview --default
