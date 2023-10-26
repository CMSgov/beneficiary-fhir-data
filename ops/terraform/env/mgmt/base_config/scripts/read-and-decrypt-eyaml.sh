#!/usr/bin/env bash
# This is intended for use in local development as well as by automated
# terraform work flows that incorporate an external data source:
# https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/data_source
#
# Globals:
#   EYAML_FILE formatted "$1" positional argument for relative file resolution
#   SSM_VAULT_PASSWORD_PATH SSM Hierarchy containing the ansible vault password
#
# Arguments:
#   $1 is the desired eyaml file that can be entered with or without qualifying path or
#      suffix. `values/foo.eyaml`, `values/foo`, `foo.eyaml` and `foo` are equivalent.

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

MODULE_DIR="$(dirname "$SCRIPT_DIR")"
readonly MODULE_DIR

REPO_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel)"
readonly REPO_ROOT

EYAML_FILE="$MODULE_DIR/values/$1.yaml"
readonly EYAML_FILE

CMK_ARN="$2"
readonly CMK_ARN

if test -f "${EYAML_FILE}"; then
  # Returns an object like
  # {
  #   "/bfd/...": {
  #     "sensitive": <"sensitive"/"nonsensitive">
  #   },
  #   ...
  # }
  # if the configuration parameter's value includes a <<CIPHER>> token, indicating that the value is
  # sensitive and should be encrypted
  params_sensitivity="$(
    yq eval -o=j <"$EYAML_FILE" |
      jq 'tostream 
        | select(length==2) 
        | .[0] |= (
          map(strings) 
          | join("/")
        ) 
        | {
          (.[0]): {
            "sensitivity": (
              if (.[1] | tostring | contains("<<CIPHER>>")) == true then 
                "sensitive" 
              else 
                "nonsensitive" 
              end
            )
          }
        }' |
      jq -s 'add'
  )"
  sensitivity_with_value="$(
    kotlin "$REPO_ROOT/apps/utils/cipher/cipher.main.kts" \
      --key "$CMK_ARN" cat "${EYAML_FILE}" |
      yq eval -o=j |
      # Merges the resulting decrypted YAML configuration (now JSON) into an object like
      # {
      #   "/bfd/...": {
      #     "value": "...",
      #     "sensitivity": <"sensitive"/"nonsensitive">
      #   },
      #   ...
      # }
      jq 'tostream 
        | select(length==2) 
        | .[0] |= (
          map(strings) 
          | join("/")
        ) 
        | {
          (.[0]): {
            "value": (.[1] | tostring)
          }
        }' |
      jq -s --argjson sensitivity "$params_sensitivity" 'add * $sensitivity'
  )"

  # FUTURE: This is awful, but done so that compatability can be had with existing Terraform state
  # and so that these changes are not as significant for services that consume configuration from
  # SSM.
  # Given an object like:
  # {
  #   "/bfd/...": {
  #     "value": "...",
  #     "sensitivity": <"sensitive"/"nonsensitive">
  #   },
  #   ...
  # }
  # Returns an object like:
  # {
  #   "/bfd/.../.../<sensitive OR nonsensitive>/...": "...",
  #   "/bfd/...": "...",
  #   ...
  # }
  jq 'with_entries(
    .value as $value 
    | .key |= (
      split("/") 
      | (
        if .[2] == "pipeline" and (.[3] == "shared" or .[3] == "rda" or .[3] == "ccw") then
          (["/" + .[0], .[1], .[2], .[3], $value.sensitivity] + .[4:length]) | join("/")
        else
          (["/" + .[0], .[1], .[2], $value.sensitivity] + .[3:length]) | join("/")
        end
      )
    ) 
    | .value |= .value
  )' <<<"$sensitivity_with_value"
else
  echo '{}'
fi
