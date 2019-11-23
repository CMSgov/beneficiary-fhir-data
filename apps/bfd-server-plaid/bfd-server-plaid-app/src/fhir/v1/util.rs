use crate::ccw_codebook;
use crate::ccw_codebook::CcwCodebookVariable;
use crate::error;
use crate::fhir::constants::*;
use crate::fhir::util::ClaimType;
use crate::fhir::v1::code_systems;
use crate::fhir::v1::structures::explanation_of_benefit::*;
use crate::fhir::v1::structures::*;
use crate::models::traits::*;
use bfd_server_plaid_lookups;

/// Maps the common claim header CCW fields into the specified `ExplanationOfBenefit`.
pub fn map_claim_header_common<T: PartABDClaim>(
    claim: &T,
    mut eob: ExplanationOfBenefit,
) -> error::Result<ExplanationOfBenefit> {
    eob.resourceType = String::from("ExplanationOfBenefit");
    eob.id = create_eob_id(ClaimType::PartDEvent, &claim.claim_id());
    eob.status = Some(match &claim.final_action_code() as &str {
        "F" => code_systems::explanation_of_benefit::status::ACTIVE
            .code
            .to_string(),
        "N" => code_systems::explanation_of_benefit::status::CANCELLED
            .code
            .to_string(),
        _ => "".to_string(), // FIXME return error
    });
    eob.patient = Some(reference_patient_by_id(&claim.beneficiary_id()));
    eob.r#type = Some(create_eob_type_concept(ClaimType::PartDEvent));

    // Populate EOB.identifier.
    let mut eob_identifiers = vec![];
    // FIXME in v2, need to use same value as EOB.id
    let claim_identifier = create_identifier(&ccw_codebook::PDE_ID, &claim.claim_id());
    eob_identifiers.push(claim_identifier);
    let claim_group_identifier = Identifier {
        system: Some(format!(
            "{}{}",
            SYSTEM_BFD_BASE, SYSTEM_BFD_IDENTIFIER_CLAIM_GROUP
        )),
        value: Some(claim.claim_group_id().to_string()),
    };
    eob_identifiers.push(claim_group_identifier);
    eob.identifier = eob_identifiers;
    eob.insurance = Some(Insurance {
        coverage: Some(reference_coverage(
            &claim.beneficiary_id(),
            &MEDICARE_SEGMENT_PART_D,
        )),
    });

    // TODO flesh out the rest of this

    Ok(eob)
}

/// Returns a `Reference` for the FHIR Patient resource with the specified `Patient.id` value.
fn create_eob_id(claim_type: ClaimType, claim_id: &str) -> String {
    let prefix = match claim_type {
        crate::fhir::util::ClaimType::PartDEvent => "pde",
    };
    format!("{}-{}", prefix, claim_id)
}

/// Returns a `Reference` for the FHIR Patient resource with the specified `Patient.id` value.
fn reference_patient_by_id(patient_id: &str) -> Reference {
    Reference {
        extension: vec![],
        reference: Some(format!("Patient/{}", patient_id.to_string())),
        identifier: None,
        display: None,
    }
}

/// Returns a `Reference` for the FHIR Coverage resource for the specified CCW `BENE_ID`.
fn reference_coverage(patient_id: &str, medicare_segment: &MedicareSegment) -> Reference {
    Reference {
        extension: vec![],
        reference: Some(format!(
            "Coverage/{}-{}",
            medicare_segment.coverage_url_prefix, patient_id
        )),
        identifier: None,
        display: None,
    }
}

/// Returns a `Reference` where `Reference.identifier` points to the specified NPI.
pub fn create_reference_to_npi(npi: &str) -> Reference {
    let npi_display = bfd_server_plaid_lookups::npi::lookup_npi_description(npi);
    Reference {
        extension: vec![],
        reference: None,
        identifier: Some(Identifier {
            system: Some(SYSTEM_NPI_US.to_string()),
            value: Some(npi.to_string()),
        }),
        display: npi_display.map(String::from),
    }
}

