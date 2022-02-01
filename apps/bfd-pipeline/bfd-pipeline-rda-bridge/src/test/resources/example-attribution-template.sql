-- This SQL script is for bookkeeping and auditing purposes
-- This script can be used to add the improved synthea data to opensbx

BEGIN;
DO $$
DECLARE Dev cclf_files.id%TYPE;
DECLARE Small cclf_files.id%TYPE;
DECLARE Large cclf_files.id%TYPE;

-- Adding three rows into cclf_files for three new RDA files associated to synthea (improved data)
BEGIN
INSERT INTO cclf_files (created_at, updated_at, cclf_num, name, aco_cms_id, timestamp, performance_year, import_status, type) VALUES (now(), now(), 8, 'SOME.A.CCLF.NUM', 'A', '2021-07-01', 21, 'Completed', 0) RETURNING id INTO Dev;
INSERT INTO cclf_files (created_at, updated_at, cclf_num, name, aco_cms_id, timestamp, performance_year, import_status, type) VALUES (now(), now(), 8, 'SOME.B.CCLF.NUM', 'B', '2021-07-01', 21, 'Completed', 0) RETURNING id INTO Small;
INSERT INTO cclf_files (created_at, updated_at, cclf_num, name, aco_cms_id, timestamp, performance_year, import_status, type) VALUES (now(), now(), 8, 'SOME.C.CCLF.NUM', 'C', '2021-07-01', 21, 'Completed', 0) RETURNING id INTO Large;

-- Adding rows into cclf_beneficiaries for ####
INSERT INTO cclf_beneficiaries (file_id, mbi)
VALUES
%%Dev-1%%

-- Adding rows into cclf_beneficiaries for ####
INSERT INTO cclf_beneficiaries (file_id, mbi)
VALUES
%%Small-2%%

-- Adding rows into cclf_beneficiaries for ###
INSERT INTO cclf_beneficiaries (file_id, mbi)
VALUES
%%Large-4%%