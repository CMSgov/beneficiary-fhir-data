import logging
import re
from datetime import UTC, datetime
from enum import StrEnum
from typing import Annotated
from uuid import UUID

import psycopg
from psycopg import sql
from psycopg.rows import dict_row
from pydantic.fields import Field
from pydantic.main import BaseModel
from pydantic.type_adapter import TypeAdapter

from loader import get_connection_string
from logger_config import configure_logger
from model.base_model import LoadMode
from model.idr_beneficiary import IdrBeneficiary
from model.idr_beneficiary_dual_eligibility import IdrBeneficiaryDualEligibility
from model.idr_beneficiary_entitlement import IdrBeneficiaryEntitlement
from model.idr_beneficiary_entitlement_reason import IdrBeneficiaryEntitlementReason
from model.idr_beneficiary_low_income_subsidy import IdrBeneficiaryLowIncomeSubsidy
from model.idr_beneficiary_ma_part_d_enrollment import IdrBeneficiaryMaPartDEnrollment
from model.idr_beneficiary_ma_part_d_enrollment_rx import IdrBeneficiaryMaPartDEnrollmentRx
from model.idr_beneficiary_mbi_id import IdrBeneficiaryMbiId
from model.idr_beneficiary_overshare_mbi import IdrBeneficiaryOvershareMbi
from model.idr_beneficiary_status import IdrBeneficiaryStatus
from model.idr_beneficiary_third_party import IdrBeneficiaryThirdParty
from model.idr_claim_institutional_nch import IdrClaimInstitutionalNch
from model.idr_claim_institutional_ss import IdrClaimInstitutionalSs
from model.idr_claim_item_institutional_nch import IdrClaimItemInstitutionalNch
from model.idr_claim_item_institutional_ss import IdrClaimItemInstitutionalSs
from model.idr_claim_item_professional_nch import IdrClaimItemProfessionalNch
from model.idr_claim_item_professional_ss import IdrClaimItemProfessionalSs
from model.idr_claim_professional_nch import IdrClaimProfessionalNch
from model.idr_claim_professional_ss import IdrClaimProfessionalSs
from model.idr_claim_rx import IdrClaimRx
from model.idr_contract_pbp_contact import IdrContractPbpContact
from model.idr_contract_pbp_number import IdrContractPbpNumber
from pydantic_utils import fields

configure_logger()
logger = logging.getLogger(__name__)

LOAD_EVENTS_TABLE = "idr_load_events"
_PART_D_TABLES = {
    IdrBeneficiaryMaPartDEnrollment.table(),
    IdrBeneficiaryMaPartDEnrollmentRx.table(),
    IdrClaimRx.table(),
    IdrContractPbpContact.table(),
    IdrContractPbpNumber.table(),
}
_SHARED_SYSTEMS_TABLES = {
    IdrClaimProfessionalSs.table(),
    IdrClaimInstitutionalSs.table(),
    IdrClaimItemProfessionalSs.table(),
    IdrClaimItemInstitutionalSs.table(),
}
_NCH_TABLES = {
    IdrClaimProfessionalNch.table(),
    IdrClaimInstitutionalNch.table(),
    IdrClaimItemProfessionalNch.table(),
    IdrClaimItemInstitutionalNch.table(),
}
_BENE_TABLES = {
    IdrBeneficiaryDualEligibility.table(),
    IdrBeneficiaryEntitlementReason.table(),
    IdrBeneficiaryEntitlement.table(),
    IdrBeneficiaryLowIncomeSubsidy.table(),
    IdrBeneficiaryMbiId.table(),
    IdrBeneficiaryOvershareMbi.table(),
    IdrBeneficiaryStatus.table(),
    IdrBeneficiaryThirdParty.table(),
    IdrBeneficiary.table(),
}


class IdrJobType(StrEnum):
    DDPS = ("DDPS_SNOWFLAKE", _PART_D_TABLES)
    FISS = ("FISS_SNOWFLAKE", _SHARED_SYSTEMS_TABLES)
    MCS = ("MCS_SNOWFLAKE", _SHARED_SYSTEMS_TABLES)
    VMS = ("VMS_SNOWFLAKE", _SHARED_SYSTEMS_TABLES)
    NCH = ("CLMNCH_SNOWFLAKE", _NCH_TABLES)
    BENE = ("BENE_SNOWFLAKE", _BENE_TABLES)

    def __init__(
        self,
        _: str,
        tables: set[str],
    ) -> None:
        self.tables = tables

    def __new__(
        cls: type[IdrJobType],
        value: str,
        tables: set[str],
    ) -> IdrJobType:
        obj = str.__new__(cls, value)
        obj._value_ = value
        obj.tables = tables
        return obj


class IdrJobLoadEvent(BaseModel):
    id: UUID
    job_type: Annotated[IdrJobType, Field(alias="job_name")]
    job_message: str
    event_time: datetime
    start_time: datetime | None
    completion_time: datetime | None
    failure_time: datetime | None


def get_eligible_events(load_mode: LoadMode, start_time: datetime) -> list[IdrJobLoadEvent]:
    with (
        psycopg.connect(get_connection_string(load_mode)) as conn,
        conn.cursor(row_factory=dict_row) as curs,
    ):
        sql_fields = sql.SQL(", ").join(
            [
                sql.Identifier(field.alias or name)
                for name, field in IdrJobLoadEvent.model_fields.items()
            ]
        )
        get_events_query = t"""
            SELECT {sql_fields:q} FROM {"idr":i}.{LOAD_EVENTS_TABLE:i}
            WHERE {fields(IdrJobLoadEvent).event_time:i} <= {start_time.astimezone(UTC)}
                AND {fields(IdrJobLoadEvent).start_time:i} IS NULL
                AND {fields(IdrJobLoadEvent).completion_time:i} IS NULL
                AND {fields(IdrJobLoadEvent).failure_time:i} IS NULL
            """
        logger.info(
            "Retrieving eligible load events; query: %s",
            re.sub(r"\s+", " ", sql.as_string(get_events_query).strip()),
        )
        results = curs.execute(get_events_query)
        load_events = TypeAdapter(list[IdrJobLoadEvent]).validate_python(results, by_alias=True)
        logger.info("Retrieved %d event(s)", len(load_events))

        return load_events
