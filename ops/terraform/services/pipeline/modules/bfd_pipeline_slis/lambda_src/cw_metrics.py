import operator
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from functools import reduce
from itertools import chain, combinations
from typing import Any, Tuple, TypeVar

T = TypeVar("T")


@dataclass(frozen=True, eq=True)
class MetricData:
    """Dataclass representing the data needed to "put" a metric up to CloudWatch Metrics. Represents
    both the metric itself (name, dimensions, unit) and the value that is put to said metric
    (timestamp, value)"""

    metric_name: str
    date_time: datetime
    value: float
    unit: str
    dimensions: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True, eq=True)
class MetricDataQuery:
    """Dataclass representing the data needed to get a metric from CloudWatch Metrics. Metrics are
    identified by their namespace, name, and dimensions"""

    metric_namespace: str
    metric_name: str
    dimensions: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True, eq=True)
class MetricDataResult:
    """Dataclass representing the result of a successful GetMetricData operation"""

    label: str
    timestamps: list[datetime]
    values: list[float]


def _powerset(items: list[T]) -> chain[Tuple[T]]:
    """This function computes the powerset (the set of all subsets including the set itself and the
    null set) of the incoming list. Used to automatically generate all possible
    dimensioned metrics for a given metric. Implementation adapted from Python's official
    itertools-recipes documentation

    Example:
        powerset([1,2,3]) --> () (1,) (2,) (3,) (1,2) (1,3) (2,3) (1,2,3)

    Args:
        items (list[T]): A list of items to compute the powerset from

    Returns:
        chain[Tuple[T]]: A generator that will yield subsets starting with the null set upto the set
    """
    return chain.from_iterable(combinations(items, r) for r in range(len(items) + 1))


def gen_all_dimensioned_metrics(
    metric_name: str, date_time: datetime, value: float, unit: str, dimensions: list[dict[str, str]]
) -> list[MetricData]:
    """Generates all of the possible dimensioned (and single undimensioned) metrics from the
    powerset of the list of dimensions passed-in. Useful as all metrics created by this Lambda
    have the same value, timestamp, and name and only differ on their aggregations

    Args:
        metric_name (str): Name of the metric
        timestamp (datetime): Timestamp to store with the metrics
        value (float): Value to store with the metrics in each dimension
        unit (str): The Unit of the metric
        dimensions (list[dict[str, str]]): The list of dimensions to compute the powerset; this
        determines the number of metrics that will be stored (2**dimensions.count)

    Returns:
        list[MetricData]: A list of metrics with each being a set in the powerset of dimensions
    """

    return [
        MetricData(
            metric_name=metric_name,
            date_time=date_time,
            value=value,
            # Merge the chain/generator of dimensions of arbitrary size using the "|" operator
            dimensions=reduce(operator.ior, x, {}),
            unit=unit,
        )
        for x in _powerset(dimensions)
    ]


def put_metric_data(cw_client: Any, metric_namespace: str, metrics: list[MetricData]):
    """Wraps the boto3 CloudWatch PutMetricData API operation to allow for usage of the MetricData
    dataclass

    Args:
        metric_namespace (str): The Namespace of the metric(s) to store in CloudWatch
        metrics (list[MetricData]): The metrics to store
    """

    # Convert from a list of the MetricData class to a list of dicts that boto3 understands for this
    # API. See https://boto3.amazonaws.com/v1/documentation/api/latest/reference/services/cloudwatch.html#CloudWatch.Client.put_metric_data
    metrics_dict_list = [
        {
            "MetricName": m.metric_name,
            "Timestamp": m.date_time,
            "Value": m.value,
            "Unit": m.unit,
            "Dimensions": [
                {
                    "Name": dim_name,
                    "Value": dim_value,
                }
                for dim_name, dim_value in m.dimensions.items()
            ],
        }
        for m in metrics
    ]

    cw_client.put_metric_data(
        Namespace=metric_namespace,
        MetricData=metrics_dict_list,
    )


def get_metric_data(
    cw_client: Any,
    metric_data_queries: list[MetricDataQuery],
    statistic: str,
    period: int = 60,
    start_time: datetime = datetime.utcnow() - timedelta(days=15),
    end_time: datetime = datetime.utcnow(),
) -> list[MetricDataResult]:
    """Wraps the GetMetricData CloudWatch Metrics API operation to allow for easier usage. By
    default, standard resolution metrics from the current time to 15 days in the past are retrieved
    from CloudWatch Metrics.

    Args:
        metric_data_queries (list[MetricDataQuery]): A list of data queries to return metric data
        for
        statistic (str): The statistic for the queried metric(s) to return
        period (int, optional): The period of the metric, correlates to its storage resolution.
        Defaults to 60.
        start_time (datetime, optional): The start of the time period to search. Defaults to
        datetime.utcnow()-timedelta(days=15).
        end_time (datetime, optional): The end of the time period to search. Defaults to
        datetime.utcnow().

    Returns:
        list[MetricDataResult]: A list of results for each data query with each label matching the
        namespace and metric name of its corresponding metric

    Raises:
        KeyError: Raised if the inner GetMetricData query fails for an unknown reason that is
        unhandled or its return value does not conform to its expected definition
    """

    # Transform the list of MetricDataQuery into a list of dicts that the boto3 GetMetricData
    # function understands
    data_queries_dict_list = [
        {
            "Id": f"m{ind}",
            "MetricStat": {
                "Metric": {
                    "Namespace": m.metric_namespace,
                    "MetricName": m.metric_name,
                    "Dimensions": [
                        {
                            "Name": dim_name,
                            "Value": dim_value,
                        }
                        for dim_name, dim_value in m.dimensions.items()
                    ],
                },
                "Period": period,
                "Stat": statistic,
            },
            "Label": f"{m.metric_namespace}/{m.metric_name}",
            "ReturnData": True,
        }
        for ind, m in enumerate(metric_data_queries)
    ]

    result = cw_client.get_metric_data(
        MetricDataQueries=data_queries_dict_list,
        StartTime=start_time,
        EndTime=end_time,
    )

    return [
        MetricDataResult(
            label=result["Label"], timestamps=result["Timestamps"], values=result["Values"]
        )
        for result in result["MetricDataResults"]
    ]
