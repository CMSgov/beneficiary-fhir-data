import argparse
import datetime
import logging
import random
import subprocess
import sys

import tqdm
from constants import (
    BENE_DUAL,
    BENE_ENTLMT,
    BENE_ENTLMT_RSN,
    BENE_HSTRY,
    BENE_LIS_CMBND,
    BENE_MAPD_ENRLMT,
    BENE_MAPD_ENRLMT_RX,
    BENE_MBI_ID,
    BENE_STUS,
    BENE_TP,
    BENE_XREF,
    CNTRCT_PBP_CNTCT,
    CNTRCT_PBP_NUM,
)
from faker import Faker
from generator_util import (
    GeneratorUtil,
    adapters_to_dicts,
    load_file_dict,
    output_table_contains_by_bene_sk,
    probability,
)
from id_generators import IdGenerator, RandomIdGenerator, SequentialIdGenerator, load_id_state
from load_synthetic_output import CsvWriter, OutputDestinationWriter, SnowflakeWriter
from row_adapter import RowAdapter

logger = logging.getLogger(__name__)

fake = Faker()

_BATCH_SIZE = 50_000

# Command line argument parsing
parser = argparse.ArgumentParser(description="Generate synthetic patient data")
parser.add_argument(
    "paths",
    nargs="*",
    help=(
        "Paths to CSVs or directories including CSVs that will be regenerated/updated with new "
        "columns. Updates are idempotent, meaning that passing in an existing table/CSV without "
        "any new columns being added to the synthetic data generation will result in a "
        "byte-identical output file. Take care to avoid providing a partial set of tables with "
        "foreign key constraints (e.g. BENE_SK) without providing the root table as this could "
        "result in broken output data"
    ),
)
parser.add_argument(
    "--patients",
    default=0,
    type=int,
    help=(
        "Number of NEW patients to generate. Does not affect patients regenerated when "
        f"{BENE_HSTRY} is provided"
    ),
)
parser.add_argument(
    "--claims",
    action="store_true",
    help="Automatically generate claims after patient generation using the generated "
    "SYNTHETIC_BENE_HSTRY",
)
parser.add_argument(
    "--exclude-empty",
    action=argparse.BooleanOptionalAction,
    help=(
        "Treat empty column values as non-existent and allow the generator to generate new values"
    ),
)
parser.add_argument(
    "--force-ztm-static-rows",
    action=argparse.BooleanOptionalAction,
    help=(
        'Allow "zero-to-many" rows (e.g. BENE_ENTLMT, c/d data, etc.) for a patient loaded from '
        "a file to be generated. This will introduce new rows for patients that previously had "
        "none. Useful if not all tables for a patient have been generated yet."
    ),
    dest="force_ztm",
)
parser.add_argument(
    "--truncate",
    action="store_true",
    default=False,
    help="Truncate tables before reloading. Default is false",
)
parser.add_argument(
    "--batch-size",
    type=int,
    default=_BATCH_SIZE,
    dest="batch_size",
    help="Batch size of BENE_SKs to process",
)
args = parser.parse_args()


available_given_names = [
    "Alex",
    "Frankie",
    "Joey",
    "Caroline",
    "Kartoffel",
    "Elmo",
    "Abby",
    "Snuffleupagus",
    "Bandit",
    "Bluey",
    "Bingo",
    "Chilli",
    "Le Petit Prince",
]
available_family_names = ["Erdapfel", "Heeler", "Coffee", "Jones", "Smith", "Sheep"]


