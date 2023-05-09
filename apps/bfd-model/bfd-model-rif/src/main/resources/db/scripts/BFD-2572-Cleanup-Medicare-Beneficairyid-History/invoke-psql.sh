#!/bin/bash
# ------------------------------------------------------------------
# Shell script to cleanup spurious rcds in the MEDICARE_BENEFICIARYID_HISTORY
# table.
#
# The MEDICARE_BENEFICIARYID_HISTORY is a BFD dtabase table that (mostly) represents
# data provided as a one-time data drop by CCW (APR-2019).
#
# Ostensibly the table could be used (at that time) to derive a BeneficiaryId
# using a historical MBI_NUM. However, that level of functionality has been
# superseded by data in the BENEFICIARIES_HISTORY table, which allows for historical
# MBI_NUM lookups using either a MBI_NUM or the more commonly used a hashed MBI_NUM.
#
# This script then does the following:
#   1) delete all records in the MEDICARE_BENEFICIARYID_HISTORY table where we
#      have existing (matching) MBI data in either the BENEFICIARIES table or
#      the BENEFICIARIES_HISTORY table.
#
#   2) Takes resultant records from MEDICARE_BENEFICIARYID_HISTORY table, and
#      makes a determination is to whether we need to insert a record into the
#      BENEFICIARIES_HISTORY table for the given bene_id / mbi_num / efctv_bgn_dt.
# ------------------------------------------------------------------
set -o pipefail

# following must be passed in as either environment variables or as cmd-line args (default)
PGHOST="${DB_HOST:-$2}"
PGUSER="${DB_USER:-$3}"
# other vars that skirt security (a bit)
PGDATABASE="${DB_NAME:-fhirdb}"
export PGPORT="${DB_PORT:-5432}"

if [ -z "${PGHOST}" ] || [ -z "${PGUSER}" ]; then
    echo "*****  E r r o r - Missing required variables  *****"
    echo "$0 requires ENV variable(s) for: DB_HOST, DB_USER";
    echo "or";
    echo "$0 requires cmd-line args for: <db script> <db host> <db username>";
    exit 1;
fi

# set to true if the SQL being executed should be displayed
SHOW_SQL=false

# STEP_TWO_SETUP_TEMP_SQL
# ==============================================================
# sql to create a temp medicare_beneficiaryid_history table that
# we'll use for the work of analyzing and merging data not found.
# It doesn't matter since we'll be dropping both the real
# medicare_beneficiaryid_history_temp table as well as the
# medicare_beneficiaryid_history table.
# ==============================================================
read -r -d '' STEP_TWO_SETUP_TEMP_SQL << EOM
DROP TABLE IF EXISTS PUBLIC.MEDICARE_BENEFICIARYID_HISTORY_TEMP CASCADE;

CREATE TABLE PUBLIC.MEDICARE_BENEFICIARYID_HISTORY_TEMP AS
SELECT BENE_MBI_ID,
	BENE_ID,
	MBI_NUM ,
	MBI_EFCTV_BGN_DT,
	MBI_EFCTV_END_DT,
	LAST_UPDATED
FROM MEDICARE_BENEFICIARYID_HISTORY;

ALTER TABLE IF EXISTS public.medicare_beneficiaryid_history_temp
    ADD CONSTRAINT medicare_beneficiaryid_history_temp_pkey PRIMARY KEY (bene_mbi_id);
	
CREATE INDEX IF NOT EXISTS medicare_beneficiaryid_history_temp_bene_id_idx
    ON public.medicare_beneficiaryid_history_temp USING btree
    (bene_id ASC NULLS LAST);
	
CREATE INDEX IF NOT EXISTS medicare_beneficiaryid_history_temp_mbi_num_idx
    ON public.medicare_beneficiaryid_history_temp USING btree
    (mbi_num ASC NULLS LAST);
EOM

# STEP_THREE_CLEANUP_SQL
# ==============================================================
# SQL to delete data from the medicare_beneficiaryid_history_temp
# table that is already stored in either the beneficiaries table or
# the beneficiaries_history table.
# ==============================================================
read -r -d '' STEP_THREE_CLEANUP_SQL << EOM
DELETE
FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP F
WHERE F.BENE_MBI_ID in (
-- matches between beneficiaries and medicare_beneficiaryid_history_temp will be deleted
--
SELECT A.BENE_MBI_ID
      FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP A,
        BENEFICIARIES B
      WHERE B.BENE_ID = A.BENE_ID
        AND B.MBI_NUM = A.MBI_NUM
        AND B.EFCTV_BGN_DT = A.MBI_EFCTV_BGN_DT
        AND (B.EFCTV_END_DT IS NULL
                  OR B.EFCTV_END_DT = A.MBI_EFCTV_END_DT)
UNION
-- matches between beneficiaries_history and medicare_beneficiaryid_history_temp will be deleted
--
SELECT A.BENE_MBI_ID
      FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP A,
        BENEFICIARIES_HISTORY B
      WHERE B.BENE_ID = A.BENE_ID
        AND B.MBI_NUM = A.MBI_NUM
        AND B.EFCTV_BGN_DT = A.MBI_EFCTV_BGN_DT
        AND (B.EFCTV_END_DT IS NULL
                  OR B.EFCTV_END_DT = A.MBI_EFCTV_END_DT) );
