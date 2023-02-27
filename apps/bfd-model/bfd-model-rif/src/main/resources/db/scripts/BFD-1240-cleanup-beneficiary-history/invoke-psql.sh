#!/bin/bash
# ------------------------------------------------------------------
# Shell script to cleanup spurious rcds in the BENEFICIARIES_HISTORY
# table.
#
# The BENEFICIARIES_HISTORY is a pseudo-audit table tracking changes
# to the following columns in the BENEFICIARIES table:
#    MBI_NUM
#    MBI_HASH
#    BENE_CRNT_HIC_NUM
#    HICN_UNHASHED
#    BENE_BIRTH_DT
#    BENE_SEX_IDENT_CD
#    EFCTV_BGN_DT
#
# The BENEFICIARIES_HISTORY data can be used to perform a lookup in
# the BENEFICIARIES table using past (historical) MBI or HICN values.
# For example, if a bene has been issued a new MBI_NUM (for whatever
# reason), then a lookup can still be performed using the original
# MBI_NUM from the BENEFICIARIES_HISTORY table.
#
# The reason we have spurious data in the BENEFICIARIES_HISTORY table
# is records were created because CCW was passing in non-null value
# EFCTV_BGN_DT for the BENEFICIARIES RIF record in the Monthly extract,
# vs. a null value EFCTV_BGN_DT in the weekly RIF extract; that null
# vs. non-null comparison(s) resulted in significant data thrashing
# resulting in numerous records that shouod not have been created in
# the first place!
#
# The logic in the ETL that creates a BENEFICIARY_HISTORY record is the
# result of a compare between the current record in the BENEFICIARIES
# table vs. an incoming RIF BENEFICIARIES object; ANY differences between
# the following db (model) columns results in a new BENEFICIARY_HISTORY rcd.
#
# actual code and comment from the ETL:
# if (table.hicn_unhashed != rif.hicn_unhashed
#   || table.mbi_num != rif.mbi_num
#   || table.efctv_bgn_dt = rif.efctv_bgn_dt
#   || table.efctv_end_dt = rif.efctv_end_dt
#   || table.bene_crnt_hic_num = rif.bene_crnt_hic_num
#   || table.mbi_hash = table.mbi_hash
#   || table.bene_birth_dt = rif.bene_birth_dt
#   || table.bene_sex_ident_cd = rif.bene_sex_ident_cd) {
#   // write beneficiaries_history record
# }
#
# Thus for cleanup. we'll look for records that meet the equality
# test between BENEFICIARY_HISTORY record(s) and its corresponding
# BENEFICIARIES record; any match is a spurious record and should
# be deleted.
# ------------------------------------------------------------------
set -o pipefail

# following must be passed in as either environment variables or as cmd-line args (default)
PGHOST="${DB_HOST:-$2}"
PGUSER="${DB_USER:-$3}"
PGPASSWORD="${DB_PSWD:-$4}"
# other vars that skirt security (a bit)
PGDATABASE="${DB_NAME:-fhirdb}"
export PGPORT="${DB_PORT:-5432}"

if [ -z "${PGHOST}" ] || [ -z "${PGUSER}" ] || [ -z "${PGPASSWORD}" ]; then
    echo "*****  E r r o r - Missing required variables  *****"
    echo "$0 requires ENV variable(s) for: DB_SCRIPT, DB_HOST, DB_USER, DB_PSWD";
    echo "or";
    echo "$0 requires cmd-line args for: <db script> <db host> <db username> <db password>";
    exit 1;
fi

# set to true if the SQL being executed should be displayed
SHOW_SQL=false

# array of bene_history_id ranages; we need to do this in batches
# as even a simple select over almost 2 billion rcds can lead to
# memory exhaustion. Trying to do actual updates (deletes) will
# exhaust the rollback buffer even sooner!!!
#
# The bene_history_id were created based on two things:
#   1) the min, the max, and the increment for the postgres
#      sequence number (currently defined as 50) used for
#      the surrogate key.
#   2) decompose the bene_history_id into reasonable range(s) that
#      allow us to process a batch without exhausting memory. Currently
#      using a value of 25M rcds.
#

