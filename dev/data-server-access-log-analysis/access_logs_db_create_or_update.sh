#!/bin/sh

# Check to see if we are running in Cygwin.
uname="$(uname 2>/dev/null)"
if [ -z "${uname}" ]; then uname="$(/usr/bin/uname 2>/dev/null)"; fi
if [ -z "${uname}" ]; then echo "Unable to find uname." >&2; exit 1; fi
case "${uname}" in
	CYGWIN*) cygwin=true ;;
	*) cygwin=false ;;
esac

# In Cygwin, some of the args will have unescaped backslashes. Fix that.
if [ "${cygwin}" = true ]; then
	set -- "$(printf '%s' "${@}" | sed -e '/\\/\\\\/g')"
fi

# Verify that all required options were specified.
if [ -z "${1}" ]; then >&2 echo 'Specify the DB file to create.'; exit 1; fi
targetDb="${1}"

# In Cygwin, some of those paths will come in as Windows-formatted. Fix that.
if [ "${cygwin}" = true ]; then
	targetDb="$(cygpath --unix "${targetDb}")"
fi

# Verify that the target DB doesn't already exist.
if [ -f "${targetDb}" ]; then
	echo >&2 "Target DB already exists."
	exit 1
fi

# Exit immediately if something fails.
set -e
error() {
	parent_lineno="$1"
	message="$2"
	code="${3:-1}"

	if [ -n "$message" ] ; then
			>&2 echo "Error on or near line ${parent_lineno} of file $(basename "${0}"): ${message}."
	else
			>&2 echo "Error on or near line ${parent_lineno} of file $(basename "${0}")."
	fi

	>&2 echo "Exiting with status ${code}."
	exit "${code}"
}
trap 'error ${LINENO}' EXIT

findSqlite3() {
  sqlite3Homebrew='/usr/local/opt/sqlite/bin/sqlite3'
  if [ -x "${sqlite3Homebrew}" ]; then
    echo "${sqlite3Homebrew}"
  else
    echo 'sqlite3'
  fi
}

sqlite3="$(findSqlite3)"

# Create the DB with its schema.
"${sqlite3}" "${targetDb}" <<EOF
-- Stores Data Server access log entries.
CREATE TABLE IF NOT EXISTS access_log (
	local_host_name TEXT NOT NULL,
	remote_host_name TEXT NOT NULL,
	remote_logical_username TEXT,
	remote_authenticated_user TEXT,
	timestamp TEXT NOT NULL,
	request TEXT NOT NULL,
	query_string TEXT,
	status_code INTEGER NOT NULL,
	bytes INTEGER NOT NULL,
	duration_milliseconds INTEGER NOT NULL,
	original_query_id TEXT,
	original_query_counter INTEGER,
	original_query_timestamp TEXT,
	developer_id INTEGER,
	developer_name TEXT,
	application_id INTEGER,
	application TEXT,
	user_id TEXT,
	user TEXT,
	beneficiary_id INTEGER
);

