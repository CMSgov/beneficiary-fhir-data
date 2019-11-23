use phf;

include!(concat!(env!("OUT_DIR"), "/npi_descriptions.rs"));

/// Returns the National Provider Identifier (NPI) description for the specified NPI.
pub fn lookup_npi_description(npi: &str) -> Option<&str> {
    match NPI_DESCRIPTIONS.get(npi) {
        Some(npi_description) => Some(npi_description),
        None => None,
    }
}
