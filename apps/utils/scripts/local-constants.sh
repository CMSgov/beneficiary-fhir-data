#!/usr/bin/env bash
set -Eeuo pipefail

BFD_LOCAL_DB_USERNAME="bfd"
export BFD_LOCAL_DB_USERNAME
BFD_LOCAL_DB_PASSWORD="InsecureLocalDev"
export BFD_LOCAL_DB_PASSWORD
BFD_LOCAL_DB_CONTAINER="bfd-db"
export BFD_LOCAL_DB_CONTAINER
MATCHBOX_SERVER="http://localhost:18080/matchboxv3"
export MATCHBOX_SERVER
