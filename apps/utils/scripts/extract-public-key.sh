#!/usr/bin/env bash
set -Eeuo pipefail

echo "" | openssl s_client -partial_chain -connect \
	"$BFD_ENV.fhirv3.bfd.cmscloud.local:443" 2>/dev/null | openssl x509 -pubkey -noout