def regenerate_static_tables(generator: GeneratorUtil, files: dict[str, list[RowAdapter]]):
    # "Generate" (extend, really) existing rows in all but the "root" table for patient (BENE_HSTRY)
    # to ensure existing rows remain idempotent in the output whilst allowing new fields to be added
    message = f"Regenerating/updating {', '.join((k for k, v in files.items() if v))}..."
    print(message)
    logger.info(message)

    for bene_mbi_id_row in files[BENE_MBI_ID]:
        # BENE_MBI_ID is a special case in that its generation function mutates both its own output
        # table and the BENE_HSTRY table. The function has special case logic (a hack) to handle the
        # regeneration case so that only the BENE_MBI_ID table is mutated here. This is also why the
        # "patient" is an empty RowAdapter. A proper implementation would do something different
        # here.
        generator.gen_mbis_for_patient(
            patient=RowAdapter({}), num_mbis=1, initial_mbi_obj=bene_mbi_id_row
        )

    for bene_stus_row in files[BENE_STUS]:
        generator.generate_bene_stus(
            stus_row=bene_stus_row,
            medicare_start_date=bene_stus_row["MDCR_STUS_BGN_DT"],
            medicare_end_date=bene_stus_row["MDCR_STUS_END_DT"],
            mdcr_stus_cd=bene_stus_row["BENE_MDCR_STUS_CD"],
        )

    for bene_entlmt_rsn_row in files[BENE_ENTLMT_RSN]:
        generator.generate_bene_entlmnt_rsn(
            rsn_row=bene_entlmt_rsn_row,
            medicare_start_date=bene_entlmt_rsn_row["BENE_RNG_BGN_DT"],
            medicare_end_date=bene_entlmt_rsn_row["BENE_RNG_END_DT"],
        )

    for bene_entlmt_row in files[BENE_ENTLMT]:
        generator.generate_bene_entlmt(
            entlmt_row=bene_entlmt_row,
            medicare_start_date=bene_entlmt_row["BENE_RNG_BGN_DT"],
            medicare_end_date=bene_entlmt_row["BENE_RNG_END_DT"],
            coverage_type=bene_entlmt_row["BENE_MDCR_ENTLMT_TYPE_CD"],
        )

    for bene_tp_row in files[BENE_TP]:
        generator.generate_bene_tp(
            tp_row=bene_tp_row,
            medicare_start_date=bene_tp_row["BENE_RNG_BGN_DT"],
            medicare_end_date=bene_tp_row["BENE_RNG_END_DT"],
            buy_in_cd=bene_tp_row["BENE_BUYIN_CD"],
            coverage_type=bene_tp_row["BENE_TP_TYPE_CD"],
        )

    for bene_dual_row in files[BENE_DUAL]:
        generator.generate_bene_dual(
            dual_row=bene_dual_row,
            dual_start_date=bene_dual_row["BENE_MDCD_ELGBLTY_BGN_DT"],
            dual_end_date=bene_dual_row["BENE_MDCD_ELGBLTY_END_DT"],
            dual_status_cd=bene_dual_row["BENE_DUAL_STUS_CD"],
            dual_type_cd=bene_dual_row["BENE_DUAL_TYPE_CD"],
            medicaid_state_cd=bene_dual_row["GEO_USPS_STATE_CD"],
        )

    for bene_mapd_enrlmt_row in files[BENE_MAPD_ENRLMT]:
        generator.generate_bene_mapd_enrlmt(enrollment_row=bene_mapd_enrlmt_row)

    for bene_mapd_enrlmt_rx_row in files[BENE_MAPD_ENRLMT_RX]:
        generator.generate_bene_mapd_enrlmt_rx(
            rx_row=bene_mapd_enrlmt_rx_row,
            contract_pbp_sk=bene_mapd_enrlmt_rx_row["CNTRCT_PBP_SK"],
            contract_num=bene_mapd_enrlmt_rx_row["BENE_CNTRCT_NUM"],
            pbp_num=bene_mapd_enrlmt_rx_row["BENE_PBP_NUM"],
        )

    for bene_lis_row in files[BENE_LIS_CMBND]:
        generator.generate_bene_lis_cmbnd(lis_row=bene_lis_row)

    for patient_xref_row in files[BENE_XREF]:
        generator.generate_bene_xref(
            bene_xref=patient_xref_row,
            new_bene_sk=patient_xref_row["BENE_SK"],
            old_bene_sk=int(patient_xref_row["BENE_XREF_SK"]),
        )

    message = "Finished regenerating/updating all files"
    print(message)
    logger.info(message)


