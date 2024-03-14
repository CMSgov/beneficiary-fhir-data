#!/bin/bash
# ------------------------------------------------------------------
# Shell script to cleanup spurious rcds caused by a bug at CCW which
# inadvertently mis-characterized records when cross-referencing via
# the BENE_LINK_KEY; this manifested itself as CCW creating a new
# beneficiary record. Downstream, this caused the BFD ETL pipeline to
# create spurious, duplicate records in the following tables:
#
#     BENEFICIARIES
#     BENEFICIARIES_HISTORY
#     BENEFICIARY_MONTHLY
#
# CCW subsequently provided a CSV file, FHIR_XRefed_BENE_ID.csv,
# containing all BENE_ID(s) they believe should not have been created.
# This script makes use of that CSV file to create a temp table,
# CCW_DUPL_BENE_FROM_CSV, that contains all the BENE_ID(s) that will
# used to cleanup spurious records.
#
# ------------------------------------------------------------------
set -o pipefail

# following must be passed in as either environment variables or as cmd-line args (default)
PGHOST="${DB_HOST:-$1}"
PGUSER="${DB_USER:-$2}"
CSVFILE="${CSVFILE:-$3}"
# other vars that skirt security (a bit)
PGDATABASE="${DB_NAME:-fhirdb}"
export PGPORT="${DB_PORT:-5432}"

if [ -z "${CSVFILE}" ]; then
  CSVFILE="${PWD}/FHIR_XRefed_BENE_ID.csv"
fi

if ! test -f "${CSVFILE}"; then
  echo "CSV file not found...exiting!"
  exit 1;
fi

if [ -z "${PGHOST}" ] || [ -z "${PGUSER}" ]; then
    echo "*****  E r r o r - Missing required variables  *****"
    echo "$0 requires ENV variable(s) for: DB_HOST, DB_USER";
    echo "or";
    echo "$0 requires cmd-line args for: <db script> <db host> <db username>";
    exit 1;
fi

# set to true if the SQL being executed should be displayed
SHOW_SQL=false

# STEP_ONE_SETUP_TEMP_SQL
# ==============================================================
# sql to create a temp CCW_DUPL_BENE_FROM_CSV table that will
# be used to drive the rest of the cleanup.
# ==============================================================
read -r -d '' STEP_ONE_SETUP_TEMP_SQL << EOM
DROP TABLE IF EXISTS CCW_DUPL_BENE_FROM_CSV CASCADE;

CREATE TABLE CCW_DUPL_BENE_FROM_CSV (
	bene_id bigint PRIMARY KEY
);
EOM

# STEP_TWO_COPY_CSV_SQL
# ==============================================================
# sql to create a temp CCW_DUPL_BENE_FROM_CSV table that will
# be used to drive the rest of the cleanup.
# ==============================================================
read -r -d '' STEP_TWO_COPY_CSV_SQL << EOM
\\copy CCW_DUPL_BENE_FROM_CSV from $CSVFILE CSV HEADER;
EOM

# STEP_THREE_BACKUP_DATA
# ==============================================================
read -r -d '' STEP_THREE_BACKUP_DATA << EOM
create table if not exists beneficiaries_ccw_remediation as
select a.* from beneficiaries a
where a.bene_id in (
  select bene_id from ccw_dupl_bene_from_csv
)
and a.mbi_num is null;

create table if not exists beneficiary_monthly_ccw_remediation as
select a.*
from beneficiary_monthly a,
beneficiaries b
where a.bene_id in (
  select bene_id from ccw_dupl_bene_from_csv
)
and b.bene_id = a.bene_id
and b.mbi_num is null;

create table if not exists beneficiaries_history_ccw_remediation as
select a.* from beneficiaries_history a,
beneficiaries b
where a.bene_id in (
  select bene_id from ccw_dupl_bene_from_csv
)
and b.bene_id = a.bene_id
and b.mbi_num is null;
EOM

# STEP_FOUR_CLEANUP_SQL
# ==============================================================
read -r -d '' STEP_FOUR_CLEANUP_SQL << EOM
delete from beneficiaries_history
where bene_id in (
  select bene_id from beneficiaries_history_ccw_remediation
);

delete from beneficiary_monthly
where bene_id in (
  select bene_id from beneficiary_monthly_ccw_remediation
);

delete from beneficiaries
where bene_id in (
  select bene_id from beneficiaries_ccw_remediation
);
EOM

