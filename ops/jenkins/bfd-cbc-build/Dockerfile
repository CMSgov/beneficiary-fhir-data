ARG JAVA_VERSION
ARG MAVEN_VERSION
ARG PACKER_VERSION
ARG YQ_VERSION

FROM maven:${MAVEN_VERSION}-amazoncorretto-${JAVA_VERSION} AS base
RUN yum update -y --security && \
    amazon-linux-extras install epel -y && \
    yum clean metadata && \
    yum -y groupinstall "Development Tools" && \
    yum install -y \
    ansible \
    gcc \
    git \
    jq \
    unzip \
    openssl-devel

# Install awscliv2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip ./aws

FROM base as prereqs

COPY toolchains.xml /root/.m2/toolchains.xml

ARG PYTHON3_TAR_SOURCE

SHELL ["/bin/bash", "-c"]
RUN curl -o python3.tgz $PYTHON3_TAR_SOURCE && \
    tar zxvf python3.tgz && \
    cd $(basename $PYTHON3_TAR_SOURCE .tgz) && \
    ./configure && make && make altinstall

RUN ln -fs /usr/local/bin/python3.11 /usr/bin/python3 && \
    ln -fs /usr/local/bin/pip3.11 /usr/bin/pip3

RUN /usr/bin/python3 -m pip install --upgrade pip

RUN rm -rf $(basename $PYTHON3_TAR_SOURCE .tgz) python3.tgz

FROM prereqs as kotlin
ARG KOTLINC_ZIP_SOURCE

WORKDIR /root
RUN curl -L $KOTLINC_ZIP_SOURCE -o kotlinc.zip && \
    unzip kotlinc.zip && \
    rm -f kotlinc/bin/*.bat

FROM prereqs as tfenv
ARG TFENV_REPO_HASH
# NOTE: versions represented as space-delimited string; Dockerfile's RUN contexts don't use arrays
ARG TFENV_VERSIONS
COPY tfenv-install.sh /root/tfenv-install.sh
RUN chmod +x /root/tfenv-install.sh && \
    /root/tfenv-install.sh "${TFENV_REPO_HASH}" "${TFENV_VERSIONS}"

# declaring `packer` and `yq` stages here allows us to inject specific versions for `COPY --from` use in `dist` below
FROM hashicorp/packer:${PACKER_VERSION} as packer
FROM mikefarah/yq:${YQ_VERSION} as yq

FROM prereqs as dist
ARG CIPHER_SCRIPT
ENV PATH="/root/.tfenv/bin:/usr/local/bin/kotlinc/bin:${PATH}"
# COPY yq binary
COPY --from=yq /usr/bin/yq /usr/bin/yq
# COPY tfenv files
COPY --from=tfenv /root/.tfenv /root/.tfenv
COPY --from=kotlin /root/kotlinc /usr/local/bin/kotlinc
# Copy packer files
COPY --from=packer /bin/packer /usr/local/bin/packer

# Run the cipher script to build and cache its dependencies for much faster subsequent runs
RUN echo "${CIPHER_SCRIPT}" > ./cipher.main.kts && \
    (kotlin ./cipher.main.kts help || true) && \
    rm -f ./cipher.main.kts
