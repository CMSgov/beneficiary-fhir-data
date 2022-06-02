ARG VERSION_JAVA
ARG MAVEN_VERSION

FROM maven:${MAVEN_VERSION}-openjdk-${VERSION_JAVA} AS base

# NOTE: necessary duplicate entry
ARG VERSION_JAVA

COPY jdk${VERSION_JAVA}.toolchains.xml /root/.m2/toolchains.xml

ARG ANSIBLE_VERSION

RUN apt -y update && \
    apt -y install python3-pip jq && \
    pip3 install ansible==${ANSIBLE_VERSION} awscli boto

FROM base as tfenv
ARG TFENV_REPO_HASH
# NOTE: versions represented as string; Dockerfile's RUN contexts don't use arrays
ARG TFENV_VERSIONS
COPY tfenv-install.sh /root/tfenv-install.sh
RUN chmod +x /root/tfenv-install.sh && \
    /root/tfenv-install.sh "${TFENV_REPO_HASH}" "${TFENV_VERSIONS}"

FROM base as hashicorp
ARG PACKER_VERSION
RUN curl -L -o packer.zip https://releases.hashicorp.com/packer/${PACKER_VERSION}/packer_${PACKER_VERSION}_linux_amd64.zip && \
    unzip packer.zip -d /usr/local/bin

FROM base as dist
ENV PATH="/root/.tfenv/bin:${PATH}"
# COPY tfenv files
COPY --from=tfenv /root/.tfenv /root/.tfenv
# Copy hashicorp files (packer)
COPY --from=hashicorp /usr/local/bin/packer /usr/local/bin/packer
