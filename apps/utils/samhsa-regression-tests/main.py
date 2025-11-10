import asyncio
import itertools
import json
import logging
import os
import ssl
import sys
from dataclasses import dataclass
from datetime import date
from enum import StrEnum, auto
from pathlib import Path
from typing import Any

import aiohttp
import anyio
import asyncclick as click
import psycopg
import yaml
from aiohttp import ClientSession, TCPConnector
from psycopg import sql
from psycopg.rows import dict_row
from pydantic import BaseModel, ConfigDict, Field, TypeAdapter

LOCALHOST_CERTIFICATE_HEADER = "X-Amzn-Mtls-Clientcert"
CLM_UNIQ_ID_IDENTIFIER_SYSTEM = "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType"
CLM_UNIQ_ID_IDENTIFIER_CODE = "uc"
SECURITY_LABEL_CPT_SYSTEM = "http://www.ama-assn.org/go/cpt"
SECURITY_LABEL_HCPCS_SYSTEM = "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets"
SECURITY_LABEL_DRG_SYSTEM = "https://www.cms.gov/Medicare/Medicare-Fee-for-Service-Payment/AcuteInpatientPPS/MS-DRG-Classifications-and-Software"
SECURITY_LABEL_HL7_ICD10_DIAGNOSIS_SYSTEM = "http://hl7.org/fhir/sid/icd-10-cm"
SECURITY_LABEL_MEDICARE_ICD10_PROCEDURE_SYSTEM = "http://www.cms.gov/Medicare/Coding/ICD10"
SECURITY_LABEL_HL7_ICD9_DIAGNOSIS_SYSTEM = "http://hl7.org/fhir/sid/icd-9-cm"
SECURITY_LABEL_MEDICARE_ICD9_PROCEDURE_SYSTEM = "http://www.cms.gov/Medicare/Coding/ICD9"

logging.basicConfig(level=os.environ.get("LOG_LEVEL", "INFO").upper())
logger = logging.getLogger()


class VerifyFilteringResult(StrEnum):
    EMPTY_RESPONSE = auto()
    PASS = auto()
    FAIL = auto()


class ClaimItemSamhsaColumn(StrEnum):
    CLM_DGNS_CD = (
        auto(),
        [
            SECURITY_LABEL_HL7_ICD10_DIAGNOSIS_SYSTEM,
            SECURITY_LABEL_HL7_ICD9_DIAGNOSIS_SYSTEM,
        ],
    )
    CLM_PRCDR_CD = (
        auto(),
        [
            SECURITY_LABEL_MEDICARE_ICD10_PROCEDURE_SYSTEM,
            SECURITY_LABEL_MEDICARE_ICD9_PROCEDURE_SYSTEM,
        ],
    )
    CLM_LINE_HCPCS_CD = auto(), [SECURITY_LABEL_HCPCS_SYSTEM, SECURITY_LABEL_CPT_SYSTEM]

    def __init__(self, _: str, systems: list[str]) -> None:
        self.systems = systems

    def __new__(
        cls: type["ClaimItemSamhsaColumn"], value: str, systems: list[str]
    ) -> "ClaimItemSamhsaColumn":
        obj = str.__new__(cls, value)
        obj._value_ = value
        obj.systems = systems
        return obj


class ClaimInstitutionalSamhsaColumn(StrEnum):
    DGNS_DRG_CD = auto(), [SECURITY_LABEL_DRG_SYSTEM]

    def __init__(self, _: str, systems: list[str]) -> None:
        self.systems = systems

    def __new__(
        cls: type["ClaimInstitutionalSamhsaColumn"], value: str, systems: list[str]
    ) -> "ClaimInstitutionalSamhsaColumn":
        obj = str.__new__(cls, value)
        obj._value_ = value
        obj.systems = systems
        return obj


class SecurityLabelModel(BaseModel):
    model_config = ConfigDict(coerce_numbers_to_str=True)

    system: str
    code: str
    start_date: date = Field(validation_alias="startDate")
    end_date: date = Field(validation_alias="endDate")


class DatabaseDetailsModel(BaseModel):
    host: str
    user: str
    password: str
    port: int = 5432
    dbname: str = "fhirdb"

    @classmethod
    def from_conn_str(cls, conn_str: str) -> "DatabaseDetailsModel":
        return DatabaseDetailsModel.model_validate(
            dict(entry.split("=") for entry in conn_str.split(" "))
        )

    @classmethod
    def from_env(cls) -> "DatabaseDetailsModel":
        return DatabaseDetailsModel.model_validate(
            {
                "host": os.environ.get("PGHOST"),
                "user": os.environ.get("PGUSER"),
                "password": os.environ.get("PGPASSWORD"),
                "port": os.environ.get("PGPORT"),
                "dbname": os.environ.get("PGDATABASE"),
            }
        )


