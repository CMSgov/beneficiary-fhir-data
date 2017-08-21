/*
* Use the following command once you have established a tmux session on the prod etl server
* You will be prompted for the database password*
*
* psql --host=db.prod.cmsfhir.systems --dbname=fhirdb2 --username=svc_fhir_etl --password -f '/u01/bb-etl/bene_max_lines';
*
* Following query will query all claim line tables to determine how many total claim lines there are for all claim types at the beneficiary id
* level so we can tell how much data could possibly be returned to the front-end application to ensure it can handle this volume
*/

create view bene_max_lines_view as (select beneId, sum(count)
 from
 (select "HHAClaims"."beneficiaryId" as beneId,  count(*)  from "HHAClaims" inner join "HHAClaimLines" on "HHAClaims"."claimId" = "HHAClaimLines"."parentClaim"
 group by beneId
 union all
 select "HospiceClaims"."beneficiaryId" as beneId,  count(*)  from "HospiceClaims" inner join "HospiceClaimLines" on "HospiceClaims"."claimId" = "HospiceClaimLines"."parentClaim"
 group by beneId
 union all
 select "DMEClaims"."beneficiaryId" as beneId,  count(*)  from "DMEClaims" inner join "DMEClaimLines" on "DMEClaims"."claimId" = "DMEClaimLines"."parentClaim"
 group by beneId
 union all
 select "SNFClaims"."beneficiaryId" as beneId,  count(*)  from "SNFClaims" inner join "SNFClaimLines" on "SNFClaims"."claimId" = "SNFClaimLines"."parentClaim"
 group by beneId
 union all
 select "InpatientClaims"."beneficiaryId" as beneId,  count(*)  from "InpatientClaims" inner join "InpatientClaimLines" on "InpatientClaims"."claimId" = "InpatientClaimLines"."parentClaim"
 group by beneId
 union all
 select "OutpatientClaims"."beneficiaryId" as beneId,  count(*)  from "OutpatientClaims" inner join "OutpatientClaimLines" on "OutpatientClaims"."claimId" = "OutpatientClaimLines"."parentClaim"
 group by beneId
 union all
 select "CarrierClaims"."beneficiaryId" as beneId,  count(*)  from "CarrierClaims" inner join "CarrierClaimLines" on "CarrierClaims"."claimId" = "CarrierClaimLines"."parentClaim"
 group by beneId
)as beneTable group by beneTable.beneId order by sum(count) desc limit 100);

/*
*  Following statement will copy the results from the above view to the prod etl server in the following directory
*
*/

\copy (select * from bene_max_lines_view) to '/u01/bb-etl/bene_max_lines.txt'; 

/*
*  Following statement will delete the view created above
*/

drop view bene_max_lines_view;
