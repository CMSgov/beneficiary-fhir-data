#!/usr/bin/env sh

export python3_version=3.11.9

# Download python3 tar
sudo curl -o python3.tgz "https://www.python.org/ftp/python/${python3_version}/Python-${python3_version}.tgz"

# Extract python3.tgz
sudo tar -xzvf python3.tgz

# NOTE: python is primarily used for infrastructure and operational support to BFD.
#       Optimizations via `--enable-optimizations` in the `configure` step below are
#       not enabled to avoid lengthy build times.
#       https://docs.python.org/3/using/configure.html#performance-options
# Configure, make, makelatinstall python3
cd "./Python-${python3_version}" || { echo "Failure"; exit 1; }
sudo ./configure
sudo make
sudo make altinstall

# name: Create symbolic links
sudo ln -ns /usr/local/bin/python3.11 /usr/bin/python3
sudo ln -ns /usr/local/bin/pip3.11 /usr/bin/pip3

# Remove python3 installation files
sudo rm -rf "./Python-${python3_version}" ./python3.tgz

# Upgrade pip3
sudo /usr/bin/python3 -m pip install --upgrade pip
