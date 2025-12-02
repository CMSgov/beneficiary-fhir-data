import random
from datetime import datetime

from hamilton.htypes import Collect, Parallelizable  # type: ignore

from constants import MIN_CLAIM_LOAD_DATE
from load_partition import LoadPartition
from model import (
    IdrBaseModel,
    IdrBeneficiary,
    IdrBeneficiaryDualEligibility,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryMbiId,
    IdrBeneficiaryOvershareMbi,
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrClaim,
    IdrClaimAnsiSignature,
    IdrClaimDateSignature,
    IdrClaimFiss,
    IdrClaimInstitutional,
    IdrClaimItem,
    IdrClaimLineInstitutional,
    IdrClaimLineProfessional,
    IdrClaimLineRx,
    IdrClaimProfessional,
    IdrProviderHistory,
    LoadMode,
    LoadType,
    get_min_transaction_date,
)
from pipeline_utils import extract_and_load

type NodePartitionedModelInput = tuple[type[IdrBaseModel], LoadPartition | None]

CLAIM_AUX_TABLES = [
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
]
BENE_AUX_TABLES = [
    IdrBeneficiaryStatus,
    IdrBeneficiaryThirdParty,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryDualEligibility,
    IdrBeneficiaryMbiId,
]


def _gen_partitioned_node_inputs(
    model_types: list[type[IdrBaseModel]],
) -> list[NodePartitionedModelInput]:
    start_date = get_min_transaction_date(MIN_CLAIM_LOAD_DATE)

    models_and_partitions = [
        (
            m,
            [
                partition
                for partition_group in m.fetch_query_partitions()
                for partition in partition_group.generate_ranges(datetime.date(start_date))
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


def stage1(load_mode: LoadMode, start_time: datetime) -> bool:
    return extract_and_load(
        cls=IdrBeneficiaryOvershareMbi,
        partition=None,
        job_start=start_time,
        load_mode=load_mode,
    )


def stage2_inputs(load_type: LoadType, stage1: bool) -> Parallelizable[NodePartitionedModelInput]:  # noqa: ARG001
    if load_type == LoadType.INITIAL:
        yield from _gen_partitioned_node_inputs(
            [*CLAIM_AUX_TABLES, *BENE_AUX_TABLES, IdrClaim, IdrBeneficiary]
        )
    else:
        yield from _gen_partitioned_node_inputs([*CLAIM_AUX_TABLES, *BENE_AUX_TABLES])


# NOTE: it would be good to use @parameterize here, but the multiprocessing executor doesn't handle
# serialization properly which is required. See notes about multiprocessing
# here https://hamilton.apache.org/concepts/parallel-task/


def do_stage2(
    stage2_inputs: NodePartitionedModelInput,
    load_mode: LoadMode,
    start_time: datetime,
) -> bool:
    model_type, partition = stage2_inputs
    return extract_and_load(
        cls=model_type,
        partition=partition,
        job_start=start_time,
        load_mode=load_mode,
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
        yield from _gen_partitioned_node_inputs([IdrClaim])
    else:
        yield from _gen_partitioned_node_inputs([])


def do_stage3(
    stage3_inputs: NodePartitionedModelInput,
    load_mode: LoadMode,
    start_time: datetime,
) -> bool:
    model_type, partition = stage3_inputs
    return extract_and_load(
        cls=model_type,
        partition=partition,
        job_start=start_time,
        load_mode=load_mode,
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
        )
    return False
