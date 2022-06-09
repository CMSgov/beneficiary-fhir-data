alter table rda.fiss_audit_trails add column rda_position smallint;
update rda.fiss_audit_trails set rda_position = (priority + 1);
alter table rda.fiss_audit_trails alter column rda_position set not null;