-- As part of the 0.8 RDA API update, record source information such as phase, phase seq number, and transmission timestamp are now available

alter table rda.claim_message_meta_data add column phase varchar(2);
alter table rda.claim_message_meta_data add column phase_seq_number smallint;
alter table rda.claim_message_meta_data add column transmission_timestamp varchar(30);

alter table rda.mcs_details add column idr_dtl_number smallint;
update rda.mcs_details set idr_dtl_number = (priority + 1);
alter table rda.mcs_details alter column idr_dtl_number set not null;