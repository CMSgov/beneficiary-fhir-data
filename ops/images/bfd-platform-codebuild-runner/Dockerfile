FROM public.ecr.aws/lambda/provided:al2023.2026.10.22.03-arm64@sha256:b75fdf03150df6f251b59833373dbf708c9e5f96d1f7dc9cbe6eec4d17fd43b3 as dnf

RUN dnf install jq unzip tar gzip libicu aws-cli git gettext java-25-amazon-corretto-headless findutils zip -y && dnf clean all

FROM dnf as opentofu

ARG opentofu_version=1.12.9
ARG opentofu_checksum=454b25e0675f13da0b794dfcc562a16f64cb997b2491d5102170102aec701d4f

RUN curl -fsSL -o /tmp/tofu.zip https://github.com/opentofu/opentofu/releases/download/v${opentofu_version}/tofu_${opentofu_version}_linux_arm64.zip \
    && checksum=$(sha256sum /tmp/tofu.zip | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${opentofu_checksum}" ]] \
    && unzip -d /usr/local/bin /tmp/tofu.zip \
    && rm /tmp/tofu.zip
ENV TF_DATA_DIR=/tmp

FROM opentofu as yq

ARG yq_version=4.54.1
ARG yq_checksum=189088da0c6429ec5178dfaab1a114805f6cab0b61b165ab236efedf1d57a71b

RUN curl -fsSL -o /usr/local/bin/yq https://github.com/mikefarah/yq/releases/download/v${yq_version}/yq_linux_arm64 \
    && checksum=$(sha256sum /usr/local/bin/yq | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${yq_checksum}" ]] \
    && chmod +x /usr/local/bin/yq

FROM yq as sops

ARG sops_version=3.13.3
ARG sops_checksum=53b0abacd38ef1b12a66d6c100956691b9cefce018d91f81e73ddf7438b94d77

RUN curl -fsSL -o /usr/local/bin/sops https://github.com/getsops/sops/releases/download/v${sops_version}/sops-v${sops_version}.linux.arm64 \
    && checksum=$(sha256sum /usr/local/bin/sops | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${sops_checksum}" ]] \
    && chmod +x /usr/local/bin/sops

FROM sops as uv

ARG uv_version=0.13.5
ARG uv_checksum=4bb69cf95ed13f23713bb7bc09f241896ea0c75b7c557e615a20604b82f9c403

RUN curl -fsSL -o /tmp/uv.tar.gz https://github.com/astral-sh/uv/releases/download/${uv_version}/uv-aarch64-unknown-linux-gnu.tar.gz \
    && checksum=$(sha256sum /tmp/uv.tar.gz | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${uv_checksum}" ]] \
    && tar -xf /tmp/uv.tar.gz -C /usr/local/bin --strip-components=1 \
    && rm /tmp/uv.tar.gz \
    && chmod +x /usr/local/bin/uv /usr/local/bin/uvx

# Fix installing python versions and packages when running on a Lambda CodeBuild Runner where only
# $LAMBDA_TASK_ROOT and /tmp are writeable
ENV XDG_CACHE_HOME=${LAMBDA_TASK_ROOT}/.cache
ENV UV_PYTHON_INSTALL_DIR=${LAMBDA_TASK_ROOT}/.python