def load_inputs(
    patients: int,
    force_ztm: bool,
    batch_size: int,
    truncate: bool,
    writer: OutputDestinationWriter | None = None,
):
    if writer is None:
        writer = CsvWriter()

    if isinstance(writer, SnowflakeWriter):
        _handle_snowflake_flow(writer, patients, force_ztm, batch_size, truncate)
    else:
        _handle_csv_flow(writer)


def _handle_csv_flow(writer: CsvWriter):
    generator: GeneratorUtil = GeneratorUtil(id_gen=RandomIdGenerator())

    csv_tables = [
        BENE_HSTRY,
        BENE_MBI_ID,
        BENE_STUS,
        BENE_ENTLMT_RSN,
        BENE_ENTLMT,
        BENE_TP,
        BENE_XREF,
        BENE_DUAL,
        BENE_MAPD_ENRLMT,
        BENE_MAPD_ENRLMT_RX,
        BENE_LIS_CMBND,
        CNTRCT_PBP_NUM,
        CNTRCT_PBP_CNTCT,
    ]

    files: dict[str, list[RowAdapter]] = {table: [] for table in csv_tables}
    load_file_dict(files=files, paths=args.paths, exclude_empty=args.exclude_empty)

    generator.gen_contract_plan(
        amount=10,
        init_contract_pbp_nums=files[CNTRCT_PBP_NUM],
        init_contract_pbp_contacts=files[CNTRCT_PBP_CNTCT],
    )

    if any(files.values()):
        regenerate_static_tables(generator, files)

    num_existing = len(files[BENE_HSTRY])
    num_new = int(args.patients)
    if num_existing == 0 and num_new == 0:
        print(f"No {BENE_HSTRY}.csv provided or --patients arg specified")
        sys.exit(1)

    log_messages = []
    if num_existing:
        log_messages.append(f"regenerating {num_existing} existing patients")
    if num_new > 0:
        log_messages.append(f"generating {num_new} new patients")
    message = f"{', and '.join(log_messages)}...".capitalize()
    print(message)
    logger.info(message)

    patients: list[RowAdapter] = files[BENE_HSTRY] + [RowAdapter({}) for _ in range(num_new)]
    patient_mbi_id_rows = {row["BENE_MBI_ID"]: row.kv for row in files[BENE_MBI_ID]}

    _generate_patients_batch(generator, patients, patient_mbi_id_rows, args.force_ztm)
    generator.save_output_files(writer)


def _handle_snowflake_flow(
    writer: SnowflakeWriter,
    patients: int,
    force_ztm: bool,
    batch_size: int,
    truncate: bool,
):
    patient_tables = [
        BENE_MBI_ID,
        BENE_STUS,
        BENE_ENTLMT_RSN,
        BENE_ENTLMT,
        BENE_TP,
        BENE_XREF,
        BENE_DUAL,
        BENE_MAPD_ENRLMT,
        BENE_MAPD_ENRLMT_RX,
        BENE_LIS_CMBND,
    ]

    if truncate:
        writer.truncate_tables([*patient_tables, BENE_HSTRY, CNTRCT_PBP_NUM, CNTRCT_PBP_CNTCT])

    id_state = load_id_state(writer)
    id_gen: IdGenerator = SequentialIdGenerator(id_state)
    generator: GeneratorUtil = GeneratorUtil(id_gen=id_gen)

    # TODO: check if we still want contracts amount to be fixed amount
    _generate_contracts(generator, writer)

    # Regenerate existing data updates in batches
    if not truncate:
        idx = 1
        for bene_sks_batch in writer.iter_bene_sk_batches(batch_size):
            existing = writer.get_patient_batch(bene_sks_batch, patient_tables)
            regenerate_static_tables(generator, existing)
            patient_mbi_id_rows = {row["BENE_MBI_ID"]: row.kv for row in existing[BENE_MBI_ID]}

            _generate_patients_batch(
                generator, existing[BENE_HSTRY], patient_mbi_id_rows, force_ztm
            )
            generator.flush_batch(writer)
            logger.info(f"Patient data generation completed for batch {idx}!")
            idx += 1
    else:
        # Generate new synthetic patients in batches
        num_new_patients = int(patients)
        for i in range(0, num_new_patients, batch_size):
            chunk = min(batch_size, num_new_patients - i)
            _generate_patients_batch(
                generator, [RowAdapter({}) for _ in range(chunk)], {}, force_ztm
            )
            generator.flush_batch(writer)
            logger.info(f"Patient data generation completed for batch {i}!")

    logger.info("Patient data generation complete!")


