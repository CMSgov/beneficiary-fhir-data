select
        '${table_name}' as table_name,
        ordinal_position,
        column_name,
        data_type,
        character_maximum_length,
        is_nullable
    from INFORMATION_SCHEMA.COLUMNS
    WHERE table_schema = '${schema}' AND table_name = '${table_name}'