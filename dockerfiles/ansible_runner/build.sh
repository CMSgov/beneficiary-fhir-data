#!/bin/bash

##
# Runs the Docker build for the sibling Dockerfile, temporarily copying the
# needed files to this directory and cleaning them up afterwards.
#
# Then saves the Docker image to an `ansible_runner.tgz` file in the
# directory. That image can be copied to Jenkins and loaded as follows:
#
#     your_dev_sys$ scp dockerfiles/ansible_runner/ansible_runner.tgz bluebutton-healthapt-lss-builds:
#     your_dev_sys$ ssh bluebutton-healthapt-lss-builds
#     jenkins_sys$ zcat ansible_runner.tgz | sudo docker load
##

set -e

# Calculate the directory that this script is in.
scriptDirectory="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "Script Directory: '${scriptDirectory}'"

# Copy the needed build source files.
cp "${scriptDirectory}/../../requirements.txt" "${scriptDirectory}/"
cp "${scriptDirectory}/../../install_roles.yml" "${scriptDirectory}/"

# Run the Docker build.
sudo docker build --tag ansible_runner "${scriptDirectory}/"

# Cleanup the temporary source files.
rm "${scriptDirectory}/requirements.txt"
rm "${scriptDirectory}/install_roles.yml"

# Export the Docker image that was built.
echo "Saving Docker image to ansible_runner.tgz..."
sudo docker save ansible_runner | gzip > "${scriptDirectory}/ansible_runner.tgz"
echo "Docker image ansible_runner.tgz saved."