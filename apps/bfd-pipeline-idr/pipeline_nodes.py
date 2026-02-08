import random
from datetime import datetime

from hamilton.htypes import Collect, Parallelizable  # type: ignore

from constants import MIN_CLAIM_LOAD_DATE
from load_partition import LoadPartition, LoadType
from model import (
    IdrBaseModel,
    IdrBeneficiary,
    IdrBeneficiaryDualEligibility,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryLowIncomeSubsidy,
    IdrBeneficiaryMaPartDEnrollment,
    IdrBeneficiaryMaPartDEnrollmentRx,
    IdrBeneficiaryMbiId,
    IdrBeneficiaryOvershareMbi,
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrClaimAnsiSignature,
    IdrClaimFiss,
    IdrClaimLineInstitutional,
    IdrClaimLineProfessional,
    IdrClaimLineRx,
    IdrClaimProfessional,
    IdrContractPbpContact,
    IdrContractPbpNumber,
    IdrProviderHistory,
    LoadMode,
    get_min_transaction_date,
)
from model2.idr_claim import IdrClaim
from model2.idr_claim_date_signature import IdrClaimDateSignature
from model2.idr_claim_institutional import IdrClaimInstitutional
from model2.idr_claim_institutional_nch import IdrClaimInstitutionalNch
from model2.idr_claim_item import IdrClaimItem
from model2.idr_claim_item_professional_nch import IdrClaimItemProfessionalNch
from model2.idr_claim_professional_nch import IdrClaimProfessionalNch
from model2.idr_claim_rx import IdrClaimRx
from pipeline_utils import extract_and_load
from settings import CLAIM_TABLES

type NodePartitionedModelInput = tuple[type[IdrBaseModel], LoadPartition | None]


def filter_claim_tables(tables: list[type[IdrBaseModel]]) -> list[type[IdrBaseModel]]:
    return [t for t in tables if CLAIM_TABLES == [] or t.table() in CLAIM_TABLES]


def claim_tables() -> list[type[IdrBaseModel]]:
    return filter_claim_tables([IdrClaim, IdrClaimProfessionalNch, IdrClaimInstitutionalNch])


def claim_aux_tables() -> list[type[IdrBaseModel]]:
    return filter_claim_tables(
        [
            IdrClaimInstitutional,
            IdrClaimDateSignature,
            IdrClaimFiss,
            IdrClaimItem,
            IdrClaimLineInstitutional,
            IdrClaimAnsiSignature,
            IdrClaimProfessional,
            IdrClaimLineProfessional,
            IdrClaimLineRx,
            IdrProviderHistory,
            # RX/Part D is special because we combine claim + claim line
            IdrClaimRx,
            IdrClaimItemProfessionalNch,
        ]
    )


BENE_AUX_TABLES = [
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


def _gen_partitioned_node_inputs(
    model_types: list[type[IdrBaseModel]], load_type: LoadType
) -> list[NodePartitionedModelInput]:
    start_date = get_min_transaction_date(MIN_CLAIM_LOAD_DATE)

    models_and_partitions = [
        (
            m,
            [
                partition
                for partition_group in m.fetch_query_partitions()
                for partition in partition_group.generate_ranges(
                    load_type, datetime.date(start_date)
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


def stage2_inputs(load_type: LoadType, stage1: bool) -> Parallelizable[NodePartitionedModelInput]:  # noqa: ARG001
    if load_type == LoadType.INITIAL:
        yield from _gen_partitioned_node_inputs(
            [*claim_aux_tables(), *BENE_AUX_TABLES, *claim_tables(), IdrBeneficiary], load_type
        )
    else:
        yield from _gen_partitioned_node_inputs([*claim_aux_tables(), *BENE_AUX_TABLES], load_type)


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
    collect_stage2: bool,  # noqa: ARG001
) -> Parallelizable[NodePartitionedModelInput]:
    if load_type == LoadType.INCREMENTAL:
        yield from _gen_partitioned_node_inputs(claim_tables(), load_type)
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


def do_stage4(
    collect_stage3: bool,  # noqa: ARG001
    load_type: LoadType,
    load_mode: LoadMode,
    start_time: datetime,
) -> bool:
    if load_type == LoadType.INCREMENTAL:
        return extract_and_load(
            cls=IdrBeneficiary,
            partition=None,
            job_start=start_time,
            load_mode=load_mode,
            load_type=load_type,
        )
    return False
