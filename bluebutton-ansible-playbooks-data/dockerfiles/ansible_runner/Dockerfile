FROM centos:7.4.1708
ENV container docker

USER root

# Update image and enable EPEL repo (which contains python-pip).
RUN yum update -y && \
	yum install -y \
		epel-release \
	;

# Install required system packages.
RUN yum install -y \
		python \
		python-pip \
		python-virtualenv \
		gcc \
		python-devel \
		openssl-devel \
		openssh-clients \
		git \
	;

# Clean up yum caches (removes over 100MB).
RUN yum clean all

# Copy Ansible setup files (temporarily).
COPY . /ansible_setup
WORKDIR /ansible_setup

# Install Ansible and other Python libs.
RUN pip install --requirement requirements.txt

# Install Ansible roles to system location.
RUN ansible-galaxy install --roles-path=/etc/ansible/roles --role-file=install_roles.yml

# Cleanup Ansible setup files.
RUN rm -rf /ansible_setup
WORKDIR /

# Declare the SSH config volume.
VOLUME /root/.ssh
