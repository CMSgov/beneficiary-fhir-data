#!/bin/sh

set -e
set -o pipefail

csvPipe='/tmp/data-server-access-log.csv'
sqliteDb='./output/access_logs.sqlite'

importCsvToSqlite() {
  mkfifo "${csvPipe}"

"${sqlite3}" "${sqliteDb}" <<EOF &
.mode csv
.headers off
.import "${csvPipe}" access_log
EOF

  cat - > "${csvPipe}"
  wait
  rm "${csvPipe}"
}

parseToCsv() {
  "${gawk}" -f ./access_logs_parse_to_csv.awk "localHostname=${localHostname}" | importCsvToSqlite
}

buildDatabaseViaSsh() {
  localHostname='pdcw10ap01'
  ssh -q bluebutton-healthapt-prod-data-server-a-1 "cat /u01/jboss/jboss-eap-7.0/standalone/log/access*" | parseToCsv

  localHostname='pdcw10ap02'
  ssh -q bluebutton-healthapt-prod-data-server-b-1 "cat /u01/jboss/jboss-eap-7.0/standalone/log/access*" | parseToCsv
}

# Normally disabled, but sometimes useful for debugging.
buildDatabaseViaScp() {
  localHostname='pdcw10ap01'
  currentCsv="/tmp/data-server-access-log-${localHostname}.csv"
  if [ ! -r "${currentCsv}" ]; then
    ssh -q bluebutton-healthapt-prod-data-server-a-1 "cat /u01/jboss/jboss-eap-7.0/standalone/log/access*" > "${currentCsv}"
  fi
  cat "${currentCsv}" | parseToCsv

  localHostname='pdcw10ap02'
  currentCsv="/tmp/data-server-access-log-${localHostname}.csv"
  if [ ! -r "${currentCsv}" ]; then
    ssh -q bluebutton-healthapt-prod-data-server-b-1 "cat /u01/jboss/jboss-eap-7.0/standalone/log/access*" > "${currentCsv}"
  fi
  cat "${currentCsv}" | parseToCsv
}

createSqliteDb() {
  if [ -f "${sqliteDb}" ]; then
    >&2 echo "Database file '${sqliteDb}' already exists. Aborting."
    exit 1
  else
    ./access_logs_db_create_or_update.sh "${sqliteDb}"
  fi
}

findSqlite3() {
  sqlite3Homebrew='/usr/local/opt/sqlite/bin/sqlite3'
  if [ -x "${sqlite3Homebrew}" ]; then
    echo "${sqlite3Homebrew}"
  else
    echo 'sqlite3'
  fi
}
sqlite3="$(findSqlite3)"

findGawk() {
  gawkHomebrew='/usr/local/opt/gawk/bin/gawk'
  if [ -x "${gawkHomebrew}" ]; then
    echo "${gawkHomebrew}"
  else
    echo 'gawk'
  fi
}
gawk="$(findGawk)"

createSqliteDb
#buildDatabaseViaScp
buildDatabaseViaSsh

echo "Access logs retrieved, parsed, and imported to DB: '${sqliteDb}'"
echo "  Total Records: $("${sqlite3}" "${sqliteDb}" "SELECT count(*) FROM access_log")"
echo "   First Record: $("${sqlite3}" "${sqliteDb}" "SELECT min(timestamp) FROM access_log WHERE timestamp LIKE '20%'")"
echo "    Last Record: $("${sqlite3}" "${sqliteDb}" "SELECT max(timestamp) FROM access_log")"

# Exit normally (don't trip the error handler).
trap - EXIT
