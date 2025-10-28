FROM public.ecr.aws/lambda/provided:al2023.2025.10.28.11-arm64 as dnf

RUN dnf install jq unzip tar gzip libicu aws-cli git gettext java-25-amazon-corretto-headless -y && dnf clean all

FROM dnf as opentofu

ARG opentofu_version=1.10.1
ARG opentofu_checksum=102ca3c409da379f13baf272d0301afcd4f5e4a558970b7bc4473f2f286d4598

RUN curl -fsSL -o /tmp/tofu.zip https://github.com/opentofu/opentofu/releases/download/v${opentofu_version}/tofu_${opentofu_version}_linux_arm64.zip \
    && checksum=$(sha256sum /tmp/tofu.zip | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${opentofu_checksum}" ]] \
    && unzip -d /usr/local/bin /tmp/tofu.zip \
    && rm /tmp/tofu.zip
ENV TF_DATA_DIR=/tmp

FROM opentofu as yq

ARG yq_version=4.45.4
ARG yq_checksum=a02cc637409db44a9f9cb55ea92c40019582ba88083c4d930a727ec4b59ed439

RUN curl -fsSL -o /usr/local/bin/yq https://github.com/mikefarah/yq/releases/download/v${yq_version}/yq_linux_arm64 \
    && checksum=$(sha256sum /usr/local/bin/yq | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${yq_checksum}" ]] \
    && chmod +x /usr/local/bin/yq

FROM yq as sops

ARG sops_version=3.10.2
ARG sops_checksum=e91ddc04e6a78f5aed9e4fc347a279b539c43b74d99e6b8078e2f2f6f5b309f5

RUN curl -fsSL -o /usr/local/bin/sops https://github.com/getsops/sops/releases/download/v${sops_version}/sops-v${sops_version}.linux.arm64 \
    && checksum=$(sha256sum /usr/local/bin/sops | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${sops_checksum}" ]] \
    && chmod +x /usr/local/bin/sops