/// Returns a `CodeableConcept` for the `ExplanationOfBenefit.type` field.
fn create_eob_type_concept(claim_type: ClaimType) -> CodeableConcept {
    // Every EOB will have a type_bfd.
    let type_bfd = Coding {
        system: Some(format!("{}{}", SYSTEM_BFD_BASE, SYSTEM_BFD_CODING_EOB_TYPE)),
        code: Some(String::from("PDE")),
        display: None,
    };
    let code_fhir = match claim_type {
        crate::fhir::util::ClaimType::PartDEvent => Some(&code_systems::claim_type::PHARMACY),
    };

    // Most EOBs will have a type_fhir.
    let type_fhir = match code_fhir {
        Some(ref code_fhir) => Some(Coding {
            system: Some(code_fhir.system.to_string()),
            code: Some(code_fhir.code.to_string()),
            display: Some(code_fhir.display.to_string()),
        }),
        None => None,
    };

    let mut coding = vec![type_bfd];
    if type_fhir.is_some() {
        coding.push(type_fhir.unwrap());
    }

    CodeableConcept { coding: coding }
}

/// Create an `Identifier` with the specified value, for the specified `CcwCodebookVariable`.
pub fn create_identifier(codebook_var: &CcwCodebookVariable, value: &str) -> Identifier {
    Identifier {
        system: Some(create_codebook_system(codebook_var)),
        value: Some(value.to_string()),
    }
}

/// Returns the system/URI/URL that should be used for `Coding`s, `Identifier`s, etc. based on the
/// specified `CcwCodebookVariable`.
fn create_codebook_system(codebook_var: &CcwCodebookVariable) -> String {
    format!(
        "{}{}/{}",
        SYSTEM_BFD_BASE,
        SYSTEM_BFD_CCW_CODEBOOK_BASE,
        codebook_var.id.to_lowercase()
    )
}

/// Creates a `CodeableConcept` containing just the specified `ValueSetCode`.
pub fn create_concept_for_value_set_code(
    value_set_code: &code_systems::ValueSetCode,
) -> CodeableConcept {
    CodeableConcept {
        coding: vec![Coding {
            system: Some(value_set_code.system.to_string()),
            code: Some(value_set_code.code.to_string()),
            display: Some(value_set_code.display.to_string()),
        }],
    }
}

/// Creates a `CodeableConcept` for use as an `Adjudication.category`.
pub fn create_concept_for_codebook_value(
    codebook_var: &CcwCodebookVariable,
    code: &str,
) -> CodeableConcept {
    CodeableConcept {
        coding: vec![Coding {
            system: Some(create_codebook_system(codebook_var)),
            code: Some(code.to_string()),
            display: codebook_var.lookup_description(code),
        }],
    }
}

/// Creates a `CodeableConcept` to represent a National Drug Code (NDC).
pub fn create_concept_for_ndc(ndc: &str) -> CodeableConcept {
    let ndc_display = bfd_server_plaid_lookups::ndc::lookup_ndc_description(ndc);
    CodeableConcept {
        coding: vec![Coding {
            system: Some(SYSTEM_NDC.to_string()),
            code: Some(ndc.to_string()),
            display: ndc_display.map(String::from),
        }],
    }
}

/// Creates a `CodeableConcept` for use as an `Adjudication.category`.
pub fn create_adjudication_category_concept(codebook_var: &CcwCodebookVariable) -> CodeableConcept {
    CodeableConcept {
        coding: vec![Coding {
            system: Some(format!(
                "{}{}",
                SYSTEM_BFD_BASE, SYSTEM_BFD_ADJUDICATION_CATEGORY
            )),
            code: Some(create_codebook_system(codebook_var)),
            display: Some(codebook_var.label.to_string()),
        }],
    }
}

/// Creates a `CodeableConcept` for use as an `Information.category`.
pub fn create_information_category_concept(codebook_var: &CcwCodebookVariable) -> CodeableConcept {
    CodeableConcept {
        coding: vec![Coding {
            system: Some(format!(
                "{}{}",
                SYSTEM_BFD_BASE, SYSTEM_BFD_INFORMATION_CATEGORY
            )),
            code: Some(create_codebook_system(codebook_var)),
            display: Some(codebook_var.label.to_string()),
        }],
    }
}

/// Creates a `CodeableConcept` for use as an `Adjudication.category`.
pub fn create_quantity_from_big_decimal(value: &bigdecimal::BigDecimal) -> Quantity {
    Quantity {
        extension: vec![],
        // FIXME Is there a cleaner way to do this, instead of this non-public API?
        value: Some(serde_json::Number::from_string_unchecked(value.to_string())),
        system: None,
        code: None,
    }
}

