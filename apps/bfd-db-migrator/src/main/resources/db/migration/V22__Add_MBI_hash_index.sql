/* 
 * Add a MedicareBeneficiaryIdentifier (MBI) hash column for mbi searches. 
 * The search is used by partners to cross-walk to the beneficiaryId. 
 * Since this is a frequent operation, the hash is indexed.
 */

create index ${logic.index-create-concurrently} "Beneficiaries_mbi_hash_idx"
    on "Beneficiaries" ("mbiHash");

create index ${logic.index-create-concurrently} "Beneficiaries_history_mbi_hash_idx"
    on "BeneficiariesHistory" ("mbiHash");
    
