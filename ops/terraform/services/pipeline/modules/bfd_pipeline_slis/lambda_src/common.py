import os
from dataclasses import dataclass
from enum import StrEnum

METRICS_NAMESPACE = os.environ.get("METRICS_NAMESPACE", "")


class RifFileType(StrEnum):
    """Represents all of the possible RIF file types that can be loaded by the BFD ETL Pipeline. The
    value of each enum is a specific substring that is used to match on each type of file"""

    BENEFICIARY_HISTORY = "beneficiary_history"
    BENEFICIARY = "bene"
    CARRIER = "carrier"
    DME = "dme"
    HHA = "hha"
    HOSPICE = "hospice"
    INPATIENT = "inpatient"
    OUTPATIENT = "outpatient"
    PDE = "pde"
    SNF = "snf"


@dataclass
class PipelineMetricMetadata:
    """Encapsulates metadata about a given pipeline metric"""

    metric_name: str
    """The name of the metric in CloudWatch Metrics, excluding namespace"""
    unit: str
    """The unit of the metric. Must conform to the list of supported CloudWatch Metrics"""

    def __hash__(self) -> int:
        return hash(self.metric_name)


class PipelineMetric(PipelineMetricMetadata, Enum):
    """Enumeration of pipeline metrics that can be stored in CloudWatch Metrics"""

    TIME_DATA_AVAILABLE = PipelineMetricMetadata("time/data-available", "Seconds")
    TIME_DATA_FIRST_AVAILABLE = PipelineMetricMetadata("time/data-first-available", "Seconds")
    TIME_DATA_LOADED = PipelineMetricMetadata("time/data-loaded", "Seconds")
    TIME_DATA_FULLY_LOADED = PipelineMetricMetadata("time/data-fully-loaded", "Seconds")
    TIME_DELTA_DATA_LOAD_TIME = PipelineMetricMetadata("time-delta/data-load-time", "Seconds")
    TIME_DELTA_FULL_DATA_LOAD_TIME = PipelineMetricMetadata(
        "time-delta/data-full-load-time", "Seconds"
    )
    TIME_DATA_FIRST_AVAILABLE_REPEATING = PipelineMetricMetadata(
        "time/data-first-available-repeating", "Seconds"
    )
    TIME_DATA_FULLY_LOADED_REPEATING = PipelineMetricMetadata(
        "time/data-fully-loaded-repeating", "Seconds"
    )

    def __init__(self, data: PipelineMetricMetadata):
        for key in data.__annotations__.keys():
            value = getattr(data, key)
            setattr(self, key, value)

    def __hash__(self) -> int:
        return hash(self.value)

    def full_name(self) -> str:
        """Returns the fully qualified name of the metric, which includes the metric namespace and
        metric name

        Returns:
            str: The "full name" of the metric
        """
        metric_metadata: PipelineMetricMetadata = self.value
        return f"{METRICS_NAMESPACE}/{metric_metadata.metric_name}"
