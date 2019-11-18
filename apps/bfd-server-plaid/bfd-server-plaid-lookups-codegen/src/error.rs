/// Type alias for `::std::result::Result<T, Error>`, where `Error` is the custom error wrapper for
/// the `bfd_server_plaid_lookups_codegen` package.
pub type Result<T> = ::std::result::Result<T, Error>;

/// Wraps errors encountered by the `bfd_server_plaid_lookups_codegen` package.
#[derive(Debug)]
pub enum Error {
    /// Wraps a `reqwest::Error`.
    ReqwestError(reqwest::Error),

    /// Wraps a `std::io::Error`.
    IoError(std::io::Error),

    /// Wraps a `zip::result::ZipError`.
    ZipError(zip::result::ZipError),

    /// Wraps a `csv::Error`.
    CsvError(csv::Error),

    /// Should be used when the lookups source data contains unexpected or unsupported values.
    InvalidLookupsDataError(String),
}

impl From<reqwest::Error> for Error {
    fn from(err: reqwest::Error) -> Error {
        Error::ReqwestError(err)
    }
}

impl From<std::io::Error> for Error {
    fn from(err: std::io::Error) -> Error {
        Error::IoError(err)
    }
}

impl From<zip::result::ZipError> for Error {
    fn from(err: zip::result::ZipError) -> Error {
        Error::ZipError(err)
    }
}

impl From<csv::Error> for Error {
    fn from(err: csv::Error) -> Error {
        Error::CsvError(err)
    }
}
