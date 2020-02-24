
create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_jan_id_idx"
    on "Beneficiaries" ("partDContractNumberJanId")
    ${logic.non-hsql-only-line} where length("partDContractNumberJanId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_feb_id_idx"
    on "Beneficiaries" ("partDContractNumberFebId")
    ${logic.non-hsql-only-line} where length("partDContractNumberFebId") = 5
    ;
create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_mar_id_idx"
    on "Beneficiaries" ("partDContractNumberMarId")
    ${logic.non-hsql-only-line} where length("partDContractNumberMarId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_apr_id_idx"
    on "Beneficiaries" ("partDContractNumberAprId")
    ${logic.non-hsql-only-line} where length("partDContractNumberAprId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_may_id_idx"
    on "Beneficiaries" ("partDContractNumberMayId")
    ${logic.non-hsql-only-line} where length("partDContractNumberMayId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_jun_id_idx"
    on "Beneficiaries" ("partDContractNumberJunId")
    ${logic.non-hsql-only-line} where length("partDContractNumberJunId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_jul_id_idx"
    on "Beneficiaries" ("partDContractNumberJulId")
    ${logic.non-hsql-only-line} where length("partDContractNumberJulId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_aug_id_idx"
    on "Beneficiaries" ("partDContractNumberAugId")
    ${logic.non-hsql-only-line} where length("partDContractNumberAugId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_sept_id_idx"
    on "Beneficiaries" ("partDContractNumberSeptId")
    ${logic.non-hsql-only-line} where length("partDContractNumberSeptId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_oct_id_idx"
    on "Beneficiaries" ("partDContractNumberOctId")
    ${logic.non-hsql-only-line} where length("partDContractNumberOctId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_nov_id_idx"
    on "Beneficiaries" ("partDContractNumberNovId")
    ${logic.non-hsql-only-line} where length("partDContractNumberNovId") = 5
    ;

create index ${logic.index-create-concurrently} "Beneficiaries_partd_contract_number_dec_id_idx"
    on "Beneficiaries" ("partDContractNumberDecId")
    ${logic.non-hsql-only-line} where length("partDContractNumberDecId") = 5
    ;
