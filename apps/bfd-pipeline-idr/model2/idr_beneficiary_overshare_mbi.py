from collections.abc import Sequence
from datetime import datetime
from typing import Annotated

from constants import (
    NON_CLAIM_PARTITION,
)
from load_partition import LoadPartition, LoadPartitionGroup
from loader import LoadMode
from model import (
    PRIMARY_KEY,
    IdrBaseModel,
)


class IdrBeneficiaryOvershareMbi(IdrBaseModel):
    bene_mbi_id: Annotated[str, {PRIMARY_KEY: True}]

    @staticmethod
    def table() -> str:
        return "idr.beneficiary_overshare_mbi"

    @staticmethod
    def should_replace() -> bool:
        return True

    @staticmethod
    def last_updated_date_table() -> str:
        return ""

    @staticmethod
    def last_updated_date_column() -> list[str]:
        return []

    @staticmethod
    def fetch_query(partition: LoadPartition, start_time: datetime, load_mode: LoadMode) -> str:  # noqa: ARG004
        # The xref data in the bene_hstry table is not completely reliable
        # because sometimes HICNs can be reused, causing two records to be
        # xref'd even if they're not the same person.

        # We'll only trust xref records that have a valid entry in the bene_xref
        # table (this means it's coming from CME). For any MBIs tied to more than
        # one bene_sk that doesn't have a valid xref, we will prevent it from being
        # shown since it may be incorrectly linked to more than one person.
        return """
               SELECT hstry.bene_mbi_id
               FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_hstry hstry
               WHERE NOT EXISTS (
                   SELECT 1
                   FROM cms_vdm_view_mdcr_prd.v2_mdcr_bene_xref xref
                   WHERE hstry.bene_xref_efctv_sk = xref.bene_sk
                     AND hstry.bene_sk = xref.bene_xref_sk
                     AND xref.bene_kill_cred_cd = '2'
               ) AND hstry.bene_mbi_id IS NOT NULL
               GROUP BY hstry.bene_mbi_id
               HAVING COUNT(DISTINCT hstry.bene_sk) > 1 \
               """

    @staticmethod
    def fetch_query_partitions() -> Sequence[LoadPartitionGroup]:
        return [NON_CLAIM_PARTITION]
