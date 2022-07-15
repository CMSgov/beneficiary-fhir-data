ARG PACKER_VERSION
ARG JAVA_VERSION
ARG MAVEN_VERSION

FROM maven:${MAVEN_VERSION}-openjdk-${JAVA_VERSION} AS base

COPY toolchains.xml /root/.m2/toolchains.xml

ARG ANSIBLE_VERSION

RUN apt-get -y update && \
    apt-get -y install python3-pip jq && \
    pip3 install ansible==${ANSIBLE_VERSION} awscli boto

FROM base as tfenv
ARG TFENV_REPO_HASH
# NOTE: versions represented as space-delimited string; Dockerfile's RUN contexts don't use arrays
ARG TFENV_VERSIONS
COPY tfenv-install.sh /root/tfenv-install.sh
RUN chmod +x /root/tfenv-install.sh && \
    /root/tfenv-install.sh "${TFENV_REPO_HASH}" "${TFENV_VERSIONS}"

# declaring `packer` stage here allows us to inject `PACKER_VERSION` unlike `COPY --from` below
FROM hashicorp/packer:${PACKER_VERSION} as packer

FROM base as dist
ENV PATH="/root/.tfenv/bin:${PATH}"
# COPY tfenv files
COPY --from=tfenv /root/.tfenv /root/.tfenv
# Copy packer files
COPY --from=packer /bin/packer /usr/local/bin/packer
