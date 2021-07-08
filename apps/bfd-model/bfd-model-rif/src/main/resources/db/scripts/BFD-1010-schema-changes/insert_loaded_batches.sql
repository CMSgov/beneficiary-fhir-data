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
from public."LoadedBatches";

insert into public.loaded_files (
    loaded_file_id,
    rif_type,
    created
)
select
	"loadedFileId",
	"rifType",
	"created"
from public."LoadedFiles";