#!/bin/bash

##
# Creates/refreshes a ~/.aws/credentials profile with an active MFA token.
# Largely derived from: <http://prasaddomala.com/configure-multi-factor-authentication-mfa-with-aws-cli/>.
#
# Usage:
#
#     $ aws-mfa-refresh.sh --source-profile <source_profile_name> --mfa-serial <mfa_arn_id>
#
# Arguments:
#
# * `--source-profile <source_profile_name>`: The name of a valid profile in
#   `~/.aws/credentials`, for an access key that has MFA enabled. This is the
#    user that an MFA/STS token will be activated for.
# * `--mfa-serial <mfa_arn_id>`: An AWS MFA serial number, of the form
#   `arn:aws:iam::123456789123:mfa/iamusername`.
##

set -e

# Use GNU getopt to parse the options passed to this script.
TEMP=`getopt \
  -o p:m:r:f: \
  --long source-profile:,mfa-serial-number:,default-region:,default-output-format: \
  -n 'test.sh' -- "$@"`
if [ $? != 0 ] ; then echo "Terminating." >&2 ; exit 1 ; fi
eval set -- "$TEMP"

# Parse the getopt results.
sourceProfile=
mfaId=
defaultRegion=us-east-1
defaultOutputFormat=json
while true; do
  case "$1" in
    -p | --source-profile )
      sourceProfile="$2"; shift 2 ;;
    -m | --mfa-serial-number )
      mfaId="$2"; shift 2 ;;
    -r | --default-region )
      defaultRegion="$2"; shift 2 ;;
    -f | --default-output-format )
      defaultOutputFormat="$2"; shift 2 ;;
    -- ) shift; break ;;
    * ) break ;;
  esac
done

# Verify that all required options were specified.
if [[ -z "${sourceProfile}" ]]; then >&2 echo 'The --source-profile option is required.'; exit 1; fi
if [[ -z "${mfaId}" ]]; then >&2 echo 'The --mfa-serial-number option is required.'; exit 1; fi
if [[ -z "${defaultRegion}" ]]; then >&2 echo 'The --default-region option is required.'; exit 1; fi
if [[ -z "${defaultOutputFormat}" ]]; then >&2 echo 'The --default-output-format option is required.'; exit 1; fi

# The name of the MFA-enabled profile that will be generated/updated.
mfaProfile="${sourceProfile}_mfa"

# Ensure that the AWS_PROFILE envvar doesn't goof up the AWS CLI calls below.
unset AWS_PROFILE

# Will we generate a new token?
generateSecurityToken=true
 
# Expiration Time: Each SessionToken will have an expiration time which by default is 12 hours and
# can range between 15 minutes and 36 hours
mfaProfileExists=$(grep "${mfaProfile}" ~/.aws/credentials | wc -l)
if [ "${mfaProfileExists}" -eq "1" ]; then
  expirationDateTime=$(aws configure get expiration --profile "${mfaProfile}")
  nowDateTime=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  if [[ "${expirationDateTime}" > "${nowDateTime}" ]]; then
    echo "The Session Token is still valid. New Security Token not required until: '${expirationDateTime}'."
    generateSecurityToken=false
  fi
fi
 
if [ $generateSecurityToken = true ]; then
  read -p "Token code for MFA Device '${mfaId}': " mfaTokenCode
  echo "Generating new IAM STS Token..."
  read -r accessKey sessionToken expiration accessKeyId < <(aws sts get-session-token --profile "${sourceProfile}" --output text --query 'Credentials.*' --serial-number "${mfaId}" --token-code "${mfaTokenCode}" --duration-seconds 129600)
  if [ $? -ne 0 ];then
    echo "An error occured. AWS credentials file not updated."
    exit 2
  else
    aws configure set aws_secret_access_key "${accessKey}" --profile "${mfaProfile}"
    aws configure set aws_session_token "${sessionToken}" --profile "${mfaProfile}"
    aws configure set aws_access_key_id "${accessKeyId}" --profile "${mfaProfile}"
    aws configure set expiration "${expiration}" --profile "${mfaProfile}"
    aws configure set region "${defaultRegion}" --profile "${mfaProfile}"
    aws configure set output "${defaultOutputFormat}" --profile "${mfaProfile}"
    echo "STS Session Token generated and updated in AWS credentials file successfully."
  fi
fi
