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

struct CcwCodebookVariableBuilder {
    id: &'static str,
    label: Option<&'static str>,
    value_groups: Vec<ValueGroup>,
}

struct ValueGroup {
    #[allow(dead_code)]
    description: Option<&'static str>,
    values: Vec<Value>,
}

struct ValueGroupBuilder {
    parent: CcwCodebookVariableBuilder,
    #[allow(dead_code)]
    description: Option<&'static str>,
    values: Vec<Value>,
}

struct Value {
    code: &'static str,
    description: &'static str,
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
            Some(matching_values.get(0).unwrap().description.to_string())
        } else {
            None
        }
    }

    /// Returns a `CcwCodebookVariableBuilder` for a `CcwCodebookVariable` with the specified `id`.
    fn new(id: &'static str) -> CcwCodebookVariableBuilder {
        CcwCodebookVariableBuilder {
            id: id,
            label: None,
            value_groups: vec![],
        }
    }
}

/// A builder for `CcwCodebookVariable`s.
impl CcwCodebookVariableBuilder {
    /// Sets `CcwCodebookVariable.label` to the specified value.
    fn label(mut self, label: &'static str) -> CcwCodebookVariableBuilder {
        self.label = Some(label);
        self
    }

    /// Returns a `ValueGroupBuilder`, which will add a new `ValueGroup` to
    /// `CcwCodebookVariable.value_groups` when `ValueGroupBuilder.group_end()` is called.
    fn group_start(self) -> ValueGroupBuilder {
        ValueGroupBuilder {
            parent: self,
            description: None,
            values: vec![],
        }
    }

    /// Completes the `CcwCodebookVariableBuilder`, returning a new `CcwCodebookVariable` based on
    /// it.
    fn build(self) -> CcwCodebookVariable {
        CcwCodebookVariable {
            id: self.id,
            label: self.label.unwrap(),
            value_groups: self.value_groups,
        }
    }
}

/// A builder for `ValueGroup`s.
impl ValueGroupBuilder {
    /// Adds a new `Value` to `ValueGroup.values`.
    fn value(mut self, code: &'static str, description: &'static str) -> ValueGroupBuilder {
        self.values.push(Value {
            code: code,
            description,
        });
        self
    }

    /// Completes the `ValueGroupBuilder`, adding the `ValueGroup` to the original
    /// `CcwCodebookVariableBuilder` and then returning that `CcwCodebookVariableBuilder`.
    fn group_end(mut self) -> CcwCodebookVariableBuilder {
        self.parent.value_groups.push(ValueGroup {
            description: self.description,
            values: self.values,
        });
        self.parent
    }
}

lazy_static! {
    pub static ref PDE_ID: CcwCodebookVariable = CcwCodebookVariable::new("PDE_ID")
        .label("CCW Encrypted Part D Event Number")
        .build();
    pub static ref RX_SRVC_RFRNC_NUM: CcwCodebookVariable =
        CcwCodebookVariable::new("RX_SRVC_RFRNC_NUM")
            .label("RX Service Reference Number")
            .build();
    pub static ref PLAN_CNTRCT_REC_ID: CcwCodebookVariable =
        CcwCodebookVariable::new("PLAN_CNTRCT_REC_ID")
            .label("Plan Contract ID")
            .build();
    pub static ref PLAN_PBP_REC_NUM: CcwCodebookVariable =
        CcwCodebookVariable::new("PLAN_PBP_REC_NUM")
            .label("Plan Benefit Package ID")
            .build();
    pub static ref CVRD_D_PLAN_PD_AMT: CcwCodebookVariable =
        CcwCodebookVariable::new("CVRD_D_PLAN_PD_AMT")
            .label("Amount paid by Part D plan for the PDE (drug is covered by Part D)")
            .build();
    pub static ref NCVRD_PLAN_PD_AMT: CcwCodebookVariable =
        CcwCodebookVariable::new("NCVRD_PLAN_PD_AMT")
            .label("Amount paid by Part D plan for the PDE (drug is not covered by Part D)")
            .build();
    pub static ref DRUG_CVRG_STUS_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("DRUG_CVRG_STUS_CD")
            .label("Drug Coverage Status Code")
            .group_start()
            .value("C", "Covered")
            .value(
                "E",
                "Supplemental drugs (reported by plans that provide Enhanced Alternative coverage)"
            )
            .value("O", "Over-the-counter drugs")
            .group_end()
            .build();
}
