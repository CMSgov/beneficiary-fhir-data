import random
from datetime import date, datetime

from faker import Faker

import field_constants as f
from claims_static import NOW
from generator_util import RowAdapter

_faker = Faker()


def match_line_num(clm_lines: list[RowAdapter] | None, clm_line_num: int):
    return next(
        (x for x in clm_lines or [] if int(x[f.CLM_LINE_NUM]) == clm_line_num),
        None,
    )


def four_part_key(row: RowAdapter):
    # It is common across many tables that the unique key is a composite key of the following four
    # columns, so this func makes it easy to generate it
    return (
        f"{int(row[f.CLM_DT_SGNTR_SK])}{row[f.CLM_NUM_SK]}{row[f.GEO_BENE_SK]}{row[f.CLM_TYPE_CD]}"
    )


def get_ric_cd_for_clm_type_cd(clm_type_cd: int):
    if clm_type_cd in (20, 30, 50, 60, 61, 62, 63, 64):
        # part A!
        return "V"  # inpatient

    if clm_type_cd == 40:
        # outpatient
        return "W"  # outpatient

    if clm_type_cd == 10:
        return random.choice(["U", "V", "W"])

    if clm_type_cd in (71, 72):
        return "O"

    if clm_type_cd in (81, 82):
        return "M"

    return None


def add_meta_timestamps(
    obj: RowAdapter,
    clm: RowAdapter,
    max_date: str = str(NOW),
):
    if (
        date.fromisoformat(clm[f.CLM_IDR_LD_DT]) < date(2021, 4, 19)
        and obj.get(f.IDR_INSRT_TS) is None
    ):
        has_insrt_ts = random.random() > 0.5
    else:
        has_insrt_ts = True

    insrt_ts = (
        _faker.date_time_between_dates(
            datetime.fromisoformat(clm[f.CLM_IDR_LD_DT]),
            datetime.fromisoformat(max_date),
        )
        if has_insrt_ts
        else None
    )
    obj[f.IDR_INSRT_TS] = insrt_ts

    obj[f.IDR_UPDT_TS] = (
        _faker.date_time_between_dates(
            insrt_ts,
            datetime.fromisoformat(max_date),
        )
        if has_insrt_ts and random.random() > 0.8
        else None
    )
