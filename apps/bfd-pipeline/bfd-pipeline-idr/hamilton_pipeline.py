import logging
import os
import sys
import time

import ray
from hamilton import driver
from hamilton.plugins.h_ray import RayGraphAdapter

import pipeline_nodes
from hamilton_loader import get_connection_string
from pipeline_utils import configure_logger

def main() -> None:
    logger = configure_logger("driver")
    logger.info("load start")

    parallelism = int(os.environ.get("PARALLELISM", "6"))
    ray.init(
        logging_level="debug",
        num_cpus=parallelism,
        include_dashboard=True,
        dashboard_port=8265)

    class DictResultBuilder:
        @staticmethod
        def build_result(**kwargs) -> dict:
            return kwargs

    adapter = RayGraphAdapter(result_builder=DictResultBuilder)
    dr = (
        driver.Builder()
        .with_modules(pipeline_nodes)
        .with_adapters(adapter)
        .build()
    )

    graph = dr.graph
    for node_name, node in graph.nodes.items():
        print(f"Node: {node_name}")
        print(f"  Dependencies (incoming edges): {list(node.input_types.keys())}")

    batch_size = int(os.environ.get("IDR_BATCH_SIZE", "100_000"))
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    if mode == "local":
        connection_string = "host=localhost dbname=idr user=bfd password=InsecureLocalDev"
    elif mode == "synthetic":
        connection_string = get_connection_string()
    else:
        connection_string = get_connection_string()

    final_vars=["idr_beneficiary"]
    dr.execute(final_vars=final_vars,
               inputs={
                   "config_mode": mode,
                   "config_batch_size": batch_size,
                   "config_connection_string": connection_string
               })

if __name__ == "__main__":
    main()