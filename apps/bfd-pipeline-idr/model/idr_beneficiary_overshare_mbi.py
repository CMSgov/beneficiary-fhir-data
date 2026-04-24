from datetime import datetime
from typing import Annotated, override

from constants import IDR_BENE_HISTORY_TABLE, IDR_BENE_XREF_TABLE
from load_partition import LoadPartition
from model.base_model import (
    PRIMARY_KEY,
    IdrBaseModel,
    ModelType,
    Source,
)


class IdrBeneficiaryOvershareMbi(IdrBaseModel):
    bene_mbi_id: Annotated[str, {PRIMARY_KEY: True}]

    @override
    @staticmethod
    def table() -> str:
        return "idr.beneficiary_overshare_mbi"

    @override
    @staticmethod
    def should_replace() -> bool:
        return True

    @override
    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @override
    @staticmethod
    def model_type() -> ModelType:
        return ModelType.BENEFICIARY

    @override
    @classmethod
    def fetch_query(cls, partition: LoadPartition, start_time: datetime, source: Source) -> str:
        # The xref data in the bene_hstry table is not completely reliable
        # because sometimes HICNs can be reused, causing two records to be
        # xref'd even if they're not the same person.

        # We'll only trust xref records that have a valid entry in the bene_xref
        # table (this means it's coming from CME). For any MBIs tied to more than
        # one bene_sk that doesn't have a valid xref, we will prevent it from being
        # shown since it may be incorrectly linked to more than one person.
        return f"""
               SELECT hstry.bene_mbi_id
               FROM {IDR_BENE_HISTORY_TABLE} hstry
               WHERE NOT EXISTS (
                   SELECT 1
                   FROM {IDR_BENE_XREF_TABLE} xref
                   WHERE hstry.bene_xref_efctv_sk = xref.bene_sk
                     AND hstry.bene_sk = xref.bene_xref_sk
                     AND xref.bene_kill_cred_cd = '2'
               ) AND hstry.bene_mbi_id IS NOT NULL
               GROUP BY hstry.bene_mbi_id
               HAVING COUNT(DISTINCT hstry.bene_sk) > 1
               """
