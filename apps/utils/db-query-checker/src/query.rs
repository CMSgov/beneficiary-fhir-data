//! Contains utilities for wrapping/monitoring SQLx queries.

use csv_async::AsyncSerializer;
use eyre::{Result, WrapErr};
use sqlx::{
    postgres::{PgArguments, PgRow},
    query::Query,
    Executor, Pool, Postgres,
};
use std::sync::Arc;
use tokio::{fs::File, sync::Mutex, time::Instant};

/// Wraps a SQLx `query(...)` to monitor and record how long it takes to run.
#[tracing::instrument(level = "trace", skip(pool, csv_serializer, query))]
pub async fn fetch_all_monitored<'q>(
    pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    query_id: crate::DatabaseQuery,
    query_params: String,
    query: Query<'q, Postgres, PgArguments>,
) -> Result<Vec<PgRow>> {
    /*
     * Grab a DB connection **before** we start the clock, as we can wait quite a while for one to be
     * available.
     */
    let mut db_connection = pool.acquire().await?;

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
    crate::output_csv_row(
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
