FROM public.ecr.aws/lambda/provided:al2023.2026.03.12.12-arm64 as dnf

RUN dnf install jq unzip tar gzip libicu aws-cli git gettext java-25-amazon-corretto-headless findutils zip -y && dnf clean all

FROM dnf as opentofu

ARG opentofu_version=1.10.6
ARG opentofu_checksum=a32f653d686a8cad9b9be82101eb5b5e834fbfa8d095842fa1820c5d27fad967

RUN curl -fsSL -o /tmp/tofu.zip https://github.com/opentofu/opentofu/releases/download/v${opentofu_version}/tofu_${opentofu_version}_linux_arm64.zip \
    && checksum=$(sha256sum /tmp/tofu.zip | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${opentofu_checksum}" ]] \
    && unzip -d /usr/local/bin /tmp/tofu.zip \
    && rm /tmp/tofu.zip
ENV TF_DATA_DIR=/tmp

FROM opentofu as yq

ARG yq_version=4.52.4
ARG yq_checksum=4c2cc022a129be5cc1187959bb4b09bebc7fb543c5837b93001c68f97ce39a5d

RUN curl -fsSL -o /usr/local/bin/yq https://github.com/mikefarah/yq/releases/download/v${yq_version}/yq_linux_arm64 \
    && checksum=$(sha256sum /usr/local/bin/yq | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${yq_checksum}" ]] \
    && chmod +x /usr/local/bin/yq

FROM yq as sops

ARG sops_version=3.12.1
ARG sops_checksum=c7904d07bf1fb3ffdc329b0103ce70ff6a884aa7af7931427402bea0c09cc522

RUN curl -fsSL -o /usr/local/bin/sops https://github.com/getsops/sops/releases/download/v${sops_version}/sops-v${sops_version}.linux.arm64 \
    && checksum=$(sha256sum /usr/local/bin/sops | cut -f 1 -d ' ') \
    && [[ "${checksum}" = "${sops_checksum}" ]] \
    && chmod +x /usr/local/bin/sops
