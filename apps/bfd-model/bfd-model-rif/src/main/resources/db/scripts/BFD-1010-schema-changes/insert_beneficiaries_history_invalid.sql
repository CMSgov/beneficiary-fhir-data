insert into cmac.beneficiaries_history_invalid_beneficiaries 
	(beneficiary_history_id, 
	 bene_id, 
	 bene_birth_dt, 
	 bene_crnt_hic_num, 
	 bene_sex_ident_cd, 
	 hicn_unhashed, 
	 mbi_num) 
select "beneficiaryHistoryId", 
       Cast("beneficiaryId" as bigint), 
       "birthDate", 
       "hicn", 
       "sex", 
       "hicnUnhashed", 
       "medicareBeneficiaryId"
from   public."BeneficiariesHistoryInvalidBeneficiaries"; 