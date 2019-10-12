use actix_web::{web, App, HttpResponse, HttpServer, Responder};
use listenfd::ListenFd;
use std::fs;
use std::io::BufReader;

#[macro_use]
extern crate slog;
extern crate slog_async;
extern crate slog_json;

use slog::Drain;

/// Stores application state. Re-created for each processing thread, but threads are reused across
/// requests (at least from some testing), so not request-specific.
///
/// I think I'd stick config info here, e.g. DB connection details.
#[derive(Debug, Clone)]
struct AppConfig {
    server_certs_filename: String,
    server_private_key_filename: String,
    client_certs_filename: String,
}

use std::env;

impl AppConfig {
    fn new() -> Result<AppConfig, std::env::VarError> {
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

fn eob_for_bene_id() -> impl Responder {
    "<claims data goes here>"
}

/// Parses the SSL certificate(s) in the specified PEM file into `Certificate` instances.
fn load_certs(certs_filename: &str) -> Vec<rustls::Certificate> {
    let certs_file = fs::File::open(certs_filename).expect("Can't open certificates file.");
    let mut certs_reader = BufReader::new(certs_file);
    rustls::internal::pemfile::certs(&mut certs_reader).unwrap()
}

/// Parses the SSL private key in the specified PEM file into a `PrivateKey` instance.
fn load_private_key(key_filename: &str) -> rustls::PrivateKey {
    let key_file = fs::File::open(key_filename).expect("Can't open private key file.");
    let mut key_reader = BufReader::new(key_file);
    let keys = rustls::internal::pemfile::pkcs8_private_keys(&mut key_reader).expect(
        "Unable to parse PKCS8 private key(s) from key file (encrypted keys not supported).",
    );
    // TODO: fail if unexpected number of keys
    keys[0].clone()
}

/// Creates the `rustls::ServerConfig` for the server to use.
fn create_rustls_config(app_config: &AppConfig) -> rustls::ServerConfig {
    let client_auth_certs = load_certs(&app_config.client_certs_filename);
    let mut client_auth_roots = rustls::RootCertStore::empty();
    for client_auth_cert in client_auth_certs {
        client_auth_roots.add(&client_auth_cert).unwrap();
    }
    let client_auth_verifier = rustls::AllowAnyAuthenticatedClient::new(client_auth_roots);

    let mut config = rustls::ServerConfig::new(client_auth_verifier);

    let server_certs = load_certs(&app_config.server_certs_filename);
    let server_private_key = load_private_key(&app_config.server_private_key_filename);
    config
        .set_single_cert(server_certs, server_private_key)
        .expect("Unable to parse server certificates and/or key.");

    // TODO: remove TLS v1.2 once we can move all of our clients off it.
    let versions = vec![
        rustls::ProtocolVersion::TLSv1_2,
        rustls::ProtocolVersion::TLSv1_3,
    ];
    config.versions = versions;

    // The list of ciphers supported by rustls is very conservative and modern.
    let mut ciphersuites: Vec<&'static rustls::SupportedCipherSuite> = Vec::new();
    for ciphersuite in &rustls::ALL_CIPHERSUITES {
        ciphersuites.push(ciphersuite);
    }
    config.ciphersuites = ciphersuites;

    /*
     * TODO add support for session resume and tickets?
     * See https://github.com/ctz/rustls/blob/master/rustls-mio/examples/tlsserver.rs#L565
     */

    config
}

/// Initializes logging and returns the root Logger for the application to use.
fn config_logging() -> slog::Logger {
    let drain = slog_json::Json::new(std::io::stdout())
        .set_pretty(true)
        .add_default_keys()
        .build()
        .fuse();
    let drain = slog_async::Async::new(drain).build().fuse();
    let logger = slog::Logger::root(drain, o!());

    logger
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    // First, initialize logging.
    let logger = config_logging();

    // Route all log crate usage (from our dependencies) to slog, instead.
    // Note: This has to stay in scope in order to keep working.
    let _scope_guard = slog_scope::set_global_logger(logger.clone());
    let _log_guard = slog_stdlog::init_with_level(log::Level::Warn).unwrap();

    // Next, parse the app confif from the env.
    let app_config = AppConfig::new()?;

    info!(logger, "Prepare ship for ludicrous speed.");
    let mut listenfd = ListenFd::from_env();
    let app_config_data = web::Data::new(app_config.clone());
    let mut server = HttpServer::new(move || {
        App::new()
            .register_data(app_config_data.clone())
            .service(
                web::scope("/v1/fhir")
                    .route("/ExplanationOfBenefit", web::get().to(eob_for_bene_id)),
            )
            .service(web::scope("/v2").route("/", web::to(|| HttpResponse::Ok())))
            .route("/", web::to(|| HttpResponse::Ok()))
    });

    info!(logger, "Ludicrous speed... go!");
    server = if let Some(l) = listenfd.take_tcp_listener(0).unwrap() {
        // Used in local development to auto-reload changes.
        server.listen(l).unwrap()
    } else {
        // Offer HTTP to localhost and HTTPS to remote clients.
        server
            .bind("127.0.0.1:3000")
            .unwrap()
            .bind_rustls("0.0.0.0:3001", create_rustls_config(&app_config))
            .unwrap()
    };

    /*
     * Run the server until it's shutdown via OS signal: SIGTERM will lead to a graceful shutdown
     * that honors the `shutdown_timeout`, while SIGINT and SIGQUIT will close all open connections
     * immediately.
     */
    server.run().unwrap();

    Ok(())
}
