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
    pub static ref GDC_BLW_OOPT_AMT: CcwCodebookVariable =
        CcwCodebookVariable::new("GDC_BLW_OOPT_AMT")
            .label("Gross Drug Cost Below Part D Out-of-Pocket Threshold (GDCB)")
            .build();
    pub static ref GDC_ABV_OOPT_AMT: CcwCodebookVariable =
        CcwCodebookVariable::new("GDC_ABV_OOPT_AMT")
            .label("Gross Drug Cost Above Part D Out-of-Pocket Threshold (GDCA)")
            .build();
    pub static ref PTNT_PAY_AMT: CcwCodebookVariable = CcwCodebookVariable::new("PTNT_PAY_AMT")
        .label("Amount Paid by Patient")
        .build();
    pub static ref OTHR_TROOP_AMT: CcwCodebookVariable = CcwCodebookVariable::new("OTHR_TROOP_AMT")
        .label("Other True Out-of-Pocket (TrOOP) Amount")
        .build();
    pub static ref LICS_AMT: CcwCodebookVariable = CcwCodebookVariable::new("LICS_AMT")
        .label("Amount paid for the PDE by Part D low income subsidy")
        .build();
    pub static ref PLRO_AMT: CcwCodebookVariable = CcwCodebookVariable::new("PLRO_AMT")
        .label("Reduction in patient liability due to payments by other payers (PLRO)")
        .build();
    pub static ref TOT_RX_CST_AMT: CcwCodebookVariable = CcwCodebookVariable::new("TOT_RX_CST_AMT")
        .label("Total drug cost (Part D)")
        .build();
    pub static ref RPTD_GAP_DSCNT_NUM: CcwCodebookVariable =
        CcwCodebookVariable::new("RPTD_GAP_DSCNT_NUM")
            .label("Gap Discount Amount")
            .build();
    pub static ref FILL_NUM: CcwCodebookVariable = CcwCodebookVariable::new("FILL_NUM")
        .label("Number of drug fills")
        .build();
    pub static ref DAYS_SUPLY_NUM: CcwCodebookVariable = CcwCodebookVariable::new("DAYS_SUPLY_NUM")
        .label("Days Supply")
        .build();
    pub static ref PHRMCY_SRVC_TYPE_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("PHRMCY_SRVC_TYPE_CD")
            .label("Pharmacy service type code")
            .group_start()
            .value("01", " Community/retail pharmacy")
            .value("02", "Compounding pharmacy")
            .value("03", "Home infusion therapy provider")
            .value("04", "Institutional pharmacy")
            .value("05", "Long-term care pharmacy")
            .value("06", "Mail order pharmacy")
            .value("07", "Managed care organization (MCO) pharmacy")
            .value("08", "Specialty care pharmacy")
            .value("99", "Other")
            .value("Null", "Pharmacy is not in any other category above")
            .group_end()
            .build();
    pub static ref DAW_PROD_SLCTN_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("DAW_PROD_SLCTN_CD")
            .label("Dispense as Written (DAW) Product Selection Code")
            .group_start()
            .value(
                "0",
                "No Product Selection Indicated (may also have missing values)"
            )
            .value("1", "Substitution Not Allowed by Prescriber")
            .value(
                "2",
                "Substitution Allowed - Patient Requested That Brand Product Be Dispensed"
            )
            .value(
                "3",
                "Substitution Allowed - Pharmacist Selected Product Dispensed"
            )
            .value("4", "Substitution Allowed - Generic Drug Not in Stock")
            .value(
                "5",
                "Substitution Allowed - Brand Drug Dispensed as Generic"
            )
            .value("6", "Override")
            .value("7", "Substitution Not Allowed - Brand Drug Mandated by Law")
            .value(
                "8",
                "Substitution Allowed - Generic Drug Not Available in Marketplace"
            )
            .value("9", "Other")
            .group_end()
            .build();
    pub static ref DSPNSNG_STUS_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("DSPNSNG_STUS_CD")
            .label("Dispensing Status Code")
            .group_start()
            .value(
                "Blank",
                "Not specified or presumably full quantity of prescription"
            )
            .value("P", "Partial fill")
            .value("C", "Completion of partial fill")
            .group_end()
            .build();
    pub static ref ADJSTMT_DLTN_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("ADJSTMT_DLTN_CD")
            .label("Adjustment Deletion Code")
            .group_start()
            .value("Blank", "Original PDE")
            .value("A", "Adjustment")
            .value("D", "Deletion")
            .value("R", "Resubmitted")
            .group_end()
            .build();
    pub static ref NSTD_FRMT_CD: CcwCodebookVariable = CcwCodebookVariable::new("NSTD_FRMT_CD")
        .label("Non-Standard Format Code")
        .group_start()
        .value("X", "X12 837")
        .value("B", "Beneficiary submitted claim")
        .value("C", "Coordination of Benefits")
        .value("P", "Paper claim from provider")
        .value("Blank", "NCPDP electronic format")
        .group_end()
        .build();
    pub static ref PRCNG_EXCPTN_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("PRCNG_EXCPTN_CD")
            .label("Pricing Exception Code")
            .group_start()
            .value("M", "Medicare is a secondary payer (MSP)")
            .value("O", "Out of network pharmacy")
            .value("Blank", "In-network pharmacy")
            .group_end()
            .build();
    pub static ref CTSTRPHC_CVRG_CD: CcwCodebookVariable =
        CcwCodebookVariable::new("CTSTRPHC_CVRG_CD")
            .label("Catastrophic Coverage Code")
            .group_start()
            .value("A", "Attachment point met on this event")
            .value("C", "Above attachment point")
            .value("Blank", "Attachment point not met")
            .group_end()
            .build();
    pub static ref RX_ORGN_CD: CcwCodebookVariable = CcwCodebookVariable::new("RX_ORGN_CD")
        .label("Prescription Origination Code")
        .group_start()
        .value("Null", "Unknown")
        .value("0", "Not specified")
        .value("1", "Written")
        .value("2", "Telephone")
        .value("3", "Electronic")
        .value("4", "Facsimile")
        .value("5", "Pharmacy")
        .group_end()
        .build();
    pub static ref BRND_GNRC_CD: CcwCodebookVariable = CcwCodebookVariable::new("BRND_GNRC_CD")
        .label("Brand-Generic Code Reported by Submitting Plan")
        .group_start()
        .value("B", "Brang")
        .value("G", "Generic")
        .value("Null/Missing", "")
        .group_end()
        .build();
    pub static ref PTNT_RSDNC_CD: CcwCodebookVariable = CcwCodebookVariable::new("PTNT_RSDNC_CD")
        .label("Patient Residence Code")
        .group_start()
        .value(
            "00",
            "Not specified, other patient residence not identified below"
        )
        .value("01", "Home")
        .value("02", "Skilled Nursing Facility")
        .value("03", "Nursing facility (long-term care facility)")
        .value("04", "Assisted living facility")
        .value(
            "05",
            "Custodial Care Facility (residential but not medical care)"
        )
        .value(
            "06",
            "Group home (e.g., congregate residential foster care)"
        )
        .value("07", "Inpatient Psychiatric Facility")
        .value("08", "Psychiatric Facility – Partial Hospitalization")
        .value(
            "09",
            "Intermediate care facility for the mentally retarded (ICF/MR)"
        )
        .value("10", "Residential Substance Abuse Treatment Facility")
        .value("11", "Hospice")
        .value("12", "Psychiatric Residential Treatment Facility")
        .value("13", "Comprehensive Inpatient Rehabilitation Facility")
        .value("14", "Homeless Shelter")
        .value("15", "Correctional Institution")
        .group_end()
        .build();
    pub static ref SUBMSN_CLR_CD: CcwCodebookVariable = CcwCodebookVariable::new("SUBMSN_CLR_CD")
        .label("Submission clarification code")
        .group_start()
        .value("00", "(Unknown value – rarely populated)")
        .value("05", "Therapy change. Physician determined that a change in therapy was required – either the medication was used faster than expected, or a different dosage form is needed.")
        .value("07", "Emergency supply of non-formulary drugs (or formulary drugs which typically require step therapy or prior authorization). Medication has been determined by the physician to be medically necessary.")
        .value("08", "Process compound for approved ingredients")
        .value("14", "LTC leave of absence – short fill required for take-home use")
        .value("16", "LTC emergency box (e box) /automated dispensing machine")
        .value("17", "LTC emergency supply remainder (remainder of drug from the emergency supply)")
        .value("18", "LTC patient admit/readmission indicator. This status required new dispensing of medication.")
        .value("19", "Split billing. The quantity dispensed is the remainder billed to a subsequent payer after Medicare Part A benefits expired (partial payment under Part A).")
        .value("21", "LTC dispensing rule for &lt;=14 day supply is not applicable due to CMS exclusion or the fact that the manufacturer’s packaging does not allow for special dispensing")
        .value("22", "LTC dispensing, 7-day supply")
        .value("23", "LTC dispensing, 4-day supply")
        .value("24", "LTC dispensing, 3-day supply")
        .value("25", "LTC dispensing, 2-day supply")
        .value("26", "LTC dispensing, 1-day supply")
        .value("27", "LTC dispensing, 4-day supply, then 3-day supply")
        .value("28", "LTC dispensing, 2-day supply, then 2-day supply, then 3-day supply")
        .value("29", "LTC dispensing, daily during the week then multiple days (3) for weekend")
        .value("30", "LTC dispensing, per shift (multiple medication passes)")
        .value("31", "LTC dispensing, per medication pass")
        .value("32", "LTC dispensing, PRN on demand")
        .value("33", "LTC dispensing, other &lt;=7 day cycle")
        .value("34", "LTC dispensing, 14-day supply")
        .value("35", "LTC dispensing, other 8-14 day dispensing not listed above")
        .value("36", "LTC dispensing, outside short cycle, determined to be Part D after originally submitted to another payer")
        .value("42", "The prescriber ID submitted has been validated and is active (rarely populated)")
        .value("43", "For the prescriber ID submitted, the associated DEA number has been renewed or the renewal is in progress (rarely populated)")
        .value("44", "(Unknown value – rarely populated)")
        .value("45", "For the prescriber ID submitted, the associated DEA number is a valid hospital DEA number with suffix (rarely populated)")
        .value("Null", "Not applicable, beneficiary not in an LTC setting (or in the first two months of 2013, the presumption is there was greater than a 14-day supply)")
        .group_end()
        .build();
}
