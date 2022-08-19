/* 
 BFD-2041
 As part of synthetic data generation and loading, errors occured during 
 loadng and a particular batch had to be regenerated and loaded 3 times. Due
 to this, there were multiple entries of the same mbi with different bene_ids in
 beneficiary_history, which causes an error in BFD since there is an expectation
 that each mbi resolves to only one bene_id.
 
 This corrects that issue by removing the entries from the first two load attempts
 from beneficiary_history, leaving only the last loaded mbi to bene_id entries.
 
 The bene range here was discovered via data investigation around where the
 duplicate associations existed
 */
 
 
 delete from beneficiaries_history 
 where TO_CHAR(last_updated , 'yyyy-mm-dd hh24') != '2022-07-07 21' 
 and bene_id > -10000004009994 and bene_id <= -10000002009997;