# ==============================
# BEGIN PROCESSING
# ==============================
# belt-and-suspenders to check connection to database.
echo "Testing db connectivity..."
now=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --quiet --tuples-only -c "select NOW();")
if [[ "$now" == *"202"* ]]; then
  echo "db connectivity: OK"
else
  echo "db connectivity: FAILED...exiting"
  exit 1;
fi

# STEP 1
# -------------------------------------------------------------------
# Create a temp CCW_DUPL_BENE_FROM_CSV table that we'll use for
# processing; this allows us to (re-)use the same list of BENE_ID's.
# -------------------------------------------------------------------
echo "Begin processing of Step 1 at: $(date +'%T')"

# create the temp table
psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -c "${STEP_ONE_SETUP_TEMP_SQL}"

if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${STEP_ONE_SETUP_TEMP_SQL}"	
fi

# check record count to derive success
SQL="SELECT COUNT(*) FROM CCW_DUPL_BENE_FROM_CSV"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : count of records in CCW_DUPL_BENE_FROM_CSV : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi
echo "Finished processing of Step 1 at: $(date +'%T.%31')"

# STEP 2
# -------------------------------------------------------------------
# Create a temp CCW_DUPL_BENE_FROM_CSV table that we'll use for
# processing; this allows us to (re-)use the same list of BENE_ID's.
# -------------------------------------------------------------------
echo "Begin processing of Step 2 at: $(date +'%T')"

# create the temp table
psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -c "${STEP_TWO_COPY_CSV_SQL}"

# check record count to derive success
SQL="SELECT COUNT(*) FROM CCW_DUPL_BENE_FROM_CSV"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : count of records in CCW_DUPL_BENE_FROM_CSV : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi
echo "Finished processing of Step 2 at: $(date +'%T.%31')"


# STEP 3
# ---------------------------------------------------------
# Backup the data we're about to delete (just in case!!!!).
# ---------------------------------------------------------
echo "Begin processing of Step 3 at: $(date +'%T')"
psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -c "${STEP_THREE_BACKUP_DATA}"

if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${STEP_THREE_BACKUP_DATA}"	
fi

# check record count to derive success
SQL="SELECT COUNT(*) FROM BENEFICIARIES_CCW_REMEDIATION"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : count of records in BENEFICIARIES_CCW_REMEDIATION : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

SQL="SELECT COUNT(*) FROM BENEFICIARY_MONTHLY_CCW_REMEDIATION"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : count of records in BENEFICIARY_MONTHLY_CCW_REMEDIATION : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

SQL="SELECT COUNT(*) FROM BENEFICIARIES_HISTORY_CCW_REMEDIATION"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : count of records in BENEFICIARIES_HISTORY_CCW_REMEDIATION : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

echo "Finished processing of Step 3 at: $(date +'%T.%31')"

# ======================================================
# come up for air and allow user to check data as needed;
# continue if things loook OK.
#
# default variable for read to store result is $REPLY
# ======================================================
echo
echo "script pausing to allow review of tables backup..."
read -p "Do you wish to continue ? [Yy] : " -n 1 -r
echo    # (optional) move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo "continuing..."
else
  echo "user interrupted execution prior to deleting any data...exiting!";
  exit 1;
fi

# STEP 4
# ---------------------------------------------------------
echo "Begin processing of Step 4 at: $(date +'%T')"

SQL="SELECT COUNT(*) FROM BENEFICIARIES"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : BEFORE count of records in BENEFICIARIES : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

SQL="SELECT COUNT(*) FROM BENEFICIARIES_HISTORY"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : BEFORE count of records in BENEFICIARIES_HISTORY : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

SQL="SELECT COUNT(*) FROM BENEFICIARY_MONTHLY"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : BEFORE count of records in BENEFICIARY_MONTHLY : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

# do the actual cleanup
psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -c "${STEP_FOUR_CLEANUP_SQL}"

if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${STEP_THREE_CLEANUP_SQL}"	
fi

SQL="SELECT COUNT(*) FROM BENEFICIARIES"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : AFTER count of records in BENEFICIARIES : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

SQL="SELECT COUNT(*) FROM BENEFICIARIES_HISTORY"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : AFTER count of records in BENEFICIARIES_HISTORY : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

SQL="SELECT COUNT(*) FROM BENEFICIARY_MONTHLY"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : AFTER count of records in BENEFICIARY_MONTHLY : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi

echo "Finished processing of Step 3 at: $(date +'%T.%31')"

exit 0;
