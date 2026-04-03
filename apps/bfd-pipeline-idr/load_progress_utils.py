from model.base_model import LoadMode
from settings import parse_bool_default_false


# Tracking load progress is disabled for synthetic data loads.
# Use this to force enabling load progress for testing.
def force_load_progress() -> bool:
    # We don't normally want to store the load progress info for synthetic data since the dates
    # won't be in order like in prod. However, we need a way to override this for the tests.
    return parse_bool_default_false("IDR_FORCE_LOAD_PROGRESS")


def should_track_load_progress(load_mode: LoadMode) -> bool:
    # Whether to read/write load progress, which is diabled for synthetic and testing loads.
    return load_mode == LoadMode.IDR or force_load_progress()
