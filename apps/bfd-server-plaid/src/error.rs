pub type Result<T> = ::std::result::Result<T, AppError>;

#[derive(Debug)]
pub enum AppError {
    VarError(std::env::VarError),
    IoError(std::io::Error),
    SetLoggerError(log::SetLoggerError),
    RustlsError(rustls::TLSError),
    TLSConfigError(crate::tls::TLSConfigError),
    DieselConnectionError(diesel::ConnectionError),
    DieselResultError(diesel::result::Error),
}

impl From<std::env::VarError> for AppError {
    fn from(err: std::env::VarError) -> AppError {
        AppError::VarError(err)
    }
}

impl From<std::io::Error> for AppError {
    fn from(err: std::io::Error) -> AppError {
        AppError::IoError(err)
    }
}

impl From<log::SetLoggerError> for AppError {
    fn from(err: log::SetLoggerError) -> AppError {
        AppError::SetLoggerError(err)
    }
}

impl From<rustls::TLSError> for AppError {
    fn from(err: rustls::TLSError) -> AppError {
        AppError::RustlsError(err)
    }
}

impl From<crate::tls::TLSConfigError> for AppError {
    fn from(err: crate::tls::TLSConfigError) -> AppError {
        AppError::TLSConfigError(err)
    }
}

impl From<diesel::result::Error> for AppError {
    fn from(err: diesel::result::Error) -> AppError {
        AppError::DieselResultError(err)
    }
}

impl From<diesel::ConnectionError> for AppError {
    fn from(err: diesel::ConnectionError) -> AppError {
        AppError::DieselConnectionError(err)
    }
}
