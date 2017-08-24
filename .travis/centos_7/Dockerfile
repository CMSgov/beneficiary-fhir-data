FROM centos:7
ENV container docker

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
RUN yum update -y
RUN yum install -y openssh-server sudo
RUN /usr/bin/systemctl enable sshd
RUN /usr/bin/systemctl enable systemd-user-sessions.service

# Create the SSH user.
RUN useradd ansible_test -G wheel
RUN echo 'ansible_test:secret' | chpasswd
RUN echo '%wheel ALL=(ALL) ALL' >> /etc/sudoers

VOLUME [ "/sys/fs/cgroup" ]
EXPOSE 22
CMD ["/usr/sbin/init"]

