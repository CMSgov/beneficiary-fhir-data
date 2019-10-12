use super::config::AppConfig;
use std::fs;
use std::io::BufReader;

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
pub fn create_rustls_config(app_config: &AppConfig) -> rustls::ServerConfig {
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
