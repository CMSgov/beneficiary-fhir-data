-- As part of the 0.10 RDA API update, new raw text fields are added for select date fields and FISS diag_cd_2 is now nullable

alter table rda.fiss_claims add adm_date_text varchar(10);
alter table rda.fiss_claims add stmt_cov_from_date_text varchar(10);
alter table rda.fiss_claims add stmt_cov_to_date_text varchar(10);

alter table rda.fiss_diagnosis_codes alter column diag_cd2 drop not null;
