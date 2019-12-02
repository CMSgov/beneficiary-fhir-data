pub type Result<T> = ::std::result::Result<T, AppError>;

#[derive(Debug)]
pub enum AppError {
    VarError(std::env::VarError),
    ParseIntError(std::num::ParseIntError),
    IoError(std::io::Error),
    SetLoggerError(log::SetLoggerError),
    RustlsError(rustls::TLSError),
    OpenSslError(openssl::error::ErrorStack),
    TLSConfigError(crate::tls::TLSConfigError),
    DieselPoolError(diesel::r2d2::PoolError),
    DieselResultError(diesel::result::Error),
    NumTryFromIntError(std::num::TryFromIntError),
    BadRequestError(String),
    ActixCanceledBlockingError(),

    /// Should be used when the CCW/RIF/source data contains unexpected or unsupported values.
    InvalidSourceDataError(String),
}

impl From<std::env::VarError> for AppError {
    fn from(err: std::env::VarError) -> AppError {
        AppError::VarError(err)
    }
}

impl From<std::num::ParseIntError> for AppError {
    fn from(err: std::num::ParseIntError) -> AppError {
        AppError::ParseIntError(err)
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

impl From<openssl::error::ErrorStack> for AppError {
    fn from(err: openssl::error::ErrorStack) -> AppError {
        AppError::OpenSslError(err)
    }
}

impl From<crate::tls::TLSConfigError> for AppError {
    fn from(err: crate::tls::TLSConfigError) -> AppError {
        AppError::TLSConfigError(err)
    }
}

impl From<diesel::r2d2::PoolError> for AppError {
    fn from(err: diesel::r2d2::PoolError) -> AppError {
        AppError::DieselPoolError(err)
    }
}

impl From<diesel::result::Error> for AppError {
    fn from(err: diesel::result::Error) -> AppError {
        AppError::DieselResultError(err)
    }
}

impl From<std::num::TryFromIntError> for AppError {
    fn from(err: std::num::TryFromIntError) -> AppError {
        AppError::NumTryFromIntError(err)
    }
}

impl From<actix_web::error::BlockingError<AppError>> for AppError {
    fn from(err: actix_web::error::BlockingError<AppError>) -> AppError {
        match err {
            actix_web::error::BlockingError::Error(err) => err,
            actix_web::error::BlockingError::Canceled => AppError::ActixCanceledBlockingError {},
        }
    }
}

impl std::fmt::Display for AppError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match *self {
            _ => write!(f, "Application error: {:?}", self),
        }
    }
}

impl actix_web::error::ResponseError for AppError {
    fn error_response(&self) -> actix_web::HttpResponse {
        match *self {
            AppError::DieselResultError(_) => {
                actix_web::HttpResponse::new(actix_web::http::StatusCode::INTERNAL_SERVER_ERROR)
            }
            AppError::BadRequestError(ref message) => actix_web::HttpResponse::BadRequest()
                .content_type("text/html")
                .body(message),
            _ => actix_web::HttpResponse::new(actix_web::http::StatusCode::INTERNAL_SERVER_ERROR),
        }
    }
}
