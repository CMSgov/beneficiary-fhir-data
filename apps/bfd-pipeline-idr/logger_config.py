import logging
import queue
from logging.handlers import QueueHandler, QueueListener
from os import getenv
from typing import Any


def configure_logger() -> None:
    # logging should only be configured once per process
    if len(logging.root.handlers) > 0:
        return
    log_queue: queue.Queue[Any] = queue.Queue()
    queue_handler = QueueHandler(log_queue)
    console_handler = logging.StreamHandler()
    queue_listener = QueueListener(log_queue, console_handler)
    queue_listener.start()
    formatter = logging.Formatter("[%(levelname)s] %(asctime)s %(message)s")
    console_handler.setFormatter(formatter)
    logging.basicConfig(level=getenv("IDR_LOG_LEVEL", "INFO").upper(), handlers=[queue_handler])
