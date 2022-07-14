--
-- This table has been replaced by claim_message_meta_data so we can safely remove it.
--

drop table rda.rda_api_claim_message_meta_data;

--
-- This sequence was only used for the dropped rda_api_claim_message_meta_data;table.
--

drop sequence rda.rda_api_claim_message_meta_data_meta_data_id_seq;
