#!/bin/bash

json_array=()

# Loop through each directory in the current directory
for dir in */; do
  # Remove trailing slash from directory name
  dirname=${dir%/}

  # Check if the directory name contains a dash
  if [[ "$dirname" == *-* ]]; then
    # Split the directory name into layer and service
    layer="${dirname%%-*}"
    service="${dirname#*-}"
    # Get the path of the service directory relative to repo root
    rel_path="$(git rev-parse --show-prefix)$dirname"

    # Check if there is a file or link named tofu.tf (and not a directory)
    if [[ -L "$dir/tofu.tf" || -f "$dir/tofu.tf" ]] && [[ ! -d "$dir/tofu.tf" ]]; then
      json_array+=("$(
        jq -nc \
          --arg l "$layer" \
          --arg s "$service" \
          --arg p "$rel_path" \
          '{layer: $l, service: $s, path: $p}'
      )")
    fi
  fi
done

# Output each object grouped by its layer
jq -s 'group_by(.layer)[] | {(.[0].layer): (.)}' <<<"${json_array[@]}" | jq -s 'add'
