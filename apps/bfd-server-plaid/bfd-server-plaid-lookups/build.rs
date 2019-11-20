use bfd_server_plaid_lookups_codegen;

/// Entry point for this `build.rs` script, which Cargo will call before compiling the rest of the
/// code (in `src/`).
fn main() -> Result<()> {
    bfd_server_plaid_lookups_codegen::generate_lookups()?;

    Ok(())
}

/// Type alias for `::std::result::Result<T, Error>`, where `Error` is the custom error wrapper for
/// the `bfd_server_plaid_lookups` package's `build.rs`.
type Result<T> = ::std::result::Result<T, Error>;

/// Wraps errors encountered by the `bfd_server_plaid_lookups` package's `build.rs`.
#[derive(Debug)]
enum Error {
    /// Wraps a `bfd_server_plaid_lookups_codegen::error::Error`.
    LookupsCodegenError(bfd_server_plaid_lookups_codegen::error::Error),
}

impl From<bfd_server_plaid_lookups_codegen::error::Error> for Error {
    fn from(err: bfd_server_plaid_lookups_codegen::error::Error) -> Error {
        Error::LookupsCodegenError(err)
    }
}
