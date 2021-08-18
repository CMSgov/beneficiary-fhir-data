# BFD Database Query Checker Results Statistics

This document just lays out some basic statistics
  that were calculated against the DB Query Checker's result files.

Using [q](http://harelba.github.io/q/) to run some SQL queries against the data:

```
$ find -s . -name '*.csv' -print -exec q --skip-header --delimiter=, --output-header --beautify "SELECT query_id, count(*) AS query_count, count(1) FILTER (WHERE query_succeeded = 'false') AS failures_count, round(avg(query_time_millis)) AS query_time_millis_avg, max(query_time_millis) AS query_time_millis_max FROM {} GROUP BY query_id" \;
./db_query_checker_2021-07-07-T14-31.csv
query_id                                              ,query_count,failures_count,query_time_millis_avg,query_time_millis_max
SelectBeneCountByPartDContractIdAndYearMonth          ,30264      ,1             ,21137.0              ,41705
SelectBeneIdsByPartDContractIdAndYearMonth            ,18446      ,6             ,77618.0              ,131857
SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,233303     ,36            ,42253.0              ,259262
SelectBeneRecordsByBeneIds                            ,251707     ,0             ,41931.0              ,258553
./db_query_checker_2021-07-08-T13-56.csv
query_id                                              ,query_count,failures_count,query_time_millis_avg,query_time_millis_max
SelectBeneCountByPartDContractIdAndYearMonth          ,30264      ,35            ,172.0                ,30241
SelectBeneIdsByPartDContractIdAndYearMonth            ,18412      ,1             ,350.0                ,30273
SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,218984     ,19            ,3152.0               ,35097
SelectBeneRecordsByBeneIds                            ,237376     ,0             ,868.0                ,49612
./db_query_checker_2021-07-09-T17-48.csv
query_id                                              ,query_count,failures_count,query_time_millis_avg,query_time_millis_max
SelectBeneCountByPartDContractIdAndYearMonth          ,30264      ,44            ,181.0                ,30040
SelectBeneIdsByPartDContractIdAndYearMonth            ,18403      ,0             ,323.0                ,24332
SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,222434     ,9             ,3228.0               ,37728
SelectBeneRecordsByBeneIds                            ,240828     ,0             ,821.0                ,116629
./db_query_checker_2021-07-12-T15-04.csv
query_id                                              ,query_count,failures_count,query_time_millis_avg,query_time_millis_max
SelectBeneCountByPartDContractIdAndYearMonth          ,30288      ,109           ,481.0                ,30148
SelectBeneIdsByPartDContractIdAndYearMonth            ,18339      ,0             ,386.0                ,16543
SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,186092     ,3             ,2147.0               ,45822
SelectBeneRecordsByBeneIds                            ,204428     ,0             ,1014.0               ,66450
./db_query_checker_2021-07-20-T13-26.csv
query_id                                              ,query_count,failures_count,query_time_millis_avg,query_time_millis_max
SelectBeneCountByPartDContractIdAndYearMonth          ,30288      ,34            ,111.0                ,30061
SelectBeneIdsByPartDContractIdAndYearMonth            ,18413      ,0             ,165.0                ,22353
SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,218420     ,26            ,3574.0               ,30098
SelectBeneRecordsByBeneIds                            ,236807     ,0             ,9.0                  ,549
```