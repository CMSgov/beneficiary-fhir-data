-- As part of the 0.8 RDA API update, record source information such as phase, phase seq number, and transmission timestamp are now available

alter table rda.claim_message_meta_data add column phase smallint;
alter table rda.claim_message_meta_data add column phase_seq_num smallint;
alter table rda.claim_message_meta_data add column extract_date date;
alter table rda.claim_message_meta_data add column transmission_timestamp timestamp with time zone;

alter table rda.mcs_details add column idr_dtl_number smallint;
update rda.mcs_details set idr_dtl_number = (priority + 1);
alter table rda.mcs_details alter column idr_dtl_number set not null;