//! A simple application that checks whether or not all possible variations of
//!   some BFD Server database queries complete within a given time limit.
//! Why?
//! Because query planners are inscrutable
//!   and we keep running into edge cases that blow past our time budgets
//!   (usually because the DB decides that a table scan is the best move).
//!
//! See the `README.md` file for further details.

use csv_async::AsyncSerializer;
use dotenv::dotenv;
use eyre::{Result, WrapErr};
use futures::{stream::FuturesUnordered, StreamExt};
use lazy_static::lazy_static;
use serde::Serialize;
use sqlx::{
    postgres::{PgPoolOptions, PgRow},
    Executor, Pool, Postgres, Row,
};
use std::{
    collections::HashMap,
    hash::Hash,
    sync::{atomic::AtomicU32, Arc},
    time::Duration,
};
use tokio::{fs::File, sync::Mutex};
use tracing::{info, warn, Instrument};
use tracing_subscriber::prelude::*;
use tracing_subscriber::{fmt, fmt::format::FmtSpan, EnvFilter};

mod query;

const BENES_PAGE_SIZE: u32 = 4000;

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
    static ref DATABASE_QUERY_SQL: HashMap<DatabaseQuery, &'static str> = {
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

/// This is the application's entry point.
/// It configures the options, tracing/logging, and DB connection pool.
/// It then pulls the data needed as input for the test script,
///  and kicks off the execution of those tests.
#[tokio::main]
async fn main() -> Result<()> {
    dotenv().ok();

    // Pull config from environment variables
    let output_path =
        std::env::var("DB_QUERIES_OUTPUT").unwrap_or("results/db_query_checker.csv".into());
    let db_uri =
        std::env::var("DB_QUERIES_URI").expect("Undefined environment variable: DB_QUERIES_URI");
    let db_connections: u32 = std::env::var("DB_QUERIES_CONNECTIONS")
        .unwrap_or("5".into())
        .parse()
        .expect("Unable to parse environment variable: DB_QUERIES_CONNECTIONS");

    let fmt_layer = fmt::layer()
        .with_writer(std::io::stderr)
        .with_span_events(FmtSpan::NEW | FmtSpan::CLOSE)
        .with_target(false);
    let filter_layer = EnvFilter::try_from_default_env()
        .or_else(|_| EnvFilter::try_new("info"))
        .unwrap();
    tracing_subscriber::registry()
        .with(filter_layer)
        .with(fmt_layer)
        .with(tracing_error::ErrorLayer::default())
        .init();
    color_eyre::install()?;

    /*
     * Create the CSV serializer, which will automatically write out a header row the first time a row is
     * sent to it.
     */
    let csv_serializer = Arc::new(Mutex::new(AsyncSerializer::from_writer(
        File::create(&output_path)
            .await
            .with_context(|| format!("Failed to create CSV file: '{}'", &output_path))?,
    )));

    /*
     * Create a DB connection pool, which will allow us to: 1) limit the number of active queries, 2) enforce
     * a 30 second timeout for every query, and 3) speed things up quite a bit (opening DB connections is
     * expensive so caching them is a very big win).
     */
    let db_pool = PgPoolOptions::new()
        .connect_timeout(Duration::from_secs(60 * 60 * 24 * 7))
        .min_connections(db_connections)
        .max_connections(db_connections)
        .after_connect(|conn| {
            Box::pin(async move {
                conn.execute("SET application_name = 'db_query_checker';")
                    .await?;
                conn.execute("SET statement_timeout = 30000;").await?;

                Ok(())
            })
        })
        .connect(&db_uri)
        .instrument(tracing::debug_span!("create primary DB connection pool"))
        .await
        .context("Failed to create primary DB connection pool")?;

    /*
     * Create a second DB connection pool for analytical
     *   or other queries that won't fit within the other pools' timeout.
     */
    let db_pool_analytics = PgPoolOptions::new()
        .min_connections(1)
        .max_connections(1)
        .after_connect(|conn| {
            Box::pin(async move {
                conn.execute("SET application_name = 'db_query_checker';")
                    .await?;
                Ok(())
            })
        })
        .connect(&db_uri)
        .instrument(tracing::debug_span!("create analytics DB connection pool"))
        .await
        .context("Failed to create analytics DB connection pool")?;

    info!("Application started!");

    let year_months = {
        let mut year_months = vec![];
        for year in 2020..=2021 {
            for month in vec![
                "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12",
            ] {
                year_months.push(format!("{}-{}-01", year, month));
            }
        }
        year_months
    };

    /*
     * Run this query on the analytical DB connection pool,
     *   as it's guaranteed to take at least 15 minutes.
     */
    let partd_contract_ids: Vec<String> = sqlx::query(
        DATABASE_QUERY_SQL
            .get(&DatabaseQuery::SelectDistinctPartDContractIds)
            .unwrap(),
    )
    .fetch_all(&db_pool_analytics)
    .instrument(tracing::debug_span!("query Part D contract IDs"))
    .await
    .context("Failed to query Part D contract IDs")?
    .into_iter()
    .map(|row| row.get(0))
    .collect();

    info!(
        partd_contract_ids_len = partd_contract_ids.len(),
        year_months_len = year_months.len(),
        "Found '{}' parameter sets to run.",
        (partd_contract_ids.len() * year_months.len())
    );
    let count_success: AtomicU32 = AtomicU32::new(0);
    let count_failure: AtomicU32 = AtomicU32::new(0);
    let search_patients_by_part_d_contract_id_params =
        itertools::iproduct!(partd_contract_ids, year_months);
    let mut search_patients_by_part_d_contract_id_futures: FuturesUnordered<_> =
        search_patients_by_part_d_contract_id_params
            .map(|(partd_contract_id, year_month)| {
                check_contract_year_month(
                    &db_pool,
                    csv_serializer.clone(),
                    partd_contract_id,
                    year_month,
                )
            })
            .collect();
    while let Some(search_result) = search_patients_by_part_d_contract_id_futures.next().await {
        let count_success_current = match search_result {
            Ok(_) => Some(count_success.fetch_add(1, std::sync::atomic::Ordering::SeqCst)),
            Err(err) => {
                count_failure.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
                warn!("Parameter set failed: {:#}", err);
                None
            }
        };

        // Log progress, occassionally.
        if let Some(count_success_current) = count_success_current {
            if count_success_current > 0 && count_success_current % 1000 == 0 {
                info!(
                    count_success = count_success_current,
                    count_failure = count_failure.load(std::sync::atomic::Ordering::Relaxed),
                    "Still running parameter sets."
                );
            }
        }
    }

    info!(
        count_success = count_success.load(std::sync::atomic::Ordering::SeqCst),
        count_failure = count_failure.load(std::sync::atomic::Ordering::SeqCst),
        "Application complete!"
    );
    Ok(())
}

