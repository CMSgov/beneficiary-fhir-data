//! Contains the FHIR structures used in v1 of this API, which uses FHIR R3.
//!
//! Note: We want to match FHIR naming conventions here, so we aren't using snake case.
#![allow(non_snake_case)]

use chrono::{DateTime, Utc};
use serde::Serialize;

#[derive(Debug, Serialize)]
#[serde(untagged)]
pub enum Resource {
    ExplanationOfBenefit(ExplanationOfBenefit),
}

#[derive(Debug, Serialize)]
pub struct ResourceMeta {
    pub lastUpdated: DateTime<Utc>,
}

#[derive(Debug, Serialize)]
#[serde(tag = "resourceType")]
pub struct Bundle {
    pub id: String,
    pub meta: ResourceMeta,
    pub r#type: String,
    // FYI: FHIR has a max of 2,147,483,647, while Rust's u32 has a max of 4,294,967,295.
    pub total: u32,
    pub link: Vec<BundleLink>,
    pub entry: Vec<BundleEntry>,
}

#[derive(Debug, Serialize)]
pub struct BundleLink {
    pub relation: String,
    pub url: String,
}

#[derive(Debug, Serialize)]
pub struct BundleEntry {
    pub resource: Resource,
}

#[derive(Debug, Default, Serialize)]
pub struct Reference {
    pub reference: Option<String>,
}

#[derive(Debug, Default, Serialize)]
pub struct CodeableConcept {
    pub coding: Vec<Coding>,
}

#[derive(Debug, Default, Serialize)]
pub struct Coding {
    pub system: Option<String>,
    pub code: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display: Option<String>,
}

#[derive(Debug, Default, Serialize)]
pub struct Identifier {
    pub system: Option<String>,
    pub value: Option<String>,
}

#[derive(Debug, Default, Serialize)]
pub struct ExplanationOfBenefitInsurance {
    pub coverage: Option<Reference>,
}

#[derive(Debug, Default, Serialize)]
pub struct ExplanationOfBenefit {
    pub resourceType: String,
    pub id: String,
    pub status: Option<String>,
    pub patient: Option<Reference>,
    pub r#type: Option<CodeableConcept>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub identifier: Vec<Identifier>,
    pub insurance: Option<ExplanationOfBenefitInsurance>,
    // TODO flesh out the rest of this
}
