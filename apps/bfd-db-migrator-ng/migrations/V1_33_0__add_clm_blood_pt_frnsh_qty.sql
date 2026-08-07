ALTER TABLE idr.claim_institutional_ss
    DROP COLUMN clm_instnl_prfnl_amt,
    DROP COLUMN clm_instnl_low_vol_pmt_amt,
    DROP COLUMN clm_mdcr_ip_bene_ddctbl_amt;

ALTER TABLE idr.claim_institutional_nch
    DROP COLUMN clm_blood_pt_frnsh_qty,
    DROP COLUMN clm_instnl_prfnl_amt,
    DROP COLUMN clm_instnl_low_vol_pmt_amt,
    DROP COLUMN clm_mdcr_ip_bene_ddctbl_amt;
    
ALTER TABLE idr.claim_professional_nch
    DROP COLUMN clm_blood_pt_frnsh_qty;    