//! Contains utilities for logging database query metadata out to a CSV file.

use csv_async::AsyncSerializer;
use eyre::{Result, WrapErr};
use serde::Serialize;
use std::{sync::Arc, time::Duration};
use tokio::{fs::File, sync::Mutex};

use crate::query::DatabaseQuery;

/// Represents the rows of the CSV file that this application will output.
///
/// The file will include one row for each database query that is checked,
///   detailing that query's parameters, results, and performance.
#[derive(Serialize)]
struct CsvOutputRow {
    /// The [DatabaseQuery] that identifies the SQL query that was run.
    query_id: DatabaseQuery,

    /// A "`param1 = value1, param2 = value2`" string that
    ///   details the parameters that the SQL query was run with.
    query_params: String,

    /// `true` if the query succeeded, `false` if it failed.
    query_succeeded: bool,

    /// The number of milliseconds that the query took to run
    ///   (roughly; for... reasons, this will typically be an overestimate).
    query_time_millis: u128,

    /// The number of result rows that the query returned,
    ///   or [None] if it failed.
    query_result_count: Option<usize>,
}

/// Writes out a [CsvOutputRow] for the specified parameters.
///
/// Parameters:
/// * `csv_serializer`: The CSV [AsyncSerializer] to output results to.
/// * `query_id`: The [DatabaseQuery] that identifies the SQL query that was run.
/// * `query_params`: A "`param1 = value1, param2 = value2`" string that
///     details the parameters that the SQL query was run with.
/// * `query_succeeded`: `true` if the query succeeded, `false` if it failed.
/// * `query_time`: How long the query took to run
///     (roughly; for... reasons, this will typically be an overestimate).
/// * `query_result_count`: The number of result rows that the query returned,
///     or [None] if it failed.
#[tracing::instrument(level = "trace", skip(csv_serializer))]
pub async fn output_csv_row(
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    query_id: DatabaseQuery,
    query_params: String,
    query_succeeded: bool,
    query_time: Duration,
    query_result_count: Option<usize>,
) -> Result<()> {
    let row = CsvOutputRow {
        query_id,
        query_params,
        query_succeeded,
        query_time_millis: query_time.as_millis(),
        query_result_count,
    };

    // TODO: If performance is slow, I could also batch these writes.

    // TODO: does moving this to a `tokio::spawn(...)` block improve performance?

    let mut csv_serializer_lock = csv_serializer.lock().await;
    csv_serializer_lock
        .serialize(&row)
        .await
        .context("Failed to write out CSV record")?;

    Ok(())
}
