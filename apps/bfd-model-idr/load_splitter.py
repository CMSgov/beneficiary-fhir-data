import csv
import itertools
from collections import defaultdict
from pathlib import Path

import click
import tqdm

import field_constants as f
from claims_util import four_part_key
from generator_util import (
    BENE_DUAL,
    BENE_ENTLMT,
    BENE_ENTLMT_RSN,
    BENE_HSTRY,
    BENE_LIS,
    BENE_MAPD_ENRLMT,
    BENE_MAPD_ENRLMT_RX,
    BENE_MBI_ID,
    BENE_STUS,
    BENE_TP,
    BENE_XREF,
    CLM,
    CLM_ANSI_SGNTR,
    CLM_DCMTN,
    CLM_DT_SGNTR,
    CLM_FISS,
    CLM_INSTNL,
    CLM_LCTN_HSTRY,
    CLM_LINE,
    CLM_LINE_DCMTN,
    CLM_LINE_INSTNL,
    CLM_LINE_PRFNL,
    CLM_LINE_RX,
    CLM_PRFNL,
    CLM_PROD,
    CLM_RLT_COND_SGNTR_MBR,
    CLM_VAL,
    CNTRCT_PBP_CNTCT,
    CNTRCT_PBP_NUM,
    PRVDR_HSTRY,
    RowAdapter,
    adapters_to_dicts,
    load_file_dict,
    partition_rows,
)


