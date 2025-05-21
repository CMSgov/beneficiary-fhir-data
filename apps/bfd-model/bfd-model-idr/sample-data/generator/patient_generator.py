import random
import copy
import datetime
import pandas as pd
from dateutil.parser import parse
from faker import Faker
from patient_generator_util import PatientGeneratorUtil

fake = Faker()

patients_to_generate = 80000
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

generator = PatientGeneratorUtil()

for i in range(patients_to_generate):
    if i > 0 and i % 10000 == 0:
        print("10000 done")

    patient = generator.create_base_patient()
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
    patient["BENE_RACE_CD"] = random.choice(
        ["~", "0", "1", "2", "3", "4", "5", "6", "7", "8"]
    )

    pt_bene_sk = generator.gen_bene_sk()
    patient["BENE_SK"] = str(pt_bene_sk)
    patient["BENE_XREF_EFCTV_SK"] = str(pt_bene_sk)
    generator.used_bene_sk.append(pt_bene_sk)

    num_mbis = random.choices([1, 2, 3, 4], weights=[0.8, 0.14, 0.05, 0.01])[0]
    generator.handle_mbis(patient, num_mbis)

    for idx in range(0, random.randint(0, 2)):
        if idx == 0:
            continue
        prior_patient = copy.deepcopy(patient)
        pt_bene_sk = generator.gen_bene_sk()
        prior_patient["BENE_SK"] = str(pt_bene_sk)
        generator.used_bene_sk.append(pt_bene_sk)
        generator.set_timestamps(
            prior_patient, datetime.date(year=2017, month=5, day=20)
        )
        generator.bene_table.append(prior_patient)

    generator.bene_table.append(patient)

generator.save_output_files()
