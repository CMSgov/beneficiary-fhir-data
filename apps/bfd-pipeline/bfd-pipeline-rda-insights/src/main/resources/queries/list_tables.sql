select table_name
    from INFORMATION_SCHEMA.TABLES
    where table_schema = '${schema}'
        and table_name in ('fiss_audit_trails', 'fiss_claims', 'fiss_diagnosis_codes', 'fiss_payers', 'fiss_proc_codes');