@dataclass(frozen=True, eq=True)
class BeneWithSamshaClaims:
    bene_sk: str
    samhsa_claim_ids: list[str]


def normalize_code(code: str) -> str:
    return code.replace(".", "")


async def __query_samhsa_claim_any_ids(
    table: str,
    column: str,
    query_params: list[Any],
    tablesample: float,
    limit: int,
    db_details: DatabaseDetailsModel,
    security_labels: list[SecurityLabelModel],
) -> list[int]:
    async with (
        await psycopg.AsyncConnection.connect(
            host=db_details.host,
            user=db_details.user,
            password=db_details.password,
            port=db_details.port,
            dbname=db_details.dbname,
        ) as conn,
        conn.cursor(row_factory=dict_row) as curs,
    ):
        logger.info(
            "Connected to %s; querying on column %s in %s...",
            db_details.dbname,
            column,
            table,
        )

        # This query looks a bit strange since it's doing a lot of JOINs, but this mimics the Claim
        # entity in the V3 Server as it joins on many tables to construct a full Claim for an
        # ExplanationOfBenefit response. In particular, if a "claim" row does not have one or more
        # corresponding "claim_item", "claim_date_signature", or "beneficiary" row(s), the V3 Server
        # will disregard the Claim as it is incomplete (likely still loading). This is why this
        # query INNER JOINs on those tables. "claim_institutional" is optional, so a LEFT JOIN
        # ensures we take it if it is there, but we do not exclude "claim" rows without it.
        result = await (
            await curs.execute(
                sql.SQL("""
                SELECT claim.clm_uniq_id, claim.clm_thru_dt, {table}.{column}
                FROM idr.claim
                TABLESAMPLE SYSTEM({tablesample})
                LEFT JOIN idr.claim_institutional
                    ON claim.clm_uniq_id = claim_institutional.clm_uniq_id
                INNER JOIN idr.claim_item
                    ON claim.clm_uniq_id = claim_item.clm_uniq_id
                INNER JOIN idr.claim_date_signature
                    ON claim.clm_dt_sgntr_sk = claim_date_signature.clm_dt_sgntr_sk
                INNER JOIN idr.beneficiary
                    ON claim.bene_sk = beneficiary.bene_sk
                WHERE {table}.{column} = ANY(%s)
                LIMIT {limit};
                """).format(
                    table=sql.Identifier(table),
                    column=sql.Identifier(column),
                    tablesample=sql.Literal(tablesample),
                    limit=sql.Literal(limit),
                ),
                query_params,
            )
        ).fetchall()
        valid_samhsa_claim_ids = [
            int(row["clm_uniq_id"])
            for row in result
            if (
                matching_label := next(
                    x
                    for x in security_labels
                    if normalize_code(x.code) == normalize_code(str(row[column]))
                )
            )
            and row["clm_thru_dt"] >= matching_label.start_date
            and row["clm_thru_dt"] <= matching_label.end_date
        ]
        logger.info(
            (
                "%d/%d valid (claim date in bounds) SAMHSA clm_uniq_ids under column %s in table "
                "%s fetched"
            ),
            len(valid_samhsa_claim_ids),
            len(result),
            column,
            table,
        )

        return valid_samhsa_claim_ids


async def query_samhsa_claim_institutional_ids(
    security_labels: list[SecurityLabelModel],
    column: ClaimInstitutionalSamhsaColumn,
    tablesample: float,
    limit: int,
    db_details: DatabaseDetailsModel,
) -> list[int]:
    return await __query_samhsa_claim_any_ids(
        table="claim_institutional",
        column=column,
        tablesample=tablesample,
        limit=limit,
        query_params=[
            [
                int(normalized_code)
                for label in security_labels
                if label.system in column.systems
                and (normalized_code := normalize_code(label.code))
                and normalized_code.isdigit()
            ]
        ],
        db_details=db_details,
        security_labels=security_labels,
    )


async def query_samhsa_claim_item_ids(
    security_labels: list[SecurityLabelModel],
    column: ClaimItemSamhsaColumn,
    tablesample: float,
    limit: int,
    db_details: DatabaseDetailsModel,
) -> list[int]:
    return await __query_samhsa_claim_any_ids(
        table="claim_item",
        column=column,
        tablesample=tablesample,
        limit=limit,
        query_params=[
            list(
                itertools.chain.from_iterable(
                    [label.code, normalize_code(label.code)]
                    for label in security_labels
                    if label.system in column.systems
                )
            )
        ],
        db_details=db_details,
        security_labels=security_labels,
    )


