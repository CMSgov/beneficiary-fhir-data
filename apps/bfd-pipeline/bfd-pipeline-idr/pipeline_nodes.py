# ruff: noqa: ARG001
# type: ignore [reportUntypedFunctionDecorator]

from datetime import datetime

from hamilton.function_modifiers import (
    config,
    parameterize,
    value,
)
from hamilton.htypes import Collect, Parallelizable

from model import (
    FetchQueryPartition,
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
)
from pipeline_utils import extract_and_load

type NodePartitionedModelInput = tuple[type[IdrBaseModel], FetchQueryPartition | None]
type PipelineInitialIdrClaims = NodePartitionedModelInput
type PipelineInitialIdrBeneficiaryOvershareMbi = NodePartitionedModelInput
type PipelineInitialIdrBeneficiaryAuxTable = NodePartitionedModelInput


def _gen_partitioned_node_inputs(
    model_types: list[type[IdrBaseModel]],
) -> list[NodePartitionedModelInput]:
    models_and_partitions = [(m, m.fetch_query_partitions()) for m in model_types]
    models_with_partitions: list[NodePartitionedModelInput] = [
        (m, p) for m, partitions in models_and_partitions if partitions for p in partitions
    ]
    models_without_partitions = [
        (m, None) for m, partitions in models_and_partitions if not partitions
    ]
    return models_with_partitions + models_without_partitions


# INITIAL FLOW
# Stage 1: Load ALL claim tables in parallel if load_type is set to initial
@config.when(load_type="initial")
def initial_idr_claims() -> Parallelizable[PipelineInitialIdrClaims]:
    yield from _gen_partitioned_node_inputs(
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
            IdrClaim,
        ]
    )


