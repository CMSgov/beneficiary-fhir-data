alter table rda.fiss_audit_trails add column rda_position smallint;
update rda.fiss_audit_trails set rda_position = (priority + 1);
alter table rda.fiss_audit_trails alter column rda_position set not null;

alter table rda.fiss_diagnosis_codes add column rda_position smallint;
update rda.fiss_diagnosis_codes set rda_position = (priority + 1);
alter table rda.fiss_diagnosis_codes alter column rda_position set not null;

alter table rda.fiss_payers add column rda_position smallint;
update rda.fiss_payers set rda_position = (priority + 1);
alter table rda.fiss_payers alter column rda_position set not null;

alter table rda.fiss_proc_codes add column rda_position smallint;
update rda.fiss_proc_codes set rda_position = (priority + 1);
alter table rda.fiss_proc_codes alter column rda_position set not null;

alter table rda.mcs_adjustments add column rda_position smallint;
update rda.mcs_adjustments set rda_position = (priority + 1);
alter table rda.mcs_adjustments alter column rda_position set not null;

alter table rda.mcs_audits add column rda_position smallint;
update rda.mcs_audits set rda_position = (priority + 1);
alter table rda.mcs_audits alter column rda_position set not null;

alter table rda.mcs_diagnosis_codes add column rda_position smallint;
update rda.mcs_diagnosis_codes set rda_position = (priority + 1);
alter table rda.mcs_diagnosis_codes alter column rda_position set not null;

alter table rda.mcs_locations add column rda_position smallint;
update rda.mcs_locations set rda_position = (priority + 1);
alter table rda.mcs_locations alter column rda_position set not null;