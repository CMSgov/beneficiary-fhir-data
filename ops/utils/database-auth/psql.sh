#!/usr/bin/env bash
set -eo pipefail
PROGNAME=${0##*/}

BFD_ACCT_NUM="${BFD_ACCT_NUM:-}"
DB_ENDPOINT="${DB_ENDPOINT:-}"
DB_PORT="${DB_PORT:-5432}"
DB_USERNAME="${DB_USERNAME:-}"
PSQL_ARGS="${PSQL_ARGS:-}"
DATABASE="${DATABASE:-fhirdb}"

usage() {
  echo -e "Usage: $PROGNAME [-h|--help] [-h|--host DB_ENDPOINT] [-p|--port DB_PORT] [-u|--username DB_USERNAME] [-x|--extra-args PSQL_ARGS] [DATABASE]"
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
  -H, --host DB_CLUSTER_ENDPOINT
    The endpoint of the database cluster. This is the endpoint you would use to connect to the database using psql.
    Note this is not the cluster identifier or an individual instance endpoint or ID.
  -p, --port DB_PORT
    Where 'DB_PORT' is the port of the database you want to connect to.
  -u, --username DB_USERNAME
    Where 'DB_USERNAME' is your EUA.
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
    -e | --env) shift; export DB_ENV="$1" ;;
    -H | --host) shift; export DB_ENDPOINT="$1" ;;
    -p | --port) shift; export DB_PORT="$1" ;;
    -u | --username) shift; export DB_USERNAME="$1" ;;
    -x | --extra-args) shift; export PSQL_ARGS="$1" ;;
    *) export DATABASE="$1"
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
if [[ -z $DB_ENDPOINT ]]; then
  usage
  echo "Missing required argument: DB_ENDPOINT"
  exit 1
fi

if [[ -z $DB_USERNAME ]]; then
  DB_USERNAME="$(get_username)"
  if [[ -z $DB_USERNAME ]]; then
    # try prompting for username
    read -rp "Enter your EUA: " DB_USERNAME
    if [[ -z $DB_USERNAME ]]; then
      usage
      echo "Please provide a username."
      exit 1
    fi
  fi
fi

if [[ -z $DATABASE ]]; then
  usage
  echo "Missing required argument: DATABASE"
  exit 1
fi

if [[ -z $BFD_ACCT_NUM ]]; then
  usage
  echo "Missing required argument: BFD_ACCT_NUM"
  exit 1
fi

if [[ -z $DB_ENV ]]; then
  usage
  echo "Missing required argument: DB_ENV"
  exit 1
fi

# Assume the role
ROLE_ARN="arn:aws:iam::$BFD_ACCT_NUM:role/bfd-${DATABASE}-${DB_ENV}-auth"
CREDS=$(aws sts assume-role --role-arn "$ROLE_ARN" --role-session-name "$DB_USERNAME" --query "Credentials" --output json)

# Extract creds
unset AWS_PROFILE
export AWS_REGION="${AWS_REGION:-us-east-1}"
export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | jq -r ".AccessKeyId")
export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | jq -r ".SecretAccessKey")
export AWS_SESSION_TOKEN=$(echo "$CREDS" | jq -r ".SessionToken")

# fetch token
if ! token="$(aws rds generate-db-auth-token --hostname "$DB_ENDPOINT" --port "$DB_PORT" --username "$DB_USERNAME")"; then
  echo "Failed to fetch token. Are you sure you have an active MFA session? Are you using the correct endpoint? Is your username correct?"
  exit 1
fi

# run psql
export PGPASSWORD="$token"
if [[ -n $PSQL_ARGS ]]; then
  exec psql -h "$DB_ENDPOINT" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DATABASE" "$PSQL_ARGS"
else
  exec psql -h "$DB_ENDPOINT" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DATABASE"
fi
