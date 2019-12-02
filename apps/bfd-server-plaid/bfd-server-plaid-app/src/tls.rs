use crate::config::AppConfig;
use crate::error;
use std::fs::{self, File};
use std::io::BufReader;
use std::io::Read;
// We only need this because some rustls methods return unexported types from it.
use openssl::ssl::{SslAcceptor, SslFiletype, SslMethod};
use webpki;

/// Enumerates some additional error conditions that `rustls` doesn't define.
#[derive(Debug)]
pub enum TLSConfigError {
    MiscError(String),
    IoError(std::io::Error, String),
    WebPKIError(webpki::Error),
}

/// Parses the SSL certificate(s) in the specified PEM file into `Certificate` instances.
fn load_certs(certs_filename: &str) -> error::Result<Vec<rustls::Certificate>> {
    let certs_file = fs::File::open(certs_filename)
        .map_err(|err| TLSConfigError::IoError(err, "Can't open certificates file.".to_string()))?;
    let mut certs_reader = BufReader::new(certs_file);
    rustls::internal::pemfile::certs(&mut certs_reader)
        .map_err(|_| TLSConfigError::MiscError("Unable to parse certificates.".to_string()))
        .map_err(|err| error::AppError::TLSConfigError(err))
}

/// Parses the SSL private key in the specified PEM file into a `PrivateKey` instance.
fn load_private_key(key_filename: &str) -> error::Result<rustls::PrivateKey> {
    let key_file = fs::File::open(key_filename)
        .map_err(|err| TLSConfigError::IoError(err, "Can't open private key file.".to_string()))?;
    let mut key_reader = BufReader::new(key_file);
    let keys = rustls::internal::pemfile::pkcs8_private_keys(&mut key_reader).map_err(|_| {
        TLSConfigError::MiscError(
            "Unable to parse PKCS8 private key(s) from key file (encrypted keys not supported)."
                .to_string(),
        )
    })?;
    // TODO: fail if unexpected number of keys
    Ok(keys[0].clone())
}

/// Creates the `rustls::ServerConfig` for the server to use.
pub fn create_rustls_config(app_config: &AppConfig) -> error::Result<rustls::ServerConfig> {
    let client_certs_filename = match &app_config.client_certs_filename {
        Some(f) => Ok(f),
        None => Err(TLSConfigError::MiscError(
            "Missing config value.".to_string(),
        )),
    };
    let client_auth_certs = load_certs(client_certs_filename?)?;
    let mut client_auth_roots = rustls::RootCertStore::empty();
    for client_auth_cert in client_auth_certs {
        client_auth_roots
            .add(&client_auth_cert)
            .map_err(|err| TLSConfigError::WebPKIError(err))?;
    }
    let client_auth_verifier = rustls::AllowAnyAuthenticatedClient::new(client_auth_roots);

    let mut config = rustls::ServerConfig::new(client_auth_verifier);

    let server_certs_filename = match &app_config.server_certs_filename {
        Some(f) => Ok(f),
        None => Err(TLSConfigError::MiscError(
            "Missing config value.".to_string(),
        )),
    };
    let server_certs = load_certs(server_certs_filename?)?;
    let server_private_key_filename = match &app_config.server_private_key_filename {
        Some(f) => Ok(f),
        None => Err(TLSConfigError::MiscError(
            "Missing config value.".to_string(),
        )),
    };
    let server_private_key = load_private_key(server_private_key_filename?)?;
    config.set_single_cert(server_certs, server_private_key)?;

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

    Ok(config)
}

/// Creates the `openssl::ssl::SslAcceptorBuilder` for the server to use.
pub fn create_openssl_config(
    app_config: &AppConfig,
) -> error::Result<openssl::ssl::SslAcceptorBuilder> {
    // Unwrap the required config values.
    let server_private_key_filename = match &app_config.server_private_key_filename {
        Some(f) => Ok(f),
        None => Err(TLSConfigError::MiscError(
            "Missing TLS server private key config.".to_string(),
        )),
    }?;
    let server_certs_filename = match &app_config.server_certs_filename {
        Some(f) => Ok(f),
        None => Err(TLSConfigError::MiscError(
            "Missing TLS server certs config value.".to_string(),
        )),
    }?;
    let client_certs_filename = match &app_config.client_certs_filename {
        Some(f) => Ok(f),
        None => Err(TLSConfigError::MiscError(
            "Missing TLS client certs config value.".to_string(),
        )),
    }?;

    // Parse the trusted client certs into an X509Store.
    let mut client_certs_bytes: Vec<u8> = Vec::new();
    let mut client_certs_file = File::open(client_certs_filename)?;
    client_certs_file.read_to_end(&mut client_certs_bytes)?;
    let client_certs_pem: Vec<openssl::x509::X509> =
        openssl::x509::X509::stack_from_pem(&client_certs_bytes)?;
    let mut client_certs_store = openssl::x509::store::X509StoreBuilder::new()?;
    for client_cert_pem in client_certs_pem {
        client_certs_store.add_cert(client_cert_pem)?;
    }
    let client_certs_store = client_certs_store.build();

    // Create and configure the OpenSSL builder.
    let mut builder = SslAcceptor::mozilla_modern(SslMethod::tls())?;
    builder.set_private_key_file(server_private_key_filename, SslFiletype::PEM)?;
    builder.set_certificate_file(server_certs_filename, openssl::ssl::SslFiletype::ASN1)?;
    builder.set_verify(openssl::ssl::SslVerifyMode::FAIL_IF_NO_PEER_CERT);
    builder.set_verify_cert_store(client_certs_store)?;

    // FIXME This absolutely, 100% doesn't work: client certs aren't required and probably aren't validated if presented, either.

    Ok(builder)
}
