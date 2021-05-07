copy public.ccw_load_temp
FROM '/Users/colinchristophermackenzie/dev/bfd766/sql/test/ccw_load_test_data_nohdr_quotes.csv'
DELIMITER ',' CSV QUOTE as '"';