# TEST_WHERE is used to assert the correctness of the script/sql.
#TEST_WHERE=(
#"WHERE bh.bene_history_id >= 0 AND bh.bene_history_id <= 25000000")

BENES_WHERE=(
"WHERE bh.bene_history_id >= 0 AND bh.bene_history_id <= 25000000"
"WHERE bh.bene_history_id >= 25000000 AND bh.bene_history_id <= 50000000"
"WHERE bh.bene_history_id >= 50000000 AND bh.bene_history_id <= 75000000"
"WHERE bh.bene_history_id >= 75000000 AND bh.bene_history_id <= 100000000"
"WHERE bh.bene_history_id >= 100000000 AND bh.bene_history_id <= 125000000"
"WHERE bh.bene_history_id >= 125000000 AND bh.bene_history_id <= 150000000"
"WHERE bh.bene_history_id >= 150000000 AND bh.bene_history_id <= 175000000"
"WHERE bh.bene_history_id >= 175000000 AND bh.bene_history_id <= 200000000"
"WHERE bh.bene_history_id >= 200000000 AND bh.bene_history_id <= 225000000"
"WHERE bh.bene_history_id >= 225000000 AND bh.bene_history_id <= 250000000"
"WHERE bh.bene_history_id >= 250000000 AND bh.bene_history_id <= 275000000"
"WHERE bh.bene_history_id >= 275000000 AND bh.bene_history_id <= 300000000"
"WHERE bh.bene_history_id >= 300000000 AND bh.bene_history_id <= 325000000"
"WHERE bh.bene_history_id >= 325000000 AND bh.bene_history_id <= 350000000"
"WHERE bh.bene_history_id >= 350000000 AND bh.bene_history_id <= 375000000"
"WHERE bh.bene_history_id >= 375000000 AND bh.bene_history_id <= 400000000"
"WHERE bh.bene_history_id >= 400000000 AND bh.bene_history_id <= 425000000"
"WHERE bh.bene_history_id >= 425000000 AND bh.bene_history_id <= 450000000"
"WHERE bh.bene_history_id >= 450000000 AND bh.bene_history_id <= 475000000"
"WHERE bh.bene_history_id >= 475000000 AND bh.bene_history_id <= 500000000"
"WHERE bh.bene_history_id >= 500000000 AND bh.bene_history_id <= 525000000"
"WHERE bh.bene_history_id >= 525000000 AND bh.bene_history_id <= 550000000"
"WHERE bh.bene_history_id >= 550000000 AND bh.bene_history_id <= 575000000"
"WHERE bh.bene_history_id >= 575000000 AND bh.bene_history_id <= 600000000"
"WHERE bh.bene_history_id >= 600000000 AND bh.bene_history_id <= 625000000"
"WHERE bh.bene_history_id >= 625000000 AND bh.bene_history_id <= 650000000"
"WHERE bh.bene_history_id >= 650000000 AND bh.bene_history_id <= 675000000"
"WHERE bh.bene_history_id >= 675000000 AND bh.bene_history_id <= 700000000"
"WHERE bh.bene_history_id >= 700000000 AND bh.bene_history_id <= 725000000"
"WHERE bh.bene_history_id >= 725000000 AND bh.bene_history_id <= 750000000"
"WHERE bh.bene_history_id >= 750000000 AND bh.bene_history_id <= 775000000"
"WHERE bh.bene_history_id >= 775000000 AND bh.bene_history_id <= 800000000"
"WHERE bh.bene_history_id >= 800000000 AND bh.bene_history_id <= 825000000"
"WHERE bh.bene_history_id >= 825000000 AND bh.bene_history_id <= 850000000"
"WHERE bh.bene_history_id >= 850000000 AND bh.bene_history_id <= 875000000"
"WHERE bh.bene_history_id >= 875000000 AND bh.bene_history_id <= 900000000"
"WHERE bh.bene_history_id >= 900000000 AND bh.bene_history_id <= 925000000"
"WHERE bh.bene_history_id >= 925000000 AND bh.bene_history_id <= 950000000"
"WHERE bh.bene_history_id >= 950000000 AND bh.bene_history_id <= 975000000"
"WHERE bh.bene_history_id >= 975000000 AND bh.bene_history_id <= 1000000000"
"WHERE bh.bene_history_id >= 1000000000 AND bh.bene_history_id <= 1025000000"
"WHERE bh.bene_history_id >= 1025000000 AND bh.bene_history_id <= 1050000000"
"WHERE bh.bene_history_id >= 1050000000 AND bh.bene_history_id <= 1075000000"
"WHERE bh.bene_history_id >= 1075000000 AND bh.bene_history_id <= 1100000000"
"WHERE bh.bene_history_id >= 1100000000 AND bh.bene_history_id <= 1125000000"
"WHERE bh.bene_history_id >= 1125000000 AND bh.bene_history_id <= 1150000000"
"WHERE bh.bene_history_id >= 1150000000 AND bh.bene_history_id <= 1175000000"
"WHERE bh.bene_history_id >= 1175000000 AND bh.bene_history_id <= 1200000000"
"WHERE bh.bene_history_id >= 1200000000 AND bh.bene_history_id <= 1225000000"
"WHERE bh.bene_history_id >= 1225000000 AND bh.bene_history_id <= 1250000000"
"WHERE bh.bene_history_id >= 1250000000 AND bh.bene_history_id <= 1275000000"
"WHERE bh.bene_history_id >= 1275000000 AND bh.bene_history_id <= 1300000000"
"WHERE bh.bene_history_id >= 1300000000 AND bh.bene_history_id <= 1325000000"
"WHERE bh.bene_history_id >= 1325000000 AND bh.bene_history_id <= 1350000000"
"WHERE bh.bene_history_id >= 1350000000 AND bh.bene_history_id <= 1375000000"
"WHERE bh.bene_history_id >= 1375000000 AND bh.bene_history_id <= 1400000000"
"WHERE bh.bene_history_id >= 1400000000 AND bh.bene_history_id <= 1425000000"
"WHERE bh.bene_history_id >= 1425000000 AND bh.bene_history_id <= 1450000000"
"WHERE bh.bene_history_id >= 1450000000 AND bh.bene_history_id <= 1475000000"
"WHERE bh.bene_history_id >= 1475000000 AND bh.bene_history_id <= 1500000000"
"WHERE bh.bene_history_id >= 1500000000 AND bh.bene_history_id <= 1525000000"
"WHERE bh.bene_history_id >= 1525000000 AND bh.bene_history_id <= 1550000000"
"WHERE bh.bene_history_id >= 1550000000 AND bh.bene_history_id <= 1575000000"
"WHERE bh.bene_history_id >= 1575000000 AND bh.bene_history_id <= 1600000000"
"WHERE bh.bene_history_id >= 1600000000 AND bh.bene_history_id <= 1625000000"
"WHERE bh.bene_history_id >= 1625000000 AND bh.bene_history_id <= 1650000000"
"WHERE bh.bene_history_id >= 1650000000 AND bh.bene_history_id <= 1675000000"
"WHERE bh.bene_history_id >= 1675000000 AND bh.bene_history_id <= 1700000000"
"WHERE bh.bene_history_id >= 1700000000 AND bh.bene_history_id <= 1725000000"
"WHERE bh.bene_history_id >= 1725000000 AND bh.bene_history_id <= 1750000000"
"WHERE bh.bene_history_id >= 1750000000 AND bh.bene_history_id <= 1775000000"
"WHERE bh.bene_history_id >= 1775000000 AND bh.bene_history_id <= 1800000000"
"WHERE bh.bene_history_id >= 1800000000 AND bh.bene_history_id <= 1825000000"
"WHERE bh.bene_history_id >= 1825000000 AND bh.bene_history_id <= 1850000000"
"WHERE bh.bene_history_id >= 1850000000 AND bh.bene_history_id <= 1875000000"
"WHERE bh.bene_history_id >= 1875000000 AND bh.bene_history_id <= 1900000000"
"WHERE bh.bene_history_id >= 1900000000 AND bh.bene_history_id <= 1925000000"
"WHERE bh.bene_history_id >= 1925000000 AND bh.bene_history_id <= 1950000000"
"WHERE bh.bene_history_id >= 1950000000 AND bh.bene_history_id <= 1975000000"
"WHERE bh.bene_history_id >= 1975000000 AND bh.bene_history_id <= 2000000000"
"WHERE bh.bene_history_id >= 2000000000 AND bh.bene_history_id <= 2025000000"
"WHERE bh.bene_history_id >= 2025000000 AND bh.bene_history_id <= 2050000000"
"WHERE bh.bene_history_id >= 2050000000 AND bh.bene_history_id <= 2075000000"
"WHERE bh.bene_history_id >= 2075000000 AND bh.bene_history_id <= 2100000000"
"WHERE bh.bene_history_id >= 2100000000 AND bh.bene_history_id <= 2125000000"
"WHERE bh.bene_history_id >= 2125000000 AND bh.bene_history_id <= 2150000000"
"WHERE bh.bene_history_id >= 2150000000 AND bh.bene_history_id <= 2175000000"
"WHERE bh.bene_history_id >= 2175000000 AND bh.bene_history_id <= 2200000000"
"WHERE bh.bene_history_id >= 2200000000 AND bh.bene_history_id <= 2225000000"
"WHERE bh.bene_history_id >= 2225000000 AND bh.bene_history_id <= 2250000000"
"WHERE bh.bene_history_id >= 2250000000 AND bh.bene_history_id <= 2275000000"
"WHERE bh.bene_history_id >= 2275000000 AND bh.bene_history_id <= 2300000000"
"WHERE bh.bene_history_id >= 2300000000 AND bh.bene_history_id <= 2325000000"
"WHERE bh.bene_history_id >= 2325000000 AND bh.bene_history_id <= 2350000000"
"WHERE bh.bene_history_id >= 2350000000 AND bh.bene_history_id <= 2375000000"
"WHERE bh.bene_history_id >= 2375000000 AND bh.bene_history_id <= 2400000000"
"WHERE bh.bene_history_id >= 2400000000 AND bh.bene_history_id <= 2425000000"
"WHERE bh.bene_history_id >= 2425000000 AND bh.bene_history_id <= 2450000000"
"WHERE bh.bene_history_id >= 2450000000 AND bh.bene_history_id <= 2475000000"
"WHERE bh.bene_history_id >= 2475000000 AND bh.bene_history_id <= 2500000000"
"WHERE bh.bene_history_id >= 2500000000 AND bh.bene_history_id <= 2525000000"
"WHERE bh.bene_history_id >= 2525000000 AND bh.bene_history_id <= 2550000000"
"WHERE bh.bene_history_id >= 2550000000 AND bh.bene_history_id <= 2575000000"
"WHERE bh.bene_history_id >= 2575000000 AND bh.bene_history_id <= 2600000000"
"WHERE bh.bene_history_id >= 2600000000 AND bh.bene_history_id <= 2625000000"
"WHERE bh.bene_history_id >= 2625000000 AND bh.bene_history_id <= 2650000000"
"WHERE bh.bene_history_id >= 2650000000 AND bh.bene_history_id <= 2675000000"
"WHERE bh.bene_history_id >= 2675000000 AND bh.bene_history_id <= 2700000000"
"WHERE bh.bene_history_id >= 2700000000 AND bh.bene_history_id <= 2725000000"
"WHERE bh.bene_history_id >= 2725000000 AND bh.bene_history_id <= 2750000000"
"WHERE bh.bene_history_id >= 2750000000 AND bh.bene_history_id <= 2775000000"
"WHERE bh.bene_history_id >= 2775000000 AND bh.bene_history_id <= 2800000000"
"WHERE bh.bene_history_id >= 2800000000 AND bh.bene_history_id <= 2825000000"
"WHERE bh.bene_history_id >= 2825000000 AND bh.bene_history_id <= 2850000000"
"WHERE bh.bene_history_id >= 2850000000 AND bh.bene_history_id <= 2875000000"
"WHERE bh.bene_history_id >= 2875000000 AND bh.bene_history_id <= 2900000000"
"WHERE bh.bene_history_id >= 2900000000 AND bh.bene_history_id <= 2925000000"
"WHERE bh.bene_history_id >= 2925000000 AND bh.bene_history_id <= 2950000000"
"WHERE bh.bene_history_id >= 2950000000 AND bh.bene_history_id <= 2975000000"
"WHERE bh.bene_history_id >= 2975000000 AND bh.bene_history_id <= 3000000000"
"WHERE bh.bene_history_id >= 3000000000 AND bh.bene_history_id <= 3025000000"
"WHERE bh.bene_history_id >= 3025000000 AND bh.bene_history_id <= 3050000000"
"WHERE bh.bene_history_id >= 3050000000 AND bh.bene_history_id <= 3075000000"
"WHERE bh.bene_history_id >= 3075000000 AND bh.bene_history_id <= 3100000000"
"WHERE bh.bene_history_id >= 3100000000 AND bh.bene_history_id <= 3125000000"
"WHERE bh.bene_history_id >= 3125000000 AND bh.bene_history_id <= 3150000000"
"WHERE bh.bene_history_id >= 3150000000 AND bh.bene_history_id <= 3175000000"
"WHERE bh.bene_history_id >= 3175000000 AND bh.bene_history_id <= 3200000000"
"WHERE bh.bene_history_id >= 3200000000 AND bh.bene_history_id <= 3225000000"
"WHERE bh.bene_history_id >= 3225000000 AND bh.bene_history_id <= 3250000000"
"WHERE bh.bene_history_id >= 3250000000 AND bh.bene_history_id <= 3275000000"
"WHERE bh.bene_history_id >= 3275000000 AND bh.bene_history_id <= 3300000000"
"WHERE bh.bene_history_id >= 3300000000 AND bh.bene_history_id <= 3325000000"
"WHERE bh.bene_history_id >= 3325000000 AND bh.bene_history_id <= 3350000000"
"WHERE bh.bene_history_id >= 3350000000 AND bh.bene_history_id <= 3375000000"
"WHERE bh.bene_history_id >= 3375000000 AND bh.bene_history_id <= 3400000000"
"WHERE bh.bene_history_id >= 3400000000 AND bh.bene_history_id <= 3425000000"
"WHERE bh.bene_history_id >= 3425000000 AND bh.bene_history_id <= 3450000000"
"WHERE bh.bene_history_id >= 3450000000 AND bh.bene_history_id <= 3475000000"
"WHERE bh.bene_history_id >= 3475000000 AND bh.bene_history_id <= 3500000000"
"WHERE bh.bene_history_id >= 3500000000 AND bh.bene_history_id <= 3525000000"
"WHERE bh.bene_history_id >= 3525000000 AND bh.bene_history_id <= 3550000000"
"WHERE bh.bene_history_id >= 3550000000 AND bh.bene_history_id <= 3575000000"
"WHERE bh.bene_history_id >= 3575000000 AND bh.bene_history_id <= 3600000000"
"WHERE bh.bene_history_id >= 3600000000 AND bh.bene_history_id <= 3625000000"
"WHERE bh.bene_history_id >= 3625000000 AND bh.bene_history_id <= 3650000000"
"WHERE bh.bene_history_id >= 3650000000 AND bh.bene_history_id <= 3675000000"
"WHERE bh.bene_history_id >= 3675000000 AND bh.bene_history_id <= 3700000000"
"WHERE bh.bene_history_id >= 3700000000 AND bh.bene_history_id <= 3725000000"
"WHERE bh.bene_history_id >= 3725000000 AND bh.bene_history_id <= 3750000000"
"WHERE bh.bene_history_id >= 3750000000 AND bh.bene_history_id <= 3775000000"
"WHERE bh.bene_history_id >= 3775000000 AND bh.bene_history_id <= 3800000000"
"WHERE bh.bene_history_id >= 3800000000 AND bh.bene_history_id <= 3825000000"
"WHERE bh.bene_history_id >= 3825000000 AND bh.bene_history_id <= 3850000000"
"WHERE bh.bene_history_id >= 3850000000 AND bh.bene_history_id <= 3875000000"
"WHERE bh.bene_history_id >= 3875000000 AND bh.bene_history_id <= 3900000000"
"WHERE bh.bene_history_id >= 3900000000 AND bh.bene_history_id <= 3925000000"
"WHERE bh.bene_history_id >= 3925000000 AND bh.bene_history_id <= 3950000000"
"WHERE bh.bene_history_id >= 3950000000 AND bh.bene_history_id <= 3975000000"
"WHERE bh.bene_history_id >= 3975000000 AND bh.bene_history_id <= 4000000000"
"WHERE bh.bene_history_id >= 4000000000 AND bh.bene_history_id <= 4025000000")

