/// The base URL/URI/system for FHIR output when the domain concept in question is owned by (or at
/// least documented by) the BFD and/or Blue Button API team.
///
/// This URL will never be used by itself; it will always be suffixed with a more specific path.
pub static SYSTEM_BFD_BASE: &'static str = "https://bluebutton.cms.gov/resources";

/// Used as the `Coding.system` suffix for the `ExplanationOfBenefit.type` entry that each EOB's
/// `ClaimType` is mapped to.
pub static SYSTEM_BFD_CODING_EOB_TYPE: &'static str = "/codesystem/eob-type";

/// The base URL/URI/system for FHIR output related to `CcwCodebookVariable`:
///
/// * `Extension.url`
/// * `Coding.system`
/// * `Identifier.system`
///
/// This URL will never be used by itself; it will always be suffixed with the (lower-cased)
/// `CcwCodebookVariable`'s `id`.
pub static SYSTEM_BFD_CCW_CODEBOOK_BASE: &'static str = "/variables";

/// Used as the `Identifier.system` that the RIF `CLM_GROUP_ID` fields are mapped to.
pub static SYSTEM_BFD_IDENTIFIER_CLAIM_GROUP: &'static str = "/identifier/claim-group";

/// Represents the Medicare segments/parts supported by the application.
pub struct MedicareSegment {
    pub coverage_url_prefix: &'static str,
}

/// A `MedicareSegment` for Part D.
pub static MEDICARE_SEGMENT_PART_D: MedicareSegment = MedicareSegment {
    coverage_url_prefix: "part-d",
};

/// The CMS-custom `Coding.system` for Medicare `Adjudication`s.
pub static SYSTEM_BFD_ADJUDICATION_CATEGORY: &'static str = "/codesystem/adjudication";

/// The standard `Money.system` for currency. (It looks odd that it has "iso" in there twice, but
/// some web searches seem to confirm that that's correct.)
pub static SYSTEM_MONEY: &'static str = "urn:iso:std:iso:4217";

/// The standard `Money.code` for US currency.
pub static CODE_MONEY_USD: &'static str = "USD";
