import random
from datetime import datetime

from hamilton.htypes import Collect, Parallelizable  # type: ignore

from load_partition import LoadPartition, LoadType
from logger_config import configure_logger
from model.base_model import (
    IdrBaseModel,
    LoadMode,
)
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
from pipeline_utils import extract_and_load, purge_non_latest_claims

type NodePartitionedModelInput = tuple[type[IdrBaseModel], LoadPartition | None]

configure_logger()

_CLAIM_PARENT_CHILD_TABLES : dict[type[IdrBaseModel], type[IdrBaseModel] | None] = [
    IdrClaimProfessionalNch: IdrClaimItemProfessionalNch,
    IdrClaimInstitutionalNch: IdrClaimItemInstitutionalNch,
    IdrClaimProfessionalSs: IdrClaimItemProfessionalSs,
    IdrClaimItemProfessionalSs: IdrClaimItemInstitutionalSs,
    IdrClaimRx: None # RX/Part D is special because we combine claim + claim line
]

_CLAIM_TABLES: list[type[IdrBaseModel]] = [
    IdrClaimProfessionalNch,
    IdrClaimInstitutionalNch,
    IdrClaimProfessionalSs,
    IdrClaimInstitutionalSs,
]
_CLAIM_AUX_TABLES: list[type[IdrBaseModel]] = [
    # RX/Part D is special because we combine claim + claim line
    IdrClaimRx,
    IdrClaimItemProfessionalNch,
    IdrClaimItemInstitutionalNch,
    IdrClaimItemProfessionalSs,
    IdrClaimItemInstitutionalSs,
]
_BENE_AUX_TABLES: list[type[IdrBaseModel]] = [
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryDualEligibility,
    IdrBeneficiaryMbiId,
    IdrContractPbpContact,
    IdrContractPbpNumber,
    IdrBeneficiaryMaPartDEnrollment,
    IdrBeneficiaryMaPartDEnrollmentRx,
    IdrBeneficiaryLowIncomeSubsidy,
]
_BENE_TABLES: list[type[IdrBaseModel]] = [IdrBeneficiary]
_LOAD_ALL_TABLES = {"all"}


def filter_tables(
    tables: list[type[IdrBaseModel]], tables_to_load: set[str] | None
) -> list[type[IdrBaseModel]]:
    return [
        t
        for t in tables
        if tables_to_load is None
        or tables_to_load == _LOAD_ALL_TABLES
        or t.table() in tables_to_load
    ]


def _gen_partitioned_node_inputs(
    model_types: list[type[IdrBaseModel]], load_type: LoadType
) -> list[NodePartitionedModelInput]:
    models_and_partitions = [
        (
            m,
            [
                partition
                for partition_group in m.model_type().partitions
                for partition in partition_group.generate_ranges(
                    load_type, datetime.date(m.model_type().min_transaction_date)
                )
            ],
        )
        for m in model_types
    ]
    models_with_partitions: list[NodePartitionedModelInput] = [
        (m, p) for m, partitions in models_and_partitions if partitions for p in partitions
    ]
    models_without_partitions = [
        (m, None) for m, partitions in models_and_partitions if not partitions
    ]
    res = models_with_partitions + models_without_partitions
    # randomize to reduce contention on a single table
    random.shuffle(res)
    return sorted(res, key=lambda m: m[1].priority if m[1] else 0)


def stage1(load_mode: LoadMode, start_time: datetime, load_type: LoadType) -> bool:
    return extract_and_load(
        cls=IdrBeneficiaryOvershareMbi,
        partition=None,
        job_start=start_time,
        load_mode=load_mode,
        load_type=load_type,
    )


def stage2_inputs(
    load_type: LoadType,
    tables_to_load: set[str] | None,
    stage1: bool,  # noqa: ARG001
) -> Parallelizable[NodePartitionedModelInput]:
    if load_type == LoadType.INITIAL:
        yield from _gen_partitioned_node_inputs(
            filter_tables(
                [
                    *_CLAIM_AUX_TABLES,
                    *_BENE_AUX_TABLES,
                    *_CLAIM_TABLES,
                    *_BENE_TABLES,
                ],
                tables_to_load,
            ),
            load_type,
        )
    else:
        yield from _gen_partitioned_node_inputs(
            filter_tables([*_CLAIM_AUX_TABLES, *_BENE_AUX_TABLES], tables_to_load), load_type
        )