EOM

# STEP_FOUR_CLEANUP_SQL
# ==============================================================
# SQL to delete data from the medicare_beneficiaryid_history_temp
# table that is already stored in either the beneficiaries table or
# the beneficiaries_history table.
# ==============================================================
read -r -d '' STEP_FOUR_CLEANUP_SQL << EOM
DELETE
FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP D
WHERE D.BENE_MBI_ID in (
   SELECT V1.BENE_MBI_ID
   FROM BENEFICIARIES_HISTORY C,
   (
      -- find records in medicare_beneficiaryid_history that match
      -- on bene_id but do not match on mbi_num. it simply means that
      -- the beneficiary's current mbi_num is not equal to any of
      -- the mbi_num(s) in the medicare_beneficiaryid_history table.
      -- This will potentially be an 'add' if there is no matching data
      -- already in the beneficiaries_history table.
      --
      SELECT A.BENE_MBI_ID,
             A.BENE_ID,
             A.MBI_NUM,
             A.MBI_EFCTV_BGN_DT AS EFCTV_BGN_DT,
             A.MBI_EFCTV_END_DT AS EFCTV_END_DT
      FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP A,
           BENEFICIARIES B
      WHERE B.BENE_ID = A.BENE_ID
      AND B.MBI_NUM <> A.MBI_NUM
   ) V1
-- find medicare_beneficiaryid_history_temp records that
-- match beneficiaries_history data already present.
WHERE (C.BENE_ID = V1.BENE_ID
AND C.MBI_NUM = V1.MBI_NUM));
EOM

# STEP_FIVE_INSERT_SQL
# ==============================================================
# SQL to extract data from the medicare_beneficiaryid_history_temp
# and beneficiares tables and create records in the beneficiaries_history
# table.
#
# we want any records that are in the medicare_beneficiaryid_history
# BUT ARE NOT in the beneficiaries_history table. Also, we'll grab
# a bene's sex, birth date and HICN info from our current beneficiaries
# table to have a somewhat more complete set of data for those records
# inserted into the beneficiaries_history table. We'll use now() for
# last_updated in case we need to back out the INSERTs.
# ==============================================================
read -r -d '' STEP_FIVE_INSERT_SQL << EOM
INSERT INTO BENEFICIARIES_HISTORY (
  BENE_HISTORY_ID,
  BENE_ID,
  MBI_NUM,
  EFCTV_BGN_DT,
  EFCTV_END_DT,
  BENE_CRNT_HIC_NUM,
  HICN_UNHASHED,
  BENE_SEX_IDENT_CD,
  BENE_BIRTH_DT,
  LAST_UPDATED)
SELECT
  NEXTVAL('beneficiaryhistory_beneficiaryhistoryid_seq'),
  V2.BENE_ID,
  V2.MBI_NUM,
  V2.EFCTV_BGN_DT,
  V2.EFCTV_END_DT,
  X.BENE_CRNT_HIC_NUM,
  X.HICN_UNHASHED,
  X.BENE_SEX_IDENT_CD,
  X.BENE_BIRTH_DT,
  NOW()
FROM
  (SELECT TBL1.BENE_MBI_ID,
      TBL1.BENE_ID,
      TBL1.MBI_NUM,
      TBL1.MBI_EFCTV_BGN_DT AS EFCTV_BGN_DT,
      TBL1.MBI_EFCTV_END_DT AS EFCTV_END_DT,
      TBL1.LAST_UPDATED
    FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP TBL1
    WHERE NOT EXISTS
        (SELECT V1.*
          FROM
            (SELECT DISTINCT
                A.BENE_ID,
                A.MBI_NUM,
                A.EFCTV_BGN_DT
              FROM BENEFICIARIES_HISTORY A
              WHERE A.BENE_ID in
                  (SELECT DISTINCT BENE_ID
                    FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP)
                AND A.EFCTV_BGN_DT IS NOT NULL ) V1
          WHERE TBL1.BENE_ID = V1.BENE_ID
            AND TBL1.MBI_NUM = V1.MBI_NUM
            AND V1.EFCTV_BGN_DT IS NOT NULL
            AND TBL1.MBI_EFCTV_BGN_DT = V1.EFCTV_BGN_DT ) ) V2
