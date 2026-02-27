import random
from datetime import date, datetime
from typing import Any

import pandas as pd
from faker import Faker

import field_constants as f
from claims_static import (
    AVAILABLE_FAMILY_NAMES,
    AVAILABLE_GIVEN_NAMES,
    AVAILABLE_PROVIDER_LEGAL_NAMES,
    AVAILABLE_PROVIDER_NAMES,
    AVAILABLE_PROVIDER_TX_CODES,
    AVAILABLE_PROVIDER_TYPE_CODES,
    NOW,
)
from generator_util import (
    CLM_ANSI_SGNTR,
    RowAdapter,
    gen_basic_id,
)

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

    def gen_synthetic_clm_ansi_sgntr(self, src_path: str = f"sample-data/{CLM_ANSI_SGNTR}.csv"):
        csv_df = pd.read_csv(  # type: ignore
            src_path,
            dtype=str,
            na_filter=False,
        )
        clm_ansi_sgntr: list[dict[str, Any]] = csv_df.to_dict(orient="records")  # type: ignore

        # Return the data from the source but with every CLM_ANSI_SGNTR_SK made negative to indicate
        # it's synthetic
        return [
            RowAdapter(x | {f.CLM_ANSI_SGNTR_SK: f"-{x[f.CLM_ANSI_SGNTR_SK]}"})
            for x in clm_ansi_sgntr
        ]

    def gen_provider_history(
        self, amount: int, init_provider_historys: list[RowAdapter] | None = None
    ):
        init_provider_historys = init_provider_historys or []
        additional_provider_historys = [
            RowAdapter({}) for _ in range(amount - len(init_provider_historys))
        ]
        all_provider_historys = init_provider_historys + additional_provider_historys

        provider_historys: list[RowAdapter] = []
        for provider_history in all_provider_historys:
            prvdr_sk = gen_basic_id(field="PRVDR_SK", length=9)
            provider_history.extend(
                {
                    f.PRVDR_SK: prvdr_sk,
                    f.PRVDR_HSTRY_EFCTV_DT: str(date.today()),
                    f.PRVDR_HSTRY_OBSLT_DT: "9999-12-31",
                    f.PRVDR_1ST_NAME: random.choice(AVAILABLE_GIVEN_NAMES),
                    f.PRVDR_MDL_NAME: random.choice(AVAILABLE_GIVEN_NAMES),
                    f.PRVDR_LAST_NAME: random.choice(AVAILABLE_FAMILY_NAMES),
                    f.PRVDR_NAME: random.choice(AVAILABLE_PROVIDER_NAMES),
                    f.PRVDR_LGL_NAME: random.choice(AVAILABLE_PROVIDER_LEGAL_NAMES),
                    f.PRVDR_NPI_NUM: prvdr_sk,
                    f.PRVDR_EMPLR_ID_NUM: gen_basic_id(field=f.PRVDR_EMPLR_ID_NUM, length=9),
                    f.PRVDR_OSCAR_NUM: gen_basic_id(field=f.PRVDR_OSCAR_NUM, length=6),
                    f.PRVDR_TXNMY_CMPST_CD: random.choice(AVAILABLE_PROVIDER_TX_CODES),
                    f.PRVDR_TYPE_CD: random.choice(AVAILABLE_PROVIDER_TYPE_CODES),
                }
            )
            self._generate_meta_sk_pair(provider_history)

            provider_historys.append(provider_history)

        return provider_historys
