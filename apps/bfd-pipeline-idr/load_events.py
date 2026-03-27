import functools
import logging
import re
import typing
from collections.abc import Callable, Iterable
from datetime import UTC, datetime, timedelta
from enum import StrEnum
from string.templatelib import Template
from typing import Annotated
from uuid import UUID

import psycopg
from psycopg import Cursor, sql
from psycopg.rows import DictRow, dict_row
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


def _connect_and_do[T](load_mode: LoadMode, func: Callable[[Cursor[DictRow]], T]) -> T:
    with (
        psycopg.connect(get_connection_string(load_mode)) as conn,
        conn.cursor(row_factory=dict_row) as curs,
    ):
        return func(curs)


def _clean_query_str(query: Template) -> str:
    return re.sub(r"\s+", " ", sql.as_string(query).strip())


def get_eligible_events(load_mode: LoadMode, start_time: datetime) -> list[IdrJobLoadEvent]:
    def _do(curs: Cursor[DictRow]) -> list[IdrJobLoadEvent]:
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
            "Retrieving eligible load events; query: %s", _clean_query_str(get_events_query)
        )
        results = curs.execute(get_events_query)
        load_events = TypeAdapter(list[IdrJobLoadEvent]).validate_python(results, by_alias=True)
        logger.info("Retrieved %d event(s)", len(load_events))

        return load_events

    return _connect_and_do(load_mode, _do)


def get_unreported_jobs(
    load_mode: LoadMode, start_time: datetime, grace_period: timedelta
) -> set[IdrJobType]:
    def _do(curs: Cursor[DictRow]) -> set[IdrJobType]:
        get_jobs_query = t"""
            SELECT {fields(IdrJobLoadEvent, by_alias=True).job_type:i}
                FROM {"idr":i}.{LOAD_EVENTS_TABLE:i}
            WHERE {fields(IdrJobLoadEvent).event_time:i}
                    >= {start_time.astimezone(UTC) - grace_period}
                AND {fields(IdrJobLoadEvent).failure_time:i} IS NULL
            """
        grace_period_hours = int(grace_period.total_seconds() / 60 / 60)
        logger.info(
            "Retrieving job types reported in the last %d hour(s); query: %s",
            grace_period_hours,
            _clean_query_str(get_jobs_query),
        )
        results = curs.execute(get_jobs_query)
        # We're retrieving the jobs that _have_ been reported within the past x hrs; we then take
        # the set difference of this set with the set of all job types to get those that haven't
        # been reported
        reported_jobs = TypeAdapter(set[IdrJobType]).validate_python(
            x[fields(IdrJobLoadEvent, by_alias=True).job_type] for x in results
        )
        unreported_jobs = set(IdrJobType).difference(reported_jobs)
        if len(unreported_jobs) > 0:
            logger.warning(
                "%d jobs were not reported as having completed in the last %d hour(s)",
                len(unreported_jobs),
                grace_period_hours,
            )
        else:
            logger.info(
                "No jobs reported as incomplete in the last %d hour(s)",
                grace_period_hours,
            )

        return unreported_jobs

    return _connect_and_do(load_mode, _do)


def _update_time_column(
    time_column: str, load_mode: LoadMode, events: list[IdrJobLoadEvent], time: datetime
) -> None:
    def _do(curs: Cursor[DictRow]) -> None:
        update_jobs_query = t"""
            UPDATE {"idr":i}.{LOAD_EVENTS_TABLE:i}
            SET {time_column:i}={time.astimezone(UTC)}
            WHERE {fields(IdrJobLoadEvent).id:i} = ANY({[event.id for event in events]})
            RETURNING *;
            """
        logger.info("Updating %s for %d event(s)...", time_column, len(events))
        logger.debug("Query: %s", _clean_query_str(update_jobs_query))
        updated_rows = list(curs.execute(update_jobs_query))
        logger.info("Updated %d event(s) successfully", len(updated_rows))
        if logger.getEffectiveLevel() == logging.DEBUG:
            updated_events = TypeAdapter(list[IdrJobLoadEvent]).validate_python(updated_rows)
            logger.debug("Updated rows: %s", updated_events)

    _connect_and_do(load_mode, _do)


def update_start_times(
    load_mode: LoadMode, events: list[IdrJobLoadEvent], start_time: datetime
) -> None:
    _update_time_column(
        time_column=typing.cast(str, fields(IdrJobLoadEvent).start_time),
        load_mode=load_mode,
        events=events,
        time=start_time,
    )


def update_completion_times(
    load_mode: LoadMode, events: list[IdrJobLoadEvent], completion_time: datetime
) -> None:
    _update_time_column(
        time_column=typing.cast(str, fields(IdrJobLoadEvent).completion_time),
        load_mode=load_mode,
        events=events,
        time=completion_time,
    )


def update_failure_times(
    load_mode: LoadMode, events: list[IdrJobLoadEvent], failure_time: datetime
) -> None:
    _update_time_column(
        time_column=typing.cast(str, fields(IdrJobLoadEvent).failure_time),
        load_mode=load_mode,
        events=events,
        time=failure_time,
    )


def get_tables_to_load(job_types: Iterable[IdrJobType]) -> set[str]:
    return functools.reduce(set[str].union, (x.tables for x in job_types)) if job_types else set()