async def query_samhsa_benes_with_claims(
    tablesample: float,
    limit: int,
    security_labels: list[SecurityLabelModel],
    db_details: DatabaseDetailsModel,
) -> list[BeneWithSamshaClaims]:
    clm_uniq_ids = set(
        itertools.chain.from_iterable(
            await asyncio.gather(
                *(
                    query_samhsa_claim_item_ids(
                        security_labels=security_labels,
                        tablesample=tablesample,
                        limit=limit,
                        column=claim_item_col,
                        db_details=db_details,
                    )
                    for claim_item_col in ClaimItemSamhsaColumn
                ),
                query_samhsa_claim_institutional_ids(
                    security_labels=security_labels,
                    tablesample=tablesample,
                    limit=limit,
                    column=ClaimInstitutionalSamhsaColumn.DGNS_DRG_CD,
                    db_details=db_details,
                ),
            )
        )
    )
    logger.info("Collected %d unique clm_uniq_ids", len(clm_uniq_ids))

    async with (
        await psycopg.AsyncConnection.connect(
            host=db_details.host,
            user=db_details.user,
            password=db_details.password,
            port=db_details.port,
            dbname=db_details.dbname,
        ) as conn,
        conn.cursor(row_factory=dict_row) as curs,
    ):
        logger.info("Connected to %s; querying for bene_sks...", db_details.dbname)
        result = await (
            await curs.execute(
                """
            SELECT bene_sk, clm_uniq_id from idr.claim
            WHERE clm_uniq_id = ANY(%s);
            """,
                [list(clm_uniq_ids)],
            )
        ).fetchall()
        logger.info("%d potential SAMHSA bene_sks returned", len(result))

        bene_sks_and_clms = [(str(row["bene_sk"]), str(row["clm_uniq_id"])) for row in result]
        uniq_bene_sks = set(x[0] for x in bene_sks_and_clms)

        return [
            BeneWithSamshaClaims(
                bene_sk=bene_sk,
                samhsa_claim_ids=[x[1] for x in bene_sks_and_clms if bene_sk == x[0]],
            )
            for bene_sk in uniq_bene_sks
        ]


def get_uniq_clm_id_for_bundle_resource(bundle_resource: dict[str, Any]) -> str:
    # If we can't find the uniq_clm_id, something's wrong and a StopIteration _should_ be raised.
    return next(
        identifier["value"]
        for identifier in bundle_resource["identifier"]
        if "type" in identifier
        and "coding" in identifier["type"]
        and any(
            coding["system"] == CLM_UNIQ_ID_IDENTIFIER_SYSTEM
            and coding["code"] == CLM_UNIQ_ID_IDENTIFIER_CODE
            for coding in identifier["type"]["coding"]
        )
    )


