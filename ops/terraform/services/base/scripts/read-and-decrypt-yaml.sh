#!/usr/bin/env bash
# This is intended for use in local development as well as by automated
# terraform work flows that incorporate an external data source:
# https://registry.terraform.io/providers/hashicorp/external/latest/docs/data-sources/data_source
#
# Arguments:
#   $1 - the environment corresponding to the .yaml file to decrypt
#   $2 (Optional) - the ARN of the CMK of the corresponding key used to encrypt the target .yaml
#                   file, if unspecified the CMK is looked up based upon the environment

set -Eeo pipefail

SCRIPT_DIR="$(readlink -f "$(dirname "${BASH_SOURCE[0]}")")"
readonly SCRIPT_DIR

MODULE_DIR="$(dirname "$SCRIPT_DIR")"
readonly MODULE_DIR

REPO_ROOT="$(cd "$SCRIPT_DIR" && git rev-parse --show-toplevel)"
readonly REPO_ROOT

BFD_SEED_ENV="$1"
readonly BFD_SEED_ENV

YAML_FILE="${MODULE_DIR}/values/${BFD_SEED_ENV}.yaml"
readonly YAML_FILE

CMK_ARN_OVERRIDE="$2"
readonly CMK_ARN_OVERRIDE

CMK_LOOKUP="$(
  # Only attempt to lookup the CMK if the override is undefined
  if [[ -z "$CMK_ARN_OVERRIDE" ]]; then
    aws kms describe-key \
      --key-id "alias/bfd-${BFD_SEED_ENV}-config-cmk" \
      --query KeyMetadata.Arn \
      --output text
  fi
)"
readonly CMK_LOOKUP

CMK_ARN="${CMK_ARN_OVERRIDE:-"$CMK_LOOKUP"}"
readonly CMK_ARN

if test -f "${YAML_FILE}"; then
  # Returns an object like
  # {
  #   "/bfd/...": {
  #     "sensitive": <"sensitive"/"nonsensitive">
  #   },
  #   ...
  # }
  # if the configuration parameter's value includes a <<CIPHER>> token, indicating that the value is
  # sensitive and should be encrypted. Note that some keys may be sensitive themselves, and so will
  # be encrypted, so the JSON string returned will need to be decrypted by cipher to restore the
  # actual keys
  encrypted_params_sensitivity="$(
    yq eval -o=j <"$YAML_FILE" |
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
  # This will decrypt any encrypted keys allowing this script to merge the sensitivity of all keys
  # with their decrypted values, even if the key is encrypted
  decrypted_params_sensitivity="$(
    kotlin "$REPO_ROOT/apps/utils/cipher/cipher.main.kts" \
      --key "$CMK_ARN" cat <(echo "$encrypted_params_sensitivity")
  )"
  sensitivity_with_value="$(
    kotlin "$REPO_ROOT/apps/utils/cipher/cipher.main.kts" \
      --key "$CMK_ARN" cat "${YAML_FILE}" |
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
      jq -s --argjson sensitivity "$decrypted_params_sensitivity" 'add * $sensitivity'
  )"

  # Address (remove) the following as part of BFD-3296
  # FUTURE: This is awful, but done so that compatability can be had with existing Terraform state
  # and so that these changes are not as significant for services that consume configuration from
  # SSM. Clean this up in the future when parameters are standardized.
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
  untemplated_json="$(jq 'with_entries(
    .value as $value 
    | .key |= (
      split("/") 
      | (
        if .[3] == "nonsensitive" then
          (["/" + .[0], .[1], .[2], $value.sensitivity] + .[4:length]) | join("/")
        elif .[2] == "pipeline" and (.[3] == "shared" or .[3] == "rda" or .[3] == "ccw") then
          (["/" + .[0], .[1], .[2], .[3], $value.sensitivity] + .[4:length]) | join("/")
        else
          (["/" + .[0], .[1], .[2], $value.sensitivity] + .[3:length]) | join("/")
        end
      )
    ) 
    | .value |= .value
  )' <<<"$sensitivity_with_value")"

  # Using eval we can exploit heredoc to template any values (like ${env}) within the final JSON,
  # provided that the templated variable is defined within the environment or within this script.
  # This enables ephemeral configuration support.
  eval "cat <<EOF
$untemplated_json
EOF" 2>/dev/null
else
  echo '{}'
fi
