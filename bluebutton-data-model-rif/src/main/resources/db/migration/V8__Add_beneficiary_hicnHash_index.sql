/*
 * Resolves https://issues.hhsdevcloud.us/browse/CBBD-403.
 *
 * Adds a non-unique index to the "Beneficiaries"."hicn" column, which is used
 * to store a 1-way cryptographic hash of each beneficiary's HICN.
 *
 * Why is this index non-unique? Because about 1700 out of 60M beneficiary
 * HICNs have duplicates. Current opinion of folks is that this is a source
 * data entry/records problem and non-resolvable.
 *
 * Why is this index needed? Because the Blue Button 2.0 frontend application
 * will lookup beneficiaries by their HICN hash.
 */


create index ${logic.index-create-concurrently} "Beneficiaries_hicn_idx"
    on "Beneficiaries" ("hicn");