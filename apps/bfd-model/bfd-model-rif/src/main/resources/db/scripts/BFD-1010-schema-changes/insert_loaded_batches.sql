insert into public.loaded_batches (
	loaded_batch_id,
	loaded_file_id,
	beneficiaries,
	created
)
select
	"loadedBatchId",
	"loadedFileId",
	"beneficiaries",
	"created"
from
	public."LoadedBatches"
on conflict
	(loaded_batches_pkey)
do nothing;

insert into public.loaded_files (
    loaded_file_id,
    rif_type,
    created
)
select
	"loadedFileId",
	"rifType",
	"created"
from
	public."LoadedFiles"
on conflict
	(loaded_files_pkey)
do nothing;