# pylint: disable=missing-class-docstring,missing-function-docstring,missing-module-docstring
# pylint: disable=too-many-lines,too-many-arguments,too-many-public-methods
import calendar
import json
import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Protocol, Type
from unittest import mock

import pytest

from common import PipelineMetric, RifFileType
from cw_metrics import MetricData
from sqs import PipelineLoadEvent, PipelineLoadEventMessage, PipelineLoadEventType
from update_pipeline_slis import handler

DEFAULT_MOCK_GROUP_ISO_STR = "2024-01-12T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_ISO = "2024-01-19T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_DATETIME = datetime.fromisoformat(
    DEFAULT_MOCK_EVENT_TIME_ISO.removesuffix("Z")
).replace(tzinfo=timezone.utc)
DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP = calendar.timegm(
    DEFAULT_MOCK_EVENT_TIME_DATETIME.utctimetuple()
)
DEFAULT_MOCK_EVENT_NAME = "ObjectCreated"
DEFAULT_MOCK_BUCKET = "mock-bucket"
DEFAULT_MOCK_QUEUE = "mock-queue"
DEFAULT_MOCK_NAMESPACE = "mock-namespace"

mock_boto3_client = mock.Mock()
mock_boto3_resource = mock.Mock()
mock_lambda_context = mock.Mock()


def generate_event(
    key: str,
    event_time_iso: str = DEFAULT_MOCK_EVENT_TIME_ISO,
    event_name: str = DEFAULT_MOCK_EVENT_NAME,
) -> dict[str, Any]:
    return {
        "Records": [
            {
                "Sns": {
                    "Message": json.dumps(
                        {
                            "Records": [
                                {
                                    "eventName": event_name,
                                    "eventTime": event_time_iso,
                                    "s3": {"object": {"key": key}},
                                }
                            ]
                        }
                    )
                }
            }
        ]
    }


def gen_log_regex(log_msg_regex: str) -> str:
    return rf"(?m)(DEBUG|INFO|WARNING|ERROR):\w+:{log_msg_regex}"


class MockedRetrieveLoadMsgs(Protocol):
    def __call__(
        self, type_filter: list[PipelineLoadEventType], *args: Any, **kwargs: dict[str, Any]
    ) -> list[PipelineLoadEventMessage]: ...


def gen_mocked_retrieve_load_msgs_side_effect(
    mocked_queue_contents: list[PipelineLoadEventMessage],
) -> "MockedRetrieveLoadMsgs":
    def _generated_func(type_filter: list[PipelineLoadEventType], *_: Any, **__: dict[str, Any]):
        return [
            messages
            for messages in mocked_queue_contents
            if messages.event.event_type in type_filter
        ]

    return _generated_func


def get_mocked_put_metrics(mock_put_metric_data: mock.Mock) -> list[MetricData]:
    # We're only testing the behavior of the handler, so we don't care about what put_metric_data
    # _actually_ does (and especially not what boto3 does), so we just extract the list of metrics
    # passed to the mocked function by the handler to use in assertions
    return [
        metric for args in mock_put_metric_data.call_args_list for metric in args.kwargs["metrics"]
    ]


def get_mocked_put_events(mock_post_load_event: mock.Mock) -> list[PipelineLoadEvent]:
    # Similar to the above helper function, we don't care about the behavior of the
    # "post_load_event" function, only what the Lambda handler calls it with. We extract those calls
    # out for assertions
    return [x.kwargs["message"] for x in mock_post_load_event.call_args_list]


def get_mocked_deleted_msgs(
    mock_delete_load_msg: mock.Mock,
) -> list[PipelineLoadEventMessage]:
    return [x.kwargs["message"] for x in mock_delete_load_msg.call_args_list]


def utc_timestamp(date_time: datetime) -> int:
    return calendar.timegm(date_time.utctimetuple())


