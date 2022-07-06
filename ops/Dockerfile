# declaring flyway as a COPY source in al2 image
FROM flyway/flyway as flyway
RUN echo flyway.placeholders.type.int4=int4 > /flyway/conf/flyway.conf \
    && echo flyway.placeholders.type.text=text >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.tablespaces-escape=-- >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.drop-tablespaces-escape= >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.alter-column-type=type >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.hsql-only-alter=-- alter >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.psql-only-alter=alter >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.alter-rename-column=rename column >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.alter-rename-constraint=rename constraint >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.rename-to=to >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.index-create-concurrently=concurrently >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.sequence-start=start >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.sequence-increment=increment >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.perms= >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.psql-only= >> /flyway/conf/flyway.conf \
    && echo flyway.placeholders.logic.hsql-only=-- >> /flyway/conf/flyway.conf \
    && echo flyway.table=schema_version >> /flyway/conf/flyway.conf \
    && echo flyway.schemas=public >> /flyway/conf/flyway.conf \
    && echo flyway.url=jdbc:postgresql://db:5432/fhirdb >> /flyway/conf/flyway.conf \
    && echo flyway.password=bfd >> /flyway/conf/flyway.conf \
    && echo flyway.user=bfd >> /flyway/conf/flyway.conf

FROM amazonlinux as al2
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
RUN amazon-linux-extras enable java-openjdk11 && \
    yum clean metadata && \
    yum install -y sudo unzip selinux-policy systemd java-11-openjdk-devel python3 && \
    find /etc/systemd/system \
         /lib/systemd/system \
         -path '*.wants/*' \
         -not -name '*journald*' \
         -not -name '*systemd-tmpfiles*' \
         -not -name '*systemd-user-sessions*' \
         -exec rm \{} \;
RUN systemctl set-default multi-user.target
STOPSIGNAL SIGRTMIN+3

VOLUME [ "/sys/fs/cgroup" ]
CMD ["/usr/sbin/init"]

FROM al2 as dist
# inject prebuilt flyway and configuration from flyway/flyway
COPY --from=flyway /flyway /flyway
ENV PATH="/flyway:${PATH}"
