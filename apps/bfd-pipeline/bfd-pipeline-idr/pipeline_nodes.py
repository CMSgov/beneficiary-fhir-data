# ruff: noqa: ARG001
# type: ignore [reportUntypedFunctionDecorator]
from hamilton.function_modifiers import config, parameterize, value

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


# INITIAL FLOW
# Stage 1: Load ALL claim tables in parallel if load_type is set to initial
@config.when(load_type="initial")
@parameterize(
    idr_claim_institutional=dict(cls=value(IdrClaimInstitutional)),
    idr_claim_date_signature=dict(cls=value(IdrClaimDateSignature)),
    idr_claim_fiss=dict(cls=value(IdrClaimFiss)),
    idr_claim_item=dict(cls=value(IdrClaimItem)),
    idr_claim_line_institutional=dict(cls=value(IdrClaimLineInstitutional)),
    idr_claim_ansi_signature=dict(cls=value(IdrClaimAnsiSignature)),
    idr_claim_professional=dict(cls=value(IdrClaimProfessional)),
    idr_claim_line_professional=dict(cls=value(IdrClaimLineProfessional)),
    idr_claim_initial=dict(cls=value(IdrClaim)),
)
def idr_claims(
    cls: type, config_connection_string: str, config_mode: str, config_batch_size: int
) -> bool:
    return extract_and_load(
        cls=cls,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )


# Stage 2: Load only overshared MBIs after loading in ALL claim tables
@config.when(load_type="initial")
def idr_beneficiary_overshare_mbi_initial(
    idr_claim_institutional: bool,
    idr_claim_date_signature: bool,
    idr_claim_fiss: bool,
    idr_claim_item: bool,
    idr_claim_line_institutional: bool,
    idr_claim_ansi_signature: bool,
    idr_claim_professional: bool,
    idr_claim_line_professional: bool,
    idr_claim_initial: bool,
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


# Stage 3: Load auxiliary beneficiary tables in parallel
@config.when(load_type="initial")
@parameterize(
    idr_beneficiary_status=dict(cls=value(IdrBeneficiaryStatus)),
    idr_beneficiary_third_party=dict(cls=value(IdrBeneficiaryThirdParty)),
    idr_beneficiary_entitlement=dict(cls=value(IdrBeneficiaryEntitlement)),
    idr_beneficiary_entitlement_reason=dict(cls=value(IdrBeneficiaryEntitlementReason)),
    idr_beneficiary_dual_eligibility=dict(cls=value(IdrBeneficiaryDualEligibility)),
    idr_beneficiary_mbi_id=dict(cls=value(IdrBeneficiaryMbiId)),
)
def idr_beneficiary_aux_table_initial(
    idr_beneficiary_overshare_mbi_initial: bool,
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
    idr_beneficiary_status: bool,
    idr_beneficiary_third_party: bool,
    idr_beneficiary_entitlement: bool,
    idr_beneficiary_entitlement_reason: bool,
    idr_beneficiary_dual_eligibility: bool,
    idr_beneficiary_mbi_id: bool,
    config_connection_string: str,
    config_mode: str,
    config_batch_size: int,
) -> bool:
    return extract_and_load(
        cls=IdrBeneficiary,
        connection_string=config_connection_string,
        mode=config_mode,
        batch_size=config_batch_size,
    )
