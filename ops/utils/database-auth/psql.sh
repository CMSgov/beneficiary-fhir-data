#!/usr/bin/env bash
set -eo pipefail
PROGNAME=${0##*/}

BFD_ACCT_NUM="${BFD_ACCT_NUM:-}"
PG_NAME="${PG_NAME:-fhirdb}"
PG_HOST="${PG_HOST:-}"
PG_PORT="${PG_PORT:-5432}"
PG_USER="${PG_USER:-}"
PG_ENV="${PG_ENV:-test}"
PSQL_ARGS="${PSQL_ARGS:-}"

usage() {
  echo -e "Usage: $PROGNAME [-h|--help] [-h|--host PG_HOST] [-p|--port PG_PORT] [-u|--username PG_USER] [-x|--extra-args PSQL_ARGS] [PG_NAME]"
}

help_message() {
  cat <<- _EOF_
  $PROGNAME
  Run psql using IAM authentication.

  Requirements:
  - Active MFA session
  - AWS command line tools installed and configured

  $(usage)

  Options:
  -h, --help  Display this help message and exit.
  -H, --host PG_CLUSTER_ENDPOINT
    The endpoint of the cluster. This is the 'host' you would connect to with psql.
  -p, --port PG_PORT
    Where 'PG_PORT' is the port of the PG_NAME you want to connect to.
  -u, --username PG_USER
    Where 'PG_USER' is your EUA.
  -x, --extra-args PSQL_ARGS
    Where PSQL_ARGS are any additional options you want to pass to psql.

_EOF_
  return
}

while [[ -n $1 ]]; do
  case $1 in
    -h | --help)
      help_message; exit ;;
    -a | --account) shift; export BFD_ACCT_NUM="$1" ;;
    -e | --env) shift; export PG_ENV="$1" ;;
    -H | --host) shift; export PG_HOST="$1" ;;
    -p | --port) shift; export PG_PORT="$1" ;;
    -u | --username) shift; export PG_USER="$1" ;;
    -x | --extra-args) shift; export PSQL_ARGS="$1" ;;
    *) export PG_NAME="$1"
  esac
  shift
done

get_username() {
  local username
  if username="$(aws sts get-caller-identity --query 'Arn' --output text | cut -d/ -f2)"; then
    echo "$username"
  fi
}

# check tools
if ! command -v aws >/dev/null 2>&1; then
  echo "AWS command line tools are not installed. Please install them and ensure you have an active MFA session before running this script."
  exit 1
fi

if ! command -v psql >/dev/null 2>&1; then
  echo "psql is not installed. Please install it before running this script."
  exit 1
fi

# check args
if [[ -z $PG_HOST ]]; then
  usage
  echo "Missing required argument: PG_HOST"
  exit 1
fi

if [[ -z $PG_USER ]]; then
  PG_USER="$(get_username)"
  if [[ -z $PG_USER ]]; then
    # try prompting for username
    read -rp "Enter your EUA: " PG_USER
    if [[ -z $PG_USER ]]; then
      usage
      echo "Please provide a username."
      exit 1
    fi
  fi
fi

if [[ -z $PG_NAME ]]; then
  usage
  echo "Missing required argument: PG_NAME"
  exit 1
fi

if [[ -z $BFD_ACCT_NUM ]]; then
  usage
  echo "Missing required argument: BFD_ACCT_NUM"
  exit 1
fi

if [[ -z $PG_ENV ]]; then
  usage
  echo "Missing required argument: PG_ENV"
  exit 1
fi

# Assume the role
ROLE_ARN="arn:aws:iam::$BFD_ACCT_NUM:role/bfd-${PG_NAME}-${PG_ENV}-auth"
CREDS=$(aws sts assume-role --role-arn "$ROLE_ARN" --role-session-name "$PG_USER" --query "Credentials" --output json)

# Extract creds
export AWS_REGION="${AWS_REGION:-us-east-1}"
export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r ".AccessKeyId")
export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r ".SecretAccessKey")
export AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r ".SessionToken")

# fetch token
if ! token="$(aws rds generate-db-auth-token --hostname "$PG_HOST" --port "$PG_PORT" --username "$PG_USER")"; then
  echo "Failed to fetch token. Are you sure you have an active MFA session? Is your username correct?"
  exit 1
fi

# run psql
export PGPASSWORD="$token"

psql "host=$PG_HOST dbname=$PG_NAME user=$PG_USER port=$PG_PORT sslmode=verify-full sslrootcert=./rds-combined-ca-bundle.pem" "$PSQL_ARGS"
