import logging
from os import getenv


def configure_logger() -> None:
    # logging should only be configured once per process
    if len(logging.root.handlers) > 0:
        return
    console_handler = logging.StreamHandler()
    formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
    console_handler.setFormatter(formatter)
    logging.basicConfig(level=getenv("IDR_LOG_LEVEL", "INFO").upper(), handlers=[console_handler])