# sql variable used for record counts
read -r -d '' FROM_SQL << EOM
DELETE
from beneficiaries_history
where bene_history_id
in (
  select bh.bene_history_id
  from beneficiaries_history bh
  inner join beneficiaries b on b.bene_id = bh.bene_id
EOM

# sql that compares BENEFICIARIES_HISTORY vs. BENEFICIARIES columns;
# we are trying to eliminate spurious rcds in bene_history which
# that should NEVER HAVE BEEN THERE IN THE FIRST PLACE!!!
read -r -d '' AND_SQL << EOM
  AND bh.hicn_unhashed = b.hicn_unhashed
  AND bh.mbi_num = b.mbi_num
  AND bh.bene_crnt_hic_num = b.bene_crnt_hic_num
  AND bh.mbi_hash = b.mbi_hash
  AND bh.bene_birth_dt = b.bene_birth_dt
  AND bh.bene_sex_ident_cd = b.bene_sex_ident_cd
  AND (
    bh.efctv_bgn_dt = b.efctv_bgn_dt
    OR
    bh.efctv_bgn_dt is null
  )
  AND (
    bh.efctv_end_dt = b.efctv_end_dt
    OR
    bh.efctv_end_dt is null
  )
);
EOM

# sql to find duplicate rows in BENEFICIARIES_HISTORY table; it does
# this by aggregating and partitioning results into a record + ROW_NUMBER,
# and then accessing all rows whose ROWNUM gt 1; those are duplicate rows.
# As an aside, we couldn't use this approach right away due to the shear
# volume of data; this query will cleanup any 'leftovers' that made it past
# STEP 2 (below). The variable, KEITHS_SQL is in honer of Ketih Adkins who
# wrote original BENEFICIARIES_HISTORY query that is mostly used here.
read -r -d '' KEITHS_SQL << EOM
DELETE
from beneficiaries_history
where bene_history_id
in (
  SELECT dups.bene_hist_id FROM (
    SELECT ROW_NUMBER() OVER(PARTITION BY 
      bene_id
    , hicn_unhashed
    , mbi_num
    , efctv_bgn_dt
    , efctv_end_dt
    , bene_crnt_hic_num
    , mbi_hash
    , bene_birth_dt
    , bene_sex_ident_cd
      ORDER BY bene_history_id asc) AS row,
        bene_history_id as bene_hist_id,
      *
    FROM
      beneficiaries_history t1
      WHERE t1.bene_history_id > 0
    ) dups 
  WHERE dups.row > 1
);
EOM

