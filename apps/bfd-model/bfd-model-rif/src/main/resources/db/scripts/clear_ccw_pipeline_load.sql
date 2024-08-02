/*
 * This script will remove all beneficiaries and their associated data that were created during a specific pipeline load.
 * This is useful for testing a pipeline load in an ephemeral environment.
 */

begin transaction;

-- set the s3 prefix to clean up, it should look something like Synthetic/Incoming/2024-07-30T20:07:46Z% (note the wildcard a the end)
create temp table s3_prefix(prefix) as values(s3_prefix);
create temp table benes_to_remove (bene_id bigint primary key);

insert into benes_to_remove(bene_id)
select cast(regexp_split_to_table(lb.beneficiaries , ',') as bigint)
from ccw.loaded_files lf
join ccw.s3_manifest_files smf on smf.manifest_id = lf.s3_manifest_id
join ccw.loaded_batches lb on lb.loaded_file_id = lf.loaded_file_id
where s3_key like (select prefix from s3_prefix)
on conflict do nothing;

delete from ccw.beneficiary_monthly bm
using benes_to_remove br
where br.bene_id = bm.bene_id;

delete from ccw.beneficiaries_history bh
using benes_to_remove br
where br.bene_id = bh.bene_id;

delete from ccw.carrier_claim_lines ccl
using ccw.carrier_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.carrier_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.inpatient_claim_lines ccl
using ccw.inpatient_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.inpatient_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.outpatient_claim_lines ccl
using ccw.outpatient_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.outpatient_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.dme_claim_lines ccl
using ccw.dme_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.dme_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.hha_claim_lines ccl
using ccw.hha_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.hha_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.partd_events cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.hospice_claim_lines ccl
using ccw.hospice_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.hospice_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.snf_claim_lines ccl
using ccw.snf_claims cc,
benes_to_remove br
where ccl.clm_id = cc.clm_id
and cc.bene_id = br.bene_id;

delete from ccw.snf_claims cc
using benes_to_remove br
where cc.bene_id = br.bene_id;

delete from ccw.beneficiaries b
using benes_to_remove br
where b.bene_id = br.bene_id;

delete from ccw.loaded_batches lb
using ccw.loaded_files lf,
ccw.s3_manifest_files smf
where lb.loaded_file_id = lf.loaded_file_id and
lf.s3_manifest_id = smf.manifest_id and
smf.s3_key like (select prefix from s3_prefix);

delete from ccw.loaded_files lf
using ccw.s3_manifest_files smf
where lf.s3_manifest_id = smf.manifest_id and
smf.s3_key like (select prefix from s3_prefix);

delete from ccw.s3_data_files sdf
using ccw.s3_manifest_files smf
where sdf.manifest_id = smf.manifest_id and
smf.s3_key like (select prefix from s3_prefix);

delete from ccw.s3_manifest_files smf
where smf.s3_key like (select prefix from s3_prefix);

drop table s3_prefix;
drop table benes_to_remove;
commit;
