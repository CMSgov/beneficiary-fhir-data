ARG UPSTREAM_IMAGE="amazonlinux"
FROM $UPSTREAM_IMAGE
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
    yum install -y sudo unzip selinux-policy systemd && \
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
