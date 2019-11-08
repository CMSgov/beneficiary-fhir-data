use crate::ccw_codebook;
use crate::ccw_codebook::CcwCodebookVariable;
use crate::error;
use crate::fhir::constants::*;
use crate::fhir::util::ClaimType;
use crate::fhir::v1::code_systems;
use crate::fhir::v1::structures::explanation_of_benefit::*;
use crate::fhir::v1::structures::*;
use crate::models::traits::*;

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

/// Create an `Extension` with an `Identifier` with the specified value, for the specified `CcwCodebookVariable`.
pub fn create_identifier_extension(codebook_var: &CcwCodebookVariable, value: &str) -> Extension {
    Extension {
        url: create_codebook_system(codebook_var),
        value: ExtensionValue::ValueIdentifier(Identifier {
            system: Some(create_codebook_system(codebook_var)),
            value: Some(value.to_string()),
        }),
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
