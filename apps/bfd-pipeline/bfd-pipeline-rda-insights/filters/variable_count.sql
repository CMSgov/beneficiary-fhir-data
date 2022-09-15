select
        '${table_name}${ordinal_position}_${column_name}' as variable_name,
        count(*) as num_rows,
        count(distinct ${column_name}) as num_distinct_,
        sum(case when ${column_name} is null then 1 else 0 end) as num_null_,
        (cast(sum(case when ${column_name} is null then 1 else 0 end) as float) / cast(count(*) as float) * 100) as perc_nulls
    from ${schema}.${table_name}
    where current_date - date(last_updated) < 30
    group by 1;