@click.command
@click.option(
    "-c",
    "--clm-batches-size",
    envvar="CLM_BATCHES_SIZE",
    type=int,
    default=5,
    show_default=True,
    help="Number of CLMs to batch each set of CSVs by",
)
@click.argument("out", nargs=1, type=click.Path(exists=True, path_type=Path))
def main(clm_batches_size: int, out: Path):
    files_to_copy: dict[str, list[RowAdapter]] = {
        BENE_HSTRY: [],
        BENE_MBI_ID: [],
        BENE_STUS: [],
        BENE_ENTLMT_RSN: [],
        BENE_ENTLMT: [],
        BENE_TP: [],
        BENE_XREF: [],
        BENE_DUAL: [],
        BENE_MAPD_ENRLMT: [],
        BENE_MAPD_ENRLMT_RX: [],
        BENE_LIS: [],
        CLM_ANSI_SGNTR: [],
        PRVDR_HSTRY: [],
        CNTRCT_PBP_NUM: [],
        CNTRCT_PBP_CNTCT: [],
    }
    load_file_dict(files=files_to_copy, paths=[Path("./out")])

    files_to_split: dict[str, list[RowAdapter]] = {
        CLM: [],
        CLM_LINE: [],
        CLM_LINE_DCMTN: [],
        CLM_VAL: [],
        CLM_DT_SGNTR: [],
        CLM_PROD: [],
        CLM_INSTNL: [],
        CLM_LINE_INSTNL: [],
        CLM_DCMTN: [],
        CLM_LCTN_HSTRY: [],
        CLM_FISS: [],
        CLM_PRFNL: [],
        CLM_LINE_PRFNL: [],
        CLM_LINE_RX: [],
        CLM_RLT_COND_SGNTR_MBR: [],
    }
    load_file_dict(files=files_to_split, paths=[Path("./out")])

    sgntr_mbrs_per_clm_uniq_id = partition_rows(
        llist=files_to_split[CLM_RLT_COND_SGNTR_MBR], part_by=lambda x: str(x[f.CLM_UNIQ_ID])
    )
    clm_lines_per_clm_uniq_id = partition_rows(
        llist=files_to_split[CLM_LINE], part_by=lambda x: str(x[f.CLM_UNIQ_ID])
    )
    clm_line_rxs_per_clm_uniq_id = partition_rows(
        llist=files_to_split[CLM_LINE_RX], part_by=lambda x: str(x[f.CLM_UNIQ_ID])
    )
    clm_dcmtns_per_fpk = partition_rows(
        llist=files_to_split[CLM_DCMTN],
        part_by=lambda x: four_part_key(x),
    )
    clm_vals_per_fpk = partition_rows(
        llist=files_to_split[CLM_VAL],
        part_by=lambda x: four_part_key(x),
    )
    clm_prods_per_fpk = partition_rows(
        llist=files_to_split[CLM_PROD],
        part_by=lambda x: four_part_key(x),
    )
    clm_dt_sgntrs_per_sk = partition_rows(
        llist=files_to_split[CLM_DT_SGNTR],
        part_by=lambda x: str(x[f.CLM_DT_SGNTR_SK]),
    )
    clm_instnls_per_fpk = partition_rows(
        llist=files_to_split[CLM_INSTNL],
        part_by=lambda x: four_part_key(x),
    )
    clm_prfnls_per_fpk = partition_rows(
        llist=files_to_split[CLM_PRFNL],
        part_by=lambda x: four_part_key(x),
    )
    clm_line_instnls_per_fpk = partition_rows(
        llist=files_to_split[CLM_LINE_INSTNL], part_by=lambda x: four_part_key(x)
    )
    clm_line_prfnls_per_fpk = partition_rows(
        llist=files_to_split[CLM_LINE_PRFNL], part_by=lambda x: four_part_key(x)
    )
    clm_fiss_per_fpk = partition_rows(
        llist=files_to_split[CLM_FISS], part_by=lambda x: four_part_key(x)
    )
    clm_lctn_hstrys_per_fpk = partition_rows(
        llist=files_to_split[CLM_LCTN_HSTRY], part_by=lambda x: four_part_key(x)
    )
    clm_line_dcmtns_per_fpk = partition_rows(
        llist=files_to_split[CLM_LINE_DCMTN], part_by=lambda x: four_part_key(x)
    )

    batched_tables: dict[int, dict[str, list[RowAdapter]]] = defaultdict(lambda: defaultdict(list))
    batched_tables[0].update(files_to_copy)
    print(f"Batching: {', '.join(files_to_split.keys())}...")
    for batch_num, batch in tqdm.tqdm(
        enumerate(itertools.batched(files_to_split[CLM], n=clm_batches_size, strict=False))
    ):
        batched_tables_per_clm = [
            {
                CLM: [clm],
                CLM_LINE: clm_lines_per_clm_uniq_id[clm[f.CLM_UNIQ_ID]],
                CLM_LINE_DCMTN: clm_line_dcmtns_per_fpk[four_part_key(clm)],
                CLM_VAL: clm_vals_per_fpk[four_part_key(clm)],
                CLM_DT_SGNTR: clm_dt_sgntrs_per_sk[clm[f.CLM_DT_SGNTR_SK]],
                CLM_PROD: clm_prods_per_fpk[four_part_key(clm)],
                CLM_INSTNL: clm_instnls_per_fpk[four_part_key(clm)],
                CLM_LINE_INSTNL: clm_line_instnls_per_fpk[four_part_key(clm)],
                CLM_DCMTN: clm_dcmtns_per_fpk[four_part_key(clm)],
                CLM_LCTN_HSTRY: clm_lctn_hstrys_per_fpk[four_part_key(clm)],
                CLM_FISS: clm_fiss_per_fpk[four_part_key(clm)],
                CLM_PRFNL: clm_prfnls_per_fpk[four_part_key(clm)],
                CLM_LINE_PRFNL: clm_line_prfnls_per_fpk[four_part_key(clm)],
                CLM_LINE_RX: clm_line_rxs_per_clm_uniq_id[clm[f.CLM_UNIQ_ID]],
                CLM_RLT_COND_SGNTR_MBR: sgntr_mbrs_per_clm_uniq_id[clm[f.CLM_UNIQ_ID]],
            }
            for clm in batch
        ]

        for per_clm_batch in batched_tables_per_clm:
            for table, rows in per_clm_batch.items():
                batched_tables[batch_num][table].extend(rows)

    print(f"Done batching, writing to {out}...")
    out.mkdir(parents=True, exist_ok=True)
    for batch_num, batch in tqdm.tqdm(batched_tables.items()):
        batch_num_dir = out.joinpath(f"{batch_num}")
        batch_num_dir.mkdir(exist_ok=True)
        for table, rows in ((k, v) for k, v in batch.items() if v):
            dict_rows = adapters_to_dicts(rows)
            with batch_num_dir.joinpath(f"{table}.csv").open("w") as csv_file:
                writer = csv.DictWriter(
                    csv_file, fieldnames=dict_rows[0].keys(), restval="", quoting=csv.QUOTE_MINIMAL
                )
                writer.writeheader()
                writer.writerows(dict_rows)
    print(f"Wrote all {len(batched_tables)} batch(s) to {out}")


if __name__ == "__main__":
    main()
