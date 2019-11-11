//! Enumerates the CCW codebook variables, from the codebook PDFs on
//! <https://www.ccwdata.org/web/guest/data-dictionaries>.

use lazy_static::lazy_static;

pub struct CcwCodebookVariable {
    // TODO should identify which codebook each comes from, rather than merging
    /// The unique-within-a-codebook identifier for this `CcwCodebookVariable`.
    pub id: &'static str,
    /// A short description for this `CcwCodebookVariable`, typically no more than a few (English)
    /// words long.
    pub label: &'static str,
    value_groups: Vec<ValueGroup>,
    // TODO flesh out
}

struct ValueGroup {
    #[allow(dead_code)]
    description: Option<String>,
    values: Vec<Value>,
}

struct Value {
    code: String,
    description: String,
}

impl CcwCodebookVariable {
    /// Returns the description for the specified code in this `CcwCodebookVariable`, or `None` if
    /// a match could not be found.
    pub fn lookup_description(&self, code: &str) -> Option<String> {
        // Find code matches.
        let matching_values: Vec<&Value> = self
            .value_groups
            .iter()
            .flat_map(|g| g.values.iter())
            .filter(|v| v.code == code)
            .collect();

        /*
         * Both our source data and the `CcwCodebookVariable` data is messy, so we may very well
         * get 0, 1, or many matches here. We only return a match for exactly one result, though,
         * to be safe.
         */
        if matching_values.len() == 1 {
            Some(matching_values.get(0).unwrap().description.clone())
        } else {
            None
        }
    }
}

lazy_static! {
    pub static ref PDE_ID: CcwCodebookVariable = CcwCodebookVariable {
        id: "PDE_ID",
        label: "CCW Encrypted Part D Event Number",
        value_groups: vec![],
    };
    pub static ref RX_SRVC_RFRNC_NUM: CcwCodebookVariable = CcwCodebookVariable {
        id: "RX_SRVC_RFRNC_NUM",
        label: "RX Service Reference Number",
        value_groups: vec![],
    };
    pub static ref PLAN_CNTRCT_REC_ID: CcwCodebookVariable = CcwCodebookVariable {
        id: "PLAN_CNTRCT_REC_ID",
        label: "Plan Contract ID",
        value_groups: vec![],
    };
    pub static ref PLAN_PBP_REC_NUM: CcwCodebookVariable = CcwCodebookVariable {
        id: "PLAN_PBP_REC_NUM",
        label: "Plan Benefit Package ID",
        value_groups: vec![],
    };
    pub static ref CVRD_D_PLAN_PD_AMT: CcwCodebookVariable = CcwCodebookVariable {
        id: "CVRD_D_PLAN_PD_AMT",
        label: "Amount paid by Part D plan for the PDE (drug is covered by Part D)",
        value_groups: vec![],
    };
    pub static ref NCVRD_PLAN_PD_AMT: CcwCodebookVariable = CcwCodebookVariable {
        id: "NCVRD_PLAN_PD_AMT",
        label: "Amount paid by Part D plan for the PDE (drug is not covered by Part D)",
        value_groups: vec![],
    };
    pub static ref DRUG_CVRG_STUS_CD: CcwCodebookVariable = CcwCodebookVariable {
        id: "DRUG_CVRG_STUS_CD",
        label: "Drug Coverage Status Code",
        value_groups: vec![ValueGroup {
            description: None,
            values: vec![Value {
                code: "C".to_string(),
                description: "Covered".to_string(),
            },
            Value {
                code: "E".to_string(),
                description: "Supplemental drugs (reported by plans that provide Enhanced Alternative coverage)".to_string(),
            },Value {
                code: "O".to_string(),
                description: "Over-the-counter drugs".to_string(),
            }],
        }],
    };
}
