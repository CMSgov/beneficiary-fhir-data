#!/usr/bin/env bash
set -eo pipefail

LOGFILE="${LOGFILE:-repack.log}"
TO_PROCESS="${TO_PROCESS:-to_repack.txt}"
PGHOST="${PGHOST:-}"
PGUSER="${PGUSER:-}"
PGDATABASE="${PGDATABASE:-}"
PGPORT="${PGPORT:-}"
PGPASSWORD="${PGPASSWORD:-}"
TABLES=

log() {
  echo "[$(date)] $1" >> "$LOGFILE"
}

error_exit() {
  echo "Error: $1" >&2 | tee -a "$LOGFILE"
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
    table_schema NOT IN ('pg_catalog', 'information_schema')
ORDER BY
    pg_total_relation_size('"' || table_schema || '"."' || table_name || '"') DESC;
EOF
}

generate_to_process_file() {
  query_tables_by_size_asc > "$TO_PROCESS"
}

load_tables_from_file(){
  # assumes the table names are the first column in a pipe deleminated file
  TABLES=$(cut -d'|' -f1 "$TO_PROCESS")
  log "Loaded $(echo "$TABLES" | wc -l) tables to repack from $TO_PROCESS"
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

# if the $TO_PROCESS file exists, ask if we should load it or start with a new one
if [ -f "$TO_PROCESS" ] ; then
  echo "Existing $TO_PROCESS file found, load this file or remove it and start new?"
  select yn in "Load" "Start new" "Quit"; do
    case $yn in
      Load ) load_tables_from_file; break;;
      "Start new" ) generate_to_process_file; load_tables_from_file; break;;
      Quit ) exit;;
    esac
  done
else
  generate_to_process_file
  load_tables_from_file
fi
[ -f "$TO_PROCESS" ] || error_exit "No $TO_PROCESS file found to process.. exiting"

echo "Repacking! Tables will be removed from the $TO_PROCESS files after they are successfully repacked. Follow along by tailing the log file: tail -f $LOGFILE"
for table in $TABLES; do
  if time pg_repack -e -h "$PGHOST" -U "$PGUSER" -t "$table" -k "$PGDATABASE" 2>&1 | tee -a "$LOGFILE"; then
    # remove the table from the to_process file
    sed -i "/^$table|/d" "$TO_PROCESS"
  else
    error_exit "Failed to repack $table"
  fi
done
echo "Repack complete! All tables have been repacked."
