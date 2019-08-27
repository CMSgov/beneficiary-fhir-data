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
		CASE
			WHEN duration_milliseconds <= 0 THEN 0
			ELSE bytes / duration_milliseconds
		END bytes_per_millisecond,
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

-- Helper view that "bins" access_log_extra entries by 'timestamp_month' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_duration_milliseconds_by_month;
CREATE VIEW
	access_log_binned_duration_milliseconds_by_month (
		timestamp_month,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		timestamp_month,
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_month, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		timestamp_month ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Helper view that "bins" access_log_extra entries by 'timestamp_day' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_duration_milliseconds_by_day;
CREATE VIEW
	access_log_binned_duration_milliseconds_by_day (
		timestamp_day,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		timestamp_day,
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_day, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		timestamp_day ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Helper view that "bins" access_log_extra entries by 'local_host_name', 'timestamp_day', 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_duration_milliseconds_by_host_and_day;
CREATE VIEW
	access_log_binned_duration_milliseconds_by_host_and_day (
		timestamp_day,
		local_host_name,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		timestamp_day,
		local_host_name,
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_day, local_host_name, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		timestamp_day ASC,
		local_host_name ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Helper view that "bins" access_log_extra entries by 'timestamp_hour' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_duration_milliseconds_by_hour;
CREATE VIEW
	access_log_binned_duration_milliseconds_by_hour (
		timestamp_hour,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		timestamp_hour,
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_hour, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		timestamp_hour ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Helper view that "bins" access_log_extra entries by 'timestamp_hour' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'bytes_per_millisecond'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_bytes_per_millisecond_by_hour;
CREATE VIEW
	access_log_binned_bytes_per_millisecond_by_hour (
		timestamp_hour,
		request_operation_type,
		bytes_per_millisecond,
		bin_index,
		bin_count
	)
	AS
	SELECT
		timestamp_hour,
		request_operation_type,
		bytes_per_millisecond,
		row_number() OVER bins,
		count(*) OVER bins
	FROM access_log_extra
	WINDOW bins AS (PARTITION BY timestamp_hour, request_operation_type ORDER BY bytes_per_millisecond ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		timestamp_hour ASC,
		request_operation_type ASC,
		bytes_per_millisecond ASC;

-- Helper view that "bins" access_log_extra entries by 'timestamp_minute' and 'request_operation_type':
-- each entry within one of those bins gets an index assigned to it, ordered by 'duration_milliseconds'.
-- This allows for simpler percentile calculations in other views.
DROP VIEW IF EXISTS access_log_binned_duration_milliseconds_by_minute;
CREATE VIEW
	access_log_binned_duration_milliseconds_by_minute (
		timestamp_minute,
		request_operation_type,
		duration_milliseconds,
		bin_index,
		bin_count
	)
	AS
	SELECT
		timestamp_minute,
		request_operation_type,
		duration_milliseconds,
		row_number() OVER binned_durations,
		count(*) OVER binned_durations
	FROM access_log_extra
	WINDOW binned_durations AS (PARTITION BY timestamp_minute, request_operation_type ORDER BY duration_milliseconds ASC ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
	ORDER BY
		timestamp_minute ASC,
		request_operation_type ASC,
		duration_milliseconds ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'timestamp_month' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_duration_milliseconds_percentiles_by_month;
CREATE VIEW
	access_log_duration_milliseconds_percentiles_by_month (
		timestamp_month,
		request_operation_type,
		count_for_all_operation_types,
		count_for_operation_type,
		duration_milliseconds_p25,
		duration_milliseconds_p50,
		duration_milliseconds_p75,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p999,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(timestamp_month, request_operation_type, bin_count) AS
			(SELECT timestamp_month, request_operation_type, bin_count FROM access_log_binned_duration_milliseconds_by_month GROUP BY timestamp_month, request_operation_type, bin_count)
	SELECT
		inner.timestamp_month,
		inner.request_operation_type,
		sum(inner.bin_count) OVER bin_all_operation_types,
		inner.bin_count,
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned_duration_milliseconds_by_month AS outer ON inner.timestamp_month = outer.timestamp_month AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.timestamp_month,
		inner.bin_count
	WINDOW bin_all_operation_types AS (PARTITION BY inner.timestamp_month ORDER BY inner.timestamp_month)
	ORDER BY
		inner.timestamp_month ASC,
		inner.request_operation_type ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'local_host_name' + 'timestamp_day' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_duration_milliseconds_percentiles_by_host_and_day;
CREATE VIEW
	access_log_duration_milliseconds_percentiles_by_host_and_day (
		timestamp_day,
		local_host_name,
		request_operation_type,
		count_for_all_operation_types,
		count_for_operation_type,
		duration_milliseconds_p25,
		duration_milliseconds_p50,
		duration_milliseconds_p75,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p999,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(timestamp_day, local_host_name, request_operation_type, bin_count) AS
			(SELECT timestamp_day, local_host_name, request_operation_type, bin_count FROM access_log_binned_duration_milliseconds_by_host_and_day GROUP BY timestamp_day, request_operation_type, bin_count)
	SELECT
		inner.timestamp_day,
		inner.local_host_name,
		inner.request_operation_type,
		sum(inner.bin_count) OVER bin_all_operation_types,
		inner.bin_count,
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned_duration_milliseconds_by_host_and_day AS outer ON inner.local_host_name = outer.local_host_name AND inner.timestamp_day = outer.timestamp_day AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.timestamp_day,
		inner.local_host_name,
		inner.request_operation_type,
		inner.bin_count
	WINDOW bin_all_operation_types AS (PARTITION BY inner.timestamp_day ORDER BY inner.timestamp_day)
	ORDER BY
		inner.timestamp_day ASC,
		inner.local_host_name ASC,
		inner.request_operation_type ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'timestamp_day' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_duration_milliseconds_percentiles_by_day;
CREATE VIEW
	access_log_duration_milliseconds_percentiles_by_day (
		timestamp_day,
		request_operation_type,
		count_for_all_operation_types,
		count_for_operation_type,
		duration_milliseconds_p25,
		duration_milliseconds_p50,
		duration_milliseconds_p75,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p999,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(timestamp_day, request_operation_type, bin_count) AS
			(SELECT timestamp_day, request_operation_type, bin_count FROM access_log_binned_duration_milliseconds_by_day GROUP BY timestamp_day, request_operation_type, bin_count)
	SELECT
		inner.timestamp_day,
		inner.request_operation_type,
		sum(inner.bin_count) OVER bin_all_operation_types,
		inner.bin_count,
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned_duration_milliseconds_by_day AS outer ON inner.timestamp_day = outer.timestamp_day AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.timestamp_day,
		inner.bin_count
	WINDOW bin_all_operation_types AS (PARTITION BY inner.timestamp_day ORDER BY inner.timestamp_day)
	ORDER BY
		inner.timestamp_day ASC,
		inner.request_operation_type ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'timestamp_hour' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_duration_milliseconds_percentiles_by_hour;
CREATE VIEW
	access_log_duration_milliseconds_percentiles_by_hour (
		timestamp_hour,
		request_operation_type,
		count_for_all_operation_types,
		count_for_operation_type,
		duration_milliseconds_mean,
		duration_milliseconds_p25,
		duration_milliseconds_p50,
		duration_milliseconds_p75,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p999,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(timestamp_hour, request_operation_type, bin_count) AS
			(SELECT timestamp_hour, request_operation_type, bin_count FROM access_log_binned_duration_milliseconds_by_hour GROUP BY timestamp_hour, request_operation_type, bin_count)
	SELECT
		inner.timestamp_hour,
		inner.request_operation_type,
		sum(inner.bin_count) OVER bin_all_operation_types,
		inner.bin_count,
		avg(outer.duration_milliseconds),
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned_duration_milliseconds_by_hour AS outer ON inner.timestamp_hour = outer.timestamp_hour AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.timestamp_hour,
		inner.bin_count
	WINDOW bin_all_operation_types AS (PARTITION BY inner.timestamp_hour ORDER BY inner.timestamp_hour)
	ORDER BY
		inner.timestamp_hour ASC,
		inner.request_operation_type ASC;

-- Calculates the percentiles of 'bytes_per_millisecond' for all responses, binned by 'timestamp_hour' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_bytes_per_millisecond_percentiles_by_hour;
CREATE VIEW
	access_log_bytes_per_millisecond_percentiles_by_hour (
		timestamp_hour,
		request_operation_type,
		count_for_all_operation_types,
		count_for_operation_type,
		bytes_per_millisecond_mean,
		bytes_per_millisecond_p25,
		bytes_per_millisecond_p50,
		bytes_per_millisecond_p75,
		bytes_per_millisecond_p90,
		bytes_per_millisecond_p99,
		bytes_per_millisecond_p999,
		bytes_per_millisecond_p100
	)
	AS
	WITH
		inner(timestamp_hour, request_operation_type, bin_count) AS
			(SELECT timestamp_hour, request_operation_type, bin_count FROM access_log_binned_bytes_per_millisecond_by_hour GROUP BY timestamp_hour, request_operation_type, bin_count)
	SELECT
		inner.timestamp_hour,
		inner.request_operation_type,
		sum(inner.bin_count) OVER bin_all_operation_types,
		inner.bin_count,
		avg(outer.bytes_per_millisecond),
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.bytes_per_millisecond END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.bytes_per_millisecond END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.bytes_per_millisecond END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.bytes_per_millisecond END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.bytes_per_millisecond END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.bytes_per_millisecond END),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.bytes_per_millisecond END)
	FROM inner
	LEFT JOIN access_log_binned_bytes_per_millisecond_by_hour AS outer ON inner.timestamp_hour = outer.timestamp_hour AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.timestamp_hour,
		inner.bin_count
	WINDOW bin_all_operation_types AS (PARTITION BY inner.timestamp_hour ORDER BY inner.timestamp_hour)
	ORDER BY
		inner.timestamp_hour ASC,
		inner.request_operation_type ASC;

-- Calculates the percentiles of 'duration_milliseconds' for all responses, binned by 'timestamp_minute' + 'request_operation_type'.
DROP VIEW IF EXISTS access_log_duration_milliseconds_percentiles_by_minute;
CREATE VIEW
	access_log_duration_milliseconds_percentiles_by_minute (
		timestamp_minute,
		request_operation_type,
		count_for_all_operation_types,
		count_for_operation_type,
		duration_milliseconds_mean,
		duration_milliseconds_p25,
		duration_milliseconds_p50,
		duration_milliseconds_p75,
		duration_milliseconds_p90,
		duration_milliseconds_p99,
		duration_milliseconds_p999,
		duration_milliseconds_p100
	)
	AS
	WITH
		inner(timestamp_minute, request_operation_type, bin_count) AS
			(SELECT timestamp_minute, request_operation_type, bin_count FROM access_log_binned_duration_milliseconds_by_minute GROUP BY timestamp_minute, request_operation_type, bin_count)
	SELECT
		inner.timestamp_minute,
		inner.request_operation_type,
		sum(inner.bin_count) OVER bin_all_operation_types,
		inner.bin_count,
		avg(outer.duration_milliseconds),
		max(CASE outer.bin_index WHEN max(1, CAST(0.25 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.50 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.75 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.90 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.99 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN max(1, CAST(0.999 * inner.bin_count AS int)) THEN outer.duration_milliseconds END),
		max(CASE outer.bin_index WHEN inner.bin_count THEN outer.duration_milliseconds END)
	FROM inner
	LEFT JOIN access_log_binned_duration_milliseconds_by_minute AS outer ON inner.timestamp_minute = outer.timestamp_minute AND inner.request_operation_type = outer.request_operation_type
	GROUP BY
		inner.request_operation_type,
		inner.timestamp_minute,
		inner.bin_count
	WINDOW bin_all_operation_types AS (PARTITION BY inner.timestamp_minute ORDER BY inner.timestamp_minute)
	ORDER BY
		inner.timestamp_minute ASC,
		inner.request_operation_type ASC;
EOF

# Exit normally (don't trip the error handler).
trap - EXIT
