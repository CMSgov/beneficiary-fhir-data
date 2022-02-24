#!/usr/bin/env bash
# Check/validate TLS endpoints and certificate files and warn of upcoming expirations
PROGNAME=${0##*/}

# whether or not to send out notifications (defaults to false, add -s flag to enable)
SEND_NOTIFICATIONS=${SEND_NOTIFICATIONS:-"false"}

# schedule info
CRON_SCHEDULE="${SCHEDULE:-"0 12 * * *"}" # run at noon everyday

# notifications
WARN_WHEN="${WARN_WHEN:-"30:45"}"     # WARN when expiration is >= 30 but less than 45 days
ALERT_WHEN="${ALERT_WHEN:-"15:30"}"   # expiration is >= 15 but less than 30 days
PAGE_WHEN="${PAGE_WHEN:-"1:15"}"      # expiration is >= 0 but less than 15 days

# notifications must be set via env vars or command line flags
WARN_SLACK_WEBHOOK_URL="${WARN_SLACK_WEBHOOK_URL:-}"
WARN_SLACK_CHAN="${WARN_SLACK_CHAN:-}"
ALERT_SLACK_WEBHOOK_URL="${ALERT_SLACK_WEBHOOK_URL:-}"
ALERT_SLACK_CHAN="${ALERT_SLACK_CHAN:-}"
PAGE_WEBHOOK_URL="${PAGE_WEBHOOK_URL:-}"

# Default set of endpoints/cert files we will check. Set these via space seperated env vars.
# example: DEFAULT_ENDPOINTS="foo:443 bar:443" ./cert-checker -s
DEFAULT_ENDPOINTS="${DEFAULT_ENDPOINTS:-}"
DEFAULT_CERTFILES="${DEFAULT_CERTFILES:-}"

# internal arrays/vars (dynamically set, do not alter)
ENDPOINTS=() # array that will hold the endpoints we will check
CERTFILES=() # array that will hold the cert files we will check
WARN_START=
WARN_END=
ALERT_START=
ALERT_END=
PAGE_START=
PAGE_END=

# print error message and exit
error_exit() {
  echo -e "${PROGNAME}: ${1:-"Unknown Error"}" >&2
  exit 1
}

usage() {
  echo -e "Usage: $PROGNAME [-h|--help] [-e|--endpoint endpoint] [-c|--cert-file cert] [-s|--send-notifications] [-p|--print-cron]"
}

help_message() {
  cat <<- _EOF_
  $PROGNAME
  Check/validate TLS certificates and endpoints and warn of upcoming expirations

  $(usage)

  Flags:
  -h, --help  Display this help message and exit.
  -s, --send-notifications  Send notifications
  -p, --print-cron Print the recommended cron schedule (does not install, just prints the cron schedule)

  Options:
  -e, --endpoint endpoint
    Where 'endpoint' is the http endpoint:port to check. Ie 'prod-sbx.bfd.cms.gov:443'.
    Check additional endpoints with multiple '-e' options. Ie '-e foo:443 -e bar:443'
  -c, --cert cert [NOT IMPLEMENTED] certificate to check
    Where 'cert' is the path to a certificate. Ie /path/to/cert.pem
    Check additional cert files with multiple '-c' options. Ie '-c /etc/foo.pem -c ./bar.pem'
  --{warn|alert}-slack slack_channel
    Set slack channel for notifications. Example --warn-slack "#info" --alert-slack "#ALERTS"
  --warn-slack-webhook-url
    Slack webhook url for warn messages (see DevOps for this info)
  --alert-slack-webhook-url
    Slack webhook url for alert messages (see DevOps for this info)
  --page-webhook-url
    Full SOC/VO REST endpoint (with routing key) to trigger pages (see DevOps for this info)
_EOF_
  return
}


#-------------------- MAIN LOGIC --------------------#

parse_day_ranges() {
  IFS=: read -r WARN_START WARN_END <<< "$WARN_WHEN"
  export WARN_START
  export WARN_END

  IFS=: read -r ALERT_START ALERT_END <<< "$ALERT_WHEN"
  export ALERT_START
  export ALERT_END

  IFS=: read -r PAGE_START PAGE_END <<< "$PAGE_WHEN"
  export PAGE_START
  export PAGE_END
}

# print cron schedule(s) to stdout
print_cron() {
  echo "# Run cert-checker every day at noon (with notifications). No logging."
  echo "$CRON_SCHEDULE /usr/local/bin/cert-checker -s >/dev/null 2>&1"
}

# Send warning slack
send_warning_slack() {
  local msg payload
  msg="$*"
  payload="payload={\"channel\": \"$WARN_SLACK_CHAN\", \"text\": \"$msg\"}"
  if [[ "$SEND_NOTIFICATIONS" == "true" ]]; then
    curl -X POST --data-urlencode "$payload" "$WARN_SLACK_WEBHOOK_URL"
  fi
}

# Send alert slack
send_alert_slack_chan() {
  local msg payload
  msg="$*"
  payload="payload={\"channel\": \"$ALERT_SLACK_CHAN\", \"text\": \"$msg\"}"
  if [[ "$SEND_NOTIFICATIONS" == "true" ]]; then
    curl -X POST --data-urlencode "$payload" "$ALERT_SLACK_WEBHOOK_URL"
  fi
}

# Trigger a page
# https://help.victorops.com/knowledge-base/rest-endpoint-integration-guide/#recommended-rest-endpoint-integration-fields
trigger_page() {
  local msg payload
  id="$1"; shift # entity_id (think of it as a primary key)
  name="$*" # entity_display_name
  msg="${name}" # state_message (I cannot seem to find this in actual pages, so just displaying name)
  payload="{\"message_type\":\"critical\",\"entity_id\":\"${id}\",\"entity_display_name\":\"${name}\",\"state_message\":\"${msg}\"}"
  if [[ "$SEND_NOTIFICATIONS" == "true" ]]; then
    curl -X POST -d "$payload" "$PAGE_WEBHOOK_URL"
  fi
}

# check a single endpoint's certificate
check_endpoint(){
  local endpoint port expires_in_days
  local expire_date expires_on today_sec expires_sec expires_in_days

  IFS=: read -r endpoint port <<< "$1"
  [[ -z "$port" ]] && port="443"
  printf " %s:%s " "$endpoint" "$port"

  expire_date=$(
    openssl s_client -connect "$endpoint:$port" 2>/dev/null \
    | openssl x509 -noout -enddate \
    | grep -e ^notAfter \
    | sed 's/^notAfter=//'
  )

  # posix 'date' (default macos) != gnu 'date' (linux)
  # handle both
  today_sec="$(date +%s)"
  if date --iso-8601 >/dev/null 2>&1; then
    # gnu date
    expires_on="$(date --date="$expire_date" --iso-8601)"
    expires_sec=$(date --date="$expire_date" +%s)
  else
    # posix date
    expires_on=$(TZ=GMT /bin/date -jf "%b %d %T %Y %Z" "$expire_date" +%Y-%m-%d)
    expires_sec=$(TZ=GMT /bin/date -jf "%b %d %T %Y %Z" "$expire_date" +%s)
  fi
  expires_in_days=$(( ( expires_sec - today_sec ) / (60*60*24) ))

  echo "expires on $expires_on (${expires_in_days} days)"

  # send warnings
  if (( expires_in_days >= WARN_START && expires_in_days < WARN_END)); then
    send_warning_slack "Heads up! ${endpoint} TLS cert expires in ${expires_in_days} days"
  fi

  # send alerts
  if (( expires_in_days >= ALERT_START && expires_in_days < ALERT_END)); then
    send_alert_slack_chan "WARNING!! ${endpoint} TLS cert expires in less than ${expires_in_days} days"
  fi

  # trigger a page (our pager service will automatically send a slack alert)
  if (( expires_in_days <= PAGE_END)); then
    trigger_page "bfd/tls/expiring_cert/${endpoint}" "${endpoint} cert expires in less than ${expires_in_days} days"
  fi
}

# TODO: check a single certificate
# example: check_cert /path/to/cert
check_cert() {
  local cert="$1"
  echo "Checking $cert"

  # fail early if we cannot read the cert file
  [[ ! -f "$1" || ! -r "$1" ]] && error_exit "cannot access $cert"
  
  # TODO
  
}

# run check_endpoint on each item in the ENDPOINTS array
check_endpoints(){
  if [[ -n $DEFAULT_ENDPOINTS ]]; then
    read -r ENDPOINTS <<< "$DEFAULT_ENDPOINTS"
  fi
  if [[ ${#ENDPOINTS[@]} -gt 0 ]]; then
    echo "Checking endpoint(s):"
    for endpoint in "${ENDPOINTS[@]}"; do
      check_endpoint "$endpoint"
    done
  fi
}

# run check_cert for each item in CERTFILES array
check_certfiles(){
  for certfile in "${CERTFILES[@]}"; do
    check_cert "$certfile"
  done
}

# Parse command-line flags and options

while [[ -n $1 ]]; do
  case $1 in
    -h | --help)
      help_message; exit ;;
    -e | --endpoint)
      # append each -e endpoint to the ENDPOINTS array (ignore defaults)
      shift; unset DEFAULT_ENDPOINTS; ENDPOINTS+=("$1") ;;
    -c | --cert)
      # append each -c cert to the CERTFILE array
      shift; unset DEFAULT_CERTFILES; CERTFILES+=("$1") ;;
    -s | --send-notifications)
      # if this flag is present, enable notifications
      SEND_NOTIFICATIONS=true ;;
    -p | --print-cron)
      # print cron tab entry (does not install)
      print_cron
    ;;
    --warn-slack-webhook-url) shift; WARN_SLACK_WEBHOOK_URL="$1" ;;
    --alert-slack-webhook-url) shift; ALERT_SLACK_WEBHOOK_URL="$1" ;;
    --page-webhook-url) shift; PAGE_WEBHOOK_URL="$1" ;;

    *)
      usage
      error_exit "Unknown option $1" ;;
  esac
  shift
done

parse_day_ranges
check_endpoints
check_certfiles
