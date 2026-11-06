#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
	echo "Directory arguments are missing"
	exit 1
fi

SRC_DIR="$1"
REGEN_DIR="$2"

for dir in "$SRC_DIR" "$REGEN_DIR"; do
	if [ ! -d "$dir" ]; then
		echo "Directory '$dir' does not exist."
		exit 1
	fi
done

echo "Comparing CSV files in '$SRC_DIR' vs '$REGEN_DIR'"
echo "=================================================="

mismatch_count=0

for original_file in "$SRC_DIR"/*.csv; do
	file_name=$(basename "$original_file")
	regenerated_file="$REGEN_DIR/$file_name"

	if [ -f "$regenerated_file" ]; then
		original_hash=$(shasum -a 256 "$original_file" | cut -d' ' -f1)
		regenerated_hash=$(shasum -a 256 "$regenerated_file" | cut -d' ' -f1)

		if [ "$original_hash" = "$regenerated_hash" ]; then
			echo "MATCH: $file_name"
		else
			echo "MISMATCH: $file_name"
			mismatch_count=$((mismatch_count + 1))
		fi
	else
		echo "'$file_name' exists in $SRC_DIR but is missing in $REGEN_DIR"
	fi
done

echo "=================================================="
if [ "$mismatch_count" -gt 0 ]; then
	echo "$mismatch_count file(s) do not have the same checksum post regeneration. Verify if this is intended."
	exit 2
else
	echo "Files are identical post regeneration. Verify if this is intended."
fi