/// Represents each data row of the CSV file that this application will output.
#[derive(Serialize)]
struct CsvOutputRow {
    query_id: DatabaseQuery,
    query_params: String,
    query_succeeded: bool,
    query_time_millis: u128,
    query_result_count: Option<usize>,
}

/// Writes out a [CsvOutputRow] for the specified parameters.
#[tracing::instrument(level = "trace", skip(csv_serializer))]
async fn output_csv_row(
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

/// Runs all of the DB queries for the BFD Server's "search for Patients by Part D contract and year-month"
/// endpoint, for the specfied parameters.
///
/// Parameters:
/// * `pool`: the DB connection pool to use
/// * `csv_serializer`: the CSV writer to output results to
/// * `partd_contract_id`: the Part D contract ID to run the DB queries for
/// * `year_month`: the "YYYY-MM-dd" date to run the DB queries for
#[tracing::instrument(level = "debug", skip(pool, csv_serializer))]
async fn check_contract_year_month(
    pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    partd_contract_id: String,
    year_month: String,
) -> Result<()> {
    let bene_count = select_bene_count_for_part_d_contract_id_and_year_month(
        pool,
        csv_serializer.clone(),
        &partd_contract_id,
        &year_month,
    )
    .await?;
    if bene_count == 0 {
        return Ok(());
    }

    let bene_ids_first_page = select_bene_ids_by_part_d_contract_id_and_year_month(
        pool,
        csv_serializer.clone(),
        &partd_contract_id,
        &year_month,
    )
    .await?;

    select_bene_records_by_bene_ids(pool, csv_serializer.clone(), &bene_ids_first_page).await?;

    let mut bene_id_max = bene_ids_first_page.last().cloned();
    while let Some(bene_id_max_value) = bene_id_max {
        let bene_ids_next_page =
            select_bene_ids_by_part_d_contract_id_and_year_month_and_min_bene_id(
                pool,
                csv_serializer.clone(),
                &partd_contract_id,
                &year_month,
                &bene_id_max_value,
            )
            .await?;

        select_bene_records_by_bene_ids(pool, csv_serializer.clone(), &bene_ids_next_page).await?;

        bene_id_max = bene_ids_next_page.last().cloned();
    }

    Ok(())
}

/// Selects the count of enrolled benes for the specified Part D Contract ID and year-month.
///
/// Parameters:
/// * `pool`: the DB connection pool to use
/// * `csv_serializer`: the CSV writer to output results to
/// * `partd_contract_id`: the Part D contract ID to query for
/// * `year_month`: the "YYYY-MM-dd" date to query for
#[tracing::instrument(level = "trace", skip(pool, csv_serializer))]
async fn select_bene_count_for_part_d_contract_id_and_year_month(
    pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    partd_contract_id: &str,
    year_month: &str,
) -> Result<i64> {
    // Create the query.
    let bene_count_query = sqlx::query(
        DATABASE_QUERY_SQL
            .get(&DatabaseQuery::SelectBeneCountByPartDContractIdAndYearMonth)
            .unwrap(),
    )
    .bind(&year_month)
    .bind(&partd_contract_id);

    // Run the query.
    let bene_count_result = query::fetch_all_monitored(
        pool,
        csv_serializer,
        DatabaseQuery::SelectBeneCountByPartDContractIdAndYearMonth,
        format!(
            "partd_contract_id='{}', year_month='{}'",
            partd_contract_id, year_month
        ),
        bene_count_query,
    )
    .await;

    // Pull the results out of the query.
    let bene_count = bene_count_result?[0].get(0);
    Ok(bene_count)
}

/// Selects a page of enrolled bene IDs for the specified Part D Contract ID and year-month.
///
/// Parameters:
/// * `pool`: the DB connection pool to use
/// * `csv_serializer`: the CSV writer to output results to
/// * `partd_contract_id`: the Part D contract ID to query for
/// * `year_month`: the "YYYY-MM-dd" date to query for
#[tracing::instrument(level = "trace", skip(pool, csv_serializer))]
async fn select_bene_ids_by_part_d_contract_id_and_year_month(
    pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    partd_contract_id: &str,
    year_month: &str,
) -> Result<Vec<String>> {
    // Create the query.
    let bene_ids_query = sqlx::query(
        DATABASE_QUERY_SQL
            .get(&DatabaseQuery::SelectBeneIdsByPartDContractIdAndYearMonth)
            .unwrap(),
    )
    .bind(&year_month)
    .bind(&partd_contract_id)
    .bind(&BENES_PAGE_SIZE);

    // Run the query.
    let bene_ids_result = query::fetch_all_monitored(
        pool,
        csv_serializer,
        DatabaseQuery::SelectBeneIdsByPartDContractIdAndYearMonth,
        format!(
            "partd_contract_id='{}', year_month='{}'",
            partd_contract_id, year_month
        ),
        bene_ids_query,
    )
    .await;

    // Pull the results out of the query.
    let bene_ids = bene_ids_result?.into_iter().map(|row| row.get(0)).collect();
    Ok(bene_ids)
}

/// Selects a page of enrolled bene IDs for the specified Part D Contract ID and year-month.
///
/// Parameters:
/// * `pool`: the DB connection pool to use
/// * `csv_serializer`: the CSV writer to output results to
/// * `partd_contract_id`: the Part D contract ID to query for
/// * `year_month`: the "YYYY-MM-dd" date to query for
/// * `min_bene_id`: the minimum bene ID to query for (sort of -- it's actually a 'greater than' query)
#[tracing::instrument(level = "trace", skip(pool, csv_serializer))]
async fn select_bene_ids_by_part_d_contract_id_and_year_month_and_min_bene_id(
    pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    partd_contract_id: &String,
    year_month: &str,
    min_bene_id: &str,
) -> Result<Vec<String>> {
    // Create the query.
    let bene_ids_query = sqlx::query(
        DATABASE_QUERY_SQL
            .get(&DatabaseQuery::SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId)
            .unwrap(),
    )
    .bind(&year_month)
    .bind(&partd_contract_id)
    .bind(&min_bene_id)
    .bind(&BENES_PAGE_SIZE);

    // Run the query.
    let bene_ids_result = query::fetch_all_monitored(
        pool,
        csv_serializer,
        DatabaseQuery::SelectBeneIdsByPartDContractIdAndYearMonthAndMinBeneId,
        format!(
            "partd_contract_id='{}', year_month='{}', min_bene_id='{}'",
            partd_contract_id, year_month, min_bene_id
        ),
        bene_ids_query,
    )
    .await;

    // Pull the results out of the query.
    let bene_ids = bene_ids_result?.into_iter().map(|row| row.get(0)).collect();
    Ok(bene_ids)
}

/// Selects the bene records for the specified bene IDs.
///
/// Parameters:
/// * `pool`: the DB connection pool to use
/// * `csv_serializer`: the CSV writer to output results to
/// * `bene_ids`: the bene IDs to run the DB query for
#[tracing::instrument(level = "trace", skip(pool, csv_serializer))]
async fn select_bene_records_by_bene_ids(
    pool: &Pool<Postgres>,
    csv_serializer: Arc<Mutex<AsyncSerializer<File>>>,
    bene_ids: &Vec<String>,
) -> Result<Vec<PgRow>> {
    /*
     * Ridiculous, but SQLx doesn't support binding `IN` parameters.
     *
     * Reference: <https://www.reddit.com/r/rust/comments/ip4a0q/sql_x_how_do_you_parameterize_an_in_statement_or/>
     */
    let bene_ids_param: Vec<String> = bene_ids.into_iter().map(|i| format!("'{}'", i)).collect();
    let bene_ids_param = bene_ids_param.join(",");

    // Create the query.
    let benes_query = sqlx::query(
        DATABASE_QUERY_SQL
            .get(&DatabaseQuery::SelectBeneRecordsByBeneIds)
            .unwrap(),
    )
    .bind(bene_ids_param)
    .bind(&BENES_PAGE_SIZE);

    // Run the query.
    let benes_result = query::fetch_all_monitored(
        pool,
        csv_serializer,
        DatabaseQuery::SelectBeneRecordsByBeneIds,
        format!("bene_ids.len='{}'", bene_ids.len()),
        benes_query,
    )
    .await;

    // Pull the results out of the query.
    let benes = benes_result?;
    Ok(benes)
}
