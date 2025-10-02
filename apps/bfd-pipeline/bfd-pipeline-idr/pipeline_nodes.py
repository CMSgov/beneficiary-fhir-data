# ruff: noqa: ARG001
# type: ignore [reportUntypedFunctionDecorator]
from hamilton.function_modifiers import parameterize, value

from hamilton_loader import PostgresLoader
from model import (
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
    IdrClaimProfessional,
)
from pipeline_utils import extract_and_load


# Stage 1: Load auxiliary claim tables in parallel
@parameterize(
    idr_claim_institutional=dict(cls=value(IdrClaimInstitutional)),
    idr_claim_date_signature=dict(cls=value(IdrClaimDateSignature)),
    idr_claim_fiss=dict(cls=value(IdrClaimFiss)),
    idr_claim_item=dict(cls=value(IdrClaimItem)),
    idr_claim_line_institutional=dict(cls=value(IdrClaimLineInstitutional)),
    idr_claim_ansi_signature=dict(cls=value(IdrClaimAnsiSignature)),
    idr_claim_professional=dict(cls=value(IdrClaimProfessional)),
    idr_claim_line_professional=dict(cls=value(IdrClaimLineProfessional)),
)
def idr_claim_aux_table(
    cls: type, config_connection_string: str, config_mode: str, config_batch_size: int
) -> tuple[PostgresLoader, bool]:
    return extract_and_load(
        cls=cls,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 2: Load main claim table
def idr_claim(
    idr_claim_institutional: tuple[PostgresLoader, bool],
    idr_claim_date_signature: tuple[PostgresLoader, bool],
    idr_claim_fiss: tuple[PostgresLoader, bool],
    idr_claim_item: tuple[PostgresLoader, bool],
    idr_claim_line_institutional: tuple[PostgresLoader, bool],
    idr_claim_ansi_signature: tuple[PostgresLoader, bool],
    idr_claim_professional: tuple[PostgresLoader, bool],
    idr_claim_line_professional: tuple[PostgresLoader, bool],
    config_mode: str,
    config_batch_size: int,
    config_connection_string: str,
) -> tuple[PostgresLoader, bool]:
    return extract_and_load(
        cls=IdrClaim,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 3: Load only overshared MBIs
def idr_beneficiary_overshare_mbi(
    idr_claim: tuple[PostgresLoader, bool],
    config_mode: str,
    config_batch_size: int,
    config_connection_string: str,
) -> tuple[PostgresLoader, bool]:
    return extract_and_load(
        cls=IdrBeneficiaryOvershareMbi,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 4: Load auxiliary beneficiary tables in parallel
@parameterize(
    idr_beneficiary_status=dict(cls=value(IdrBeneficiaryStatus)),
    idr_beneficiary_third_party=dict(cls=value(IdrBeneficiaryThirdParty)),
    idr_beneficiary_entitlement=dict(cls=value(IdrBeneficiaryEntitlement)),
    idr_beneficiary_entitlement_reason=dict(cls=value(IdrBeneficiaryEntitlementReason)),
    idr_beneficiary_dual_eligibility=dict(cls=value(IdrBeneficiaryDualEligibility)),
    idr_beneficiary_mbi_id=dict(cls=value(IdrBeneficiaryMbiId)),
)
def idr_beneficiary_aux_table(
    idr_beneficiary_overshare_mbi: tuple[PostgresLoader, bool],
    cls: type,
    config_connection_string: str,
    config_mode: str,
    config_batch_size: int,
) -> tuple[PostgresLoader, bool]:
    return extract_and_load(
        cls=cls,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 5: Load main beneficiary tables last
def idr_beneficiary(
    idr_beneficiary_status: tuple[PostgresLoader, bool],
    idr_beneficiary_third_party: tuple[PostgresLoader, bool],
    idr_beneficiary_entitlement: tuple[PostgresLoader, bool],
    idr_beneficiary_entitlement_reason: tuple[PostgresLoader, bool],
    idr_beneficiary_dual_eligibility: tuple[PostgresLoader, bool],
    idr_beneficiary_mbi_id: tuple[PostgresLoader, bool],
    config_connection_string: str,
    config_mode: str,
    config_batch_size: int,
) -> tuple[PostgresLoader, bool]:
    return extract_and_load(
        cls=IdrBeneficiary,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )
