use std::env;

/// Stores application state. Re-created for each processing thread, but threads are reused across
/// requests (at least from some testing), so not request-specific.
///
/// I think I'd stick config info here, e.g. DB connection details.
#[derive(Debug, Clone)]
pub struct AppConfig {
    pub server_certs_filename: String,
    pub server_private_key_filename: String,
    pub client_certs_filename: String,
}

impl AppConfig {
    pub fn new() -> Result<AppConfig, std::env::VarError> {
        // If present, load environment variables from a `.env` file in the working directory.
        dotenv::dotenv().ok();

        // Parse the server cert config entry.
        let server_certs_filename = env::var("BFD_TLS_SERVER_CERT");
        let server_private_key_filename = env::var("BFD_TLS_SERVER_KEY");
        let client_certs_filename = env::var("BFD_TLS_CLIENT_CERTS");

        Ok(AppConfig {
            server_certs_filename: server_certs_filename?,
            server_private_key_filename: server_private_key_filename?,
            client_certs_filename: client_certs_filename?,
        })
    }
}
