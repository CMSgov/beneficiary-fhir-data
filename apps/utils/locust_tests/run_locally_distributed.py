"""Small python helper for running Locust tests distributedly and headlessly on the local machine.
"""

import argparse
import subprocess
from typing import List

arg_parser = argparse.ArgumentParser(
    description=(
        "Script to run Locust tests locally in distributed, headless mode. Arguments unspecified by"
        " this script are passed to Locustfile."
    ),
    formatter_class=argparse.ArgumentDefaultsHelpFormatter,
)
arg_parser.add_argument(
    "--workers",
    dest="workers",
    type=int,
    help="Number of Locust worker processes to spin up; defaults to 1",
    default=1,
)
arg_parser.add_argument(
    "--locustfile",
    "-f",
    dest="locustfile",
    type=str,
    help="Locustfile to run Tasks from",
    default="locustfile.py",
)
arg_parser.add_argument(
    "--tags",
    "-T",
    dest="tags",
    type=str,
    help="Space-delimited. The locust tasks with ANY of the given tags will be executed",
    default=""
)
arg_parser.add_argument(
    "--exclude-tags",
    "-E",
    dest="exclude-tags",
    type=str,
    help="Space-delimited. The locust tasks with ANY of the given tags will be excluded from execution",
    default=""
)
raw_args, unknown_args = arg_parser.parse_known_args()
config = vars(raw_args)
num_workers = config.get("workers", 1)
locustfile = config.get("locustfile", "locustfile.py")
tags = config.get("tags", "")
exclude_tags = config.get("exclude-tags", "")

master_process = subprocess.Popen(
    [
        "locust",
        "-f",
        locustfile,
        "--master",
        "--headless",
        f"--expect-workers={num_workers}",
        f"--tags={tags}",
        f"--exclude-tags={exclude_tags}",
        *unknown_args,
    ],
    stderr=subprocess.STDOUT,
)

worker_processes: List[subprocess.Popen] = []
for i in range(int(num_workers)):
    print(f"Creating worker #{i}")
    worker_processes.append(
        subprocess.Popen(
            ["locust", "-f", locustfile, "--worker"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    )

master_process.wait()
for process in worker_processes:
    process.terminate()
