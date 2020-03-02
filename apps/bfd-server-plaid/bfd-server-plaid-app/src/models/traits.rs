//! Contains the traits (and implementations) for the auto-generated structs in the
//! `super::structs` module.
use super::structs::*;
use chrono::{DateTime, Utc};

pub trait PartABDClaim {
    fn claim_id(&self) -> &str;
    fn last_updated(&self) -> &Option<DateTime<Utc>>;
    fn claim_group_id(&self) -> String;
    fn final_action_code(&self) -> &str;
    fn beneficiary_id(&self) -> &str;
}

impl PartABDClaim for PartDEvent {
    fn claim_id(&self) -> &str {
        &self.PDE_ID
    }

    fn last_updated(&self) -> &Option<DateTime<Utc>> {
        &self.LAST_UPDATED
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
