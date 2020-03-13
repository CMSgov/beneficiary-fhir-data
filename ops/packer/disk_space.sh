#!/bin/bash
set -e

exec > >(tee -a /var/log/user_data.log 2>&1)

# Extend gold image defined root partition with all available free space
sudo growpart /dev/nvme0n1 2
sudo pvresize /dev/nvme0n1p2
sudo lvextend -l +100%FREE /dev/VolGroup00/rootVol
sudo xfs_growfs /