async def verify_samhsa_filtering(
    url: str,
    samhsa_session: ClientSession,
    no_samhsa_session: ClientSession,
    samhsa_bene: BeneWithSamshaClaims,
) -> VerifyFilteringResult:
    patient_query = {"patient": samhsa_bene.bene_sk}
    try:
        async with (
            samhsa_session.get(url=url, params=patient_query) as samhsa_response,
            no_samhsa_session.get(url=url, params=patient_query) as no_samhsa_response,
        ):
            samhsa_bundle = json.loads(await samhsa_response.read())
            no_samhsa_bundle = json.loads(await no_samhsa_response.read())

            samhsa_bundle_entries: list[dict[str, Any]] = samhsa_bundle.get("entry", [])
            no_samhsa_bundle_entries: list[dict[str, Any]] = no_samhsa_bundle.get("entry", [])
            samhsa_entry_size = len(samhsa_bundle_entries)
            no_samhsa_entry_size = len(no_samhsa_bundle_entries)
            logger.debug(
                "Bene SK: %s, Non-SAMHSA bundle 'entry' size: %d, SAMHSA-bundle 'entry' size: %d",
                samhsa_bene.bene_sk,
                no_samhsa_entry_size,
                samhsa_entry_size,
            )

            if samhsa_entry_size == 0:
                logger.warning("Response bundle for %s was empty", samhsa_bene.bene_sk)
                return VerifyFilteringResult.EMPTY_RESPONSE

            all_samhsa_allowed_clm_ids = [
                get_uniq_clm_id_for_bundle_resource(entry["resource"])
                for entry in samhsa_bundle_entries
            ]
            all_samhsa_filtered_clm_ids = [
                get_uniq_clm_id_for_bundle_resource(entry["resource"])
                for entry in no_samhsa_bundle_entries
            ]
            bene_samhsa_claims_set = set(samhsa_bene.samhsa_claim_ids)
            # We should expect the intersection of the set of all claims on the SAMHSA unauthorized
            # response to have _zero_ SAMHSA claim IDs on it.
            samhsa_claims_filtered_when_not_authorized = (
                len(set(all_samhsa_filtered_clm_ids).intersection(bene_samhsa_claims_set)) == 0
            )
            # We should expect the intersection of the set of all claims on the SAMHSA _authorized_
            # response to be exactly the set of SAMHSA claims retrieved from the database, as we
            # expect that no SAMHSA claims are filtered
            samhsa_claims_unfiltered_when_authorized = (
                set(all_samhsa_allowed_clm_ids).intersection(bene_samhsa_claims_set)
                == bene_samhsa_claims_set
            )
            final_result = (
                VerifyFilteringResult.PASS
                if samhsa_claims_filtered_when_not_authorized
                and samhsa_claims_unfiltered_when_authorized
                else VerifyFilteringResult.FAIL
            )
            logger.log(
                logging.INFO if final_result == VerifyFilteringResult.PASS else logging.ERROR,
                (
                    "Bene SK: %s, SAMHSA claims excluded for non-SAMHSA cert: %s, SAMHSA claims "
                    "included for SAMHSA cert: %s, final result: %s"
                ),
                samhsa_bene.bene_sk,
                samhsa_claims_filtered_when_not_authorized,
                samhsa_claims_unfiltered_when_authorized,
                final_result,
            )

            if final_result == VerifyFilteringResult.FAIL:
                logger.debug(
                    (
                        "Bene SK: %s, SAMHSA claim IDs: %s, authorized response claim IDs: %s, "
                        "unauthorized response claim IDs: %s"
                    ),
                    samhsa_bene.bene_sk,
                    samhsa_bene.samhsa_claim_ids,
                    all_samhsa_allowed_clm_ids,
                    all_samhsa_filtered_clm_ids,
                )

            return final_result
    except Exception:
        logger.exception("Failed to query for %s", samhsa_bene.bene_sk)
        return VerifyFilteringResult.EMPTY_RESPONSE


