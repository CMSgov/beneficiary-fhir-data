# declaring flyway as a COPY source in al2 image
FROM flyway/flyway:9 as flyway
RUN echo flyway.placeholders.type.int4=int4 > /flyway/conf/flyway.conf \
    && echo flyway.placeholders.type.text=text >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.tablespaces-escape=-- >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.drop-tablespaces-escape= >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.alter-column-type=type >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.psql-only-alter=alter >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.alter-rename-column=rename column >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.alter-rename-constraint=rename constraint >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.rename-to=to >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.index-create-concurrently=concurrently >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.sequence-start=start >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.sequence-increment=increment >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.perms= >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.psql-only= >> /flyway/conf/flyway.conf \
    && echo flyway.table=schema_version >> /flyway/conf/flyway.conf \
    && echo flyway.schemas=public >> /flyway/conf/flyway.conf \
    && echo flyway.url=jdbc:postgresql://db:5432/fhirdb >> /flyway/conf/flyway.conf \
    && echo flyway.password=bfd >> /flyway/conf/flyway.conf \
    && echo flyway.user=bfd >> /flyway/conf/flyway.conf \
    && echo flyway.baselineOnMigrate=true >> /flyway/conf/flyway.conf \
    && echo flyway.baselineVersion=0 >> /flyway/conf/flyway.conf \
    && echo flyway.postgresql.transactional.lock=false >> /flyway/conf/flyway.conf

FROM amazonlinux:2 as al2
ARG PYTHON3_TAR_SOURCE="https://www.python.org/ftp/python/3.11.9/Python-3.11.9.tgz"
LABEL org.opencontainers.image.source=https://github.com/CMSgov/beneficiary-fhir-data

# Getting systemd to run correctly inside Docker is very tricky. Need to
# ensure that it doesn't start things it shouldn't, without stripping out so
# much as to make it useless.
#
# References:
#
# * <https://hub.docker.com/_/centos/>: Good start, but badly broken.
# * <https://github.com/solita/docker-systemd>: For Ubuntu, but works!
# * <https://github.com/moby/moby/issues/28614>: Also some useful info.
#
# NOTE: Packages specified below should generally be kept up-to-date
#       with the dependencies encoded in the BFD platinum image
#       instructions.

RUN rpm --import https://yum.corretto.aws/corretto.key && \
    curl -L -o /etc/yum.repos.d/corretto.repo https://yum.corretto.aws/corretto.repo && \
    yum clean metadata && \
    yum update -y --security && \
    amazon-linux-extras install epel -y && \
    yum -y groupinstall "Development Tools" && \
    yum install -y \
    java-21-amazon-corretto-devel \
    amazon-cloudwatch-agent \
    ansible \
    gcc \
    git \
    libffi-devel \
    openssl-devel \
    selinux-policy \
    sudo \
    systemd \
    tar \
    unzip && \
    find /etc/systemd/system \
         /lib/systemd/system \
         -path '*.wants/*' \
         -not -name '*journald*' \
         -not -name '*systemd-tmpfiles*' \
         -not -name '*systemd-user-sessions*' \
         -exec rm \{} \;

SHELL ["/bin/bash", "-c"]
RUN curl -o python3.tgz $PYTHON3_TAR_SOURCE && \
    tar zxvf python3.tgz && \
    cd $(basename $PYTHON3_TAR_SOURCE .tgz) && \
    ./configure && make && make altinstall

RUN ln -fs /usr/local/bin/python3.11 /usr/bin/python3 && \
    ln -fs /usr/local/bin/pip3.11 /usr/bin/pip3

RUN /usr/bin/python3 -m pip install --upgrade pip

RUN rm -rf $(basename $PYTHON3_TAR_SOURCE .tgz) python3.tgz

RUN systemctl set-default multi-user.target
STOPSIGNAL SIGRTMIN+3

VOLUME [ "/sys/fs/cgroup" ]
CMD ["/usr/sbin/init"]

FROM al2 as dist
# inject prebuilt flyway and configuration from flyway/flyway
COPY --from=flyway /flyway /flyway
ENV PATH="/flyway:${PATH}"