INNER JOIN BENEFICIARIES X ON X.BENE_ID = V2.BENE_ID
WHERE X.MBI_NUM <> V2.MBI_NUM
ORDER BY 2, 3;
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
# --------------------------------------------------------------------
# Get a count of records in the MEDICARE_BENEFICIARYID_HISTORY table.
# --------------------------------------------------------------------
echo "checking current record counts..."
SQL=$(printf "%s\n\n%s" "SELECT COUNT(*) FROM MEDICARE_BENEFICIARYID_HISTORY")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : Start count of records in MEDICARE_BENEFICIARYID_HISTORY : %d\n\n" "${now}" "${CNT}";
fi
SQL=$(printf "%s\n\n%s" "SELECT COUNT(*) FROM BENEFICIARIES_HISTORY")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : Start count of records in BENEFICIARIES_HISTORY : %d\n\n" "${now}" "${CNT}";
fi

# STEP 2
# -------------------------------------------------------------------
# Create a temp MEDICARE_BENEFICIARYID_HISTORY_TEMP table that we'll
# use for processing; we do it this way as we'll be doing some destructive
# operations (DELETEs) as we go through the various steps.
# -------------------------------------------------------------------
echo "Begin processing of Step 2 at: $(date +'%T')"
echo "${STEP_TWO_SETUP_TEMP_SQL}"

# create the temp table
$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${STEP_TWO_SETUP_TEMP_SQL}")

# check record count to derive success
SQL=$(printf "%s\n\n%s" "SELECT COUNT(*) FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
# if we got a value back from the sql, we'll log some info; else 'pull the plug'
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : Start count of records in MEDICARE_BENEFICIARYID_HISTORY_TEMP : %d\n\n" "${now}" "${CNT}";
else
  exit 1;
fi
if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${SQL}"	
fi
echo "Finished processing of Step 2 at: $(date +'%T.%31')"


# STEP 3
# ---------------------------------------------------------
# Initial cleanup of rcds that are dead-nuts wrong!
# This step prunes rcds in the MEDICARE_BENEFICIARYID_HISTORY_TEMP
# that is already stored in either the BENEFICIARES table or
# the BENEFICIARIES_HISTORY table.
# ---------------------------------------------------------

echo "Begin processing of Step 3 at: $(date +'%T')"

CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${STEP_THREE_CLEANUP_SQL}")
# if we got a value back from the sql, we'll log some info
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : %s\n" "${now}" "${CNT}";
fi
if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${STEP_THREE_CLEANUP_SQL}"	
fi
echo "Finished processing of Step 3 at: $(date +'%T.%31')"

# STEP 4
# ---------------------------------------------------------
# Delete records in the MEDICARE_BENEFICIARYID_HISTORY_TEMP
# table where there is a match on BENEFICIARIES_HISTORY data
# (meaning bene_id + mbi_num already present).
# ---------------------------------------------------------
echo "Begin processing of Step 4 at: $(date +'%T')"

CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${STEP_FOUR_CLEANUP_SQL}")
# if we got a value back from the sql, we'll log some info
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : %s\n" "${now}" "${CNT}";
fi
if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${STEP_FOUR_CLEANUP_SQL}"	
fi
echo "Finished processing of Step 4 at: $(date +'%T.%31')"

# STEP 5
# ---------------------------------------------------------
# Finally, insert all records from the MEDICARE_BENEFICIARYID_HISTORY_TEMP
# table that do not yet exist in the beneficiares_history table.
# For the last_updated we'll use a timestamp that can differentiate
# new records in case we need to back them out.
# ---------------------------------------------------------
echo "Begin processing of Step 5 at: $(date +'%T')"

CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${STEP_FIVE_INSERT_SQL}")
# if we got a value back from the sql, we'll log some info
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : %s\n" "${now}" "${CNT}";
fi
if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${STEP_FIVE_INSERT_SQL}"	
fi
echo "Finished processing of Step 5 at: $(date +'%T.%31')"


# STEP 6
# -------------------------------------------------------------------
# Get a count of records in the BENEFICIARIES_HISTORY table.
# -------------------------------------------------------------------
echo "checking current record counts..."
SQL=$(printf "%s\n\n%s" "SELECT COUNT(*) FROM MEDICARE_BENEFICIARYID_HISTORY_TEMP")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "\n%s : End count of records in MEDICARE_BENEFICIARYID_HISTORY_TEMP : %d\n\n" "${now}" "${CNT}";
fi
SQL=$(printf "%s\n\n%s" "SELECT COUNT(*) FROM beneficiaries_history")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "\n%s : End count of records in BENEFICIARIES_HISTORY : %d\n\n" "${now}" "${CNT}";
fi
exit 0;