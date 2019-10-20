use crate::db::{PgPool, PgPooledConnection};
use crate::error;
use crate::fhir::util;
use actix_web::{web, HttpResponse, Responder};
use serde::Deserialize;

#[derive(Deserialize)]
pub struct EobQueryParams {
    patient: String,
}

pub fn eob_for_bene_id(
    db_pool: web::Data<PgPool>,
    query_params: web::Query<EobQueryParams>,
) -> error::Result<HttpResponse> {
    let bene_id = util::parse_relative_reference_expected(&query_params.patient, "Patient").ok_or(
        error::AppError::BadRequestError(String::from(
            "Unable to parse the specified 'patient' parameter.",
        )),
    )?;
    let db_connection = db_pool
        .get()
        .map_err(|err| error::AppError::DieselPoolError(err))?;
    let claims_partd = crate::db::claims_partd_by_bene_id(&db_connection, &bene_id);
    Ok(HttpResponse::Ok()
        .content_type("text/html")
        .body("claims data goes here"))
}