@mock.patch("update_pipeline_slis.boto3.client", new=mock_boto3_client)
@mock.patch("update_pipeline_slis.boto3.resource", new=mock_boto3_resource)
@mock.patch("update_pipeline_slis.ETL_BUCKET_ID", DEFAULT_MOCK_BUCKET)
@mock.patch("update_pipeline_slis.EVENTS_QUEUE_NAME", DEFAULT_MOCK_QUEUE)
@mock.patch("update_pipeline_slis.METRICS_NAMESPACE", DEFAULT_MOCK_NAMESPACE)
class TestUpdatePipelineSlisHandler:
    @pytest.mark.parametrize(
        "event,expected_error",
        [
            (None, TypeError),
            ("", TypeError),
            ({"Records": []}, ValueError),
            ({"Records": ""}, ValueError),
            ({"Records": [""]}, TypeError),
            ({"Records": [{"OtherKey": ""}]}, KeyError),
            ({"Records": [{"Sns": {"OtherKey": ""}}]}, KeyError),
            ({"Records": [{"Sns": {"Message": "invalid"}}]}, ValueError),
            ({"Records": [{"Sns": {"Message": {"NotJsonStr": ""}}}]}, TypeError),
            ({"Records": [{"Sns": {"Message": "{}"}}]}, KeyError),
            ({"Records": [{"Sns": {"Message": '{"Records":[]}'}}]}, ValueError),
            ({"Records": [{"Sns": {"Message": '{"Records":""}'}}]}, ValueError),
            ({"Records": [{"Sns": {"Message": '{"Records":[{}]}'}}]}, KeyError),
            (
                generate_event(
                    key=f"Incoming/{DEFAULT_MOCK_GROUP_ISO_STR}/bene_1234.txt", event_name="invalid"
                ),
                ValueError,
            ),
            (
                {
                    "Records": [
                        {
                            "Sns": {
                                "Message": json.dumps(
                                    {"Records": [{"eventName": DEFAULT_MOCK_EVENT_NAME}]}
                                )
                            }
                        }
                    ]
                },
                KeyError,
            ),
            (
                generate_event(
                    key=f"Incoming/{DEFAULT_MOCK_GROUP_ISO_STR}/bene_1234.txt",
                    event_time_iso="24-01-19T00:00:00Z",
                ),
                ValueError,
            ),
            (
                {
                    "Records": [
                        {
                            "Sns": {
                                "Message": json.dumps(
                                    {
                                        "Records": [
                                            {
                                                "eventName": DEFAULT_MOCK_EVENT_NAME,
                                                "eventTime": DEFAULT_MOCK_EVENT_TIME_ISO,
                                                "s3": {"object": {}},
                                            }
                                        ]
                                    }
                                )
                            }
                        }
                    ]
                },
                KeyError,
            ),
        ],
    )
    def test_it_raises_exception_if_event_is_invalid(
        self, event: Any, expected_error: Type[Exception]
    ):
        with pytest.raises(expected_error):
            handler(event=event, context=mock_lambda_context)

    def test_it_fails_if_key_not_incoming_or_done(self, caplog: pytest.LogCaptureFixture):
        # Arrange
        invalid_key = f"{DEFAULT_MOCK_GROUP_ISO_STR}/bene_1234.txt"

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(key=invalid_key),
                context=mock_lambda_context,
            )

        # Assert
        assert "ETL file or path does not match expected format, skipping" in caplog.text

    def test_it_fails_if_key_not_known_rif(self, caplog: pytest.LogCaptureFixture):
        # Arrange
        invalid_key = f"Incoming/{DEFAULT_MOCK_GROUP_ISO_STR}/unknown_1234.txt"

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(key=invalid_key),
                context=mock_lambda_context,
            )

        # Assert
        assert "ETL file or path does not match expected format, skipping" in caplog.text

    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.post_load_event", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_valid_first_incoming_event(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_post_load_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
    ):
        # Arrange
        rif = RifFileType.BENEFICIARY
        folder = "Incoming"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mock_retrieve_load_event_msgs.return_value = []

        # Act
        handler(event=generate_event(key=key), context=mock_lambda_context)

        # Assert
        # We put metrics for the RIF dimensioned by group and its data type, metrics for the
        # load becoming first available dimensioned by the group, and a repeating metrics for the
        # first available load metric.
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.unit,
                dimensions={},
            ),
        ]
        # pytest doesn't have a built-in assertion helper for ensuring that two lists are the same
        # ignoring order. We could sort the lists, but the MetricData dataclass cannot be ordered
        # as-is due to its inner dict, so checking that both lists are of equal length and that all
        # elements of the expected list are in the actual list is the best workaround
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        # We put events indicating when this RIF file was loaded as well as when the load was made
        # first available to compute deltas later
        actual_put_events = get_mocked_put_events(mock_post_load_event=mock_post_load_event)
        expected_put_events = [
            PipelineLoadEvent(
                event_type=PipelineLoadEventType.RIF_AVAILABLE,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif_type=rif,
            ),
            PipelineLoadEvent(
                event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif_type=rif,
            ),
        ]
        assert len(actual_put_events) == len(expected_put_events)
        assert all((expected in actual_put_events) for expected in expected_put_events)

    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.post_load_event", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_valid_subsequent_incoming_events(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_post_load_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
    ):
        # Arrange
        rif = RifFileType.CARRIER
        event_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=1)
        folder = "Incoming"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=[
                PipelineLoadEventMessage(
                    receipt_handle="1",
                    event=PipelineLoadEvent(
                        event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                        date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                        group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                        rif_type=RifFileType.BENEFICIARY,
                    ),
                ),
                PipelineLoadEventMessage(
                    receipt_handle="2",
                    event=PipelineLoadEvent(
                        event_type=PipelineLoadEventType.RIF_AVAILABLE,
                        date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                        group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                        rif_type=RifFileType.BENEFICIARY,
                    ),
                ),
            ]
        )

        # Act
        handler(
            event=generate_event(key=key, event_time_iso=event_time.isoformat()),
            context=mock_lambda_context,
        )

        # Assert
        # We put metrics for the RIF dimensioned by group and its data type. No metrics
        # should be put for the load, as the mocked queue indicates the load has already begun and
        # those metrics have already been processed
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        # We put a single event indicating when this CARRIER rif was loaded. No event should be
        # posted for the load itself, as the mocked queue indicates that has already happened and
        # the load was made available with the BENEFICIARY rif first
        actual_put_events = get_mocked_put_events(mock_post_load_event=mock_post_load_event)
        expected_put_events = [
            PipelineLoadEvent(
                event_type=PipelineLoadEventType.RIF_AVAILABLE,
                date_time=event_time,
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif_type=rif,
            ),
        ]
        assert len(actual_put_events) == len(expected_put_events)
        assert all((expected in actual_put_events) for expected in expected_put_events)

    @mock.patch("update_pipeline_slis.delete_load_msg_from_queue", autospec=True)
    @mock.patch("update_pipeline_slis._is_incoming_folder_empty", autospec=True)
    @mock.patch("update_pipeline_slis._is_pipeline_load_complete", autospec=True)
    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_nonfinal_done_event(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        mock_delete_load_msg: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.BENEFICIARY
        event_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=1)
        event_time_delta = round((event_time - DEFAULT_MOCK_EVENT_TIME_DATETIME).total_seconds())
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mocked_queued_events = [
            PipelineLoadEventMessage(
                receipt_handle="1",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                    date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=rif,
                ),
            ),
            PipelineLoadEventMessage(
                receipt_handle="2",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.RIF_AVAILABLE,
                    date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=rif,
                ),
            ),
        ]
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=mocked_queued_events
        )
        mock_load_complete.return_value = False
        mock_incoming_empty.return_value = False

        # Act
        with caplog.at_level(logging.INFO):
            handler(
                event=generate_event(key=key, event_time_iso=event_time.isoformat()),
                context=mock_lambda_context,
            )

        # Assert
        assert (
            f"Not all files have yet to be loaded for group {DEFAULT_MOCK_GROUP_ISO_STR}. Data load"
            " is not complete. Stopping..."
            in caplog.text
        )

        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        actual_deleted_msgs = get_mocked_deleted_msgs(mock_delete_load_msg=mock_delete_load_msg)
        expected_deleted_msgs = [
            x
            for x in mocked_queued_events
            if x.event.event_type == PipelineLoadEventType.RIF_AVAILABLE
        ]
        assert len(actual_deleted_msgs) == len(expected_deleted_msgs)
        assert all((expected in actual_deleted_msgs) for expected in expected_deleted_msgs)

    @mock.patch("update_pipeline_slis.delete_load_msg_from_queue", autospec=True)
    @mock.patch("update_pipeline_slis._is_incoming_folder_empty", autospec=True)
    @mock.patch("update_pipeline_slis._is_pipeline_load_complete", autospec=True)
    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_nonfinal_done_event_with_queue_missing_rif_available_event(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        mock_delete_load_msg: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        # Queue is intentionally missing RIF_AVAILABLE event indicating when the DME RIF was made
        # available. This is invalid state, as we should never run into this situation, but _if_ we
        # do then the Lambda should be able to still generate "data loaded" metrics
        mocked_queued_events = [
            PipelineLoadEventMessage(
                receipt_handle="1",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                    date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=RifFileType.BENEFICIARY,
                ),
            ),
        ]
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=mocked_queued_events
        )
        mock_load_complete.return_value = False
        mock_incoming_empty.return_value = False

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(key=key, event_time_iso=rif_done_time.isoformat()),
                context=mock_lambda_context,
            )

        # Assert
        assert (
            f"No corresponding messages found for {rif.value} RIF in group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in queue {DEFAULT_MOCK_QUEUE}; no time delta metrics"
            " can be computed for this RIF. Continuing..."
            in caplog.text
        )

        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        assert mock_delete_load_msg.call_count == 0

    @mock.patch("update_pipeline_slis.delete_load_msg_from_queue", autospec=True)
    @mock.patch("update_pipeline_slis._is_incoming_folder_empty", autospec=True)
    @mock.patch("update_pipeline_slis._is_pipeline_load_complete", autospec=True)
    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_final_done_event(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        mock_delete_load_msg: mock.Mock,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_start_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=11)
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        rif_done_time_delta = round((rif_done_time - rif_start_time).total_seconds())
        load_done_time_delta = round(
            (rif_done_time - DEFAULT_MOCK_EVENT_TIME_DATETIME).total_seconds()
        )
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mocked_queued_events = [
            PipelineLoadEventMessage(
                receipt_handle="1",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                    date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=RifFileType.BENEFICIARY,
                ),
            ),
            PipelineLoadEventMessage(
                receipt_handle="2",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.RIF_AVAILABLE,
                    date_time=rif_start_time,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=rif,
                ),
            ),
        ]
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=mocked_queued_events
        )
        mock_load_complete.return_value = True
        mock_incoming_empty.return_value = True

        # Act
        handler(
            event=generate_event(key=key, event_time_iso=rif_done_time.isoformat()),
            context=mock_lambda_context,
        )

        # Assert
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        actual_deleted_msgs = get_mocked_deleted_msgs(mock_delete_load_msg=mock_delete_load_msg)
        expected_deleted_msgs = mocked_queued_events
        assert len(actual_deleted_msgs) == len(expected_deleted_msgs)
        assert all((expected in actual_deleted_msgs) for expected in expected_deleted_msgs)

    @mock.patch("update_pipeline_slis.delete_load_msg_from_queue", autospec=True)
    @mock.patch("update_pipeline_slis._is_incoming_folder_empty", autospec=True)
    @mock.patch("update_pipeline_slis._is_pipeline_load_complete", autospec=True)
    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_final_done_event_with_queue_missing_load_available_event(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        mock_delete_load_msg: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_start_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=11)
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        rif_done_time_delta = round((rif_done_time - rif_start_time).total_seconds())
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        # Queue is intentionally missing RIF_AVAILABLE event indicating when the DME RIF was made
        # available. This is invalid state, as we should never run into this situation, but _if_ we
        # do then the Lambda should be able to still generate "fully loaded" metrics excluding any
        # RIF-specific time delta metrics
        mocked_queued_events = [
            PipelineLoadEventMessage(
                receipt_handle="1",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.RIF_AVAILABLE,
                    date_time=rif_start_time,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=rif,
                ),
            ),
        ]
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=mocked_queued_events
        )
        mock_load_complete.return_value = True
        mock_incoming_empty.return_value = True

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(key=key, event_time_iso=rif_done_time.isoformat()),
                context=mock_lambda_context,
            )

        # Assert
        assert (
            f"No corresponding {PipelineLoadEventType.LOAD_AVAILABLE.value} message found for group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in queue {DEFAULT_MOCK_QUEUE}; no time delta metrics"
            " can be computed for this data load"
            in caplog.text
        )
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.unit,
                dimensions={},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        actual_deleted_msgs = get_mocked_deleted_msgs(mock_delete_load_msg=mock_delete_load_msg)
        expected_deleted_msgs = mocked_queued_events
        assert len(actual_deleted_msgs) == len(expected_deleted_msgs)
        assert all((expected in actual_deleted_msgs) for expected in expected_deleted_msgs)

    @mock.patch("update_pipeline_slis.delete_load_msg_from_queue", autospec=True)
    @mock.patch("update_pipeline_slis._is_incoming_folder_empty", autospec=True)
    @mock.patch("update_pipeline_slis._is_pipeline_load_complete", autospec=True)
    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_final_done_event_with_queue_missing_rif_available_event(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        mock_delete_load_msg: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        load_done_time_delta = round(
            (rif_done_time - DEFAULT_MOCK_EVENT_TIME_DATETIME).total_seconds()
        )
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        # Queue is intentionally missing RIF_AVAILABLE event indicating when the DME RIF was made
        # available. This is invalid state, as we should never run into this situation, but _if_ we
        # do then the Lambda should be able to still generate "fully loaded" metrics
        mocked_queued_events = [
            PipelineLoadEventMessage(
                receipt_handle="1",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                    date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                    group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                    rif_type=RifFileType.BENEFICIARY,
                ),
            ),
        ]
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=mocked_queued_events
        )
        mock_load_complete.return_value = True
        mock_incoming_empty.return_value = True

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(key=key, event_time_iso=rif_done_time.isoformat()),
                context=mock_lambda_context,
            )

        # Assert
        assert (
            f"No corresponding messages found for {rif.value} RIF in group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in queue {DEFAULT_MOCK_QUEUE}; no time delta metrics"
            " can be computed for this RIF. Continuing..."
            in caplog.text
        )
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        actual_deleted_msgs = get_mocked_deleted_msgs(mock_delete_load_msg=mock_delete_load_msg)
        expected_deleted_msgs = mocked_queued_events
        assert len(actual_deleted_msgs) == len(expected_deleted_msgs)
        assert all((expected in actual_deleted_msgs) for expected in expected_deleted_msgs)

    @mock.patch("update_pipeline_slis.delete_load_msg_from_queue", autospec=True)
    @mock.patch("update_pipeline_slis._is_incoming_folder_empty", autospec=True)
    @mock.patch("update_pipeline_slis._is_pipeline_load_complete", autospec=True)
    @mock.patch("update_pipeline_slis.put_metric_data", autospec=True)
    @mock.patch("update_pipeline_slis.retrieve_load_event_msgs", autospec=True)
    def test_it_handles_final_done_event_with_queue_missing_both_events(
        self,
        mock_retrieve_load_event_msgs: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        mock_delete_load_msg: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        # Queue is intentionally missing _both_ RIF_AVAILABLE and LOAD_AVAILABLE events. This is the
        # worst-case scenario, and should never happen, but we should still see "data loaded" and
        # "fully loaded" metrics be generated for CloudWatch
        mock_retrieve_load_event_msgs.side_effect = gen_mocked_retrieve_load_msgs_side_effect(
            mocked_queue_contents=[]
        )
        mock_load_complete.return_value = True
        mock_incoming_empty.return_value = True

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(key=key, event_time_iso=rif_done_time.isoformat()),
                context=mock_lambda_context,
            )

        # Assert
        assert (
            f"No corresponding messages found for {rif.value} RIF in group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in queue {DEFAULT_MOCK_QUEUE}; no time delta metrics"
            " can be computed for this RIF. Continuing..."
            in caplog.text
        )
        assert (
            f"No corresponding {PipelineLoadEventType.LOAD_AVAILABLE.value} message found for group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in queue {DEFAULT_MOCK_QUEUE}; no time delta metrics"
            " can be computed for this data load"
            in caplog.text
        )
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.unit,
                dimensions={},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        assert mock_delete_load_msg.call_count == 0
