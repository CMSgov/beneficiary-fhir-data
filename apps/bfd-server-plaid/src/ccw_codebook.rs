//! Enumerates the CCW codebook variables, from the codebook PDFs on
//! <https://www.ccwdata.org/web/guest/data-dictionaries>.

pub struct CcwCodebookVariable {
    // TODO should identify which codebook each comes from, rather than merging
    pub id: &'static str,
    // TODO flesh out
}

pub static PDE_ID: CcwCodebookVariable = CcwCodebookVariable { id: "PDE_ID" };