-- Adds helper fields to 'access_log':
-- * timestamp_month
-- * timestamp_day
-- * timestamp_hour
-- * timestamp_minute
-- * timestamp_second
-- * request_operation_type
DROP VIEW IF EXISTS access_log_extra;
CREATE VIEW access_log_extra
	AS
	SELECT
		local_host_name,
		remote_host_name,
		remote_logical_username,
		remote_authenticated_user,
		timestamp,
		strftime('%Y-%m', timestamp) AS timestamp_month,
		strftime('%Y-%m-%d', timestamp) AS timestamp_day,
		strftime('%Y-%m-%dT%H', timestamp) AS timestamp_hour,
		strftime('%Y-%m-%dT%H:%M', timestamp) AS timestamp_minute,
		strftime('%Y-%m-%dT%H:%M:%S', timestamp) AS timestamp_second,
		CASE
			WHEN request LIKE '%/metadata%' THEN 'metadata'
			WHEN request LIKE '%/Patient%' AND query_string NOT LIKE '%identifier=%' THEN 'patient_by_id'
			WHEN request LIKE '%/Patient%' AND query_string LIKE '%identifier=%' THEN 'patient_by_identifier'
			WHEN request LIKE '%/Coverage%' AND query_string LIKE '%beneficiary=%' THEN 'coverage_by_patient_id'
			WHEN request LIKE '%/ExplanationOfBenefit%' AND query_string LIKE '%patient=%' AND query_string NOT LIKE '%_count=%' THEN 'eob_by_patient_id_all'
			WHEN request LIKE '%/ExplanationOfBenefit%' AND query_string LIKE '%patient=%' AND query_string LIKE '%_count=%' THEN 'eob_by_patient_id_paged'
			ELSE NULL
		END request_operation_type,
		request,
		query_string,
		status_code,
		bytes,
		duration_milliseconds,
		original_query_id,
		original_query_counter,
		original_query_timestamp,
		developer_id,
		developer_name,
		application_id,
		application,
		user_id,
		user,
		beneficiary_id
	FROM
		access_log;

-- Calculates percentile response times for each 'request_operation_type', over all time.
DROP VIEW IF EXISTS access_log_percentiles_all;
CREATE VIEW
	access_log_percentiles_all (
		request_operation_type,
		date_range_start,
		date_range_end,
		count,
		duration_milliseconds_p50,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p100
	)
	AS
	WITH
		requests_metadata(timestamp, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, request_operation_type, duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'metadata' ORDER BY duration_milliseconds ASC),
		requests_patient_by_id(timestamp, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, request_operation_type, duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'patient_by_id' ORDER BY duration_milliseconds ASC),
		requests_patient_by_identifier(timestamp, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, request_operation_type, duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'patient_by_identifier' ORDER BY duration_milliseconds ASC),
		requests_coverage_by_patient_id(timestamp, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, request_operation_type, duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'coverage_by_patient_id' ORDER BY duration_milliseconds ASC),
		requests_eob_by_patient_id_all(timestamp, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, request_operation_type, duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all' ORDER BY duration_milliseconds ASC),
		requests_eob_by_patient_id_paged(timestamp, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, request_operation_type, duration_milliseconds FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_paged' ORDER BY duration_milliseconds ASC)
	SELECT
			request_operation_type,
			MIN(timestamp),
			MAX(timestamp),
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) - 1)
		FROM requests_metadata
	UNION ALL
	SELECT
			request_operation_type,
			MIN(timestamp),
			MAX(timestamp),
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id) - 1)
		FROM requests_patient_by_id
	UNION ALL
	SELECT
			request_operation_type,
			MIN(timestamp),
			MAX(timestamp),
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier) - 1)
		FROM requests_patient_by_identifier
	UNION ALL
	SELECT
			request_operation_type,
			MIN(timestamp),
			MAX(timestamp),
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id) - 1)
		FROM requests_coverage_by_patient_id
	UNION ALL
	SELECT
			request_operation_type,
			MIN(timestamp),
			MAX(timestamp),
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all) - 1)
		FROM requests_eob_by_patient_id_all
	UNION ALL
	SELECT
			request_operation_type,
			MIN(timestamp),
			MAX(timestamp),
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged) - 1)
		FROM requests_eob_by_patient_id_paged;

-- Doesn't work. Not sure why, but SQLite throws odd errors.
DROP VIEW IF EXISTS access_log_percentiles_monthly;
CREATE VIEW
	access_log_percentiles_monthly (
		request_operation_type,
		month,
		count,
		duration_milliseconds_p50,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p100
	)
	AS
	WITH
		months(month, month_start, month_end) AS
			(SELECT strftime('%Y-%m', timestamp), date(timestamp, 'start of month'), date(timestamp, 'start of month','+1 month','-1 day') FROM access_log GROUP BY strftime('%Y-%m', timestamp) ORDER BY timestamp ASC),
		requests_metadata(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'metadata' ORDER BY duration_milliseconds ASC),
		requests_eob_by_patient_id(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'eob_by_patient_id_all' ORDER BY duration_milliseconds ASC)
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata) - 1)
		FROM requests_metadata
	UNION ALL
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id AS inner
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id) - 1)
		FROM requests_eob_by_patient_id;

