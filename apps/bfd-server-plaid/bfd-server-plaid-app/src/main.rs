//! This crate produces the main Beneficiary FHIR Data (BFD) Server application, which is an HTTP/S
//! FHIR-compliant web server that supports other CMS applications needing access to Medicare
//! beneficiaries' demographic, enrollment, and claims data.
//!
//! # Launching the Application
//!
//! The application's configuration is all provided via environment variables. For full details on
//! that configuration, see the following files in the application source repository:
//!
//! * `bfd-server-plaid-app/src/config.rs`: Handles the configuration parsing for the application.
//! * `.env`: Specifies the environment variable values that will be used during local development.
//!
//! In local development, the application can be launched as follows:
//!
//! ```sh
//! # Change to the BFD Server's directory, then to the Rust app's.
//! $ cd beneficiary-fhir-data.git/
//! $ cd apps/bfd-server-plaid/
//!
//! # Stand up the PostgreSQL database server.
//! $ POSTGRES_PORT=5432 docker-compose --file ../dev/docker-compose.yml up --detach
//!
//! # Provision the database schema via Diesel. Note that the schema migration files for Diesel
//! # are separate from the Java app's and may diverge if not kept up to date by developers. See
//! # the `bfd-server-plaid-app/migrations/` directory for details.
//! $ cd bfd-server-plaid-app/
//! $ diesel migration run
//! $ cd ..
//!
//! # Build the Rust crates.
//! $ cargo build
//!
//! # Run the tests in the Rust crates (will build first if necessary).
//! $ cargo test
//!
//! # Launch the BFD Server application (will build first if necessary). This will wait/block while
//! # the server is running. Press `ctrl+c` to stop it when you're done.
//! $ cargo run
//!
//! # Stand down the PostgreSQL database server.
//! $ docker-compose --file ../dev/docker-compose.yml down
//! ```
//!
//! Alternatively, during the transition from the Java app to the Rust app, launching the Java app
//! will also automagically launch the Rust app:
//!
//! ```sh
//! # Change to the BFD Server's directory, then to the main app directory.
//! $ cd beneficiary-fhir-data.git/
//! $ cd apps/
//!
//! # Stand up the PostgreSQL database server.
//! $ POSTGRES_PORT=5432 docker-compose --file dev/docker-compose.yml up --detach
//!
//! # Build the Java apps.
//! $ mvn clean install
//!
//! # Change to the directory for the Java BFD Server application.
//! $ cd bfd-server/bfd-server-war/
//!
//! # Run the Java BFD Server, which will also launch an instance of the Rust server. This will
//! # launch the servers and then return, leaving them running in separate processes.
//! $ mvn \
//!     '-Dits.db.url=jdbc:postgresql://localhost:5432/bfd?user=bfd&password=InsecureLocalDev' \
//!     dependency:copy \
//!     org.codehaus.mojo:build-helper-maven-plugin:reserve-network-port@reserve-server-ports \
//!     org.codehaus.mojo:exec-maven-plugin:exec@server-start
//!
//! # Stop the Java (and Rust) BFD Server processes when you're done.
//! $ mvn org.codehaus.mojo:exec-maven-plugin:exec@server-stop
//!
//! # Change back to the main apps directory.
//! $ cd ../..
//!
//! # Stand down the PostgreSQL database server.
//! $ docker-compose --file dev/docker-compose.yml down
//! ```
//!
//! # Querying the Application
//!
//! The server listens for both HTTP and HTTPS requests, though HTTP requests are only accessible
//! from `localhost`.
//!
//! By default, the server selects port `3000` for HTTP and port `3001` for HTTPS. The server can
//! be queried for a specific beneficiaries' claims, as follows:
//!
//! ```sh
//! $ curl 'http://localhost:3000/v1/fhir/ExplanationOfBenefit?patient=Patient/1234'
//! ```
//!
//! Note: Your database will likely be empty, and so that search will likely not return any
//! results. For now, it's generally simplest to test the Rust app via the Java app's integration
//! tests, which automatically handle loading test data.

// Necessary for `schema_views.rs` to compile.
#![recursion_limit = "2048"]

mod ccw_codebook;
mod config;
mod db;
mod error;
mod fhir;
mod models;
mod schema_views;
mod tls;

#[macro_use]
extern crate diesel;

use actix_web::{web, App, HttpResponse, HttpServer};
use config::AppConfig;
use listenfd::ListenFd;
use slog::{info, o, Drain, trace};
use slog_async;
use slog_json;

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

fn main() -> error::Result<()> {
    // First, initialize logging.
    let logger = config_logging();

    // Route all log crate usage (from our dependencies) to slog, instead.
    // Note: This has to stay in scope in order to keep working.
    let _scope_guard = slog_scope::set_global_logger(logger.clone());
    let _log_guard = slog_stdlog::init_with_level(log::Level::Trace)?;

    // Parse the app confif from the env.
    trace!(logger, "Application configuration: parsing...");
    let app_config = AppConfig::new()?;
    trace!(logger, "Application configuration: parsed.");

    // Verify that the DB connection is copacetic.
    trace!(logger, "DB connection pool: creating...");
    let db_connection_pool = db::create_db_connection_pool(&app_config)?;
    trace!(logger, "DB connection pool: created.");

    info!(logger, "Prepare ship for ludicrous speed.");
    let mut listenfd = ListenFd::from_env();
    let app_config_data = web::Data::new(app_config.clone());
    let db_connection_pool_data = web::Data::new(db_connection_pool.clone());
    let mut server = HttpServer::new(move || {
        App::new()
            .register_data(db_connection_pool_data.clone())
            .register_data(app_config_data.clone())
            .service(web::scope("/v1/fhir").route(
                "/ExplanationOfBenefit",
                web::get().to(fhir::v1::eob::eob_for_bene_id),
            ))
            .service(web::scope("/v2").route("/", web::to(|| HttpResponse::Ok())))
            .route("/", web::to(|| HttpResponse::Ok()))
    });

    info!(logger, "Ludicrous speed... go!");
    server = if let Some(l) = listenfd.take_tcp_listener(0)? {
        // Used in local development to auto-reload changes.
        server.listen(l)?
    } else {
        if app_config.server_certs_filename.is_some()
            && app_config.server_private_key_filename.is_some()
            && app_config.client_certs_filename.is_some()
        {
            // Offer HTTP to localhost and HTTPS to remote clients.
            server
                .bind(format!("127.0.0.1:{}", &app_config.server_http_port))?
                // FIXME Figure out why RusTLS isn't working.
                //.bind_rustls("0.0.0.0:3001", tls::create_rustls_config(&app_config)?)?
                .bind_ssl("0.0.0.0:3001", tls::create_openssl_config(&app_config)?)?
        } else {
            server.bind(format!("127.0.0.1:{}", &app_config.server_http_port))?
        }
    };

    /*
     * Run the server until it's shutdown via OS signal: SIGTERM will lead to a graceful shutdown
     * that honors the `shutdown_timeout`, while SIGINT and SIGQUIT will close all open connections
     * immediately.
     */
    server.run()?;

    Ok(())
}
