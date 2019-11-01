/// Represents an entry in one of the FHIR predefined value set from
/// <https://hl7.org/fhir/STU3/terminologies-valuesets.html>.
pub struct ValueSetCode {
    pub system: &'static str,
    pub code: &'static str,
    pub display: &'static str,
    pub definition: &'static str,
}

pub mod claim_type {
    static SYSTEM: &'static str = "http://hl7.org/fhir/ex-claimtype";

    /// Entry for <https://hl7.org/fhir/STU3/valueset-claim-type.html>.
    pub static PHARMACY: super::ValueSetCode = super::ValueSetCode {
        system: SYSTEM,
        code: "pharmacy",
        display: "Pharmacy",
        definition: "Pharmacy claims for goods and services.",
    };
}
