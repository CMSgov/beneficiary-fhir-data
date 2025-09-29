import logging
import os
import sys
import time
from datetime import datetime

from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from hamilton_loader import PostgresLoader
from model import (
    IdrBeneficiary,
    IdrBeneficiaryDualEligibility,
    IdrBeneficiaryEntitlement,
    IdrBeneficiaryEntitlementReason,
    IdrBeneficiaryMbiId,
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
def idr_claim_institutional(config_connection_string: str,
                            config_mode: str,
                            config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimInstitutional, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_date_signature(config_connection_string: str,
                             config_mode: str,
                             config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimDateSignature, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_fiss(config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimFiss, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_item(config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimItem, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_line_institutional(config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimLineInstitutional, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_ansi_signature(config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimAnsiSignature, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_professional(config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimProfessional, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_claim_line_professional(config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaimLineProfessional, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

# Stage 2: Load main claim table
def idr_claim(config_connection_string: str,
              idr_claim_institutional: tuple[PostgresLoader, bool],
              idr_claim_date_signature: tuple[PostgresLoader, bool],
              idr_claim_fiss: tuple[PostgresLoader, bool],
              idr_claim_item: tuple[PostgresLoader, bool],
              idr_claim_line_institutional: tuple[PostgresLoader, bool],
              idr_claim_ansi_signature: tuple[PostgresLoader, bool],
              idr_claim_professional: tuple[PostgresLoader, bool],
              idr_claim_line_professional: tuple[PostgresLoader, bool],
              config_mode: str, config_batch_size: int
              ) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrClaim, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

# Stage 3: Load auxiliary beneficiary tables in parallel
def idr_beneficiary_status(idr_claim: tuple[PostgresLoader, bool], config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiaryStatus, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_beneficiary_third_party(idr_claim: tuple[PostgresLoader, bool], config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiaryThirdParty, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_beneficiary_entitlement(idr_claim: tuple[PostgresLoader, bool], config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiaryEntitlement, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_beneficiary_entitlement_reason(idr_claim: tuple[PostgresLoader, bool], config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiaryEntitlementReason, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_beneficiary_dual_eligibility(idr_claim: tuple[PostgresLoader, bool], config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiaryDualEligibility, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

def idr_beneficiary_mbi_id(idr_claim: tuple[PostgresLoader, bool], config_connection_string: str, config_mode: str, config_batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiaryMbiId, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)

# Stage 4: Load main beneficiary tables last
def idr_beneficiary(config_connection_string: str,
                    idr_beneficiary_status: tuple[PostgresLoader, bool],
                    idr_beneficiary_third_party: tuple[PostgresLoader, bool],
                    idr_beneficiary_entitlement: tuple[PostgresLoader, bool],
                    idr_beneficiary_entitlement_reason: tuple[PostgresLoader, bool],
                    idr_beneficiary_dual_eligibility: tuple[PostgresLoader, bool],
                    idr_beneficiary_mbi_id: tuple[PostgresLoader, bool],
                    config_mode: str,
                    config_batch_size: int
                    ) -> tuple[PostgresLoader, bool]:
    return extract_and_load(cls=IdrBeneficiary, connection_string=config_connection_string, mode=config_mode, batch_size=config_batch_size)
