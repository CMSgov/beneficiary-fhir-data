import itertools
import random
import string
from datetime import date, datetime
from typing import Any

import pandas as pd
from dateutil.relativedelta import relativedelta
from faker import Faker

import field_constants as f
from claims_static import (
    AVAIL_CONTRACT_NAMES,
    AVAIL_PBP_TYPE_CODES,
    AVAILABLE_FAMILY_NAMES,
    AVAILABLE_GIVEN_NAMES,
    AVAILABLE_PROVIDER_LEGAL_NAMES,
    AVAILABLE_PROVIDER_NAMES,
    AVAILABLE_PROVIDER_TX_CODES,
    AVAILABLE_PROVIDER_TYPE_CODES,
    NOW,
)
from generator_util import (
    AVAIL_CONTRACT_NUMS,
    AVAIL_PBP_NUMS,
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

    def gen_contract_plan(
        self,
        amount: int,
        init_contract_pbp_nums: list[RowAdapter] | None = None,
        init_contract_pbp_contacts: list[RowAdapter] | None = None,
    ):
        init_contract_pbp_nums = init_contract_pbp_nums or []
        init_contract_pbp_contacts = init_contract_pbp_contacts or []
        additional_pbp_nums = [RowAdapter({}) for _ in range(amount - len(init_contract_pbp_nums))]
        additional_pbp_contacts = [
            RowAdapter({}) for _ in range(amount - len(init_contract_pbp_contacts))
        ]
        all_pbp_nums = init_contract_pbp_nums + additional_pbp_nums
        all_pbp_contacts = init_contract_pbp_contacts + additional_pbp_contacts
        today = date.today()
        last_day = today.replace(month=12, day=31)

        contract_pbp_nums: list[RowAdapter] = []
        contract_pbp_contacts: list[RowAdapter] = []
        contract_num_and_pbp_num_pairs = list(
            itertools.product(AVAIL_CONTRACT_NUMS, AVAIL_PBP_NUMS)
        )
        random.shuffle(contract_num_and_pbp_num_pairs)
        used_pairs = set()

        for pbp_num in init_contract_pbp_nums:
            cntrct = pbp_num.get(f.CNTRCT_NUM)
            pbp = pbp_num.get(f.CNTRCT_PBP_NUM)
            if cntrct and pbp:
                used_pairs.add((cntrct, pbp))

        available_contract_num_pairs = [
            pair for pair in contract_num_and_pbp_num_pairs if pair not in used_pairs
        ]
        pair_index = 0

        for i in range(amount):
            pbp_num = all_pbp_nums[i]
            pbp_contact = all_pbp_contacts[i]
            contract_num = pbp_num.get(f.CNTRCT_NUM)
            pbp_val = pbp_num.get(f.CNTRCT_PBP_NUM)
            if not contract_num or not pbp_val:
                contract_num, pbp_val = available_contract_num_pairs[pair_index]
                pair_index += 1
            sk = pbp_num.get(f.CNTRCT_PBP_SK) or gen_basic_id(field=f.CNTRCT_PBP_SK, length=12)
            effective_date = _faker.date_between_dates(date.fromisoformat("2020-01-01"), NOW)
            end_date = _faker.date_between_dates(effective_date, NOW + relativedelta(years=3))
            obsolete_date = random.choice(
                [_faker.date_between_dates(effective_date, NOW), date.fromisoformat("9999-12-31")]
            )

            pbp_num.extend(
                {
                    f.CNTRCT_PBP_SK: sk,
                    f.CNTRCT_NUM: contract_num,
                    f.CNTRCT_PBP_NUM: pbp_val,
                    f.CNTRCT_PBP_NAME: random.choice(AVAIL_CONTRACT_NAMES),
                    f.CNTRCT_PBP_TYPE_CD: random.choice(AVAIL_PBP_TYPE_CODES),
                    f.CNTRCT_DRUG_PLAN_IND_CD: random.choice(["Y", "N"]),
                    f.CNTRCT_PBP_SK_EFCTV_DT: effective_date.isoformat(),
                    f.CNTRCT_PBP_END_DT: end_date.isoformat(),
                    f.CNTRCT_PBP_SK_OBSLT_DT: obsolete_date.isoformat(),
                }
            )

            pbp_contact.extend(
                {
                    f.CNTRCT_PBP_SK: sk,
                    f.CNTRCT_PLAN_CNTCT_OBSLT_DT: "9999-12-31",
                    f.CNTRCT_PLAN_CNTCT_TYPE_CD: random.choice(["~", "30", "62"]),
                    f.CNTRCT_PLAN_FREE_EXTNSN_NUM: "".join(random.choices(string.digits, k=7)),
                    f.CNTRCT_PLAN_CNTCT_FREE_NUM: "".join(random.choices(string.digits, k=10)),
                    f.CNTRCT_PLAN_CNTCT_EXTNSN_NUM: "".join(random.choices(string.digits, k=7)),
                    f.CNTRCT_PLAN_CNTCT_TEL_NUM: "".join(random.choices(string.digits, k=10)),
                    f.CNTRCT_PBP_END_DT: last_day.isoformat(),
                    f.CNTRCT_PBP_BGN_DT: today.isoformat(),
                    f.CNTRCT_PLAN_CNTCT_ST_1_ADR: random.choice(
                        [
                            "319 E. Street",
                            "North Street",
                            "West Street",
                        ]
                    ),
                    f.CNTRCT_PLAN_CNTCT_ST_2_ADR: random.choice(["Avenue M", ""]),
                    f.CNTRCT_PLAN_CNTCT_CITY_NAME: random.choice(
                        [
                            "Los Angeles",
                            "San Jose",
                            "San Francisco",
                        ]
                    ),
                    f.CNTRCT_PLAN_CNTCT_STATE_CD: "CA",
                    f.CNTRCT_PLAN_CNTCT_ZIP_CD: "".join(random.choices(string.digits, k=9)),
                }
            )

            contract_pbp_nums.append(pbp_num)
            contract_pbp_contacts.append(pbp_contact)

        return contract_pbp_nums, contract_pbp_contacts
