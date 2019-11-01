/// The base URL/URI/system for FHIR output when the domain concept in question is owned by (or at
/// least documented by) the BFD and/or Blue Button API team.
///
/// This URL will never be used by itself; it will always be suffixed with a more specific path.
pub static CODING_SYSTEM_BFD_BASE: &'static str = "https://bluebutton.cms.gov/resources";

/// Used as the `Coding.system` suffix for the `ExplanationOfBenefit.type` entry that each EOB's
/// `ClaimType` is mapped to.
pub static CODING_SYSTEM_BFD_EOB_TYPE: &'static str = "/codesystem/eob-type";
