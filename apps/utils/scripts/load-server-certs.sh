#!/usr/bin/env bash
set -Eeuo pipefail

tmpdir="${TMPDIR:-'/tmp'}"
combined_filename="$tmpdir/${BFD_ENV}_combined.pem"
save_pem() {
	filename="$tmpdir/${BFD_ENV}_$1.pem"
	if [ -f "$filename" ] && [ -f "$combined_filename" ]; then
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

rm "$combined_filename"
save_pem "cert"
save_pem "key"
chmod 600 "$combined_filename"
