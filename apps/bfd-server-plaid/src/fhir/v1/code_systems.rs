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

    pub static PHARMACY: super::ValueSetCode = super::ValueSetCode {
        system: SYSTEM,
        code: "pharmacy",
        display: "Pharmacy",
        definition: "Pharmacy claims for goods and services.",
    };
}

pub mod explanation_of_benefit {
    pub mod status {
        static SYSTEM: &'static str = "http://hl7.org/fhir/ValueSet/explanationofbenefit-status";

        pub static ACTIVE: super::super::ValueSetCode = super::super::ValueSetCode {
            system: SYSTEM,
            code: "active",
            display: "Active",
            definition: "The resource instance is currently in-force.",
        };

        pub static CANCELLED: super::super::ValueSetCode = super::super::ValueSetCode {
            system: SYSTEM,
            code: "cancelled",
            display: "Cancelled",
            definition: "The resource instance is withdrawn, rescinded or reversed.",
        };
    }
    pub mod act_invoice_group {
        static SYSTEM: &'static str = "http://hl7.org/fhir/v3/ActCode";

        pub static RXCINV: super::super::ValueSetCode = super::super::ValueSetCode {
            system: SYSTEM,
            code: "RXCINV",
            display: "Rx compound invoice",
            definition: "Pharmacy dispense invoice for a compound.",
        };

        pub static RXDINV: super::super::ValueSetCode = super::super::ValueSetCode {
            system: SYSTEM,
            code: "RXDINV",
            display: "Rx dispense invoice",
            definition: "Pharmacy dispense invoice not involving a compound",
        };
    }
}
