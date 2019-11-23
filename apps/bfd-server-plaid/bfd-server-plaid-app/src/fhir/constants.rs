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

/// The CMS-custom `Coding.system` value for Medicare `Information.category`s.
pub static SYSTEM_BFD_INFORMATION_CATEGORY: &'static str = "/codesystem/information";

/// The standard `Money.system` for currency. (It looks odd that it has "iso" in there twice, but
/// some web searches seem to confirm that that's correct.)
pub static SYSTEM_MONEY: &'static str = "urn:iso:std:iso:4217";

/// The standard `Money.code` for US currency.
pub static CODE_MONEY_USD: &'static str = "USD";

/// The `Identifier.system` for United States National Provider Identifiers, as available at
/// [NPI/NPPES File](http://download.cms.gov/nppes/NPI_Files.html).
pub static SYSTEM_NPI_US: &'static str = "http://hl7.org/fhir/sid/us-npi";

/// Used to identify the drugs that were purchased as part of Part D, Carrier, and DME claims. See
/// here for more information on using NDC codes with FHIR:
/// [Using NDC and NHRIC Codes with FHIR](http://hl7.org/fhir/ndc.html).
pub static SYSTEM_NDC: &'static str = "http://hl7.org/fhir/sid/ndc";
