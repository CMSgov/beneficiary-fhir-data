//! Contains the traits (and implementations) for the auto-generated structs in the
//! `super::structs` module.
use super::structs::*;

pub trait PartABDClaim {
    fn claim_id(&self) -> &str;
    fn beneficiary_id(&self) -> &str;
}

impl PartABDClaim for PartDEvent {
    fn claim_id(&self) -> &str {
        &self.PDE_ID
    }

    fn beneficiary_id(&self) -> &str {
        &self.BENE_ID
    }
}