def _generate_patients_batch(
    generator: GeneratorUtil,
    patients: list[RowAdapter],
    patient_mbi_id_rows: dict[str, dict],
    force_ztm: bool,
) -> None:
    for patient in tqdm.tqdm(patients):
        generator.create_base_patient(patient)
        patient["BENE_1ST_NAME"] = random.choice(available_given_names)
        if probability(0.5):
            patient["BENE_MIDL_NAME"] = random.choice(available_given_names)
        patient["BENE_LAST_NAME"] = random.choice(available_family_names)
        dob = generator.fake.date_of_birth(minimum_age=45)
        patient["BENE_BRTH_DT"] = str(dob)
        ssn_prefix = random.choices(
            ["000", "666", str(random.randint(900, 999))], weights=[1, 1, 100]
        )[0]
        patient["BENE_SSN_NUM"] = ssn_prefix + "".join(
            [str(random.randint(0, 9)) for _ in range(6)]
        )
        if probability(0.2):
            # death!
            death_date = generator.fake.date_between_dates(
                datetime.date(year=2020, month=1, day=1), datetime.date.today()
            )
            patient["BENE_DEATH_DT"] = str(death_date)
            patient["BENE_VRFY_DEATH_DAY_SW"] = "Y" if probability(0.5) else "N"
        patient["BENE_SEX_CD"] = str(random.randint(1, 2))
        patient["BENE_RACE_CD"] = random.choice(["~", "0", "1", "2", "3", "4", "5", "6", "7", "8"])

        pt_bene_sk = generator.gen_bene_sk()
        patient["BENE_SK"] = str(pt_bene_sk)
        patient["BENE_XREF_EFCTV_SK"] = str(pt_bene_sk)
        patient["BENE_XREF_SK"] = patient["BENE_XREF_EFCTV_SK"]

        patient_static_mbi_row = patient_mbi_id_rows.get(patient.get("BENE_MBI_ID"))
        if not patient_static_mbi_row:
            # If the patient has no corresponding static MBIs and is loaded from a file (static) we
            # generate a single MBI ID to ensure a static table size, otherwise (if the patient is
            # totally generated) we generate upto 4 MBIs (n - 1 being obsolete)
            num_mbis = (
                1
                if (patient.loaded_from_file and not force_ztm)
                else random.choices([1, 2, 3, 4], weights=[0.8, 0.14, 0.05, 0.01])[0]
            )
            generator.gen_mbis_for_patient(patient, num_mbis)

        generator.generate_coverages(patient=patient, force_ztm=force_ztm)

        # pt c / d data
        # 50% of the time, generate part C
        # 25% of time, PDP only
        # 25% of time, no part C or D.
        if (not patient.loaded_from_file or force_ztm) and probability(0.5):
            initial_kv_template = {"BENE_SK": patient["BENE_SK"]}

            if not output_table_contains_by_bene_sk(
                table=generator.bene_mapd_enrlmt,
                for_file=BENE_MAPD_ENRLMT,
                bene_sk=patient["BENE_SK"],
            ):
                contract_pbp_sk, contract_num, pbp_num = generator.generate_bene_mapd_enrlmt(
                    enrollment_row=RowAdapter(initial_kv_template.copy()),
                    pdp_only=probability(0.5),
                )
                generator.generate_bene_mapd_enrlmt_rx(
                    rx_row=RowAdapter(initial_kv_template.copy()),
                    contract_pbp_sk=contract_pbp_sk,
                    contract_num=contract_num,
                    pbp_num=pbp_num,
                )

            # We don't need to check !force_ztm or loaded_from_file because this is unreachable if
            # any of those are true
            if probability(0.5) and not output_table_contains_by_bene_sk(
                table=generator.bene_lis_cmbnd,
                for_file=BENE_LIS_CMBND,
                bene_sk=patient["BENE_SK"],
            ):
                generator.generate_bene_lis_cmbnd(RowAdapter(initial_kv_template.copy()))

        if (not patient.loaded_from_file or force_ztm) and probability(0.05):
            # Exclude rows from the original patient that will be modified so that RowAdapter does
            # not ignore those changes
            prior_patient = RowAdapter(
                {
                    k: v
                    for k, v in patient.kv.items()
                    if k
                    not in {
                        "BENE_SK",
                        "IDR_LTST_TRANS_FLG",
                        "IDR_TRANS_OBSLT_TS",
                        "IDR_TRANS_EFCTV_TS",
                        "IDR_INSRT_TS",
                        "IDR_UPDT_TS",
                    }
                }
            )
            pt_bene_sk = generator.gen_bene_sk()
            prior_patient["BENE_SK"] = str(pt_bene_sk)
            prior_patient["IDR_LTST_TRANS_FLG"] = "Y"
            # 90% of the time we want the historical patient to have a different MBI than the
            # current patient as this is by far the most common case in prod data
            if probability(0.9):
                generator.gen_mbis_for_patient(patient=prior_patient, num_mbis=1)

            bene_xref = RowAdapter({})
            generator.generate_bene_xref(
                bene_xref=bene_xref, new_bene_sk=patient["BENE_SK"], old_bene_sk=pt_bene_sk
            )

            generator.set_timestamps(prior_patient, datetime.date(year=2017, month=5, day=20))

            # Override the obsolete timestamp to be in the past year instead of future
            past_year_date = datetime.date.today() - datetime.timedelta(
                days=random.randint(30, 365)
            )
            # We update the underlying dict to avoid RowAdapter ignoring the change
            prior_patient.kv["IDR_TRANS_OBSLT_TS"] = f"{past_year_date}T00:00:00.000000"

            generator.bene_hstry_table.append(prior_patient.kv)

        generator.bene_hstry_table.append(patient.kv)

    message = f"Done generating {len(patients)} patients"
    print(message)
    logger.info(message)


