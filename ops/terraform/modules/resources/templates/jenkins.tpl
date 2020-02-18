#!/bin/bash
set -e

exec > >(tee -a /var/log/user_data.log 2>&1)

sudo mount -t nfs -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2,noresvport fs-21c667a1.efs.us-east-1.amazonaws.com:/   /var/lib/jenkins

systemctl restart jenkins
