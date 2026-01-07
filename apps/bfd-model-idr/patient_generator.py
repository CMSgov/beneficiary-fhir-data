import argparse
import copy
import datetime
import random
import subprocess
import sys

import tqdm
from faker import Faker

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
    GeneratorUtil,
    RowAdapter,
    load_file_dict,
    output_table_contains_by_bene_sk,
    probability,
)

fake = Faker()

# Command line argument parsing
parser = argparse.ArgumentParser(description="Generate synthetic patient data")
parser.add_argument("files", nargs="*")
parser.add_argument("--patients", default=15, help="Number of patients to generate")
parser.add_argument(
    "--claims",
    action="store_true",
    help="Automatically generate claims after patient generation using the generated "
    "SYNTHETIC_BENE_HSTRY.csv file",
)
parser.add_argument(
    "--exclude-empty",
    action=argparse.BooleanOptionalAction,
    help=(
        "Treat empty column values as non-existant and allow the generator to generate new values"
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
            contract_num=bene_mapd_enrlmt_rx_row["BENE_CNTRCT_NUM"],
            pbp_num=bene_mapd_enrlmt_rx_row["BENE_PBP_NUM"],
        )

    for bene_lis_row in files[BENE_LIS]:
        generator.generate_bene_lis(lis_row=bene_lis_row)

    for patient_xref_row in files[BENE_XREF]:
        generator.generate_bene_xref(
            bene_xref=patient_xref_row,
            new_bene_sk=patient_xref_row["BENE_SK"],
            old_bene_sk=int(patient_xref_row["BENE_XREF_SK"]),
        )


def load_inputs():
    generator = GeneratorUtil()

    files: dict[str, list[RowAdapter]] = {
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
    }
    load_file_dict(files=files, file_paths=args.files, exclude_empty=args.exclude_empty)

    regenerate_static_tables(generator, files)

    patients: list[RowAdapter] = files[BENE_HSTRY] or [RowAdapter({})] * args.patients
    patient_mbi_id_rows = {row["BENE_MBI_ID"]: row.kv for row in files[BENE_MBI_ID]}

    for patient in tqdm.tqdm(patients):
        generator.create_base_patient(patient)
        patient["BENE_1ST_NAME"] = random.choice(available_given_names)
        if probability(0.5):
            patient["BENE_MIDL_NAME"] = random.choice(available_given_names)
        patient["BENE_LAST_NAME"] = random.choice(available_family_names)
        dob = generator.fake.date_of_birth(minimum_age=45)
        patient["BENE_BRTH_DT"] = str(dob)
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
        generator.used_bene_sk.append(pt_bene_sk)

        patient_static_mbi_row = patient_mbi_id_rows.get(patient["BENE_MBI_ID"])
        if not patient_static_mbi_row:
            # If the patient has no corresponding static MBIs and is loaded from a file (static) we
            # generate a single MBI ID to ensure a static table size, otherwise (if the patient is
            # totally generated) we generate upto 4 MBIs (n - 1 being obsolete)
            num_mbis = (
                1
                if patient.loaded_from_file
                else random.choices([1, 2, 3, 4], weights=[0.8, 0.14, 0.05, 0.01])[0]
            )
            generator.gen_mbis_for_patient(patient, num_mbis)

        generator.generate_coverages(patient=patient, force_ztm=args.force_ztm)

        # pt c / d data
        # 50% of the time, generate part C
        # 25% of time, PDP only
        # 25% of time, no part C or D.
        if (not patient.loaded_from_file or args.force_ztm) and probability(0.5):
            initial_kv_template = {"BENE_SK": patient["BENE_SK"]}

            if not output_table_contains_by_bene_sk(
                table=generator.bene_mapd_enrlmt,
                for_file=BENE_MAPD_ENRLMT,
                bene_sk=patient["BENE_SK"],
            ):
                contract_num, pbp_num = generator.generate_bene_mapd_enrlmt(
                    enrollment_row=RowAdapter(initial_kv_template.copy()), pdp_only=probability(0.5)
                )
                generator.generate_bene_mapd_enrlmt_rx(
                    rx_row=RowAdapter(initial_kv_template.copy()),
                    contract_num=contract_num,
                    pbp_num=pbp_num,
                )

            # We don't need to check !force_ztm or loaded_from_file because this is unreachable if
            # any of those are true
            if probability(0.5) and not output_table_contains_by_bene_sk(
                table=generator.bene_lis,
                for_file=BENE_LIS,
                bene_sk=patient["BENE_SK"],
            ):
                generator.generate_bene_lis(RowAdapter(initial_kv_template.copy()))

        if (not patient.loaded_from_file or args.force_ztm) and probability(0.05):
            prior_patient = copy.deepcopy(patient)
            pt_bene_sk = generator.gen_bene_sk()
            prior_patient["BENE_SK"] = str(pt_bene_sk)
            prior_patient["IDR_LTST_TRANS_FLG"] = "N"
            generator.used_bene_sk.append(pt_bene_sk)

            bene_xref = RowAdapter({})
            generator.generate_bene_xref(
                bene_xref=bene_xref, new_bene_sk=patient["BENE_SK"], old_bene_sk=pt_bene_sk
            )
            generator.bene_xref_table.append(bene_xref.kv)

            generator.set_timestamps(prior_patient, datetime.date(year=2017, month=5, day=20))

            # Override the obsolete timestamp to be in the past year instead of future
            past_year_date = datetime.date.today() - datetime.timedelta(
                days=random.randint(30, 365)
            )
            prior_patient["IDR_TRANS_OBSLT_TS"] = f"{past_year_date}T00:00:00.000000+0000"

            generator.bene_hstry_table.append(prior_patient.kv)

        generator.bene_hstry_table.append(patient.kv)

    generator.save_output_files()


if __name__ == "__main__":
    load_inputs()

    # If --claims flag is provided, automatically call claims_generator.py
    if args.claims:
        print("Generating claims for generated benes")
        try:
            # Call claims_generator.py with the generated SYNTHETIC_BENE_HSTRY.csv file
            result = subprocess.run(
                args=[
                    sys.executable,
                    "claims_generator.py",
                    f"out/{BENE_HSTRY}.csv",
                    *[file for file in args.files if BENE_HSTRY not in file],
                ],
                check=True,
                capture_output=True,
                text=True,
            )

            print("Claims generation completed successfully!")
            if result.stdout:
                print("Claims generator output:")
                print(result.stdout)

        except subprocess.CalledProcessError as e:
            print(f"Error running claims generator: {e}")
            if e.stderr:
                print("Error output:")
                print(e.stderr)
            sys.exit(1)
