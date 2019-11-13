use crate::ccw_codebook;
use crate::db::PgPool;
use crate::error;
use crate::fhir::util;
use crate::fhir::v1::code_systems;
use crate::fhir::v1::structures::bundle::*;
use crate::fhir::v1::structures::explanation_of_benefit::*;
use crate::fhir::v1::structures::*;
use crate::fhir::v1::util::*;
use crate::models::structs::PartDEvent;
use actix_web::{web, HttpResponse};
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
    let eobs: error::Result<Vec<ExplanationOfBenefit>> =
        claims_partd.iter().map(transform_claim_partd).collect();
    let eobs: Vec<ExplanationOfBenefit> = eobs?;
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
        entry: eobs
            .into_iter()
            .map(|eob| BundleEntry {
                resource: Resource::ExplanationOfBenefit(eob),
            })
            .collect(),
    };
    Ok(HttpResponse::Ok()
        .content_type("application/fhir+json")
        .json(bundle))
}

/// Returns an `ExplanationOfBenefit` that represents the specified `PartDEvent`.
fn transform_claim_partd(claim: &PartDEvent) -> error::Result<ExplanationOfBenefit> {
    let eob = ExplanationOfBenefit::default();
    let mut eob = map_claim_header_common(claim, eob)?;
    eob.identifier.push(create_identifier(
        &ccw_codebook::RX_SRVC_RFRNC_NUM,
        &claim.RX_SRVC_RFRNC_NUM.to_string(),
    ));
    if let Some(ref mut insurance) = eob.insurance {
        if let Some(ref mut coverage) = insurance.coverage {
            coverage.extension = vec![
                create_identifier_extension(
                    &ccw_codebook::PLAN_CNTRCT_REC_ID,
                    &claim.PLAN_CNTRCT_REC_ID,
                ),
                create_identifier_extension(
                    &ccw_codebook::PLAN_PBP_REC_NUM,
                    &claim.PLAN_PBP_REC_NUM,
                ),
            ];
        }
    }
    if let Some(pd_dt) = claim.PD_DT {
        eob.payment = Some(Payment { date: Some(pd_dt) });
    }

    // Create the EOB's single Item, its Adjudications, and its single Detail.
    let mut item = explanation_of_benefit::Item::default();
    item.sequence = 1;
    let mut adjudications = vec![];
    let mut detail = explanation_of_benefit::Detail::default();

    // Map the EOB.item.detail.type field from CMPND_CD.
    let compound_code = match claim.CMPND_CD {
        0 => Ok(None),
        1 => Ok(Some(
            &code_systems::explanation_of_benefit::act_invoice_group::RXDINV,
        )),
        2 => Ok(Some(
            &code_systems::explanation_of_benefit::act_invoice_group::RXCINV,
        )),
        _ => Err(error::AppError::InvalidSourceDataError(format!(
            "Unsupported 'CMPND_CD' value."
        ))),
    }?;
    if let Some(compound_code) = compound_code {
        detail.r#type = Some(create_concept_from_value_set_code(compound_code));
    };

    // Map the prescription fill date.
    item.serviced = Some(Serviced::ServicedDate(claim.SRVC_DT));

    /*
     * Create an adjudication for either CVRD_D_PLAN_PD_AMT or NCVRD_PLAN_PD_AMT, depending on the
     * value of DRUG_CVRG_STUS_CD. Stick DRUG_CVRG_STUS_CD into the adjudication.reason field.
     */
    // FIXME should always map both CVRD_D_PLAN_PD_AMT and NCVRD_PLAN_PD_AMT
    let (category, amount) = match claim.DRUG_CVRG_STUS_CD.as_str() {
        "C" => (
            create_adjudication_category_concept(&ccw_codebook::CVRD_D_PLAN_PD_AMT),
            create_money_from_big_decimal(&claim.CVRD_D_PLAN_PD_AMT),
        ),
        _ => (
            create_adjudication_category_concept(&ccw_codebook::NCVRD_PLAN_PD_AMT),
            create_money_from_big_decimal(&claim.NCVRD_PLAN_PD_AMT),
        ),
    };
    let adjudication_drug_payment = Adjudication {
        category: Some(category),
        reason: Some(create_concept_for_codebook_value(
            &ccw_codebook::DRUG_CVRG_STUS_CD,
            &claim.DRUG_CVRG_STUS_CD,
        )),
        amount: Some(amount),
    };
    adjudications.push(adjudication_drug_payment);

    // Map the miscellaneous adjudication amounts.
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::GDC_BLW_OOPT_AMT,
        &claim.GDC_BLW_OOPT_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::GDC_ABV_OOPT_AMT,
        &claim.GDC_ABV_OOPT_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::PTNT_PAY_AMT,
        &claim.PTNT_PAY_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::OTHR_TROOP_AMT,
        &claim.OTHR_TROOP_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::LICS_AMT,
        &claim.LICS_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::PLRO_AMT,
        &claim.PLRO_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::TOT_RX_CST_AMT,
        &claim.TOT_RX_CST_AMT,
    ));
    adjudications.push(create_adjudication_amount(
        &ccw_codebook::RPTD_GAP_DSCNT_NUM,
        &claim.RPTD_GAP_DSCNT_NUM,
    ));

    // Map PRSCRBR_ID_QLFYR_CD.
    match claim.PRSCRBR_ID_QLFYR_CD.as_ref() {
        "" | "01" => {
            return Err(error::AppError::InvalidSourceDataError(
                "Invalid PRSCRBR_ID_QLFYR_CD value.".to_string(),
            ));
        }
        _ => {
            // FIXME why don't we map this?
        }
    }

    // Attach the EOB's single Item, Adjudications, and Detail.
    item.adjudication = adjudications;
    item.detail = vec![detail];
    eob.item = vec![item];

    // TODO flesh out the rest of this

    Ok(eob)
}
