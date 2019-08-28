#!/bin/bash

# Stop immediately if any command returns a non-zero result.
set -e

# Determine the directory that this script is in.
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Run everything from that directory.
cd "${SCRIPT_DIR}"

# Get the SSH public key to use from the args passed in.
sshPublicKey="$1"
if [[ ! -f "${sshPublicKey}" ]]; then
  echo "SSH key not found: '${sshPublicKey}'." 1>&2
  exit 1
fi

# Create and activate the Python virtualenv needed by Ansible.
if [[ ! -d venv/ ]]; then
  virtualenv -p /usr/bin/python2.7 venv
fi
source venv/bin/activate

# Install Ansible into the venv.
pip install "${ANSIBLE_SPEC}"

# Install any requirements needed by the role or its tests.
if [[ -f ../requirements.txt ]]; then pip install --requirement ../requirements.txt; fi
if [[ -f requirements.txt ]]; then pip install --requirement requirements.txt; fi

# Prep the Ansible roles that the test will use.
if [[ ! -d roles ]]; then mkdir roles; fi
if [[ ! -x "roles/${ROLE}" ]]; then ln -s "$(cd .. && pwd)" "roles/${ROLE}"; fi
if [[ -f ../install_roles.yml ]]; then ansible-galaxy --role-file=../install_roles.yml --roles-path=./roles; fi
if [[ -f install_roles.yml ]]; then ansible-galaxy install --role-file=install_roles.yml --roles-path=./roles; fi

# Prep the Docker container that will be used (if it's not already running).
if [[ $(sudo docker ps -f "name=${CONTAINER_PREFIX}.${PLATFORM}" --format '{{.Names}}') != "${CONTAINER_PREFIX}.${PLATFORM}" ]]; then
  sudo docker build \
    --tag ${CONTAINER_PREFIX}/${PLATFORM} \
    docker_platforms/${PLATFORM}
  sudo docker run \
    --cap-add=SYS_ADMIN \
    --detach \
    --publish 127.0.0.1:13022:22 \
    --volume=/sys/fs/cgroup:/sys/fs/cgroup:ro \
    --tmpfs /run \
    --tmpfs /run/lock \
    --name ${CONTAINER_PREFIX}.${PLATFORM} \
    ${CONTAINER_PREFIX}/${PLATFORM}
  cat "${sshPublicKey}" | sudo docker exec \
    --interactive ${CONTAINER_PREFIX}.${PLATFORM} \
    /bin/bash -c "mkdir /home/ansible_test/.ssh && cat >> /home/ansible_test/.ssh/authorized_keys"
fi