/// Creates a `CodeableConcept` for use as an `Adjudication.category`.
pub fn create_money_from_big_decimal(value: &bigdecimal::BigDecimal) -> Quantity {
    let mut quantity = create_quantity_from_big_decimal(value);
    quantity.system = Some(SYSTEM_MONEY.to_string());
    quantity.code = Some(CODE_MONEY_USD.to_string());

    quantity
}

/// Creates a `CodeableConcept` for use as an `Adjudication.category`.
pub fn create_adjudication_amount(
    codebook_var: &CcwCodebookVariable,
    value: &bigdecimal::BigDecimal,
) -> Adjudication {
    Adjudication {
        category: Some(create_adjudication_category_concept(codebook_var)),
        reason: None,
        amount: Some(create_money_from_big_decimal(value)),
    }
}

/// Adds the specified NPI to the `EOB.careTeam`, if it's not already present.
///
/// # Arguments
/// * `eob` - The `ExplanationOfBenefit` to modify.
/// * `item` - The `Item` whose `careTeamLinkId` should reference the `CareTeam`, or `None` if no such reference is needed.
/// * `npi` - The NPI identifier that a `CareTeam` should be created for.
/// * `care_team_role` - The code to use in `CareTeam.role`.
pub fn map_care_team_npi(
    mut eob: ExplanationOfBenefit,
    item: Option<&mut Item>,
    npi: &str,
    care_team_role: &code_systems::ValueSetCode,
) -> error::Result<ExplanationOfBenefit> {
    // Is a matching CareTeam already present?
    // FIXME also verify that `CareTeam.role` matches.
    let reference = create_reference_to_npi(npi);
    if !eob.careTeam.iter().any(|c| c.provider == reference) {
        let care_team = CareTeam {
            sequence: eob.careTeam.iter().map(|c| c.sequence).max().unwrap_or(0) + 1,
            provider: reference,
            role: Some(create_concept_for_value_set_code(care_team_role)),
        };
        eob.careTeam.push(care_team);
        match item {
            Some(item) => {
                item.careTeamLinkId = Some(eob.careTeam.last().unwrap().sequence);
            }
            None => {}
        }
    }
    Ok(eob)
}

/// Adds a new `Information` entry to `EOB.information`, and returns it.
///
/// # Arguments
/// * `eob` - The `ExplanationOfBenefit` to modify.
/// * `codebook_var` - The `CcwCodebookVariable` to use for the `Information.category`.
fn add_information(
    eob: &mut ExplanationOfBenefit,
    codebook_var: &CcwCodebookVariable,
) -> Information {
    Information {
        sequence: eob
            .information
            .iter()
            .map(|i| i.sequence)
            .max()
            .unwrap_or(0)
            + 1,
        category: create_information_category_concept(codebook_var),
        code: None,
    }
}

/// Adds a new `Information` entry to `EOB.information`, and returns it.
///
/// # Arguments
/// * `eob` - The `ExplanationOfBenefit` to modify.
/// * `codebook_var` - The `CcwCodebookVariable` to use for the `Information.category`.
/// * `code` - The code value to use in `Information.code`.
pub fn add_information_with_code(
    eob: &mut ExplanationOfBenefit,
    codebook_var: &CcwCodebookVariable,
    code: &str,
) -> Information {
    let mut information = add_information(eob, codebook_var);
    information.code = Some(create_concept_for_codebook_value(codebook_var, code));
    information
}

/// Converts a `Quantity` to a `Extension` with an `ExtensionValue::ValueIdentifier`.
pub fn create_extension_identifier(
    codebook_var: &CcwCodebookVariable,
    value: Identifier,
) -> Extension {
    Extension {
        url: create_codebook_system(codebook_var),
        value: ExtensionValue::ValueIdentifier(value),
    }
}

/// Converts a `Quantity` to a `Extension` with an `ExtensionValue::ValueQuanitity`.
pub fn create_extension_quantity(codebook_var: &CcwCodebookVariable, value: Quantity) -> Extension {
    Extension {
        url: create_codebook_system(codebook_var),
        value: ExtensionValue::ValueQuantity(value),
    }
}

/// Converts a `CodeableConcept` to a `Extension` with an `ExtensionValue::ValueCodeableConcept`.
pub fn create_extension_concept(
    codebook_var: &CcwCodebookVariable,
    value: CodeableConcept,
) -> Extension {
    Extension {
        url: create_codebook_system(codebook_var),
        value: ExtensionValue::ValueCodeableConcept(value),
    }
}
