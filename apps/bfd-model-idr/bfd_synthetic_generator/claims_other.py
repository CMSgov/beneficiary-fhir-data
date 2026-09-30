import csv
import io
import os
from pathlib import Path
import random
from datetime import date, datetime
from typing import Any
import zipfile

import constants as f
import pandas as pd
from claims_static import (
    AVAILABLE_FAMILY_NAMES,
    AVAILABLE_GIVEN_NAMES,
    AVAILABLE_PROVIDER_LEGAL_NAMES,
    AVAILABLE_PROVIDER_NAMES,
    AVAILABLE_PROVIDER_TX_CODES,
    AVAILABLE_PROVIDER_TYPE_CODES,
    NOW,
)
from faker import Faker
from generator_util import GeneratorUtil
from row_adapter import RowAdapter

_faker = Faker()


class OtherGeneratorUtil:
    def _generate_meta_sk_pair(self, obj: RowAdapter):
        def encode(d: datetime | date):
            d = d.date() if isinstance(d, datetime) else d
            yyyymmdd = d.year * 10000 + d.month * 100 + d.day
            base = (yyyymmdd - 19000000) * 1000
            seq = random.randint(1, 999)
            return base + seq

        max_dt = datetime.fromisoformat(str(NOW))
        min_dt = datetime(2010, 1, 1)

        if random.random() < 0.05:
            update_dt = _faker.date_time_between_dates(min_dt, max_dt)
            obj[f.META_SK] = 501
            obj[f.META_LST_UPDT_SK] = encode(update_dt)
            return

        insert_dt = _faker.date_time_between_dates(min_dt, max_dt)
        obj[f.META_SK] = encode(insert_dt)

        roll = random.random()
        if roll > 0.8:
            update_dt = _faker.date_time_between_dates(insert_dt, max_dt)
            obj[f.META_LST_UPDT_SK] = encode(update_dt)
        elif roll > 0.6:
            obj[f.META_LST_UPDT_SK] = obj[f.META_SK]
        else:
            obj[f.META_LST_UPDT_SK] = 0

    def load_addresses(self):
        base_dir = Path(os.path.realpath(__file__)).parent
        target_path = base_dir.joinpath("SYNTHETIC_CLM_ANSI_SGNTR.csv")
        path_str = str(target_path)

        # check if inside a zip archive (stored procedure)
        if ".zip" in path_str:
            zip_part, internal_part = path_str.split(".zip", 1)
            zip_path = zip_part + ".zip"
            internal_file_path = internal_part.lstrip("/\\")

            with zipfile.ZipFile(zip_path, "r") as z, z.open(internal_file_path) as f:
                file_text = f.read().decode("utf-8")
                file_stream = io.StringIO(file_text)
                csvreader = csv.reader(file_stream)
                self._parse_csv_rows(csvreader)
        else:
            # fallback if normal directory (local development)
            with target_path.open(encoding="utf-8") as file:
                csvreader = csv.reader(file)
                self._parse_csv_rows(csvreader)

    def _parse_csv_rows(self, csvreader):
        clm_ansi_sgntr: list[dict[str, Any]] = []
        header = next(csvreader)
        for row in csvreader:
            cur_row: dict[str, Any] = {}
            for col in range(len(row)):
                cur_row[header[col]] = row[col]
            clm_ansi_sgntr.append(cur_row)

        # Return the data from the source but with every CLM_ANSI_SGNTR_SK made negative to indicate
        # it's synthetic
        return [
            RowAdapter(x | {f.CLM_ANSI_SGNTR_SK: f"-{x[f.CLM_ANSI_SGNTR_SK]}"})
            for x in clm_ansi_sgntr
        ]

    def gen_synthetic_clm_ansi_sgntr(self):
        base_dir = Path(os.path.realpath(__file__)).parent
        target_path = base_dir.joinpath("SYNTHETIC_CLM_ANSI_SGNTR.csv")
        path_str = str(target_path)

        # Check if inside a zip archive (stored procedure)
        if ".zip" in path_str:
            zip_part, internal_part = path_str.split(".zip", 1)
            zip_path = zip_part + ".zip"
            internal_file_path = internal_part.lstrip("/\\")

            with zipfile.ZipFile(zip_path, "r") as z, z.open(internal_file_path) as file_stream:
                csv_df = pd.read_csv(
                    file_stream,
                    dtype=str,
                    na_filter=False,
                )
        else:
            # Fallback if normal directory (local development)
            csv_df = pd.read_csv(  # type: ignore
                target_path,
                dtype=str,
                na_filter=False,
            )

        clm_ansi_sgntr: list[dict[str, Any]] = csv_df.to_dict(orient="records")  # type: ignore

        # Return the data from the source but with every CLM_ANSI_SGNTR_SK made negative to indicate it's synthetic
        return [
            RowAdapter(
                {k: (None if v == "" else v) for k, v in x.items()}
                | {f.CLM_ANSI_SGNTR_SK: f"-{x[f.CLM_ANSI_SGNTR_SK]}"}
            )
            for x in clm_ansi_sgntr
        ]

    def gen_provider_history(
        self,
        amount: int,
        gen_utils: GeneratorUtil,
        init_provider_historys: list[RowAdapter] | None = None,
    ):
        init_provider_historys = init_provider_historys or []
        additional_provider_historys = [
            RowAdapter({}) for _ in range(amount - len(init_provider_historys))
        ]
        all_provider_historys = init_provider_historys + additional_provider_historys

        provider_historys: list[RowAdapter] = []
        generated_type_1_npis = set()
        generated_type_2_npis = set()
        for idx, provider_history in enumerate(all_provider_historys):
            prvdr_sk = gen_utils.id_gen.npi_id(field="PRVDR_SK")
            # make half of providers type 1 npi and half type 2
            # type 1 npis never have a legal name
            # need to return both the subsets of type 1/2 npis that were used so that
            # generated claims can reference provider histories that actually exist
            if idx % 2 == 0:
                prvdr_lgl_name = ""
                generated_type_1_npis.add(prvdr_sk)
            else:
                prvdr_lgl_name = random.choice(AVAILABLE_PROVIDER_LEGAL_NAMES)
                generated_type_2_npis.add(prvdr_sk)
            provider_history.extend(
                {
                    f.PRVDR_SK: prvdr_sk,
                    f.PRVDR_HSTRY_EFCTV_DT: str(date.today()),
                    f.PRVDR_HSTRY_OBSLT_DT: "9999-12-31",
                    f.PRVDR_1ST_NAME: random.choice(AVAILABLE_GIVEN_NAMES),
                    f.PRVDR_MDL_NAME: random.choice(AVAILABLE_GIVEN_NAMES),
                    f.PRVDR_LAST_NAME: random.choice(AVAILABLE_FAMILY_NAMES),
                    f.PRVDR_NAME: random.choice(AVAILABLE_PROVIDER_NAMES),
                    f.PRVDR_LGL_NAME: prvdr_lgl_name,
                    f.PRVDR_NPI_NUM: prvdr_sk,
                    f.PRVDR_EMPLR_ID_NUM: gen_utils.id_gen.gen_basic_id(
                        field=f.PRVDR_EMPLR_ID_NUM, length=9
                    ),
                    f.PRVDR_OSCAR_NUM: gen_utils.id_gen.gen_basic_id(
                        field=f.PRVDR_OSCAR_NUM, length=6
                    ),
                    f.PRVDR_TXNMY_CMPST_CD: random.choice(AVAILABLE_PROVIDER_TX_CODES),
                    f.PRVDR_TYPE_CD: random.choice(AVAILABLE_PROVIDER_TYPE_CODES),
                }
            )
            self._generate_meta_sk_pair(provider_history)

            provider_historys.append(provider_history)

        return provider_historys, list(generated_type_1_npis), list(generated_type_2_npis)
