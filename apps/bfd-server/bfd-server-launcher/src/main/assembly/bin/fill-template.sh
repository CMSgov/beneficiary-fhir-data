#!/usr/bin/env sh
TMPL=${1}
TGT=${2}
EXTRAS=${3:-}
# shellcheck disable=SC3046,SC1090
if [ "${EXTRAS}" != "" ]
then
    source ${EXTRAS}
fi
envsubst < ${TMPL} > ${TGT}