# Data Server Access Log Analysis

This is basically a junk drawer of "random scripts we've cobbled together to help analyze our access logs".
Many or most of these bits & pieces will (hopefully) eventually be converted into Splunk reports.
Until then, though, this is what we use when analyzing the performance of the Data Server in production.

### `access_logs_db_build.sh`

Copies down all of our access logs, parses them, and imports them into a local SQLite DB.
Takes something like 4 hours to run?
I don't remember exactly, but basically: start it before going to sleep.

Run it as follows:

    $ cd dev/data-server-access-log-analysis
    $ ./access_logs_db_build.sh

This will produce a `./output/access_logs.sqlite` DB file.
See below for details on interesting things you can do with that DB.

Uses the following scripts as helpers:

* `access_logs_db_create_or_update.sh`:
  (see below)
* `access_logs_parse_to_csv.awk.sh`:
  (see below)
* `print_log_events.py`:
  Not currently used, but will eventually be needed.
  Can pull the access logs from CloudWatch, instead of via `scp`, like `access_logs_db_build.sh` currently does.
  This is slower, but will work once we switch to auto-scaling.
  It can be run as follows:
      
      ```
      pipenv run ./print_log_events.py pd_jboss_access --start 2019-06-26
      ```
      

### `access_logs_db_create_or_update.sh`

Creates (or updates) the SQLite DB used for analysis.
This database can be opened for interactive querying, as follows:

    $ /usr/local/opt/sqlite3/bin/sqlite3 ./output/access_logs.sqlite
    sqlite> SELECT count(*) FROM access_log_extra WHERE timestamp LIKE '20%';
    count(*)
    ----------
    35982651
    Run Time: real 86.138 user 7.291287 sys 9.794020
    sqlite> .quit

Has a bunch of different views, most of which were failed attempts at getting SQLite to perform percentile analysis.
I think I finally got that working with the `access_log_percentiles_monthly4` view, which bins all queries by request type and month, and reports on various response duration percentiles.
For example, the following query returns all percentiles for all query types:

    $ /usr/local/opt/sqlite3/bin/sqlite3 -csv -header \
        ./output/access_logs.sqlite \
        "SELECT * FROM access_log_percentiles_monthly4"

It will take about 21 minutes to run (on my box) and return output like the following:

```
month,request_operation_type,count,duration_milliseconds_p25_index,duration_milliseconds_p25,duration_milliseconds_p50_index,duration_milliseconds_p50,duration_milliseconds_p75_index,duration_milliseconds_p75,duration_milliseconds_p90_index,duration_milliseconds_p90,duration_milliseconds_p99_index,duration_milliseconds_p99,duration_milliseconds_p999_index,duration_milliseconds_p999,duration_milliseconds_p100_index,duration_milliseconds_p100
...
2019-05,NULL,1,1,NULL,1,NULL,1,NULL,1,NULL,1,NULL,1,NULL,1,NULL
2019-05,coverage_by_patient_id,1897,474,3,948,5,1422,9,1707,12,1878,78,1895,219,1897,240
2019-05,eob_by_patient_id_all,170940,42735,496,85470,785,128205,1355,153846,2182,169230,4522,170769,14069,170940,1135139
2019-05,metadata,2612178,653044,0,1306089,0,1959133,0,2350960,0,2586056,1,2609565,1,2612178,2241
2019-05,patient_by_id,29008,7252,4,14504,6,21756,9,26107,12,28717,35,28978,156,29008,8004
2019-05,patient_by_identifier,7274,1818,6,3637,9,5455,12,6546,21,7201,13201,7266,13680,7274,293289
...
```

I later added the `access_log_percentiles_hourly` view, which is bascially the same thing as `access_log_percentiles_monthly4`, except binned by hour.

#### Other Queries

Here's a bunch of other random queries & commands that I see in my SQLite history, which may be useful:

    sqlite> .timer on
    sqlite> .mode column
    sqlite> .headers on
    sqlite> .separator ROW "\n"
    sqlite> .nullvalue NULL
    sqlite> SELECT * FROM access_log_percentiles_monthly4 WHERE request_operation_type = 'eob_by_patient_id_all';
    sqlite> SELECT avg(bytes), max(bytes) FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all';
    sqlite> SELECT avg(bytes), max(bytes) FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all' AND timestamp LIKE '2018-12%';
    sqlite> SELECT count(*) FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all';
    sqlite> SELECT duration_milliseconds, bytes FROM access_log_extra WHERE request_operation_type = 'eob_by_patient_id_all' AND timestamp LIKE '2019-05%';

### `access_logs_parse_to_csv.awk.sh`

This Gawk script (note: definitely requires GNU AWK; not "regular" AWK) parses our Wildfly/JBoss access log format and converts it to CSV.
This is used by `access_logs_db_build.sh` to get our access logs into SQLite.

It may also be useful in the future (with some tweaking) to convert our current access logs to JSON,
  if that's ever something we need to do.

### `hgram_ifier.groovy`

This simple Groovy script reads in response durations (in milliseconds, one value per line)
  and outputs an HGRAM of their distribution, using the most excellent
  <https://github.com/HdrHistogram/HdrHistogram> library.

See the comment at the top of this script for more details,
  but this has quickly become my primary tool for answering the question
  "are we meeting our SLOs, and if not, how far do we have to go?"
HDR histograms are really a **fantastic** analysis tool!