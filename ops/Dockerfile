ARG UPSTREAM_IMAGE="centos:7"
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
RUN find /etc/systemd/system \
         /lib/systemd/system \
         -path '*.wants/*' \
         -not -name '*journald*' \
         -not -name '*systemd-tmpfiles*' \
         -not -name '*systemd-user-sessions*' \
         -exec rm \{} \;
RUN systemctl set-default multi-user.target
STOPSIGNAL SIGRTMIN+3

# Install and start SSH.
#
# References:
#
# * <https://stackoverflow.com/questions/18173889/cannot-access-centos-sshd-on-docker>
RUN yum update -y && yum install -y openssh-server sudo unzip selinux-policy && \
    /usr/bin/systemctl enable sshd && \
    /usr/bin/systemctl enable systemd-user-sessions.service

# Create the SSH user.
RUN useradd ansible_test -G wheel && \
    echo 'ansible_test:secret' | chpasswd && \
    echo '%wheel ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers

VOLUME [ "/sys/fs/cgroup" ]
EXPOSE 22
CMD ["/usr/sbin/init"]
