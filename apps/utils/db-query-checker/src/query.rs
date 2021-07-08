//! Contains utilities for wrapping/monitoring SQLx queries.

use csv_async::AsyncSerializer;
use eyre::{Result, WrapErr};
use lazy_static::lazy_static;
use serde::Serialize;
use sqlx::{
    postgres::{PgArguments, PgRow},
    query::Query,
    Executor, Pool, Postgres,
};
use std::{collections::HashMap, hash::Hash, sync::Arc};
use tokio::{fs::File, sync::Mutex, time::Instant};

use crate::csv_log::output_csv_row;

/// Used to uniquely identify each DB query.
#[derive(Clone, Debug, Eq, Hash, PartialEq, Serialize)]
pub enum DatabaseQuery {
    SelectDistinctPartDContractIds,
    SelectBeneCountByPartDContractIdAndYearMonth,
    SelectBeneIdsByPartDContractIdAndYearMonth,
    SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,
    SelectBeneRecordsByBeneIds,
}

lazy_static! {
    /// Stores the SQL queries for each [DatabaseQuery].
    pub static ref DATABASE_QUERY_SQL: HashMap<DatabaseQuery, &'static str> = {
        let mut sql_queries = HashMap::new();

        /*
         * Moved all the DB queries out to separate files, as some of them are ginormous Also, `cargo fmt`
         * seems to get a bit goofy if they're in here.
         */
         sql_queries.insert(
            DatabaseQuery::SelectDistinctPartDContractIds,
            include_str!("./db_queries/select_distinct_part_d_contract_ids.sql"));
         sql_queries.insert(
            DatabaseQuery::SelectBeneCountByPartDContractIdAndYearMonth,
            include_str!("./db_queries/select_bene_count_by_part_d_contract_id_and_year_month.sql"));
         sql_queries.insert(
            DatabaseQuery::SelectBeneIdsByPartDContractIdAndYearMonth,
            include_str!("./db_queries/select_bene_ids_by_part_d_contract_id_and_year_month.sql"));
         sql_queries.insert(
            DatabaseQuery::SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,
            include_str!("./db_queries/select_bene_ids_by_part_d_contract_id_and_year_month_and_min_bene_id.sql"));
         sql_queries.insert(
            DatabaseQuery::SelectBeneRecordsByBeneIds,
            include_str!("./db_queries/select_bene_records_by_bene_ids.sql"));

        sql_queries
    };
}

/// Wraps a SQLx `query(...)` to monitor and record how long it takes to run.
///
/// Parameters:
/// * `db_pool`: The database connection pool to run the query on.
/// * `csv_serializer`: The CSV [AsyncSerializer] to output results to.
/// * `query_id`: The [DatabaseQuery] that identifies the SQL query that was run.
/// * `query_params`: A "`param1 = value1, param2 = value2`" string that
///     details the parameters that the SQL query was run with.
/// * `query`: The SQLx [Query] to run, which must be ready to run
///     (i.e. its parameters are bound, etc.).
#[tracing::instrument(level = "trace", skip(db_pool, csv_serializer, query))]
pub async fn fetch_all_monitored<'q>(
    db_pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    query_id: DatabaseQuery,
    query_params: String,
    query: Query<'q, Postgres, PgArguments>,
) -> Result<Vec<PgRow>> {
    /*
     * Grab a DB connection **before** we start the clock, as we can wait quite a while for one to be
     * available.
     */
    let mut db_connection = db_pool.acquire().await?;

    /*
     * Run the query, timing it.
     * Note that the time here will likely be an overestimate,
     *   as this async task might spend time waiting to be scheduled
     *   during these next few statements.
     * It will _hopefully_ be close enough, though.
     */
    let query_before = Instant::now();
    let query_result = db_connection.fetch_all(query).await.wrap_err_with(|| {
        format!(
            "Error running '{:?}' query for parameters: '{}",
            query_id, query_params
        )
    });
    let query_elapsed = query_before.elapsed();

    // Log the now-completed query.
    output_csv_row(
        csv_serializer,
        query_id,
        query_params,
        query_result.is_ok(),
        query_elapsed,
        match query_result {
            Ok(ref bene_ids) => Some(bene_ids.len()),
            Err(_) => None,
        },
    )
    .await?;

    // Return the query's results.
    query_result
}
