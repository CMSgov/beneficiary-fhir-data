use crate::db::{PgPool, PgPooledConnection};
use crate::error;
use crate::fhir::util;
use crate::fhir::v1::structure::*;
use crate::models::PartDEvent;
use actix_web::{web, HttpResponse, Responder};
use chrono::Utc;
use serde::Deserialize;
use std::convert::TryFrom;

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
    let claims_partd = crate::db::claims_partd_by_bene_id(&db_connection, &bene_id)?;
    let bundle = Bundle {
        id: String::from("TODO"),
        meta: ResourceMeta {
            lastUpdated: Utc::now(),
        },
        r#type: String::from("searchset"),
        // FYI: FHIR has a max of 2,147,483,647, while Rust's u32 has a max of 4,294,967,295.
        total: u32::try_from(claims_partd.len())?,
        link: vec![BundleLink {
            relation: String::from("self"),
            url: String::from("TODO"),
        }],
        entry: claims_partd
            .iter()
            .map(transform_claim_partd)
            .map(|eob| BundleEntry {
                resource: Resource::ExplanationOfBenefit(eob),
            })
            .collect(),
    };
    Ok(HttpResponse::Ok()
        .content_type("application/fhir+json")
        .json(bundle))
}

fn transform_claim_partd(claim: &PartDEvent) -> ExplanationOfBenefit {
    ExplanationOfBenefit {
        id: claim.PDE_ID.clone(),
        // TODO flesh out the rest of this
    }
}
