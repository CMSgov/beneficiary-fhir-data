# type: ignore [reportUnknownMemberType]
import logging
import os
import sys

import ray
from hamilton import base, driver
from hamilton.plugins.h_ray import RayGraphAdapter

import pipeline_nodes
from loader import get_connection_string

console_handler = logging.StreamHandler()
formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
console_handler.setFormatter(formatter)
logging.basicConfig(level=logging.INFO, handlers=[console_handler])

logger = logging.getLogger(__name__)


def parse_bool(var: str) -> bool:
    # bool(str) interprets anything non-empty as true so we gotta do it manually
    return var.lower() == "true" or var == "1"


def main() -> None:
    logger.info("load start")

    parallelism = int(os.environ.get("PARALLELISM", "6"))
    ray.init(logging_level="info", num_cpus=parallelism)

    dict_builder = base.DictResult()
    adapter = RayGraphAdapter(result_builder=dict_builder)
    load_type = str(os.environ.get("LOAD_TYPE", "incremental"))
    logger.info(f"load_type: {load_type}")
    dr = (
        driver.Builder()
        .with_config({"load_type": load_type})
        .with_modules(pipeline_nodes)
        .with_adapters(adapter)
        .build()
    )

    batch_size = int(os.environ.get("IDR_BATCH_SIZE", "100_000"))
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "local":
        connection_string = "host=localhost dbname=fhirdb user=bfd password=InsecureLocalDev"
    elif mode == "synthetic":
        connection_string = get_connection_string()
    else:
        connection_string = get_connection_string()

    # temporary flags to load a subset of data for testing
    load_benes = parse_bool(os.environ.get("IDR_LOAD_BENES", "true"))
    load_claims = parse_bool(os.environ.get("IDR_LOAD_CLAIMS", "true"))

    logger.info(f"load_benes: {load_benes}")
    logger.info(f"load_claims: {load_claims}")

    if load_benes and load_claims:
        dr.execute(
            final_vars=["idr_beneficiary"],
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
            },
        )
    elif load_benes:
        # Since the DAG contains the dependency ordering, we need to override all claims nodes
        # in order to skip them and load only beneficiary data
        overrides = {
            "idr_claim": None,
            "idr_claim_initial": None,
            "idr_claim_institutional": None,
            "idr_claim_date_signature": None,
            "idr_claim_fiss": None,
            "idr_claim_item": None,
            "idr_claim_line_institutional": None,
            "idr_claim_ansi_signature": None,
            "idr_claim_professional": None,
            "idr_claim_line_professional": None,
        }
        dr.execute(
            final_vars=["idr_beneficiary"],
            overrides=overrides,
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
            },
        )
    elif load_claims and load_type == "initial":
        # idr_claim only depends on claim aux nodes so we set idr_claim as our last node to execute
        dr.execute(
            final_vars=[
                "idr_claim_institutional",
                "idr_claim_date_signature",
                "idr_claim_fiss",
                "idr_claim_item",
                "idr_claim_line_institutional",
                "idr_claim_ansi_signature",
                "idr_claim_professional",
                "idr_claim_line_professional",
                "idr_claim_initial",
            ],
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
            },
        )
    elif load_claims and load_type == "incremental":
        # idr_claim only depends on claim aux nodes so we set idr_claim as our last node to execute
        dr.execute(
            final_vars=["idr_claim"],
            inputs={
                "config_mode": mode,
                "config_batch_size": batch_size,
                "config_connection_string": connection_string,
            },
        )


if __name__ == "__main__":
    main()
