use crate::error;
use std::env;

/// The number of concurrent queries that a single (logical/virtual) CPU can support for this
/// application.
///
/// This number is very much a SWAG, and probably ought to be derived from careful benchmarking.
/// That said, if our database queries average 100 milliseconds each, and the remainder of request
/// processing averages 10 milliseconds each, that works out to about a 1:10 ratio. This makes
/// sense, given that the DB queries are relatively slow, but CPU-cheap while we're just waiting
/// for them to complete.
const QUERIES_PER_CPU: u8 = 10;

/// Stores application configuration. Re-created for each processing thread, but threads are reused
/// across requests, so not request-specific.
#[derive(Debug, Clone)]
pub struct AppConfig {
    pub actix_threadpool_size: u8,
    pub server_http_port: u16,
    pub server_certs_filename: Option<String>,
    pub server_private_key_filename: Option<String>,
    pub client_certs_filename: Option<String>,
    pub database_url: String,
    pub database_pool_size: u32,
}

impl AppConfig {
    pub fn new() -> error::Result<AppConfig> {
        // If present, load environment variables from a `.env` file in the working directory.
        dotenv::dotenv().ok();

        // Parse the configurable entries.
        let server_http_port: Result<String, std::env::VarError> =
            env::var("BFD_PLAID_HTTP_PORT").or(Ok(String::from("3000")));
        let server_http_port: u16 = server_http_port?.parse()?;
        let server_certs_filename = env::var("BFD_TLS_SERVER_CERT");
        let server_private_key_filename = env::var("BFD_TLS_SERVER_KEY");
        let client_certs_filename = env::var("BFD_TLS_CLIENT_CERTS");
        let database_url = env::var("DATABASE_URL");

        // Calculate the non-configurable entries.
        //
        // Notes:
        // * The default ACTIX_THREADPOOL is `num_cpus::get() * 5`.
        // * The default r2d2 `max_size` is `10`.
        use std::convert::TryInto;
        let num_logical_cpus: u8 = num_cpus::get().try_into().unwrap();
        let actix_threadpool_size = (QUERIES_PER_CPU * num_logical_cpus) + 2;
        let database_pool_size: u32 = (QUERIES_PER_CPU * num_logical_cpus).into();

        Ok(AppConfig {
            actix_threadpool_size: actix_threadpool_size,
            server_http_port: server_http_port,
            server_certs_filename: server_certs_filename.ok(),
            server_private_key_filename: server_private_key_filename.ok(),
            client_certs_filename: client_certs_filename.ok(),
            database_url: database_url?,
            database_pool_size: database_pool_size,
        })
    }
}
