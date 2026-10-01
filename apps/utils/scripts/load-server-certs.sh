#!/usr/bin/env bash
set -Eeuo pipefail

tmpdir="${TMPDIR:-'/tmp'}"
combined_filename="$tmpdir/${BFD_ENV}_combined.pem"
save_pem() {
	filename="$tmpdir/${BFD_ENV}_$1.pem"
	if [ -f "$filename" ]; then
		cat "$filename" >>"$combined_filename"
		return
	fi
	text="$(aws ssm get-parameter --name "/bfd/${BFD_ENV}/server/sensitive/test_client_$1" --with-decryption --query Parameter.Value --output text)"
	tmpdir="${TMPDIR:-'/tmp'}"
	filename="$tmpdir/${BFD_ENV}_$1.pem"
	echo "${text}" >"$filename"
	chmod 600 "$filename"
	echo "${text}" >>"$combined_filename"
}

rm -f "$combined_filename"
save_pem "cert"
save_pem "key"
chmod 600 "$combined_filename"

pub_cert_path="$tmpdir/${BFD_ENV}_pub_cert.pem"

if [ ! -f "$pub_cert_path" ]; then
	pub_cert=$(curl -k -w "%{certs}" "https://$BFD_ENV.fhirv3.bfd.cmscloud.local")
	echo "${pub_cert}" >"$pub_cert_path"
	chmod 600 "$pub_cert_path"
fi

export BFD_CLIENT_CERT="$tmpdir/${BFD_ENV}_cert.pem"
export BFD_CLIENT_KEY="$tmpdir/${BFD_ENV}_key.pem"
export BFD_COMBINED_CERT="$tmpdir/${BFD_ENV}_combined.pem"
export BFD_PUBLIC_CERT="$tmpdir/${BFD_ENV}_pub_cert.pem"
