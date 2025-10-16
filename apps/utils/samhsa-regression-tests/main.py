import asyncio
import itertools
import json
import logging
import os
import ssl
import sys
from dataclasses import dataclass
from datetime import datetime
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

logging.basicConfig(level=os.environ.get("LOG_LEVEL", "INFO").upper())
logger = logging.getLogger()


class SamhsaFilterResult(StrEnum):
    EMPTY_RESPONSE = auto()
    FILTERED_RESPONSE = auto()
    UNFILTERED_RESPONSE = auto()


class ClaimItemSamhsaColumn(StrEnum):
    CLM_DGNS_CD = auto()
    CLM_PRCDR_CD = auto()
    CLM_LINE_HCPCS_CD = auto()


class ClaimInstitutionalSamhsaColumn(StrEnum):
    DGNS_DRG_CD = auto()


class SecurityLabelModel(BaseModel):
    model_config = ConfigDict(coerce_numbers_to_str=True)

    system: str
    code: str
    start_date: datetime = Field(validation_alias="startDate")
    end_date: datetime = Field(validation_alias="endDate")


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
        return DatabaseDetailsModel.model_validate({
            "host": os.environ.get("PGHOST"),
            "user": os.environ.get("PGUSER"),
            "password": os.environ.get("PGPASSWORD"),
            "port": os.environ.get("PGPORT"),
            "dbname": os.environ.get("PGDATABASE"),
        })


@dataclass(frozen=True, eq=True)
class BeneWithSamshaClaims:
    bene_sk: str
    samhsa_claim_ids: list[str]


async def __query_samhsa_claim_any_ids(
    table: str,
    column: str,
    query_params: list[Any],
    tablesample: int,
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

        result = await (
            await curs.execute(
                sql.SQL("""
                SELECT {table}.clm_uniq_id, claim.clm_thru_dt, {table}.{column} from idr.{table}
                TABLESAMPLE SYSTEM({tablesample})
                LEFT JOIN idr.claim ON claim.clm_uniq_id = {table}.clm_uniq_id
                WHERE {column} = ANY(%s)
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
            if (matching_label := next(x for x in security_labels if x.code == str(row[column])))
            and (claim_datetime := datetime.combine(row["clm_thru_dt"], datetime.min.time()))
            and claim_datetime >= matching_label.start_date
            and claim_datetime <= matching_label.end_date
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
    tablesample: int,
    limit: int,
    db_details: DatabaseDetailsModel,
) -> list[int]:
    return await __query_samhsa_claim_any_ids(
        table="claim_institutional",
        column=column,
        tablesample=tablesample,
        limit=limit,
        query_params=[[int(label.code) for label in security_labels if label.code.isdigit()]],
        db_details=db_details,
        security_labels=security_labels,
    )


async def query_samhsa_claim_item_ids(
    security_labels: list[SecurityLabelModel],
    column: ClaimItemSamhsaColumn,
    tablesample: int,
    limit: int,
    db_details: DatabaseDetailsModel,
) -> list[int]:
    return await __query_samhsa_claim_any_ids(
        table="claim_item",
        column=column,
        tablesample=tablesample,
        limit=limit,
        query_params=[[label.code for label in security_labels]],
        db_details=db_details,
        security_labels=security_labels,
    )


async def query_samhsa_benes_with_claims(
    tablesample: int,
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


async def verify_samhsa_filtered(
    url: str, samhsa_session: ClientSession, no_samhsa_session: ClientSession, bene_sk: str
) -> SamhsaFilterResult:
    patient_query = {"patient": bene_sk}
    try:
        async with (
            samhsa_session.get(url=url, params=patient_query) as samhsa_response,
            no_samhsa_session.get(url=url, params=patient_query) as no_samhsa_response,
        ):
            samhsa_bundle = json.loads(await samhsa_response.read())
            no_samhsa_bundle = json.loads(await no_samhsa_response.read())

            samhsa_entry_size = len(samhsa_bundle.get("entry", []))
            no_samhsa_entry_size = len(no_samhsa_bundle.get("entry", []))
            logger.log(
                logging.DEBUG,
                "Bene SK: %s, Non-SAMHSA bundle 'entry' size: %d, SAMHSA-bundle 'entry' size: %d",
                bene_sk,
                no_samhsa_entry_size,
                samhsa_entry_size,
            )

            if samhsa_entry_size == 0:
                return SamhsaFilterResult.EMPTY_RESPONSE

            return (
                SamhsaFilterResult.FILTERED_RESPONSE
                if no_samhsa_entry_size < samhsa_entry_size
                else SamhsaFilterResult.UNFILTERED_RESPONSE
            )
    except Exception:
        logger.exception("Unable to get url %s")

    return SamhsaFilterResult.EMPTY_RESPONSE


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
    type=int,
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
        security_labels=samhsa_labels, db_details=db_details, tablesample=tablesample, limit=limit
    )

    full_eob_url = f"https://{hostname}/v3/fhir/ExplanationOfBenefit"
    samhsa_ssl_ctx = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH)
    samhsa_ssl_ctx.load_cert_chain(
        certfile=samhsa_cert.absolute(),
        keyfile=samhsa_cert_key.absolute(),
    )
    no_samhsa_ssl_ctx = ssl.create_default_context(purpose=ssl.Purpose.SERVER_AUTH)
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
        aiohttp.ClientSession(connector=TCPConnector(ssl=samhsa_ssl_ctx)) as samhsa_session,
        aiohttp.ClientSession(connector=TCPConnector(ssl=no_samhsa_ssl_ctx)) as no_samhsa_session,
    ):
        results = await asyncio.gather(
            *(
                verify_samhsa_filtered(
                    url=full_eob_url,
                    samhsa_session=samhsa_session,
                    no_samhsa_session=no_samhsa_session,
                    bene_sk=samhsa_bene.bene_sk,
                )
                for samhsa_bene in samhsa_benes
            )
        )

        all_samhsa_filtered = all(
            res == SamhsaFilterResult.FILTERED_RESPONSE
            for res in results
            if res != SamhsaFilterResult.EMPTY_RESPONSE
        )

        logger.log(
            logging.INFO,
            "Filtered responses count: %d",
            len([res for res in results if res == SamhsaFilterResult.FILTERED_RESPONSE]),
        )
        logger.log(
            logging.INFO,
            "Unfiltered responses count: %d",
            len([res for res in results if res == SamhsaFilterResult.UNFILTERED_RESPONSE]),
        )
        logger.log(
            logging.INFO,
            "Empty responses count: %d",
            len([res for res in results if res == SamhsaFilterResult.EMPTY_RESPONSE]),
        )
        logger.log(
            logging.INFO,
            "Filtering result of %d non-empty SAMHSA EoBs: %s",
            len([
                res
                for res in results
                if res
                in [SamhsaFilterResult.FILTERED_RESPONSE, SamhsaFilterResult.UNFILTERED_RESPONSE]
            ]),
            "SUCCEEDED" if all_samhsa_filtered else "FAILED",
        )

        return all_samhsa_filtered


if __name__ == "__main__":
    if not anyio.run(main.main):
        sys.exit(1)
