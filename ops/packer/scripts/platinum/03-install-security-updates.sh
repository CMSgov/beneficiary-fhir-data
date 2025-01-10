#!/usr/bin/env sh

# Apply security patches
sudo yum update-minimal --security -y

# Aggressively reconfigure grub configuration
sudo grub2-mkconfig -o /boot/grub2/grub.cfg
