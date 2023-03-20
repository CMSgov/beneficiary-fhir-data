import time
from typing import Callable, Optional, Type, TypeVar

T = TypeVar("T")

DEFAULT_MAX_RETRIES = 10
"""The maximum number of exponentially backed-off retries to attempt when trying to retry an
operation"""


def backoff_retry(
    func: Callable[[], T],
    retries: int = DEFAULT_MAX_RETRIES,
    ignored_exceptions: Optional[list[Type[BaseException]]] = None,
) -> T:
    """Generic function for wrapping another callable (function) that may raise errors and require
    some form of retry mechanism. Supports passing a list of exceptions/errors for the retry logic
    to ignore and instead raise to the calling function to handle

    Args:
        func (Callable[[], T]): The function to retry
        retries (int, optional): The number of times to retry before raising the error causing the
        failure. Defaults to PUT_METRIC_DATA_MAX_RETRIES.
        ignored_exceptions (list[Type[BaseException]] , optional): A list of exceptions to skip
        iretrying and nstead immediately raise to the calling function. Defaults to [].

    Raises:
        exc: Any exception in ignored_exceptions, or the exception thrown on the final retry

    Returns:
        T: The return type of func
    """
    if ignored_exceptions is None:
        ignored_exceptions = []

    for try_number in range(1, retries):
        try:
            return func()
        except Exception as exc:  # pylint: disable=W0718
            # Raise the exception if it is any of the explicitly ignored exceptions or if this
            # was the last try or if the exception is one of a few special base exceptions
            if (
                any(isinstance(exc, ignored_exc) for ignored_exc in ignored_exceptions)
                or try_number == retries
            ):
                raise exc

            # Exponentially back-off from hitting the API to ensure we don't hit the API limit.
            # See https://docs.aws.amazon.com/general/latest/gr/api-retries.html
            sleep_time = (2**try_number * 100.0) / 1000.0
            print(
                f"Unhandled error occurred, retrying in {sleep_time} seconds; attempt"
                f" #{try_number} of {retries}, err: {exc}"
            )
            time.sleep(sleep_time)

    raise RuntimeError(f"Number of retries exceeded the maximum of {retries} attempts")
