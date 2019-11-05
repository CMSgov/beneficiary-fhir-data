//! Contains the traits (and implementations) for the auto-generated structs in the
//! `super::structs` module.
use super::structs::*;

pub trait PartABDClaim {
    fn claim_id(&self) -> &str;
    fn claim_group_id(&self) -> String;
    fn final_action_code(&self) -> &str;
    fn beneficiary_id(&self) -> &str;
}

impl PartABDClaim for PartDEvent {
    fn claim_id(&self) -> &str {
        &self.PDE_ID
    }

    fn claim_group_id(&self) -> String {
        self.CLM_GRP_ID.to_string()
    }

    fn final_action_code(&self) -> &str {
        &self.FINAL_ACTION
    }

    fn beneficiary_id(&self) -> &str {
        &self.BENE_ID
    }
}
