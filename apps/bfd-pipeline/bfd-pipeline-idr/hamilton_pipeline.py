import logging
import os
import sys

import ray
from hamilton import driver
from hamilton.plugins.h_ray import RayGraphAdapter

import pipeline_nodes

console_handler = logging.StreamHandler()
formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
console_handler.setFormatter(formatter)
logging.basicConfig(level=logging.INFO, handlers=[console_handler])
logger = logging.getLogger(__name__)

def main() -> None:
    logger.info("load start")

    parallelism = int(os.environ.get("PARALLELISM", "6"))
    ray.init(
        logging_level="debug", num_cpus=parallelism)

    class DictResultBuilder:
        def build_result(self, results: dict) -> dict:
            return results

    adapter = RayGraphAdapter(result_builder=DictResultBuilder())
    dr = (
        driver.Builder()
        .with_modules(pipeline_nodes)
        .with_adapters(adapter)
        .build()
    )

    graph = dr.graph
    print("Registered Nodes", list(graph.nodes))

    final_vars=["idr_beneficiary"]
    dr.execute(final_vars=final_vars)

    logger.info("done")

if __name__ == "__main__":
    main()