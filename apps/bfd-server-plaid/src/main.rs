mod config;
mod error;
mod fhir;
mod tls;

use actix_web::{web, App, HttpResponse, HttpServer};
use config::AppConfig;
use listenfd::ListenFd;
use slog::{info, o, Drain};
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
    let _log_guard = slog_stdlog::init_with_level(log::Level::Warn)?;

    // Next, parse the app confif from the env.
    let app_config = AppConfig::new()?;

    info!(logger, "Prepare ship for ludicrous speed.");
    let mut listenfd = ListenFd::from_env();
    let app_config_data = web::Data::new(app_config.clone());
    let mut server = HttpServer::new(move || {
        App::new()
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
        // Offer HTTP to localhost and HTTPS to remote clients.
        server
            .bind("127.0.0.1:3000")?
            .bind_rustls("0.0.0.0:3001", tls::create_rustls_config(&app_config)?)?
    };

    /*
     * Run the server until it's shutdown via OS signal: SIGTERM will lead to a graceful shutdown
     * that honors the `shutdown_timeout`, while SIGINT and SIGQUIT will close all open connections
     * immediately.
     */
    server.run()?;

    Ok(())
}
