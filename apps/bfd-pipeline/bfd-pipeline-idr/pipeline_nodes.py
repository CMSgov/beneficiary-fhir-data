import logging
import os
import sys
import time
from datetime import datetime

from snowflake.connector.network import ReauthenticationRequest, RetryRequest

from loader import PostgresLoader, get_connection_string
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

def batch_size() -> int:
    config_batch_size = int(os.environ.get("IDR_BATCH_SIZE", "100_000"))
    return config_batch_size

def mode() -> str:
    config_mode = sys.argv[1] if len(sys.argv) > 1 else ""
    return config_mode

def connection_string(mode: str) -> str:

    if mode == "local":
        config_connection_string = "host=localhost dbname=idr user=bfd password=InsecureLocalDev"
    elif mode == "synthetic":
        config_connection_string = get_connection_string()
    else:
        config_connection_string = get_connection_string()

    return config_connection_string

# Stage 1: Load auxiliary claim tables in parallel
def idr_claim_institutional(connection_string: str,
                            mode: str,
                            batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimInstitutional, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_date_signature(connection_string: str,
                             mode: str,
                             batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimDateSignature, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_fiss(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimFiss, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_item(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimItem, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_line_institutional(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimLineInstitutional, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_ansi_signature(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimAnsiSignature, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_professional(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimProfessional, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_claim_line_professional(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaimLineProfessional, connection_string=connection_string, mode=mode, batch_size=batch_size)

# Stage 2: Load main claim table
def idr_claim(connection_string: str,
              idr_claim_institutional: tuple[PostgresLoader, bool],
              idr_claim_date_signature: tuple[PostgresLoader, bool],
              idr_claim_fiss: tuple[PostgresLoader, bool],
              idr_claim_item: tuple[PostgresLoader, bool],
              idr_claim_line_institutional: tuple[PostgresLoader, bool],
              idr_claim_ansi_signature: tuple[PostgresLoader, bool],
              idr_claim_professional: tuple[PostgresLoader, bool],
              idr_claim_line_professional: tuple[PostgresLoader, bool],
              mode: str, batch_size: int
              ) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrClaim, connection_string=connection_string, mode=mode, batch_size=batch_size)

# Stage 3: Load auxiliary beneficiary tables in parallel
def idr_beneficiary_status(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiaryStatus, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_beneficiary_third_party(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiaryThirdParty, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_beneficiary_entitlement(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiaryEntitlement, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_beneficiary_entitlement_reason(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiaryEntitlementReason, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_beneficiary_dual_eligibility(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiaryDualEligibility, connection_string=connection_string, mode=mode, batch_size=batch_size)

def idr_beneficiary_mbi_id(connection_string: str, mode: str, batch_size: int) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiaryMbiId, connection_string=connection_string, mode=mode, batch_size=batch_size)

# Stage 4: Load main beneficiary tables last
def idr_beneficiary(connection_string: str,
                    idr_beneficiary_status: tuple[PostgresLoader, bool],
                    idr_beneficiary_third_party: tuple[PostgresLoader, bool],
                    idr_beneficiary_entitlement: tuple[PostgresLoader, bool],
                    idr_beneficiary_entitlement_reason: tuple[PostgresLoader, bool],
                    idr_beneficiary_dual_eligibility: tuple[PostgresLoader, bool],
                    idr_beneficiary_mbi_id: tuple[PostgresLoader, bool],
                    mode: str,
                    batch_size: int
                    ) -> tuple[PostgresLoader, bool]:
    return extract_and_load(IdrBeneficiary, connection_string=connection_string, mode=mode, batch_size=batch_size)
