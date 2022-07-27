-- As part of the 0.8 RDA API update, record source information such as phase, phase seq number, and transmission timestamp are now available

alter table rda.claim_message_meta_data add column phase varchar(2);
alter table rda.claim_message_meta_data add column phaseSeqNumber int;
alter table rda.claim_message_meta_data add column transmissionTimestamp varchar(30);