# belt-and-suspenders to check connection to database.
echo "Testing db connectivity..."
now=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --quiet --tuples-only -c "select NOW();")
if [[ "$now" == *"202"* ]]; then
  echo "db connectivity: OK"
else
  echo "db connectivity: FAILED...exiting"
  exit 1;
fi

# index value used to display the bene_history_id range from the BENES_WHERE array
COUNTER=0
# keep track of the total count of records that should not have been created.
TOT_COUNT=0

# STEP 1
# ---------------------------------------------------------
# Get a count of records in the BENEFICIARIES_HISTORY table.
# ---------------------------------------------------------
echo "checking current record counts..."
SQL=$(printf "%s\n\n%s" "${PARALLEL_SQL}" "SELECT COUNT(*) FROM BENEFICIARIES_HISTORY")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "%s : Start count of records in BENEFICIARIES_HISTORY : %d\n\n" "${now}" "${CNT}";
fi

# STEP 2
# ---------------------------------------------------------
# Initial cleanup of rcds that are dead-nuts wrong! Essentially
# this cleanup compares rcds in the BENEFICIARIES_HISTORY vs.
# a rcd in the BENEFICIARIES table on those table columns that
# are (were) compared when creating a BENEFICIARIES_HISTORY rcd.
# However, due to some CCW data thrashing in which the EFCTV_BGN_DT
# column in the BENEFICIARIES table would alternate between a null
# value and a non-null value (weekly vs monthly extracts), causing
# spurious BENEFICIARIES_HISTORY rcds to be created, when in fact
# nothing in the BENEFICIARIES data really changed and therefore
# BENEFICIARIES_HISTORY should not have been created!!!
# ---------------------------------------------------------

