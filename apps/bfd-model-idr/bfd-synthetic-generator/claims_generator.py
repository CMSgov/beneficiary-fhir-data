import logging
import random
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

import click
import constants as f
import tqdm
from claim_type_codes import (
    FISS_SS_CDS,
    INSTITUTIONAL_CDS,
    MCS_SS_CDS,
    PHARMACY_CDS,
    PROFESSIONAL_CDS,
)
from claims_adj import AdjudicatedGeneratorUtil
from claims_other import OtherGeneratorUtil
from claims_pac import PacGeneratorUtil
from claims_priorauth import PriorAuthGeneratorUtil
from claims_static import DISPROPORTIONATE_AMOUNT_CLM_VAL_CD, INDIRECT_AMOUNT_CLM_VAL_CD
from claims_util import four_part_key, match_line_num
from constants import GeneratePacDataMode
from generator_util import (
    GeneratorUtil,
    as_list,
    partition_rows,
    probability,
)
from load_synthetic_output import (
    BeneSkMode,
    CsvWriter,
    SnowflakeWriter,
)
from row_adapter import RowAdapter
from shared_options import common_options, ensure_directory_exists

logger = logging.getLogger(__name__)

_BATCH_SIZE = 50_000


@click.command
@common_options
@click.option(
    "--out-dir",
    type=click.Path(file_okay=False, path_type=Path),
    default=Path("./out"),
    show_default=True,
    callback=ensure_directory_exists,
    help="Directory to write generated CSV files to.",
)
@click.argument("paths", nargs=-1, type=click.Path(exists=True))
def generate_locally(
    min_claims: int,
    max_claims: int,
    samhsa: bool,
    pac_gen: GeneratePacDataMode,
    bene_sk_mode: BeneSkMode,
    paths: tuple[Path, ...],
    out_dir: Path,
    truncate: bool = False,
    batch_size: int = _BATCH_SIZE,
    overwrite: str | None = None,
    **kwargs: Any,  # noqa: ANN401, ARG001
) -> None:
    generate(
        min_claims=min_claims,
        max_claims=max_claims,
        samhsa=samhsa,
        pac_gen=pac_gen,
        bene_sk_mode=bene_sk_mode,
        writer=CsvWriter(out_dir=out_dir, overwrite_fields=overwrite),
        paths=paths,
        truncate=truncate,
        batch_size=batch_size,
    )


