import functools
import itertools
import random
from datetime import datetime

from batch_worker import LoadingBatchWorkerClient
from load_partition import LoadPartition, LoadType
from model.base_model import (
    IdrBaseModel,
    LoadMode,
    Source,
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
from model.idr_prior_auth import IdrPriorAuth
from parallel_executor import ParallelStagesExecutor, Stage
from pipeline_utils import extract_and_load, prune_phase_1_ss_claims
from settings import enable_prior_auth_ingestion

type NodePartitionedModelInput = tuple[type[IdrBaseModel], LoadPartition | None]


_CLAIM_TABLES: list[type[IdrBaseModel]] = [
    IdrClaimProfessionalNch,
    IdrClaimInstitutionalNch,
    IdrClaimProfessionalSs,
    IdrClaimInstitutionalSs,
]
_CLAIM_SS_TABLES: list[type[IdrBaseModel]] = [
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
_PRIOR_AUTH_TABLES: list[type[IdrBaseModel]] = [IdrPriorAuth]
_LOAD_ALL_TABLES = {"all"}


class StagedIdrPipeline:
    def __init__(
        self,
        max_workers: int,
        load_mode: LoadMode,
        start_time: datetime,
        load_type: LoadType,
        source: Source,
        worker_client: LoadingBatchWorkerClient,
        tables_to_load: set[str] | None,
    ) -> None:
        self.load_mode = load_mode
        self.start_time = start_time
        self.load_type = load_type
        self.source = source
        self.worker_client = worker_client
        self.tables_to_load = tables_to_load
        self._executor = ParallelStagesExecutor(max_workers)

    async def start(self) -> bool:
        return all(
            itertools.chain.from_iterable(
                await self._executor.execute(
                    [
                        self._stage1_do_bene_overshare_mbi(),
                        self._stage2_do_claims_and_benes_tbls(),
                        self._stage3_do_parent_claims_tbls(),
                        self._stage4_do_beneficiary(),
                        self._stage5_do_phase_1_prune(),
                    ],
                )
            )
        )

    def _stage1_do_bene_overshare_mbi(self) -> Stage[bool]:
        yield from self._extract_and_load_stage([(IdrBeneficiaryOvershareMbi, None)])

    def _stage2_do_claims_and_benes_tbls(self) -> Stage[bool]:
        tables = [*_CLAIM_AUX_TABLES, *_BENE_AUX_TABLES]
        if self.load_type == LoadType.INITIAL:
            tables.extend([*_CLAIM_TABLES, *_BENE_TABLES])
        if enable_prior_auth_ingestion():
            tables.append(*_PRIOR_AUTH_TABLES)

        filtered_tables = self._filter_tables(tables)

        yield from self._extract_and_load_stage(self._gen_partitioned_node_inputs(filtered_tables))

    def _stage3_do_parent_claims_tbls(self) -> Stage[bool]:
        if self.load_type == LoadType.INITIAL:
            return

        yield from self._extract_and_load_stage(
            self._gen_partitioned_node_inputs(self._filter_tables(_CLAIM_TABLES))
        )

    def _stage4_do_beneficiary(self) -> Stage[bool]:
        if self.load_type == LoadType.INITIAL:
            return

        yield from self._extract_and_load_stage(
            self._gen_partitioned_node_inputs(self._filter_tables(_BENE_TABLES))
        )

    def _stage5_do_phase_1_prune(self) -> Stage[bool]:
        if self.load_type == LoadType.INITIAL:
            return

        for model in self._filter_tables(_CLAIM_SS_TABLES):
            yield functools.partial(
                prune_phase_1_ss_claims,
                model,
                self.load_mode,
                self.start_time,
            )

    def _filter_tables(self, tables: list[type[IdrBaseModel]]) -> list[type[IdrBaseModel]]:
        return [
            t
            for t in tables
            if self.tables_to_load is None
            or self.tables_to_load == _LOAD_ALL_TABLES
            or t.table() in self.tables_to_load
        ]

    def _gen_partitioned_node_inputs(
        self, model_types: list[type[IdrBaseModel]]
    ) -> list[NodePartitionedModelInput]:
        models_and_partitions = [
            (
                m,
                [
                    partition
                    for partition_group in m.model_type().partitions
                    for partition in partition_group.generate_ranges(
                        self.load_type, datetime.date(m.model_type().min_transaction_date)
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

    def _extract_and_load_stage(
        self, model_and_parts: list[NodePartitionedModelInput]
    ) -> Stage[bool]:
        for model, partition in model_and_parts:
            # We use functools.partial to "close over" all of the necessary inputs to
            # extract_and_load because a typical lambda is not pickleable. This partial function
            # _is_ pickleable and so can be sent to the subprocess worker to be called
            yield functools.partial(
                extract_and_load,
                partition=partition,
                cls=model,
                job_start=self.start_time,
                load_mode=self.load_mode,
                load_type=self.load_type,
                source=self.source,
                worker_client=self.worker_client,
            )
