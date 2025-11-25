import argparse
import copy
import datetime
import random
import subprocess
import sys

import pandas as pd
from faker import Faker
from generator_util import GeneratorUtil

fake = Faker()

# Command line argument parsing
parser = argparse.ArgumentParser(description="Generate synthetic patient data")
parser.add_argument("--benes", type=str, help="Path to CSV file containing beneficiary data")
parser.add_argument(
    "--claims",
    action="store_true",
    help="Automatically generate claims after patient generation using the generated "
    "SYNTHETIC_BENE_HSTRY.csv file",
)
args = parser.parse_args()

patients_to_generate = 15
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

generator = GeneratorUtil()

# Load CSV data if provided
csv_data = None
if args.benes:
    try:
        csv_data = pd.read_csv(args.benes, dtype={"BENE_SEX_CD": "Int64", "BENE_RACE_CD": "Int64"})
        print(f"Loaded {len(csv_data)} rows from CSV file: {args.benes}")

        patients_to_generate = len(csv_data)
    except Exception as e:
        print(f"Error loading CSV file: {e}")
        print("Falling back to random generation")
        csv_data = None

for i in range(patients_to_generate):
    if i > 0 and i % 10000 == 0:
        print("10000 done")

    patient = generator.create_base_patient()

    # Use CSV data if available, otherwise use random generation
    if csv_data is not None and i < len(csv_data):
        row = csv_data.iloc[i]

        # Use CSV data for populated fields, random for empty ones
        patient["BENE_1ST_NAME"] = (
            row.get("BENE_1ST_NAME")
            if pd.notna(row.get("BENE_1ST_NAME"))
            else random.choice(available_given_names)
        )

        if pd.notna(row.get("BENE_MIDL_NAME")):
            patient["BENE_MIDL_NAME"] = row["BENE_MIDL_NAME"]
        elif random.randint(0, 1):
            patient["BENE_MIDL_NAME"] = random.choice(available_given_names)

        patient["BENE_LAST_NAME"] = (
            row.get("BENE_LAST_NAME")
            if pd.notna(row.get("BENE_LAST_NAME"))
            else random.choice(available_family_names)
        )

        # Handle birth date
        if pd.notna(row.get("BENE_BRTH_DT")):
            patient["BENE_BRTH_DT"] = str(row["BENE_BRTH_DT"])
        else:
            dob = generator.fake.date_of_birth(minimum_age=45)
            patient["BENE_BRTH_DT"] = str(dob)

        # Handle death date
        if pd.notna(row.get("BENE_DEATH_DT")):
            patient["BENE_DEATH_DT"] = str(row["BENE_DEATH_DT"])
            if pd.notna(row.get("BENE_VRFY_DEATH_DAY_SW")):
                patient["BENE_VRFY_DEATH_DAY_SW"] = str(row["BENE_VRFY_DEATH_DAY_SW"])
            else:
                patient["BENE_VRFY_DEATH_DAY_SW"] = "Y" if random.randint(0, 1) == 1 else "N"
        else:
            if random.randint(0, 10) < 2:
                # death!
                death_date = generator.fake.date_between_dates(
                    datetime.date(year=2020, month=1, day=1), datetime.date.today()
                )
                patient["BENE_DEATH_DT"] = str(death_date)
                if random.randint(0, 1) == 1:
                    patient["BENE_VRFY_DEATH_DAY_SW"] = "Y"
                else:
                    patient["BENE_VRFY_DEATH_DAY_SW"] = "N"
            else:
                patient["BENE_DEATH_DT"] = None
                patient["BENE_VRFY_DEATH_DAY_SW"] = "~"

        # Handle sex code
        if pd.notna(row.get("BENE_SEX_CD")):
            # Safely handle non-numeric sex codes
            sex_code = str(row["BENE_SEX_CD"])
            if sex_code.isdigit():
                patient["BENE_SEX_CD"] = str(int(sex_code))
            else:
                patient["BENE_SEX_CD"] = sex_code
        else:
            patient["BENE_SEX_CD"] = str(random.randint(1, 2))

        # Handle race code
        if pd.notna(row.get("BENE_RACE_CD")):
            # Safely handle non-numeric race codes to prevent ValueError
            race_code = str(row["BENE_RACE_CD"])
            if race_code.isdigit():
                patient["BENE_RACE_CD"] = str(int(race_code))
            else:
                patient["BENE_RACE_CD"] = race_code
        else:
            patient["BENE_RACE_CD"] = random.choice(
                ["~", "0", "1", "2", "3", "4", "5", "6", "7", "8"]
            )

        if pd.notna(row.get("BENE_SK")):
            pt_bene_sk = int(row["BENE_SK"])
            if pt_bene_sk in generator.used_bene_sk:
                pt_bene_sk = generator.gen_bene_sk()
        else:
            pt_bene_sk = generator.gen_bene_sk()

        patient["BENE_SK"] = str(pt_bene_sk)
        patient["BENE_XREF_EFCTV_SK"] = str(pt_bene_sk)
        patient["BENE_XREF_SK"] = patient["BENE_XREF_EFCTV_SK"]
        generator.used_bene_sk.append(pt_bene_sk)

        if pd.notna(row.get("BENE_MBI_ID")):
            custom_mbi = str(row["BENE_MBI_ID"])
            num_mbis = random.choices([1, 2, 3, 4], weights=[0.8, 0.14, 0.05, 0.01])[0]
            generator.handle_mbis(patient, num_mbis, custom_first_mbi=custom_mbi)
        else:
            num_mbis = random.choices([1, 2, 3, 4], weights=[0.8, 0.14, 0.05, 0.01])[0]
            generator.handle_mbis(patient, num_mbis)

    else:
        # Original random generation logic
        patient["BENE_1ST_NAME"] = random.choice(available_given_names)
        if random.randint(0, 1):
            patient["BENE_MIDL_NAME"] = random.choice(available_given_names)
        patient["BENE_LAST_NAME"] = random.choice(available_family_names)

        dob = generator.fake.date_of_birth(minimum_age=45)
        patient["BENE_BRTH_DT"] = str(dob)

        if random.randint(0, 10) < 2:
            # death!
            death_date = generator.fake.date_between_dates(
                datetime.date(year=2020, month=1, day=1), datetime.date.today()
            )
            patient["BENE_DEATH_DT"] = str(death_date)
            if random.randint(0, 1) == 1:
                patient["BENE_VRFY_DEATH_DAY_SW"] = "Y"
            else:
                patient["BENE_VRFY_DEATH_DAY_SW"] = "N"
        else:
            patient["BENE_DEATH_DT"] = None
            patient["BENE_VRFY_DEATH_DAY_SW"] = "~"

        patient["BENE_SEX_CD"] = str(random.randint(1, 2))
        patient["BENE_RACE_CD"] = random.choice(["~", "0", "1", "2", "3", "4", "5", "6", "7", "8"])

        pt_bene_sk = generator.gen_bene_sk()
        patient["BENE_SK"] = str(pt_bene_sk)
        patient["BENE_XREF_EFCTV_SK"] = str(pt_bene_sk)
        patient["BENE_XREF_SK"] = patient["BENE_XREF_EFCTV_SK"]
        generator.used_bene_sk.append(pt_bene_sk)

        num_mbis = random.choices([1, 2, 3, 4], weights=[0.8, 0.14, 0.05, 0.01])[0]
        generator.handle_mbis(patient, num_mbis)

    generator.generate_coverages(patient)

    # pt c / d data
    # 50% of the time, generate part C
    # 25% of time, PDP only
    # 25% of time, no part C or D.
    if random.randint(0, 10) >= 5:
        contract_info = generator.generate_bene_mapd_enrlmt(patient, pdp_only=False)
        generator.generate_bene_mapd_enrlmt_rx(patient, contract_info)
        generator.generate_bene_lis(patient)
    elif random.choice([True, False]):
        contract_info = generator.generate_bene_mapd_enrlmt(patient, pdp_only=True)
        generator.generate_bene_mapd_enrlmt_rx(patient, contract_info)
        generator.generate_bene_lis(patient)

    if random.random() < 0.05:
        prior_patient = copy.deepcopy(patient)
        old_bene_sk = prior_patient["BENE_SK"]
        pt_bene_sk = generator.gen_bene_sk()
        prior_patient["BENE_SK"] = str(pt_bene_sk)
        prior_patient["IDR_LTST_TRANS_FLG"] = "N"
        generator.used_bene_sk.append(pt_bene_sk)

        generator.generate_bene_xref(pt_bene_sk, old_bene_sk)

        generator.set_timestamps(prior_patient, datetime.date(year=2017, month=5, day=20))

        # Override the obsolete timestamp to be in the past year instead of future
        past_year_date = datetime.date.today() - datetime.timedelta(days=random.randint(30, 365))
        prior_patient["IDR_TRANS_OBSLT_TS"] = f"{past_year_date}T00:00:00.000000+0000"

        generator.bene_hstry_table.append(prior_patient)

    generator.bene_hstry_table.append(patient)

generator.save_output_files()

# If --claims flag is provided, automatically call claims_generator.py
if args.claims:
    print("Generating claims for generated benes")
    try:
        # Call claims_generator.py with the generated SYNTHETIC_BENE_HSTRY.csv file
        result = subprocess.run(
            [
                sys.executable,
                "claims_generator.py",
                "--benes",
                "out/SYNTHETIC_BENE_HSTRY.csv",
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