def generate(
    min_claims: int,
    max_claims: int,
    samhsa: bool,
    pac_gen: GeneratePacDataMode,
    bene_sk_mode: BeneSkMode,
    writer: CsvWriter | SnowflakeWriter,
    paths: tuple[Path, ...] | None = None,
    truncate: bool = False,
    batch_size: int = _BATCH_SIZE,
) -> None:
    """Generate synthetic claims data. Provided file PATHS will be updated with new fields."""
    if min_claims > max_claims:
        raise click.UsageError(
            f"min claims value of {min_claims} is greater than max claims value of {max_claims}"
        )

    files: dict[str, list[RowAdapter]] = {
        f.CLM: [],
        f.CLM_LINE: [],
        f.CLM_LINE_DCMTN: [],
        f.CLM_VAL: [],
        f.CLM_ANSI_SGNTR: [],
        f.CLM_DT_SGNTR: [],
        f.CLM_PROD: [],
        f.CLM_INSTNL: [],
        f.CLM_LINE_INSTNL: [],
        f.CLM_DCMTN: [],
        f.CLM_LCTN_HSTRY: [],
        f.CLM_FISS: [],
        f.CLM_PRFNL: [],
        f.CLM_LINE_PRFNL: [],
        f.CLM_LINE_RX: [],
        f.CLM_LINE_MCS: [],
        f.CLM_LINE_FISS: [],
        f.CLM_RLT_COND_SGNTR_MBR: [],
        f.CLM_OCRNC_SGNTR_MBR: [],
        f.CLM_RLT_OCRNC_SGNTR_MBR: [],
    }

    claims_tables = list(files.keys())

    if truncate:
        tables = [*claims_tables, f.PRVDR_HSTRY, f.PRAUC]
        writer.truncate_tables(tables)

    gen_utils = GeneratorUtil(writer.id_gen)

    dependency_files: dict[str, list[RowAdapter]] = {
        f.BENE_HSTRY: [],
        f.CNTRCT_PBP_NUM: [],
        f.PRVDR_HSTRY: [],
        f.PRAUC: [],
    }

    files = writer.load_files(files=files | dependency_files, paths=paths or ())
    if paths and (not files[f.BENE_HSTRY] and not files[f.CLM] and not files[f.CNTRCT_PBP_NUM]):
        print(
            f"{f.BENE_HSTRY} and {f.CNTRCT_PBP_NUM} and/or {f.CLM} must be provided for "
            f"claims data generation to proceed using the CSV flow."
        )
        sys.exit(1)

    gen_utils.cntrct_pbp_num = writer.get_cntrct_pbp_nums(files)

    other_util = OtherGeneratorUtil()
    existing_providers = writer.get_provider_histories(files)
    generated_provider_histories, generated_type_1_npis, generated_type_2_npis = (
        other_util.gen_provider_history(
            amount=14,
            gen_utils=gen_utils,
            init_provider_historys=existing_providers,
        )
    )
    gen_utils.export_table(generated_provider_histories, f.PRVDR_HSTRY, writer)

    # This table is special in that its data is mostly static and read from a static file, so we
    # don't need to do anything fancy with it
    clm_ansi_sgntr_rows = other_util.gen_synthetic_clm_ansi_sgntr()
    gen_utils.export_table(clm_ansi_sgntr_rows, f.CLM_ANSI_SGNTR, writer)

    adj_util = AdjudicatedGeneratorUtil(enable_samhsa=samhsa)
    pac_util = PacGeneratorUtil()
    claim_child_tables = [
        table for table in claims_tables if table not in (f.CLM, f.CLM_ANSI_SGNTR)
    ]

    # An operator could provide a BENE_HSTRY with new beneficiaries that have no corresponding CLMs,
    # so we need to resolve the unique union of BENE_SKs from both files here. Below we will check
    # whether a given BENE_SK has CLMs rows already and either regenerate them or generate new ones
    # correspondingly. Additionally, we need to preserve the order of the bene_sks from the source
    # files, else there will be drift in the order of generated rows
    for bene_sks_batch in writer.get_bene_sks(batch_size, bene_sk_mode, files):
        existing_claims = writer.get_claims_batch(
            bene_sks_batch, claim_child_tables, files, truncate
        )

        out_tables = _generate_batch(
            bene_sks_batch,
            existing_claims,
            gen_utils,
            adj_util,
            pac_util,
            min_claims,
            max_claims,
            pac_gen,
            generated_type_1_npis,
            generated_type_2_npis,
        )

        pa_util = PriorAuthGeneratorUtil()
        out_tables[f.PRVDR_HSTRY] = generated_provider_histories
        out_tables[f.BENE_HSTRY] = writer.get_bene_sk_to_mbi(bene_sks_batch, files)
        existing_prior_auths = writer.get_prior_auths(out_tables[f.BENE_HSTRY], files)
        prior_auths = pa_util.gen_prior_auths(
            gen_utils=gen_utils,
            out_tables=out_tables,
            generated_type_1_npis=generated_type_1_npis,
            generated_type_2_npis=generated_type_2_npis,
            init_prior_auth=existing_prior_auths,
        )
        gen_utils.export_table(prior_auths, f.PRAUC, writer)

        for table_name, rows in out_tables.items():
            if table_name == f.CLM_ANSI_SGNTR or table_name not in claims_tables:
                continue
            gen_utils.export_table(rows, table_name, writer)

    logger.info("Done generating synthetic claims/prior auth data for provided BENE_SKs")