@click.command()
@click.option(
    "-h",
    "--hostname",
    envvar="HOSTNAME",
    type=str,
    required=True,
    help="The hostname with optional port of the V3 server to send requests to",
)
@click.option(
    "-s",
    "--security-labels",
    envvar="SECURITY_LABELS",
    type=Path,
    required=True,
    help="Path to the security labels YML file",
)
@click.option(
    "-c",
    "--samhsa-cert",
    envvar="SAMHSA_CERT",
    type=Path,
    required=True,
    help="Path to the PEM-encoded certificate authorized to retrieve SAMHSA data",
)
@click.option(
    "-k",
    "--samhsa-cert-key",
    envvar="SAMHSA_CERT_KEY",
    type=Path,
    required=True,
    help="Path to the private key of the certificate authorized to retrieve SAMHSA data",
)
@click.option(
    "-C",
    "--no-samhsa-cert",
    envvar="NO_SAMHSA_CERT",
    type=Path,
    required=True,
    help="Path to the PEM-encoded certificate NOT authorized to retrieve SAMHSA data",
)
@click.option(
    "-K",
    "--no-samhsa-cert-key",
    envvar="NO_SAMHSA_CERT_KEY",
    type=Path,
    required=True,
    help="Path to the private key of the certificate NOT authorized to retrieve SAMHSA data",
)
@click.option(
    "-H",
    "--host-cert",
    envvar="HOST_CERT",
    type=Path,
    required=False,
    help=(
        "Path to the PEM-encoded certificate to verify the V3 Server with; if empty, no "
        "verification will be done"
    ),
)
@click.option(
    "-d",
    "--db-conn-str",
    envvar="DB_CONN_STR",
    type=str,
    required=False,
    help=(
        "Database connection string in key-value string form; if empty, details will be taken from "
        "typical 'PG...' environment variables"
    ),
)
@click.option(
    "-t",
    "--tablesample",
    envvar="TABLESAMPLE",
    type=float,
    default=10,
    help=(
        "Tamplesample percentage from 0-100 of which claim_item and claim_institutional rows will "
        "be sampled; defaults to 10%"
    ),
)
@click.option(
    "-l",
    "--limit",
    envvar="LIMIT",
    type=int,
    default=300,
    help=(
        "Limit of unique claim IDs (not necessarily beneficiaries) to return from queries on"
        "claim_item and claim_institutional; defaults to 300 rows"
    ),
)
async def main(
    hostname: str,
    security_labels: Path,
    samhsa_cert: Path,
    samhsa_cert_key: Path,
    no_samhsa_cert: Path,
    no_samhsa_cert_key: Path,
    host_cert: Path | None,
    db_conn_str: str | None,
    tablesample: int,
    limit: int,
) -> bool:
    samhsa_labels = TypeAdapter(list[SecurityLabelModel]).validate_python(
        yaml.safe_load(security_labels.read_text()),
        by_alias=True,
    )

    db_details = (
        DatabaseDetailsModel.from_conn_str(db_conn_str)
        if db_conn_str
        else DatabaseDetailsModel.from_env()
    )

    samhsa_benes = await query_samhsa_benes_with_claims(
        security_labels=samhsa_labels,
        db_details=db_details,
        tablesample=tablesample,
        limit=limit,
    )

    # We check for localhost or 127.0.0.1 (the most common local addresses) to determine if this is
    # a local test. If it is, we don't supply real certificate values or worry about loading
    # certificates at all. We just provide the dummy certificates directly via the header. We use
    # any() instead of a single "in" expression to allow for hostnames like "localhost:8080" and
    # so-on
    is_local = any(x in hostname for x in ["localhost", "127.0.0.1"])

    base_url = f"https://{hostname}" if not is_local else f"http://{hostname}"
    full_eob_url = f"{base_url.lstrip('/')}/v3/fhir/ExplanationOfBenefit"

    samhsa_ssl_ctx = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH)
    no_samhsa_ssl_ctx = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH)
    if not is_local:
        samhsa_ssl_ctx.load_cert_chain(
            certfile=samhsa_cert.absolute(),
            keyfile=samhsa_cert_key.absolute(),
        )
        no_samhsa_ssl_ctx.load_cert_chain(
            certfile=no_samhsa_cert.absolute(),
            keyfile=no_samhsa_cert_key.absolute(),
        )

        if host_cert:
            samhsa_ssl_ctx.load_verify_locations(cafile=host_cert.absolute())
            no_samhsa_ssl_ctx.load_verify_locations(cafile=host_cert.absolute())
        else:
            samhsa_ssl_ctx.check_hostname = False
            samhsa_ssl_ctx.verify_mode = ssl.CERT_NONE
            no_samhsa_ssl_ctx.check_hostname = False
            no_samhsa_ssl_ctx.verify_mode = ssl.CERT_NONE

    async with (
        aiohttp.ClientSession(
            connector=TCPConnector(ssl=samhsa_ssl_ctx) if not is_local else None,
            headers={LOCALHOST_CERTIFICATE_HEADER: "samhsa_allowed"} if is_local else None,
        ) as samhsa_session,
        aiohttp.ClientSession(
            connector=TCPConnector(ssl=no_samhsa_ssl_ctx) if not is_local else None,
            headers={LOCALHOST_CERTIFICATE_HEADER: "samhsa_not_allowed"} if is_local else None,
        ) as no_samhsa_session,
    ):
        results = await asyncio.gather(
            *(
                verify_samhsa_filtering(
                    url=full_eob_url,
                    samhsa_session=samhsa_session,
                    no_samhsa_session=no_samhsa_session,
                    samhsa_bene=samhsa_bene,
                )
                for samhsa_bene in samhsa_benes
            )
        )

        all_samhsa_filtered = all(
            res == VerifyFilteringResult.PASS
            for res in results
            if res != VerifyFilteringResult.EMPTY_RESPONSE
        )

        logger.info(
            "Passing responses count: %d",
            len([res for res in results if res == VerifyFilteringResult.PASS]),
        )
        logger.info(
            "Failing responses count: %d",
            len([res for res in results if res == VerifyFilteringResult.FAIL]),
        )
        logger.info(
            "Empty responses count: %d",
            len([res for res in results if res == VerifyFilteringResult.EMPTY_RESPONSE]),
        )
        logger.log(
            logging.INFO if all_samhsa_filtered else logging.ERROR,
            "Filtering validation test of %d non-empty SAMHSA EoBs: %s",
            len(
                [
                    res
                    for res in results
                    if res
                    in [
                        VerifyFilteringResult.PASS,
                        VerifyFilteringResult.FAIL,
                    ]
                ]
            ),
            "PASSED" if all_samhsa_filtered else "FAILED",
        )

        return all_samhsa_filtered


if __name__ == "__main__":
    if not anyio.run(main.main):
        sys.exit(1)