@config.when(load_type="initial")
def do_initial_idr_claims(
    initial_idr_claims: PipelineInitialIdrClaims,
    config_connection_string: str,
    config_mode: str,
    config_batch_size: int,
    start_time: datetime,
) -> bool:
    model_type, partition = initial_idr_claims
    return extract_and_load(
        cls=model_type,
        partition=partition,
        batch_start=start_time,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


@config.when(load_type="initial")
def collect_initial_idr_claims(
    do_initial_idr_claims: Collect[bool],
) -> bool:
    return all(do_initial_idr_claims)


@config.when(load_type="initial")
def idr_beneficiary_overshare_mbi(
    collect_initial_idr_claims: bool,
    config_connection_string: str,
    start_time: datetime,
    config_mode: str,
    config_batch_size: int,
) -> bool:
    return extract_and_load(
        cls=IdrBeneficiaryOvershareMbi,
        connection_string=config_connection_string,
        batch_start=start_time,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 3: Load auxiliary beneficiary tables in parallel
@config.when(load_type="initial")
def initial_idr_beneficiary_aux_table(
    idr_beneficiary_overshare_mbi: bool,
) -> Parallelizable[PipelineInitialIdrBeneficiaryAuxTable]:
    yield from _gen_partitioned_node_inputs(
        [
            IdrBeneficiaryStatus,
            IdrBeneficiaryThirdParty,
            IdrBeneficiaryEntitlement,
            IdrBeneficiaryEntitlementReason,
            IdrBeneficiaryDualEligibility,
            IdrBeneficiaryMbiId,
        ]
    )


@config.when(load_type="initial")
def do_initial_idr_beneficiary_aux_table(
    initial_idr_beneficiary_aux_table: PipelineInitialIdrBeneficiaryAuxTable,
    config_connection_string: str,
    start_time: datetime,
    config_mode: str,
    config_batch_size: int,
) -> bool:
    model_type, partition = initial_idr_beneficiary_aux_table
    return extract_and_load(
        cls=model_type,
        partition=partition,
        batch_start=start_time,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


@config.when(load_type="initial")
def collect_initial_idr_beneficiary_aux_table(
    do_initial_idr_beneficiary_aux_table: Collect[bool],
) -> bool:
    return all(do_initial_idr_beneficiary_aux_table)


# INCREMENTAL FLOW
# Stage 1: Load auxiliary claim tables in parallel
@config.when(load_type="incremental")
@parameterize(
    idr_claim_institutional=dict(cls=value(IdrClaimInstitutional)),
    idr_claim_date_signature=dict(cls=value(IdrClaimDateSignature)),
    idr_claim_fiss=dict(cls=value(IdrClaimFiss)),
    idr_claim_item=dict(cls=value(IdrClaimItem)),
    idr_claim_line_institutional=dict(cls=value(IdrClaimLineInstitutional)),
    idr_claim_ansi_signature=dict(cls=value(IdrClaimAnsiSignature)),
    idr_claim_professional=dict(cls=value(IdrClaimProfessional)),
    idr_claim_line_professional=dict(cls=value(IdrClaimLineProfessional)),
    idr_claim_line_rx=dict(cls=value(IdrClaimLineRx)),
)
def idr_claim_aux_table(
    cls: type, config_connection_string: str, config_mode: str, config_batch_size: int
) -> bool:
    return extract_and_load(
        cls=cls,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 2: Load main claim table separately after loading all aux claim tables
@config.when(load_type="incremental")
def idr_claim(
    idr_claim_institutional: bool,
    idr_claim_date_signature: bool,
    idr_claim_fiss: bool,
    idr_claim_item: bool,
    idr_claim_line_institutional: bool,
    idr_claim_ansi_signature: bool,
    idr_claim_professional: bool,
    idr_claim_line_professional: bool,
    idr_claim_line_rx: bool,
    config_mode: str,
    config_batch_size: int,
    config_connection_string: str,
) -> bool:
    return extract_and_load(
        cls=IdrClaim,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 3: Load only overshared MBIs after loading in claim table
@config.when(load_type="incremental")
def idr_beneficiary_overshare_mbi_incremental(
    idr_claim: bool,
    config_mode: str,
    config_batch_size: int,
    config_connection_string: str,
) -> bool:
    return extract_and_load(
        cls=IdrBeneficiaryOvershareMbi,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 4: Load auxiliary beneficiary tables in parallel
@config.when(load_type="incremental")
@parameterize(
    idr_beneficiary_status=dict(cls=value(IdrBeneficiaryStatus)),
    idr_beneficiary_third_party=dict(cls=value(IdrBeneficiaryThirdParty)),
    idr_beneficiary_entitlement=dict(cls=value(IdrBeneficiaryEntitlement)),
    idr_beneficiary_entitlement_reason=dict(cls=value(IdrBeneficiaryEntitlementReason)),
    idr_beneficiary_dual_eligibility=dict(cls=value(IdrBeneficiaryDualEligibility)),
    idr_beneficiary_mbi_id=dict(cls=value(IdrBeneficiaryMbiId)),
)
def idr_beneficiary_aux_table_incremental(
    idr_beneficiary_overshare_mbi_incremental: bool,
    cls: type,
    config_connection_string: str,
    config_mode: str,
    config_batch_size: int,
) -> bool:
    return extract_and_load(
        cls=cls,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Final Stage for EITHER flow: Load main beneficiary tables last
def idr_beneficiary(
    # idr_beneficiary_status: bool,
    # idr_beneficiary_third_party: bool,
    # idr_beneficiary_entitlement: bool,
    # idr_beneficiary_entitlement_reason: bool,
    # idr_beneficiary_dual_eligibility: bool,
    # idr_beneficiary_mbi_id: bool,
    collect_initial_idr_beneficiary_aux_table: bool,
    config_connection_string: str,
    config_mode: str,
    config_batch_size: int,
    start_time: datetime,
) -> bool:
    return extract_and_load(
        cls=IdrBeneficiary,
        connection_string=config_connection_string,
        batch_start=start_time,
        mode=config_mode,
        batch_size=config_batch_size,
    )
