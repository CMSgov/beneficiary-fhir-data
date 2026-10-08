#!/usr/bin/env bash
set -Eeuo pipefail

tmpdir="${TMPDIR:-'/tmp'}"
combined_filename="$tmpdir/${BFD_ENV}_default_combined.pem"
combined_samhsa_filename="$tmpdir/${BFD_ENV}_samhsa_combined.pem"
save_pem() {
	filename="$tmpdir/${BFD_ENV}_$1.pem"
	cur_combined="$tmpdir/${BFD_ENV}_${2}_combined.pem"
	if [ -f "$filename" ]; then
		cat "$filename" >>"$cur_combined"
		return
	fi
	text="$(aws ssm get-parameter --name "/bfd/${BFD_ENV}/server/sensitive/test_client_$1" --with-decryption --query Parameter.Value --output text)"
	tmpdir="${TMPDIR:-'/tmp'}"
	filename="$tmpdir/${BFD_ENV}_$1.pem"
	echo "${text}" >"$filename"
	chmod 600 "$filename"
	echo "${text}" >>"$cur_combined"
}

rm -f "$combined_filename"
rm -f "$combined_samhsa_filename"
save_pem "cert" "default"
save_pem "key" "default"
save_pem "samhsa_cert" "samhsa"
save_pem "samhsa_key" "samhsa"
chmod 600 "$combined_filename"
chmod 600 "$combined_samhsa_filename"

pub_cert_path="$tmpdir/${BFD_ENV}_pub_cert.pem"

if [ ! -f "$pub_cert_path" ]; then
	pub_cert=$(curl -k -w "%{certs}" "https://$BFD_ENV.fhirv3.bfd.cmscloud.local")
	echo "${pub_cert}" >"$pub_cert_path"
	chmod 600 "$pub_cert_path"
fi

export BFD_CLIENT_CERT="$tmpdir/${BFD_ENV}_cert.pem"
export BFD_CLIENT_KEY="$tmpdir/${BFD_ENV}_key.pem"
export BFD_COMBINED_CERT="$tmpdir/${BFD_ENV}_default_combined.pem"

export BFD_SAMHSA_CLIENT_CERT="$tmpdir/${BFD_ENV}_samhsa_cert.pem"
export BFD_SAMHSA_CLIENT_KEY="$tmpdir/${BFD_ENV}_samhsa_key.pem"
export BFD_SAMHSA_COMBINED_CERT="$tmpdir/${BFD_ENV}_samhsa_combined.pem"

export BFD_PUBLIC_CERT="$tmpdir/${BFD_ENV}_pub_cert.pem"