# NOTE: it would be good to use @parameterize here, but the multiprocessing executor doesn't handle
# serialization properly which is required. See notes about multiprocessing
# here https://hamilton.apache.org/concepts/parallel-task/


def do_stage2(
    stage2_inputs: NodePartitionedModelInput,
    load_mode: LoadMode,
    start_time: datetime,
    load_type: LoadType,
) -> bool:
    model_type, partition = stage2_inputs
    return extract_and_load(
        cls=model_type,
        partition=partition,
        job_start=start_time,
        load_mode=load_mode,
        load_type=load_type,
    )


def collect_stage2(
    do_stage2: Collect[bool],
) -> bool:
    return all(do_stage2)


def stage3_inputs(
    load_type: LoadType,
    tables_to_load: set[str] | None,
    collect_stage2: bool,  # noqa: ARG001
) -> Parallelizable[NodePartitionedModelInput]:
    if load_type == LoadType.INCREMENTAL:
        yield from _gen_partitioned_node_inputs(
            filter_tables(_CLAIM_TABLES, tables_to_load), load_type
        )
    else:
        yield from _gen_partitioned_node_inputs([], load_type)


def do_stage3(
    stage3_inputs: NodePartitionedModelInput,
    load_mode: LoadMode,
    start_time: datetime,
    load_type: LoadType,
) -> bool:
    model_type, partition = stage3_inputs
    return extract_and_load(
        cls=model_type,
        partition=partition,
        job_start=start_time,
        load_mode=load_mode,
        load_type=load_type,
    )


def collect_stage3(
    do_stage3: Collect[bool],
) -> bool:
    return all(do_stage3)


def stage4_inputs(
    load_type: LoadType,
    tables_to_load: set[str] | None,
    collect_stage3: bool,  # noqa: ARG001
) -> Parallelizable[NodePartitionedModelInput]:
    if load_type == LoadType.INCREMENTAL:
        yield from _gen_partitioned_node_inputs(
            filter_tables(_BENE_TABLES, tables_to_load), load_type
        )
    else:
        yield from _gen_partitioned_node_inputs([], load_type)


def do_stage4(
    stage4_inputs: NodePartitionedModelInput,
    load_type: LoadType,
    load_mode: LoadMode,
    start_time: datetime,
) -> bool:
    model_type, partition = stage4_inputs
    if load_type == LoadType.INCREMENTAL:
        return extract_and_load(
            cls=model_type,
            partition=partition,
            job_start=start_time,
            load_mode=load_mode,
            load_type=load_type,
        )

    return False

def collect_stage4(
    do_stage4: Collect[bool],
) -> bool:
    return all(do_stage4)

# stage 5 is purging non-latest claims after extract and load is complete
def stage5_inputs(
    load_type: LoadType,
    tables_to_load: set[str] | None,
    collect_stage4: bool,  # noqa: ARG001
) -> Parallelizable[NodePartitionedModelInput]:
    yield from _gen_partitioned_node_inputs(
        filter_tables([*_CLAIM_TABLES, IdrClaimRx], tables_to_load), load_type # only want parent tables and ClaimRx
    )

def do_stage5(
    stage5_inputs: NodePartitionedModelInput,
    load_type: LoadType,
    load_mode: LoadMode,
    start_time: datetime,) -> bool:
    model_type, partition = stage5_inputs
    return purge_non_latest_claims(
        cls=model_type,
        partition=partition,
        parent_child_tables=_CLAIM_PARENT_CHILD_TABLES,
        job_start=start_time,
        load_mode=load_mode,
        load_type=load_type
    )

def collect_stage5(
    do_stage5: Collect[bool],
) -> bool:
    return all(do_stage5)
