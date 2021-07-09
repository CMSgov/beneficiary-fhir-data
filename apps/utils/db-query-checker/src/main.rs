//! The main binary crate for the application,
//!   which is just a thin wrapper around the project's library crate.
//!
//! Why bother doing this; why not just have a bin-only crate?
//! Because otherwise, we can't run doc tests or integration tests.

use eyre::Result;

/// The application's OG entry point,
///   which just runs [db_query_checker::run_db_query_checker()].
#[tokio::main]
async fn main() -> Result<()> {
    db_query_checker::run_db_query_checker().await
}