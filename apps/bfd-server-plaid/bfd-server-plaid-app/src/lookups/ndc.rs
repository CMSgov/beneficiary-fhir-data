use phf;

include!(concat!(env!("OUT_DIR"), "/ndc_descriptions.rs"));

/// Returns the National Drug Code (NDC) description for the specified NDC code.
pub fn lookup_ndc_description(ndc_code: &str) -> Option<&str> {
    match NDC_DESCRIPTIONS.get(ndc_code) {
        Some(ndc_description) => Some(ndc_description),
        None => None,
    }
}
