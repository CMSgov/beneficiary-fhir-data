#!/bin/bash

set -o pipefail
#set -e

read -r -d '' COUNT_SQL << EOM
select count(*)
from "BeneficiariesHistory" bh,
(
	select
		"beneficiaryId",
		"birthDate",
		"sex",
		"hicnUnhashed",
		"medicareBeneficiaryId",
		"mbiEffectiveDate",
		"mbiObsoleteDate"
	from
		"Beneficiaries"
) v1
where bh."beneficiaryId" = v1."beneficiaryId"
and bh."sex" = v1."sex"
and bh."birthDate" = v1."birthDate"
and bh."hicnUnhashed" = v1."hicnUnhashed"
and bh."medicareBeneficiaryId" = v1."medicareBeneficiaryId";
EOM


read -r -d '' DELETE_SQL << EOM
delete
from "BeneficiariesHistory"
where "beneficiaryHistoryId"
in (
	select "beneficiaryHistoryId"
	from "BeneficiariesHistory" bh,
	(
		select
			"beneficiaryId",
			"birthDate",
			"sex",
			"hicnUnhashed",
			"medicareBeneficiaryId"
		from
			"Beneficiaries"
	) v1
	where bh."beneficiaryId" = v1."beneficiaryId"
	and bh."sex" = v1."sex"
	and bh."birthDate" = v1."birthDate"
	and bh."hicnUnhashed" = v1."hicnUnhashed"
	and bh."medicareBeneficiaryId" = v1."medicareBeneficiaryId"
);
EOM

DB_ACTION="${1:-count}"
if [[ $(fgrep -ix $DB_ACTION <<< "delete") ]]; then
	SQL="${DELETE_SQL}"
else
	SQL="${COUNT_SQL}"
fi

# following must be passed in as either environment variables or as cmd-line args (default)
PGHOST="${DB_HOST:-$2}"
PGUSER="${DB_USER:-$3}"
PGPASSWORD="${DB_PSWD:-$4}"

# other vars that skirt security (a bit)
PGDATABASE="${DB_NAME:-fhirdb}"
export PGPORT="${DB_PORT:-5432}"

if [ -z "$PGHOST" ] || [ -z "$PGUSER" ] || [ -z "$PGPASSWORD" ]; then
    echo "*****  E r r o r - Missing required variables  *****"
    echo "$0 requires ENV variable(s) for: DB_SCRIPT, DB_HOST, DB_USER, DB_PSWD";
    echo "or";
    echo "$0 requires cmd-line args for: <db script> <db host> <db username> <db password>";
    exit 1;
fi

echo "Testing db connectivity..."
now=$(psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" --quiet --tuples-only -c "select NOW();")
if [[ "$now" == *"202"* ]]; then
  echo "db connectivity: OK"
else
  echo "db connectivity: FAILED...exiting"
  exit 1;
fi

echo "Begin processing at: $(date +'%T')"
CNT=$(psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" --tuples-only -c "$SQL")


echo "All DONE at: $(date +'%T.%31')"
if [ ! -z "${CNT}" ]; then
	echo "TOTAL records processed: $CNT";
fi

exit 0;