"""Small python helper for running Locust tests distributedly and headlessly
on the local machine.
"""

import argparse
import signal
import subprocess
import sys

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
    "--host", dest="host", type=str, help="host to run against", default="localhost"
)
arg_parser.add_argument(
    "--locust-tags",
    dest="locust-tags",
    type=str,
    help='Space-delimited. Run the locust tasks with ANY of the given @tag(s). Will run all tasks \
        if not provided (Optional, Default: "")',
    default="",
)
arg_parser.add_argument(
    "--locust-exclude-tags",
    dest="locust-exclude-tags",
    type=str,
    help='Space-delimited. Exclude the locust tasks with ANY of the given @tag(s) \
        (Optional, Default: "")',
    default="",
)
arg_parser.add_argument(
    "--database-connection-string",
    type=str,
    required=True,
    help=(
        "Specifies database connection string including username, url-encoded password, URI,"
        " port and DB schema; ex: postgres://USERNAME:URL_ENCODED_PASSWORD@URI:PORT/SCHEMA."
        " Required"
    ),
    dest="database_constr",
)
arg_parser.add_argument(
    "--client-cert-path",
    type=str,
    required=True,
    help='Specifies path to client cert, ex: "<path/to/client/pem/file>" (Required)',
    dest="client_cert_path",
)
raw_args, unknown_args = arg_parser.parse_known_args()
config = vars(raw_args)
num_workers = config.get("workers", 1)
locustfile = config.get("locustfile", "locustfile.py")
tags = config.get("locust-tags", "")
exclude_tags = config.get("locust-exclude-tags", "")
conn_str = config.get("database_constr", "")
cert_path = config.get("client_cert_path")
host = config.get("host", "")

if not conn_str or not cert_path:
    raise ValueError("Bad values given")

master_process = subprocess.Popen(
    [
        "locust",
        "-f",
        locustfile,
        "--master",
        "--headless",
        f"--expect-workers={num_workers}",
        f"--host={host}",
        f"--locust-tags={tags}",
        f"--locust-exclude-tags={exclude_tags}",
        f"--database-connection-string={conn_str}",
        f"--client-cert-path={cert_path}",
        *unknown_args,
    ],
    stderr=subprocess.STDOUT,
)


def sigint_handler(
    signum: int,
    master: subprocess.Popen[bytes],
    workers: list[subprocess.Popen[bytes]],
) -> None:
    signal.signal(signalnum=signum, handler=signal.SIG_IGN)
    print("CTRL+C pressed, stopping...")
    for worker in workers:
        worker.terminate()

    master.terminate()
    sys.exit(0)


worker_processes: list[subprocess.Popen[bytes]] = []

signal.signal(
    signalnum=signal.SIGINT,
    handler=lambda _, __: sigint_handler(
        signum=signal.SIGINT, master=master_process, workers=worker_processes
    ),
)
for i in range(int(num_workers)):
    print(f"Creating worker #{i}")
    worker_processes.append(
        subprocess.Popen(
            [
                "locust",
                "-f",
                locustfile,
                "--worker",
                f"--host={host}",
                "--database-connection-string",
                conn_str,
                "--client-cert-path",
                cert_path,
            ],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )
    )
master_process.wait()

for process in worker_processes:
    process.terminate()
