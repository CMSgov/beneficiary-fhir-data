/*
 * Create an index for each of the Part D Contract Number fields (jan - dec).
 * This makes finding a patient by a specific contract number tenable.
 * 
 * More context can be found in BLUEBUTTON-1841 and related ticktes.
 */

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_jan_id_idx"
    on "Beneficiaries" ("partDContractNumberJanId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_feb_id_idx"
    on "Beneficiaries" ("partDContractNumberFebId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_mar_id_idx"
    on "Beneficiaries" ("partDContractNumberMarId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_apr_id_idx"
    on "Beneficiaries" ("partDContractNumberAprId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_may_id_idx"
    on "Beneficiaries" ("partDContractNumberMayId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_jun_id_idx"
    on "Beneficiaries" ("partDContractNumberJunId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_jul_id_idx"
    on "Beneficiaries" ("partDContractNumberJulId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_aug_id_idx"
    on "Beneficiaries" ("partDContractNumberAugId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_sept_id_idx"
    on "Beneficiaries" ("partDContractNumberSeptId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_oct_id_idx"
    on "Beneficiaries" ("partDContractNumberOctId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_nov_id_idx"
    on "Beneficiaries" ("partDContractNumberNovId");

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_dec_id_idx"
    on "Beneficiaries" ("partDContractNumberDecId");
