use crate::error;
use std::env;

/// Stores application configuration. Re-created for each processing thread, but threads are reused
/// across requests, so not request-specific.
#[derive(Debug, Clone)]
pub struct AppConfig {
    pub server_http_port: u16,
    pub server_certs_filename: Option<String>,
    pub server_private_key_filename: Option<String>,
    pub client_certs_filename: Option<String>,
    pub database_url: String,
}

impl AppConfig {
    pub fn new() -> error::Result<AppConfig> {
        // If present, load environment variables from a `.env` file in the working directory.
        dotenv::dotenv().ok();

        // Parse the server cert config entry.
        let server_http_port: Result<String, std::env::VarError> =
            env::var("BFD_PLAID_HTTP_PORT").or(Ok(String::from("3000")));
        let server_http_port: u16 = server_http_port?.parse()?;
        let server_certs_filename = env::var("BFD_TLS_SERVER_CERT");
        let server_private_key_filename = env::var("BFD_TLS_SERVER_KEY");
        let client_certs_filename = env::var("BFD_TLS_CLIENT_CERTS");
        let database_url = env::var("DATABASE_URL");

        Ok(AppConfig {
            server_http_port: server_http_port,
            server_certs_filename: server_certs_filename.ok(),
            server_private_key_filename: server_private_key_filename.ok(),
            client_certs_filename: client_certs_filename.ok(),
            database_url: database_url?,
        })
    }
}
