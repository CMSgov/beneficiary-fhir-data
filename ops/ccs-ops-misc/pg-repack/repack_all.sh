#!/usr/bin/env bash
set -eo pipefail

LOGFILE="${LOGFILE:-repack.log}"
TO_REPACK="${TO_REPACK:-repack_list.txt}"
PGHOST="${PGHOST:-}"
PGUSER="${PGUSER:-}"
PGDATABASE="${PGDATABASE:-}"
PGPORT="${PGPORT:-}"
PGPASSWORD="${PGPASSWORD:-}"
SKIP_SCHEMAS="${SKIP_SCHEMAS:-"('pg_catalog', 'information_schema', 'repack', 'palm_beach')"}"
TABLES=

prepend_timestamp() {
  while IFS= read -r line; do
    echo "$(date '+%Y-%m-%d %H:%M:%S') $line"
  done
}

log() {
  echo "$1" | prepend_timestamp >> "$LOGFILE"
}

cleanup() {
  echo "Cleaning up.." | prepend_timestamp | tee -a "$LOGFILE"
  rm -f "$TO_REPACK" || true
  reset_repack || echo "Failed to reset pg_repack extension.. please reset manually" | prepend_timestamp | tee -a "$LOGFILE"
}

error_exit() {
  echo "Error: $1" >&2 | prepend_timestamp | tee -a "$LOGFILE"
  cleanup
  exit 1
}

is_screen() {
  if [ -n "$STY" ] || [ -n "$TMUX" ]; then
    return 0
  else
    return 1
  fi
}

# this returns a list of all user tables in the database, ordered by size ascending (smallest tables first)
# results look like:
#  schema.table|size
query_tables_by_size_asc() {
 psql -t -A <<EOF | awk '{print $1}'
SELECT
    table_schema || '.' || table_name AS "table",
    pg_size_pretty(pg_total_relation_size('"' || table_schema || '"."' || table_name || '"')) AS size
FROM
    information_schema.tables
WHERE
    table_schema NOT IN $SKIP_SCHEMAS
    AND
    table_name NOT LIKE 'pg_%'
    AND
    table_type = 'BASE TABLE'
ORDER BY
    pg_total_relation_size('"' || table_schema || '"."' || table_name || '"');
EOF
}

# run this function to remove the pg_repack extension and all of its objects from the database, this will remove any
# repack triggers and other objects that may be left behind after a failed repack (easier than manually removing them)
reset_repack() {
  psql -c "DROP EXTENSION IF EXISTS pg_repack CASCADE;" || true
  psql -c "CREATE EXTENSION IF NOT EXISTS pg_repack;" || true
  log "pg_repack extension reset"
}

generate_repack_file() {
  query_tables_by_size_asc > "$TO_REPACK"
}

load_tables_from_file(){
  # assumes the table names are the first column in a pipe deleminated file
  TABLES=$(cut -d'|' -f1 "$TO_REPACK")
  log "Loaded $(echo "$TABLES" | wc -l) tables to repack from $TO_REPACK"
  export TABLES
}

repack_is_running() {
  if pgrep -f "pg_repack" > /dev/null; then
    return 0
  else
    return 1
  fi
}

## Begin

if repack_is_running; then
  error_exit "pg_repack is already running.. please wait for it to finish"
fi

if ! is_screen; then
  error_exit "Not running in a screen, please run in a screen or tmux session"
fi

if [ -z "$PGHOST" ]; then
  error_exit "Please set PGHOST"
fi

if [ -z "$PGUSER" ]; then
  error_exit "Please set PGUSER"
fi

if [ -z "$PGDATABASE" ]; then
  error_exit "Please set PGDATABASE"
fi

if [ -z "$PGPORT" ]; then
  error_exit "Please set PGPORT"
fi

if [ -z "$PGPASSWORD" ]; then
  echo "PGPASSWORD not set, please enter it now: "
  read -r -s PGPASSWORD
  echo
fi

export PGHOST PGUSER PGDATABASE PGPORT PGPASSWORD

touch "$LOGFILE"

case "$1" in
  reset) reset_repack; exit;;
  gen*) generate_repack_file; exit;;
esac

# if the $TO_REPACK file exists, ask if we should load it or start with a new one
if [ -f "$TO_REPACK" ] ; then
  echo "Existing $TO_REPACK file found, load this file or remove it and start new?"
  select yn in "Load" "Start new" "Quit"; do
    case $yn in
      Load ) load_tables_from_file; break;;
      "Start new" ) generate_repack_file; load_tables_from_file; break;;
      "Generate repack list and quit" ) generate_repack_file; load_tables_from_file; break;;
      Quit ) exit;;
    esac
  done
else
  # NOTE: the following repacking by schema bit is untested.. YMMV
  # list schemas in the database
  if [ -z "$SCHEMAS" ]; then
    SCHEMAS=$(psql -t -A -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('pg_catalog', 'information_schema');")
  fi
  [ -n "$SCHEMAS" ] || error_exit "No schemas found in the database"

  # select a schema to repack
  echo "Select a schema to repack"
  select schema in $SCHEMAS; do
    SCHEMAS=$schema
    break
  done
  generate_repack_file "$SCHEMAS"
  load_tables_from_file
fi

# short circuit if there are no tables to repack
[ -n "$TABLES" ] || error_exit "No tables found to repack.. exiting"

# prompt to continue
echo "Press any key to continue or Ctrl+C to quit"
read -r -n 1 -s

echo "Repacking! Tables will be removed from the $TO_REPACK files after they are successfully repacked. Follow along by tailing the log file: tail -f $LOGFILE"

for table in $TABLES; do
  start_time=$(date '+%Y-%m-%d %H:%M:%S')

  # Run pg_repack and redirect stdout and stderr to the log file
  if pg_repack -e -h "$PGHOST" -U "$PGUSER" -t "$table" -k "$PGDATABASE" 2>&1 | prepend_timestamp >> "$LOGFILE" 2>&1; then
    end_time=$(date '+%Y-%m-%d %H:%M:%S')
    log "${table} Start time: $start_time, End time: $end_time"
    # Remove the table from the TO_REPACK fil
    sed -i "/^$table|/d" "$TO_REPACK"
  else
    error_exit "Failed to repack $table"
  fi
done
cleanup
log "repack completed successfully"
