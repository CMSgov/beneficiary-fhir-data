ARG PACKER_VERSION
ARG JAVA_VERSION
ARG MAVEN_VERSION

FROM maven:${MAVEN_VERSION}-amazoncorretto-${JAVA_VERSION} AS base

# TODO: install pip3 from a known requirements.txt file instead
RUN yum update -y --security && \
    amazon-linux-extras install epel -y && \
    yum clean metadata && \
    yum -y groupinstall "Development Tools" && \
    yum install -y \
    awscli \
    gcc \
    git \
    jq \
    openssl-devel

FROM base as prereqs

COPY toolchains.xml /root/.m2/toolchains.xml

ARG PYTHON3_TAR_SOURCE

SHELL ["/bin/bash", "-c"]
RUN curl -o python3.tgz $PYTHON3_TAR_SOURCE && \
    tar zxvf python3.tgz && \
    cd $(basename $PYTHON3_TAR_SOURCE .tgz) && \
    ./configure && make && make altinstall

RUN ln -fs /usr/local/bin/python3.9 /usr/bin/python3 && \
    ln -fs /usr/local/bin/pip3.9 /usr/bin/pip3

RUN /usr/bin/python3 -m pip install --upgrade pip

RUN rm -rf $(basename $PYTHON3_TAR_SOURCE .tgz) python3.tgz

FROM prereqs as tfenv
ARG TFENV_REPO_HASH
# NOTE: versions represented as space-delimited string; Dockerfile's RUN contexts don't use arrays
ARG TFENV_VERSIONS
COPY tfenv-install.sh /root/tfenv-install.sh
RUN chmod +x /root/tfenv-install.sh && \
    /root/tfenv-install.sh "${TFENV_REPO_HASH}" "${TFENV_VERSIONS}"

# declaring `packer` stage here allows us to inject `PACKER_VERSION` unlike `COPY --from` below
FROM hashicorp/packer:${PACKER_VERSION} as packer

FROM prereqs as dist
ENV PATH="/root/.tfenv/bin:${PATH}"
# COPY tfenv files
COPY --from=tfenv /root/.tfenv /root/.tfenv
# Copy packer files
COPY --from=packer /bin/packer /usr/local/bin/packer
