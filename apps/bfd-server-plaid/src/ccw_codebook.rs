//! Enumerates the CCW codebook variables, from the codebook PDFs on
//! <https://www.ccwdata.org/web/guest/data-dictionaries>.

pub struct CcwCodebookVariable {
    // TODO should identify which codebook each comes from, rather than merging
    pub id: &'static str,
    // TODO flesh out
}

pub static PDE_ID: CcwCodebookVariable = CcwCodebookVariable { id: "PDE_ID" };

pub static PLAN_CNTRCT_REC_ID: CcwCodebookVariable = CcwCodebookVariable {
    id: "PLAN_CNTRCT_REC_ID",
};

pub static PLAN_PBP_REC_NUM: CcwCodebookVariable = CcwCodebookVariable {
    id: "PLAN_PBP_REC_NUM",
};
