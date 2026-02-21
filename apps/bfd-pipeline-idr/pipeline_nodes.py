import random
from datetime import datetime

from hamilton.htypes import Collect, Parallelizable  # type: ignore

from constants import MIN_CLAIM_LOAD_DATE
from load_partition import LoadPartition, LoadType
from model import (
    IdrBaseModel,
    LoadMode,
    get_min_transaction_date,
)
from model2.idr_beneficiary import IdrBeneficiary
from model2.idr_beneficiary_dual_eligibility import IdrBeneficiaryDualEligibility
from model2.idr_beneficiary_entitlement import IdrBeneficiaryEntitlement
from model2.idr_beneficiary_entitlement_reason import IdrBeneficiaryEntitlementReason
from model2.idr_beneficiary_low_income_subsidy import IdrBeneficiaryLowIncomeSubsidy
from model2.idr_beneficiary_ma_part_d_enrollment import IdrBeneficiaryMaPartDEnrollment
from model2.idr_beneficiary_ma_part_d_enrollment_rx import IdrBeneficiaryMaPartDEnrollmentRx
from model2.idr_beneficiary_mbi_id import IdrBeneficiaryMbiId
from model2.idr_beneficiary_overshare_mbi import IdrBeneficiaryOvershareMbi
from model2.idr_beneficiary_status import IdrBeneficiaryStatus
from model2.idr_beneficiary_third_party import IdrBeneficiaryThirdParty
from model2.idr_claim import IdrClaim
from model2.idr_claim_ansi_signature import IdrClaimAnsiSignature
from model2.idr_claim_date_signature import IdrClaimDateSignature
from model2.idr_claim_fiss import IdrClaimFiss
from model2.idr_claim_institutional import IdrClaimInstitutional
from model2.idr_claim_institutional_nch import IdrClaimInstitutionalNch
from model2.idr_claim_institutional_ss import IdrClaimInstitutionalSs
from model2.idr_claim_item import IdrClaimItem
from model2.idr_claim_item_institutional_nch import IdrClaimItemInstitutionalNch
from model2.idr_claim_item_institutional_ss import IdrClaimItemInstitutionalSs
from model2.idr_claim_item_professional_nch import IdrClaimItemProfessionalNch
from model2.idr_claim_item_professional_ss import IdrClaimItemProfessionalSs
from model2.idr_claim_line_institutional import IdrClaimLineInstitutional
from model2.idr_claim_line_professional import IdrClaimLineProfessional
from model2.idr_claim_line_rx import IdrClaimLineRx
from model2.idr_claim_professional import IdrClaimProfessional
from model2.idr_claim_professional_nch import IdrClaimProfessionalNch
from model2.idr_claim_professional_ss import IdrClaimProfessionalSs
from model2.idr_claim_rx import IdrClaimRx
from model2.idr_contract_pbp_contact import IdrContractPbpContact
from model2.idr_contract_pbp_number import IdrContractPbpNumber
from model2.idr_provider_history import IdrProviderHistory
from pipeline_utils import extract_and_load
from settings import TABLES_TO_LOAD

type NodePartitionedModelInput = tuple[type[IdrBaseModel], LoadPartition | None]


def filter_tables(tables: list[type[IdrBaseModel]]) -> list[type[IdrBaseModel]]:
    return [t for t in tables if TABLES_TO_LOAD == [] or t.table() in TABLES_TO_LOAD]


def claim_tables() -> list[type[IdrBaseModel]]:
    return filter_tables(
        [
            IdrClaim,
            IdrClaimProfessionalNch,
            IdrClaimInstitutionalNch,
            IdrClaimProfessionalSs,
            IdrClaimInstitutionalSs,
        ]
    )


def claim_aux_tables() -> list[type[IdrBaseModel]]:
    return filter_tables(
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
            IdrClaimItemInstitutionalNch,
            IdrClaimItemProfessionalSs,
            IdrClaimItemInstitutionalSs,
        ]
    )


def bene_aux_tables() -> list[type[IdrBaseModel]]:
    return filter_tables(
        [
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
    )


def bene_tables() -> list[type[IdrBaseModel]]:
    return filter_tables([IdrBeneficiary])


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
            [*claim_aux_tables(), *bene_aux_tables(), *claim_tables(), *bene_tables()], load_type
        )
    else:
        yield from _gen_partitioned_node_inputs(
            [*claim_aux_tables(), *bene_aux_tables()], load_type
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
