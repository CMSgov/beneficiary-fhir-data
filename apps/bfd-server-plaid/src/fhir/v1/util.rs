use crate::fhir::constants::*;
use crate::fhir::util::ClaimType;
use crate::fhir::v1::code_systems::*;
use crate::fhir::v1::structure::*;

/// Returns a `Reference` for the FHIR Patient resource with the specified `Patient.id` value.
pub fn create_eob_id(claim_type: ClaimType, claim_id: &str) -> String {
    let prefix = match claim_type {
        crate::fhir::util::ClaimType::PartDEvent => "pde",
    };
    format!("{}-{}", prefix, claim_id)
}

/// Returns a `Reference` for the FHIR Patient resource with the specified `Patient.id` value.
pub fn reference_patient_by_id(patient_id: &str) -> Reference {
    Reference {
        reference: Some(patient_id.to_string()),
    }
}

/// Returns a `CodeableConcept` for the
pub fn create_eob_type_concept(claim_type: ClaimType) -> CodeableConcept {
    // Every EOB will have a type_bfd.
    let type_bfd = Coding {
        system: Some(format!(
            "{}{}",
            CODING_SYSTEM_BFD_BASE, CODING_SYSTEM_BFD_EOB_TYPE
        )),
        code: Some(String::from("PDE")),
        display: None,
    };
    let code_fhir = match claim_type {
        crate::fhir::util::ClaimType::PartDEvent => Some(&claim_type::PHARMACY),
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
