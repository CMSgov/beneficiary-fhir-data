//! Contains the FHIR structures used in v1 of this API, which uses FHIR R3.

use chrono::{DateTime, Utc};
use serde::Serialize;

#[derive(Serialize)]
pub enum Resource {
    ExplanationOfBenefit(ExplanationOfBenefit),
}

#[derive(Serialize)]
pub struct ResourceMeta {
    pub lastUpdated: DateTime<Utc>,
}

#[derive(Serialize)]
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

#[derive(Serialize)]
pub struct BundleLink {
    pub relation: String,
    pub url: String,
}

#[derive(Serialize)]
pub struct BundleEntry {
    pub resource: Resource,
}

#[derive(Serialize)]
pub struct ExplanationOfBenefit {
    pub id: String,
    // TODO flesh out the rest of this
}