def _generate_contracts(
    generator: GeneratorUtil, writer: OutputDestinationWriter, amount: int = 10
) -> None:
    existing_contracts = [
        RowAdapter(row, loaded_from_file=True) for row in writer.get_rows(CNTRCT_PBP_NUM)
    ]
    existing_contacts = [
        RowAdapter(row, loaded_from_file=True) for row in writer.get_rows(CNTRCT_PBP_CNTCT)
    ]
    contract_pbp_nums, contract_pbp_contacts = generator.gen_contract_plan(
        amount=amount,
        init_contract_pbp_nums=existing_contracts,
        init_contract_pbp_contacts=existing_contacts,
    )
    generator.export_table(adapters_to_dicts(contract_pbp_nums), CNTRCT_PBP_NUM, writer=writer)

    generator.export_table(
        adapters_to_dicts(contract_pbp_contacts),
        CNTRCT_PBP_CNTCT,
        writer=writer,
    )


if __name__ == "__main__":
    load_inputs(
        patients=args.patients,
        force_ztm=args.force_ztm,
        batch_size=args.batch_size,
        truncate=args.truncate,
    )

    # If --claims flag is provided, automatically call claims_generator.py
    if args.claims:
        print("Generating claims for generated benes")
        try:
            # Call claims_generator.py with the generated SYNTHETIC_BENE_HSTRY and SYNTHETIC_CNTRCT_PBP_NUM
            claims_args = [
                sys.executable,
                "claims_generator.py",
                f"../out/{BENE_HSTRY}.csv",
                f"../out/{CNTRCT_PBP_NUM}.csv",
            ]

            result = subprocess.run(
                args=claims_args,
                check=True,
                stdout=sys.stdout,
                stderr=sys.stderr,
            )

            print("Claims generation completed successfully!")
        except subprocess.CalledProcessError as e:
            print(f"Error running claims generator: {e}")
            if e.stderr:
                print("Error output:")
                print(e.stderr)
            sys.exit(1)