-- Doesn't work. Not sure why, but SQLite throws odd errors.
DROP VIEW IF EXISTS access_log_percentiles_monthly2;
CREATE VIEW
	access_log_percentiles_monthly2 (
		request_operation_type,
		month,
		count,
		duration_milliseconds_p50,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p100
	)
	AS
	WITH
		months(month, month_start, month_end) AS
			(SELECT strftime('%Y-%m', timestamp), date(timestamp, 'start of month'), date(timestamp, 'start of month','+1 month','-1 day') FROM access_log GROUP BY strftime('%Y-%m', timestamp) ORDER BY timestamp ASC),
		requests_metadata(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'metadata' ORDER BY duration_milliseconds ASC),
		requests_patient_by_id(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'patient_by_id' ORDER BY duration_milliseconds ASC),
		requests_patient_by_identifier(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'patient_by_identifier' ORDER BY duration_milliseconds ASC),
		requests_coverage_by_patient_id(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'coverage_by_patient_id' ORDER BY duration_milliseconds ASC),
		requests_eob_by_patient_id_all(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'eob_by_patient_id_all' ORDER BY duration_milliseconds ASC),
		requests_eob_by_patient_id_paged(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'eob_by_patient_id_paged' ORDER BY duration_milliseconds ASC)
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) - 1)
		FROM requests_metadata AS outer
		GROUP BY month
	UNION ALL
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_id AS inner WHERE inner.month = month) - 1)
		FROM requests_patient_by_id AS outer
		GROUP BY month
	UNION ALL
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_patient_by_identifier AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_patient_by_identifier AS inner WHERE inner.month = month) - 1)
		FROM requests_patient_by_identifier AS outer
		GROUP BY month
	UNION ALL
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_coverage_by_patient_id AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_coverage_by_patient_id AS inner WHERE inner.month = month) - 1)
		FROM requests_coverage_by_patient_id AS outer
		GROUP BY month
	UNION ALL
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_all AS inner WHERE inner.month = month) - 1)
		FROM requests_eob_by_patient_id_all AS outer
		GROUP BY month
	UNION ALL
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_eob_by_patient_id_paged AS inner WHERE inner.month = month) - 1)
		FROM requests_eob_by_patient_id_paged AS outer
		GROUP BY month;

-- Doesn't work. Not sure why, but SQLite throws odd errors.
DROP VIEW IF EXISTS access_log_percentiles_monthly_for_metadata;
CREATE VIEW
	access_log_percentiles_monthly_for_metadata (
		request_operation_type,
		month,
		count,
		duration_milliseconds_p50,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p100
	)
	AS
	WITH
		months(month, month_start, month_end) AS
			(SELECT strftime('%Y-%m', timestamp), date(timestamp, 'start of month'), date(timestamp, 'start of month','+1 month','-1 day') FROM access_log GROUP BY strftime('%Y-%m', timestamp) ORDER BY timestamp ASC),
		requests_metadata(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'metadata' ORDER BY duration_milliseconds ASC)
	SELECT
			request_operation_type,
			month,
			COUNT(*),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) * 50 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) * 90 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) * 99 / 100 - 1),
			(SELECT inner.duration_milliseconds FROM requests_metadata AS inner WHERE inner.month = outer.month
				LIMIT 1 OFFSET (SELECT COUNT(*) FROM requests_metadata AS inner WHERE inner.month = month) - 1)
		FROM requests_metadata AS outer
		GROUP BY month;