def _generate_batch(
    bene_sks: list[int],
    existing: dict[str, list[RowAdapter]],
    gen_utils: GeneratorUtil,
    adj_util: AdjudicatedGeneratorUtil,
    pac_util: PacGeneratorUtil,
    min_claims: int,
    max_claims: int,
    pac_gen: GeneratePacDataMode,
    generated_type_1_npis: list[str],
    generated_type_2_npis: list[str],
) -> dict[str, list[RowAdapter]]:
    out_tables: dict[str, list[RowAdapter]] = {k: [] for k in existing}
    clms_per_bene_sk = partition_rows(llist=existing[f.CLM], part_by=lambda x: int(x[f.BENE_SK]))

    cond_sgntr_mbr_per_cond_sgntr_sk = partition_rows(
        llist=existing[f.CLM_RLT_COND_SGNTR_MBR],
        part_by=lambda x: x[f.CLM_RLT_COND_SGNTR_SK],
    )

    ocrnc_sgntr_mbr_per_ocrnc_sk = partition_rows(
        llist=existing[f.CLM_OCRNC_SGNTR_MBR],
        part_by=lambda x: x[f.CLM_OCRNC_SGNTR_SK],
    )

    rlt_ocrnc_sgntr_mbr_per_ocrnc_sk = partition_rows(
        llist=existing[f.CLM_RLT_OCRNC_SGNTR_MBR],
        part_by=lambda x: x[f.CLM_RLT_OCRNC_SGNTR_SK],
    )

    rx_clm_line_per_clm_uniq_id = {
        str(x[f.CLM_UNIQ_ID]): x
        for x in existing[f.CLM_LINE]
        if x.get(f.CLM_TYPE_CD) in PHARMACY_CDS
    }
    norm_clm_lines_per_clm_uniq_id = partition_rows(
        llist=existing[f.CLM_LINE],
        part_by=lambda x: x[f.CLM_UNIQ_ID],
        filter_by=lambda x: x.get(f.CLM_TYPE_CD) not in PHARMACY_CDS,
    )
    clm_line_rx_per_clm_uniq_id = {str(row[f.CLM_UNIQ_ID]): row for row in existing[f.CLM_LINE_RX]}
    clm_dcmtns_per_fpk = partition_rows(
        llist=existing[f.CLM_DCMTN],
        part_by=four_part_key,
    )
    dsprtnt_clm_val_per_fpk = {
        four_part_key(x): x
        for x in existing[f.CLM_VAL]
        if x[f.CLM_VAL_CD] == DISPROPORTIONATE_AMOUNT_CLM_VAL_CD
    }
    ime_clm_val_per_fpk = {
        four_part_key(x): x
        for x in existing[f.CLM_VAL]
        if x[f.CLM_VAL_CD] == INDIRECT_AMOUNT_CLM_VAL_CD
    }
    proc_clm_prod_per_fpk = partition_rows(
        llist=existing[f.CLM_PROD],
        part_by=four_part_key,
        filter_by=lambda x: x[f.CLM_PROD_TYPE_CD] == "S",
    )
    diag_clm_prod_per_fpk = partition_rows(
        llist=existing[f.CLM_PROD],
        part_by=four_part_key,
        filter_by=lambda x: x[f.CLM_PROD_TYPE_CD] != "S",
    )
    clm_dt_sgntr_per_sk = {row[f.CLM_DT_SGNTR_SK]: row for row in existing[f.CLM_DT_SGNTR]}
    clm_instnl_per_fpk = {four_part_key(row): row for row in existing[f.CLM_INSTNL]}
    clm_prfnls_per_fpk = partition_rows(
        llist=existing[f.CLM_PRFNL],
        part_by=four_part_key,
    )
    clm_line_mcs_per_fpk = partition_rows(
        llist=existing[f.CLM_LINE_MCS],
        part_by=four_part_key,
    )
    clm_line_fiss_per_fpk = partition_rows(llist=existing[f.CLM_LINE_FISS], part_by=four_part_key)
    clm_line_instnls_per_fpk = partition_rows(
        llist=existing[f.CLM_LINE_INSTNL], part_by=four_part_key
    )
    clm_line_prfnls_per_fpk = partition_rows(
        llist=existing[f.CLM_LINE_PRFNL], part_by=four_part_key
    )
    clm_fiss_per_fpk = {four_part_key(row): row for row in existing[f.CLM_FISS]}
    clm_lctn_hstry_per_fpk = {four_part_key(row): row for row in existing[f.CLM_LCTN_HSTRY]}
    clm_line_dcmtns_per_clk = partition_rows(
        llist=existing[f.CLM_LINE_DCMTN], part_by=four_part_key
    )

    # pac CLM generation is random per-generation iteration, so the number of pac CLMs rows is
    # unstable regardless of whether we provide existing CLMs or not. We need a switch to ensure pac
    # claims data is not generated if it already exists, only regenerated, so this checks if there's
    # any pac CLMs in the provided FILEs or if the --force-pac-claims flag is disable and disables
    # _new_ pac claim data generation if so
    any_pac_clms = any(x[f.CLM_TYPE_CD] >= 1011 for x in existing[f.CLM])
    gen_new_pac_clms = pac_gen == GeneratePacDataMode.ALWAYS or (
        not any_pac_clms and pac_gen == GeneratePacDataMode.IF_NONE
    )

    logger.info("Generating synthetic claims data for provided BENE_SKs...")

    for pt_bene_sk in tqdm.tqdm(bene_sks):
        existing_clms = clms_per_bene_sk.get(pt_bene_sk, [])
        existing_adj_clms = [x for x in existing_clms if x[f.CLM_TYPE_CD] < 1011]
        existing_pac_clms = [x for x in existing_clms if x[f.CLM_TYPE_CD] >= 1011]
        init_adj_clms = (
            existing_adj_clms
            if existing_adj_clms or existing_pac_clms
            else [RowAdapter({}) for _ in range(random.randint(min_claims, max_claims))]
        )
        # This is dumb, but fresh pac claims are derived/generated from adjudicated claims generated
        # here, so we need a way to provide those claims to the pac-generation logic in the next
        # loop
        all_adj_clms_tbls: list[dict[str, list[RowAdapter]]] = []
        for init_adj_clm in init_adj_clms:
            # Store this iteration's generated claims-related tables (list[RowAdapter]) for use in
            # pac generation
            adj_clms_tbls: dict[str, list[RowAdapter]] = defaultdict(list)

            clm_from_dt_min = "2018-01-01"
            clm = adj_util.gen_clm(
                gen_utils=gen_utils,
                bene_sk=str(pt_bene_sk),
                init_clm=init_adj_clm,
                min_date=clm_from_dt_min,
                type_1_npis=generated_type_1_npis,
                type_2_npis=generated_type_2_npis,
            )
            adj_clms_tbls[f.CLM].append(clm)

            clm_rlt_cond_sgntr_mbrs = [
                adj_util.gen_clm_rlt_cond_sgntr_mbr(
                    clm=clm, gen_utils=gen_utils, init_clm_rlt_cond_sgntr_mbr=row
                )
                for row in cond_sgntr_mbr_per_cond_sgntr_sk.get(
                    clm[f.CLM_RLT_COND_SGNTR_SK],
                    [] if cond_sgntr_mbr_per_cond_sgntr_sk else [None],
                )
            ]
            adj_clms_tbls[f.CLM_RLT_COND_SGNTR_MBR].extend(clm_rlt_cond_sgntr_mbrs)

            clm_ocrnc_sgntr_mbr = [
                adj_util.gen_clm_ocrnc_sgntr_mbr(
                    clm=clm, gen_utils=gen_utils, init_clm_ocrnc_sgntr_mbr=row
                )
                for row in ocrnc_sgntr_mbr_per_ocrnc_sk.get(
                    clm[f.CLM_OCRNC_SGNTR_SK],
                    [] if ocrnc_sgntr_mbr_per_ocrnc_sk else [None],
                )
            ]
            adj_clms_tbls[f.CLM_OCRNC_SGNTR_MBR].extend(clm_ocrnc_sgntr_mbr)

            clm_rlt_ocrnc_sgntr_mbr = [
                adj_util.gen_clm_rlt_ocrnc_sgntr_mbr(
                    clm=clm, gen_utils=gen_utils, init_clm_rlt_ocrnc_sgntr_mbr=row
                )
                for row in rlt_ocrnc_sgntr_mbr_per_ocrnc_sk.get(
                    clm[f.CLM_RLT_OCRNC_SGNTR_SK],
                    [] if rlt_ocrnc_sgntr_mbr_per_ocrnc_sk else [None],
                )
            ]
            adj_clms_tbls[f.CLM_RLT_OCRNC_SGNTR_MBR].extend(clm_rlt_ocrnc_sgntr_mbr)

            clm_type_cd = clm[f.CLM_TYPE_CD]
            if clm_type_cd in PHARMACY_CDS:
                pharm_clm_line = adj_util.gen_pharm_clm_line(
                    clm=clm,
                    init_clm_line=rx_clm_line_per_clm_uniq_id.get(str(clm[f.CLM_UNIQ_ID])),
                )
                pharm_clm_line_rx = adj_util.gen_pharm_clm_line_rx(
                    gen_utils=gen_utils,
                    clm=clm,
                    init_clm_line_rx=clm_line_rx_per_clm_uniq_id.get(str(clm[f.CLM_UNIQ_ID])),
                )
                adj_clms_tbls[f.CLM_LINE].append(pharm_clm_line)
                adj_clms_tbls[f.CLM_LINE_RX].append(pharm_clm_line_rx)

            clm_dcmtns = [
                adj_util.gen_clm_dcmtn(clm=clm, gen_utils=gen_utils, init_clm_dcmtn=x)
                for x in clm_dcmtns_per_fpk.get(four_part_key(clm), [RowAdapter({})])
            ]
            adj_clms_tbls[f.CLM_DCMTN].extend(clm_dcmtns)

            if clm_type_cd in (20, 40, 60, 61, 62, 63, 64):
                dsprtnt_clm_val = adj_util.gen_dsprtnt_clm_val(
                    clm=clm,
                    init_clm_val=dsprtnt_clm_val_per_fpk.get(four_part_key(clm)),
                )
                adj_clms_tbls[f.CLM_VAL].append(dsprtnt_clm_val)

                ime_clm_val = adj_util.gen_ime_clm_val(
                    clm=clm,
                    init_clm_val=ime_clm_val_per_fpk.get(four_part_key(clm)),
                )
                adj_clms_tbls[f.CLM_VAL].append(ime_clm_val)

            if clm_type_cd in (10, 20, 30, 40, 50, 60, 61, 62, 63, 64):
                init_procs = proc_clm_prod_per_fpk.get(four_part_key(clm)) or [
                    RowAdapter({}) for _ in range(random.randint(1, 5))
                ]
                for proc_idx, proc in enumerate(init_procs):
                    _proc = adj_util.gen_proc_clm_prod(
                        clm=clm, clm_val_sqnc_num=proc_idx, init_clm_prod=proc
                    )
                    adj_clms_tbls[f.CLM_PROD].append(_proc)

            diagnoses = adj_util.gen_diag_clm_prod_list(
                clm=clm, init_diagnoses=diag_clm_prod_per_fpk.get(four_part_key(clm))
            )
            adj_clms_tbls[f.CLM_PROD].extend(diagnoses)

            clm_dt_sgntr = adj_util.gen_clm_dt_sgntr(
                clm=clm,
                init_clm_dt_sgntr=clm_dt_sgntr_per_sk.get(clm[f.CLM_DT_SGNTR_SK]),
            )
            adj_clms_tbls[f.CLM_DT_SGNTR].append(clm_dt_sgntr)

            clm_instnl = None
            if clm_type_cd in INSTITUTIONAL_CDS:
                clm_instnl = adj_util.gen_clm_instnl(
                    gen_utils=gen_utils,
                    clm=clm,
                    init_clm_instnl=clm_instnl_per_fpk.get(four_part_key(clm)),
                )
                adj_clms_tbls[f.CLM_INSTNL].append(clm_instnl)

            if clm_type_cd in PROFESSIONAL_CDS:
                clm_prfnls = [
                    adj_util.gen_clm_prfnl(
                        gen_utils=gen_utils,
                        clm=clm,
                        init_clm_prfnl=x,
                    )
                    for x in clm_prfnls_per_fpk.get(four_part_key(clm), [RowAdapter({})])
                ]
                adj_clms_tbls[f.CLM_PRFNL].extend(clm_prfnls)

            init_clm_lines = norm_clm_lines_per_clm_uniq_id.get(clm[f.CLM_UNIQ_ID]) or [
                RowAdapter({}) for _ in range(random.randint(1, 15))
            ]
            for idx, init_clm_line in enumerate(init_clm_lines, start=1):
                if clm_type_cd in PHARMACY_CDS:
                    continue

                clm_line = adj_util.gen_clm_line(
                    gen_utils=gen_utils,
                    clm=clm,
                    clm_line_num=init_clm_line[f.CLM_LINE_NUM]
                    if init_clm_line.get(f.CLM_LINE_NUM)
                    else idx,
                    diagnoses=diagnoses,
                    init_clm_line=init_clm_line,
                    type_1_npis=generated_type_1_npis,
                )
                adj_clms_tbls[f.CLM_LINE].append(clm_line)

                line_num = clm_line[f.CLM_LINE_NUM]
                fpk = four_part_key(clm)
                if clm_type_cd >= 10 and clm_type_cd <= 65:
                    clm_line_instnl = adj_util.gen_clm_line_instnl(
                        gen_utils=gen_utils,
                        clm=clm,
                        clm_line_num=line_num,
                        init_clm_line_instnl=match_line_num(
                            clm_lines=clm_line_instnls_per_fpk.get(fpk),
                            clm_line_num=line_num,
                        ),
                    )
                    adj_clms_tbls[f.CLM_LINE_INSTNL].append(clm_line_instnl)
                elif clm_type_cd >= 71 and clm_type_cd <= 82:
                    clm_line_prfnl = adj_util.gen_clm_line_prfnl(
                        gen_utils=gen_utils,
                        clm=clm,
                        clm_line_num=line_num,
                        init_clm_line_prfnl=match_line_num(
                            clm_lines=clm_line_prfnls_per_fpk.get(fpk),
                            clm_line_num=line_num,
                        ),
                    )
                    adj_clms_tbls[f.CLM_LINE_PRFNL].append(clm_line_prfnl)

            for k, v in adj_clms_tbls.items():
                out_tables[k].extend(v)

            all_adj_clms_tbls.append(adj_clms_tbls)

        # Looks awful, but this is essentially equivalent to looking up each table's row(s)
        # sequentially in the generation algorithm (as we do for adjudicated claims data above). We
        # "pre-lookup" these row(s) so that a BENE_SK with no pac CLMs can have pac claims data
        # generated for it from the freshly-generated adjudicated claims data above
        # ("all_adj_clms_tbls")
        pac_clms_tbls_from_file = [
            {
                f.CLM: as_list(file_pac_clm),  # as_list ensures None values return empty list
                f.CLM_FISS: as_list(clm_fiss_per_fpk.get(four_part_key(file_pac_clm))),
                f.CLM_LCTN_HSTRY: as_list(clm_lctn_hstry_per_fpk.get(four_part_key(file_pac_clm))),
                f.CLM_RLT_COND_SGNTR_MBR: cond_sgntr_mbr_per_cond_sgntr_sk.get(
                    file_pac_clm[f.CLM_RLT_COND_SGNTR_SK], []
                ),
                f.CLM_OCRNC_SGNTR_MBR: ocrnc_sgntr_mbr_per_ocrnc_sk.get(
                    file_pac_clm[f.CLM_OCRNC_SGNTR_SK], []
                ),
                f.CLM_RLT_OCRNC_SGNTR_MBR: rlt_ocrnc_sgntr_mbr_per_ocrnc_sk.get(
                    file_pac_clm[f.CLM_RLT_OCRNC_SGNTR_SK], []
                ),
                f.CLM_LINE: [
                    *as_list(rx_clm_line_per_clm_uniq_id.get(str(file_pac_clm[f.CLM_UNIQ_ID]))),
                    *norm_clm_lines_per_clm_uniq_id.get(file_pac_clm[f.CLM_UNIQ_ID], []),
                ],
                f.CLM_LINE_MCS: clm_line_mcs_per_fpk.get(four_part_key(file_pac_clm), []),
                f.CLM_LINE_FISS: clm_line_fiss_per_fpk.get(four_part_key(file_pac_clm), []),
                f.CLM_DCMTN: clm_dcmtns_per_fpk.get(four_part_key(file_pac_clm), []),
                f.CLM_PRFNL: clm_prfnls_per_fpk.get(four_part_key(file_pac_clm), []),
                f.CLM_VAL: [
                    *as_list(dsprtnt_clm_val_per_fpk.get(four_part_key(file_pac_clm))),
                    *as_list(ime_clm_val_per_fpk.get(four_part_key(file_pac_clm))),
                ],
                f.CLM_LINE_DCMTN: clm_line_dcmtns_per_clk.get(four_part_key(file_pac_clm), []),
                f.CLM_LINE_INSTNL: clm_line_instnls_per_fpk.get(four_part_key(file_pac_clm), []),
                f.CLM_LINE_PRFNL: clm_line_prfnls_per_fpk.get(four_part_key(file_pac_clm), []),
                f.CLM_INSTNL: as_list(clm_instnl_per_fpk.get(four_part_key(file_pac_clm))),
                f.CLM_PROD: [
                    *proc_clm_prod_per_fpk.get(four_part_key(file_pac_clm), []),
                    *diag_clm_prod_per_fpk.get(four_part_key(file_pac_clm), []),
                ],
                f.CLM_DT_SGNTR: as_list(clm_dt_sgntr_per_sk.get(file_pac_clm[f.CLM_DT_SGNTR_SK])),
            }
            for file_pac_clm in existing_pac_clms
        ]
        init_pac_clms_tbls = pac_clms_tbls_from_file or (
            [
                x
                for x in all_adj_clms_tbls
                if probability(0.5)
                and x[f.CLM][0][f.CLM_TYPE_CD]
                not in (1, 2, 3, 4)  # obviously we don't have pac claims for PD claims
            ]
            if gen_new_pac_clms
            else []
        )
        for claims_tbls in init_pac_clms_tbls:
            pac_clm = pac_util.gen_pac_clm(init_clm=claims_tbls[f.CLM][0], gen_utils=gen_utils)
            out_tables[f.CLM].append(pac_clm)

            pac_clm_fiss = pac_util.gen_clm_fiss(
                clm=pac_clm, init_clm_fiss=next(iter(claims_tbls[f.CLM_FISS]), None)
            )
            out_tables[f.CLM_FISS].append(pac_clm_fiss)

            pac_clm_lctn_hstry = pac_util.gen_clm_lctn_hstry(
                clm=pac_clm,
                init_clm_lctn_hstry=next(iter(claims_tbls[f.CLM_LCTN_HSTRY]), None),
            )
            out_tables[f.CLM_LCTN_HSTRY].append(pac_clm_lctn_hstry)

            pac_clm_lines = [
                pac_util.gen_pac_clm_line(clm=pac_clm, init_clm_line=clm_line)
                for clm_line in claims_tbls[f.CLM_LINE]
            ]
            out_tables[f.CLM_LINE].extend(pac_clm_lines)

            if claims_tbls[f.CLM_VAL]:
                out_tables[f.CLM_VAL].extend(
                    [
                        pac_util.gen_pac_clm_val(clm=pac_clm, init_clm_val=x)
                        for x in claims_tbls[f.CLM_VAL]
                    ]
                )

            for clm_line in claims_tbls[f.CLM_LINE]:
                # CLM_LINE_DCMTN is a bit special in that if we're creating it from an adjudicated
                # CLM_LINE we get the tracking number from the CLM_LINE, but if we already have a
                # CLM_LINE_DCMTN (we're regenerating it) the CLM_LINE does not have that field
                # anymore so we need to get it from the CLM_LINE_DCMTN. Pretty dumb, but it works
                clm_line_dcmtns = claims_tbls.get(f.CLM_LINE_DCMTN, [])
                clm_line_num = clm_line[f.CLM_LINE_NUM]
                init_clm_line_dcmtn = match_line_num(
                    clm_lines=clm_line_dcmtns,
                    clm_line_num=clm_line_num,
                )
                tracking_num = (
                    clm_line.get(f.CLM_LINE_PA_UNIQ_TRKNG_NUM)
                    or clm_line.get(f.CLM_LINE_PMD_UNIQ_TRKNG_NUM)
                    or (
                        init_clm_line_dcmtn[f.CLM_LINE_PA_UNIQ_TRKNG_NUM]
                        if init_clm_line_dcmtn
                        else None
                    )
                )
                if tracking_num:
                    out_tables[f.CLM_LINE_DCMTN].append(
                        pac_util.gen_pac_clm_line_dcmtn(
                            clm=pac_clm,
                            clm_line_num=clm_line_num,
                            tracking_num=tracking_num,
                            init_clm_line_dcmtn=match_line_num(
                                clm_lines=clm_line_dcmtns,
                                clm_line_num=clm_line_num,
                            ),
                        )
                    )

            out_tables[f.CLM_LINE_INSTNL].extend(
                [
                    pac_util.gen_pac_clm_instnl(clm=pac_clm, init_clm_instnl=clm_line_instnl)
                    for clm_line_instnl in claims_tbls[f.CLM_LINE_INSTNL]
                ]
            )

            out_tables[f.CLM_LINE_PRFNL].extend(
                [
                    pac_util.gen_pac_clm_line_prfnl(clm=pac_clm, init_clm_line_prfnl=clm_line_prfnl)
                    for clm_line_prfnl in claims_tbls[f.CLM_LINE_PRFNL]
                ]
            )

            if claims_tbls[f.CLM_INSTNL]:
                out_tables[f.CLM_INSTNL].append(
                    pac_util.gen_pac_clm_instnl(
                        clm=pac_clm, init_clm_instnl=claims_tbls[f.CLM_INSTNL][0]
                    )
                )

            out_tables[f.CLM_PROD].extend(
                [
                    pac_util.gen_pac_clm_prod(clm=pac_clm, init_clm_prod=clm_prod)
                    for clm_prod in claims_tbls[f.CLM_PROD]
                ]
            )

            out_tables[f.CLM_DT_SGNTR].append(
                pac_util.gen_pac_clm_dt_sgntr(
                    clm=pac_clm,
                    init_clm_dt_sgntr=next(iter(claims_tbls[f.CLM_DT_SGNTR]), RowAdapter({})),
                )
            )

            out_tables[f.CLM_RLT_COND_SGNTR_MBR].extend(
                [
                    pac_util.gen_pac_clm_rlt_cond_sgntr_mbr(
                        clm=pac_clm,
                        init_clm_rlt_cond_sgntr_mbr=x,
                        gen_utils=gen_utils,
                    )
                    for x in claims_tbls[f.CLM_RLT_COND_SGNTR_MBR]
                ]
            )

            out_tables[f.CLM_DCMTN].extend(
                [
                    pac_util.gen_pac_clm_dcmtn(clm=pac_clm, gen_utils=gen_utils, init_clm_dcmtn=x)
                    for x in claims_tbls[f.CLM_DCMTN]
                ]
            )

            clm_type_cd = pac_clm[f.CLM_TYPE_CD]
            if clm_type_cd in PROFESSIONAL_CDS:
                clm_prfnls = [
                    pac_util.gen_pac_clm_prfnl(
                        gen_utils=gen_utils,
                        clm=pac_clm,
                        init_clm_prfnl=x,
                    )
                    for x in claims_tbls[f.CLM_PRFNL]
                ]
                out_tables[f.CLM_PRFNL].extend(clm_prfnls)

                if clm_type_cd in MCS_SS_CDS:
                    init_clm_line_mcss = claims_tbls[f.CLM_LINE_MCS] or [
                        RowAdapter({}) for _ in range(random.randint(1, 15))
                    ]
                    for idx, init_clm_line_mcs in enumerate(init_clm_line_mcss, start=1):
                        clm_line_mcs = pac_util.gen_pac_clm_line_mcs(
                            clm=pac_clm,
                            clm_line_num=int(init_clm_line_mcs[f.CLM_LINE_NUM])
                            if init_clm_line_mcs.get(f.CLM_LINE_NUM)
                            else idx,
                            init_clm_line_mcs=init_clm_line_mcs,
                        )
                        out_tables[f.CLM_LINE_MCS].append(clm_line_mcs)

            if clm_type_cd in FISS_SS_CDS:
                init_clm_line_fisss = claims_tbls[f.CLM_LINE_FISS] or [
                    RowAdapter({}) for _ in range(random.randint(1, 15))
                ]
                for idx, init_clm_line_fiss in enumerate(init_clm_line_fisss, start=1):
                    clm_line_fiss = pac_util.gen_pac_clm_line_fiss(
                        clm=pac_clm,
                        clm_line_num=int(init_clm_line_fiss[f.CLM_LINE_NUM])
                        if init_clm_line_fiss.get(f.CLM_LINE_NUM)
                        else idx,
                        init_clm_line_fiss=init_clm_line_fiss,
                    )
                    out_tables[f.CLM_LINE_FISS].append(clm_line_fiss)

    return out_tables


if __name__ == "__main__":
    generate_locally()
