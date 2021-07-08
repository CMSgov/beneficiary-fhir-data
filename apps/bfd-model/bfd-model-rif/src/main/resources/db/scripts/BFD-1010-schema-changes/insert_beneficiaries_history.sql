insert into public.beneficiaries_history 
	(beneficiary_history_id, 
	 bene_id, 
	 bene_birth_dt, 
	 bene_crnt_hicn, 
	 bene_sex_ident_cd, 
	 hicn_unhashed, 
	 mbi_num, 
	 mbi_hash, 
	 mbi_efctv_bgn_dt, 
	 efctv_end_dt, 
	 last_updated) 
select "beneficiaryHistoryId", 
       Cast("beneficiaryId" as bigint), 
       "birthDate", 
       "hicn", 
       "sex", 
       "hicnUnhashed", 
       "medicareBeneficiaryId", 
       "mbiHash", 
       "mbiEffectiveDate", 
       "mbiObsoleteDate", 
       "lastupdated" 
from   public."BeneficiariesHistory"; 