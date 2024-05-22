-- Hot fix for the 0.12 RDA API update, remove not null constraint on clm_typ_ind

alter table rda.fiss_claims alter column clm_typ_ind drop not null;