-- Doesn't work. Not sure why, but SQLite throws odd errors.
DROP VIEW IF EXISTS access_log_percentiles_monthly_for_eob_by_patient_id_all;
CREATE VIEW
	access_log_percentiles_monthly_for_eob_by_patient_id_all (
		request_operation_type,
		month,
		count,
		duration_milliseconds_p99_index,
		duration_milliseconds_p99
	)
	AS
	WITH
		months(month, month_start, month_end) AS
			(SELECT strftime('%Y-%m', timestamp), date(timestamp, 'start of month'), date(timestamp, 'start of month','+1 month','-1 day') FROM access_log GROUP BY strftime('%Y-%m', timestamp) ORDER BY timestamp ASC),
		requests_eob_by_patient_id_all(timestamp, month, request_operation_type, duration_milliseconds) AS
			(SELECT timestamp, month, request_operation_type, duration_milliseconds FROM access_log_extra INNER JOIN months ON strftime('%Y-%m', access_log_extra.timestamp) = months.month WHERE request_operation_type = 'eob_by_patient_id_all' ORDER BY duration_milliseconds ASC)
	SELECT
			request_operation_type,
			month,
			count(*),
			count(*) * 99 / 100 - 1,
			(
				SELECT
					duration_milliseconds
				FROM
					(
						SELECT
							duration_milliseconds,
							row_number() OVER (ORDER BY duration_milliseconds ASC) AS rn,
							count(*) OVER (ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS rc
						FROM requests_eob_by_patient_id_all AS inner
						WHERE
							inner.month = outer.month
					)
				WHERE
					rn = rc * 99 / 100 - 1
			)
		FROM requests_eob_by_patient_id_all AS outer
		GROUP BY outer.month;

-- Helper view that "bins" access_log_extra entries by 'month' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned;
CREATE VIEW
	access_log_binned (
		month,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		strftime('%Y-%m', timestamp),
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_month, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		strftime('%Y-%m', timestamp) ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Helper view that "bins" access_log_extra entries by 'hour' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_hourly;
CREATE VIEW
	access_log_binned_hourly (
		hour,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		strftime('%Y-%m-%dT%H', timestamp),
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_hour, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		strftime('%Y-%m-%dT%H', timestamp) ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Finally have a query that appears to work!
-- Unfortunately, it runs so slowly that it may never complete.  :-(
DROP VIEW IF EXISTS access_log_percentiles_monthly3;
CREATE VIEW
	access_log_percentiles_monthly3 (
		request_operation_type,
		month,
		count,
		duration_milliseconds_p25_index,
		duration_milliseconds_p25,
		duration_milliseconds_p50_index,
		duration_milliseconds_p50,
		duration_milliseconds_p75_index,
		duration_milliseconds_p75,
		duration_milliseconds_p90_index,
		duration_milliseconds_p90,
		duration_milliseconds_p99_index,
		duration_milliseconds_p99,
		duration_milliseconds_p999_index,
		duration_milliseconds_p999,
		duration_milliseconds_p100_index,
		duration_milliseconds_p100
	)
	AS
	SELECT
		base.month,
		base.request_operation_type,
		base.bin_count,
		max(1, CAST(0.25 * base.bin_count AS int)),
		p25.duration_milliseconds,
		max(1, CAST(0.50 * base.bin_count AS int)),
		p50.duration_milliseconds,
		max(1, CAST(0.75 * base.bin_count AS int)),
		p75.duration_milliseconds,
		max(1, CAST(0.90 * base.bin_count AS int)),
		p90.duration_milliseconds,
		max(1, CAST(0.99 * base.bin_count AS int)),
		p99.duration_milliseconds,
		max(1, CAST(0.999 * base.bin_count AS int)),
		p999.duration_milliseconds,
		base.bin_count,
		p100.duration_milliseconds
	FROM access_log_binned AS base
	LEFT JOIN access_log_binned AS p25 ON p25.month = base.month AND p25.request_operation_type = base.request_operation_type AND base.bin_index = max(1, CAST(0.25 * base.bin_count AS int))
	LEFT JOIN access_log_binned AS p50 ON p50.month = base.month AND p50.request_operation_type = base.request_operation_type AND base.bin_index = max(1, CAST(0.50 * base.bin_count AS int))
	LEFT JOIN access_log_binned AS p75 ON p75.month = base.month AND p75.request_operation_type = base.request_operation_type AND base.bin_index = max(1, CAST(0.75 * base.bin_count AS int))
	LEFT JOIN access_log_binned AS p90 ON p90.month = base.month AND p90.request_operation_type = base.request_operation_type AND base.bin_index = max(1, CAST(0.90 * base.bin_count AS int))
	LEFT JOIN access_log_binned AS p99 ON p99.month = base.month AND p99.request_operation_type = base.request_operation_type AND base.bin_index = max(1, CAST(0.99 * base.bin_count AS int))
	LEFT JOIN access_log_binned AS p999 ON p999.month = base.month AND p999.request_operation_type = base.request_operation_type AND base.bin_index = max(1, CAST(0.999 * base.bin_count AS int))
	LEFT JOIN access_log_binned AS p100 ON p100.month = base.month AND p100.request_operation_type = base.request_operation_type AND base.bin_index = base.bin_count
	GROUP BY
		base.request_operation_type,
		base.month,
		base.bin_count
	ORDER BY
		base.month ASC,
		base.request_operation_type ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'month' + 'request_operation_type'.
-- Note: This view works! And reasonably quickly! Takes 486 seconds to query it, when run run on pdcw10ap01 with 16993177 access log entries from 2017-10-27 through 2018-12-17.
DROP VIEW IF EXISTS access_log_percentiles_monthly4;
CREATE VIEW
	access_log_percentiles_monthly4 (
		month,
		request_operation_type,
		count,
		duration_milliseconds_p25_index,
		duration_milliseconds_p25,
		duration_milliseconds_p50_index,
		duration_milliseconds_p50,
		duration_milliseconds_p75_index,
		duration_milliseconds_p75,
		duration_milliseconds_p90_index,
		duration_milliseconds_p90,
		duration_milliseconds_p99_index,
		duration_milliseconds_p99,
		duration_milliseconds_p999_index,
		duration_milliseconds_p999,
		duration_milliseconds_p100_index,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(month, request_operation_type, bin_count) AS
			(SELECT month, request_operation_type, bin_count FROM access_log_binned GROUP BY month, request_operation_type, bin_count)
	SELECT
		inner.month,
		inner.request_operation_type,
		inner.bin_count,
		max(1, CAST(0.25 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.50 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.75 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.90 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.99 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.999 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(inner.bin_count),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned AS outer ON inner.month = outer.month AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.month,
		inner.bin_count
	ORDER BY
		inner.month ASC,
		inner.request_operation_type ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'hour' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_percentiles_hourly;
CREATE VIEW
	access_log_percentiles_hourly (
		hour,
		request_operation_type,
		count,
		duration_milliseconds_p25_index,
		duration_milliseconds_p25,
		duration_milliseconds_p50_index,
		duration_milliseconds_p50,
		duration_milliseconds_p75_index,
		duration_milliseconds_p75,
		duration_milliseconds_p90_index,
		duration_milliseconds_p90,
		duration_milliseconds_p99_index,
		duration_milliseconds_p99,
		duration_milliseconds_p999_index,
		duration_milliseconds_p999,
		duration_milliseconds_p100_index,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(hour, request_operation_type, bin_count) AS
			(SELECT hour, request_operation_type, bin_count FROM access_log_binned_hourly GROUP BY hour, request_operation_type, bin_count)
	SELECT
		inner.hour,
		inner.request_operation_type,
		inner.bin_count,
		max(1, CAST(0.25 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.50 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.75 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.90 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.99 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(1, CAST(0.999 * inner.bin_count AS int)),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(inner.bin_count),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned_hourly AS outer ON inner.hour = outer.hour AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.hour,
		inner.bin_count
	ORDER BY
		inner.hour ASC,
		inner.request_operation_type ASC;
EOF

# Exit normally (don't trip the error handler).
trap - EXIT