# we'll iterate over the BENES_WHERE array until we are done.
echo "Begin processing of Step 2 at: $(date +'%T')"

# use the BENES_TEST subset for testing; otherwise use the
# BENES_WHERE array for the real thing.
#for MY_WHERE in "${TEST_WHERE[@]}"; do

for MY_WHERE in "${BENES_WHERE[@]}"; do
  ((COUNTER++))
  SQL=$(printf "%s\n  %s\n  %s" "${FROM_SQL}" "${MY_WHERE}" "${AND_SQL}")
  CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
  # if we got a value back from the sql, we'll log some info
  if [ -n "${CNT}" ]; then
    # if we are deleting rcds, CNT will be "DELETE nnn" so we'll
	  # simply nuke the "DELETE "" part of it to get a number.
    CNT=${CNT#"DELETE "}
  	TOT_COUNT=$((TOT_COUNT+CNT))
    now=$(date +'%T')
	  printf "%s : Batch[%d] Count: %-7d     Running Total Count: %d\n" "${now}" "${COUNTER}" "${CNT}" "${TOT_COUNT}";
  fi
  if [ "${SHOW_SQL}" = true ] ; then
	  printf "==========================\n%s\n==========================\n\n" "${SQL}"	
  fi
done
echo "Finished processing of Step 2 at: $(date +'%T.%31')"

# STEP 3
# ---------------------------------------------------------
# BENEFICIARIES_HISTORY data has been pruned down to a
# manageable state; however, there is still some data that
# needs pruning because there are dupe rows that fail the
# BENEFICIARIES vs. BENEFICIARIES_HISTORY criteria. For
# a real example, check BENE_ID 526; the BENE_BIRTH_DT
# differs between the two tables. This results in numerous
# dupe rows in the BENEFICIARIES_HISTORY table, so we'll
# tackle duplicate rows there using a query that can now
# run on the slim-downed BENEFICIARIES_HISTORY table.
# ---------------------------------------------------------
echo "Begin processing of Step 3 at: $(date +'%T')"
if [ "${SHOW_SQL}" = true ] ; then
  printf "==========================\n%s\n==========================\n\n" "${KEITHS_SQL}"	
fi
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${KEITHS_SQL}")
# if we got a value back from the sql, we'll log some info
if [ -n "${CNT}" ]; then
  # if we are deleting rcds, CNT will be "DELETE nnn" so we'll
  # simply nuke the "DELETE "" part of it to get a number.
  CNT=${CNT#"DELETE "}
  TOT_COUNT=$((TOT_COUNT+CNT))
fi
echo "Finished processing of Step 3 at: $(date +'%T.%31')"

# STEP 4
# ---------------------------------------------------------
# Perform VACUUM on BENEFICIARIES_HISTORY to reclaim space;
# DO NOT use FULL parameter as this will lock the table from
# readers.
# ---------------------------------------------------------
echo "Begin processing of Step 4 at: $(date +'%T')"
SQL="VACUUM ANALYZE BENEFICIARIES_HISTORY"
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" -c "${SQL}")
echo "Finished processing of Step 4 at: $(date +'%T.%31')"

# STEP 5
# ---------------------------------------------------------
# Get a count of records in the BENEFICIARIES_HISTORY table.
# ---------------------------------------------------------
echo "checking current record counts, post VACUUM..."
SQL=$(printf "%s\n\n%s" "${PARALLEL_SQL}" "SELECT COUNT(*) FROM BENEFICIARIES_HISTORY")
CNT=$(psql -h "${PGHOST}" -U "${PGUSER}" -d "${PGDATABASE}" --tuples-only -c "${SQL}")
if [ -n "${CNT}" ]; then
  now=$(date +'%T')
  printf "\n%s : End count of records in BENEFICIARIES_HISTORY : %d\n\n" "${now}" "${CNT}";
fi
exit 0;