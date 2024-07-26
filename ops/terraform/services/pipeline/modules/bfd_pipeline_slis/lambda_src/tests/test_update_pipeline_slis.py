# pylint: disable=missing-class-docstring,missing-function-docstring,missing-module-docstring
# pylint: disable=too-many-lines,too-many-arguments,too-many-public-methods
import calendar
import json
import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Type
from unittest import mock

import pytest

from common import PipelineMetric, RifFileType
from cw_metrics import MetricData, put_metric_data
from dynamo_db import (
    LoadAvailableEvent,
    RifAvailableEvent,
    get_load_available_event,
    get_rif_available_event,
    put_load_available_event,
    put_rif_available_event,
)
from update_pipeline_slis import (
    _handle_s3_event,
    _is_incoming_folder_empty,  # type: ignore
    _is_pipeline_load_complete,  # type: ignore
    handler,
)

DEFAULT_MOCK_GROUP_ISO_STR = "2024-01-12T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_ISO = "2024-01-19T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_DATETIME = datetime.fromisoformat(
    DEFAULT_MOCK_EVENT_TIME_ISO.removesuffix("Z")
).replace(tzinfo=timezone.utc)
DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP = calendar.timegm(
    DEFAULT_MOCK_EVENT_TIME_DATETIME.utctimetuple()
)
DEFAULT_MOCK_EVENT_NAME = "ObjectCreated:Put"
DEFAULT_MOCK_BUCKET = "mock-bucket"
DEFAULT_MOCK_NAMESPACE = "mock-namespace"
DEFAULT_MOCK_RIF_AVAIL_TBL = "mock-rif-available-tbl"
DEFAULT_MOCK_LOAD_AVAIL_TBL = "mock-load-available-tbl"
MODULE_UNDER_TEST = "update_pipeline_slis"
IS_INCOMING_FOLDER_EMPTY_PATCH_PATH = f"{MODULE_UNDER_TEST}.{_is_incoming_folder_empty.__name__}"
IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH = f"{MODULE_UNDER_TEST}.{_is_pipeline_load_complete.__name__}"
HANDLE_S3_EVENT_PATCH_PATH = f"{MODULE_UNDER_TEST}.{_handle_s3_event.__name__}"
PUT_METRIC_DATA_PATCH_PATH = f"{MODULE_UNDER_TEST}.{put_metric_data.__name__}"
PUT_RIF_AVAILABLE_EVENT_PATCH_PATH = f"{MODULE_UNDER_TEST}.{put_rif_available_event.__name__}"
PUT_LOAD_AVAILABLE_EVENT_PATCH_PATH = f"{MODULE_UNDER_TEST}.{put_load_available_event.__name__}"
GET_RIF_AVAILABLE_EVENT_PATCH_PATH = f"{MODULE_UNDER_TEST}.{get_rif_available_event.__name__}"
GET_LOAD_AVAILABLE_EVENT_PATCH_PATH = f"{MODULE_UNDER_TEST}.{get_load_available_event.__name__}"

mock_boto3_client = mock.Mock()
mock_boto3_resource = mock.Mock()
mock_lambda_context = mock.Mock()


def generate_event(
    key: str = f"Incoming/{DEFAULT_MOCK_GROUP_ISO_STR}/bene_1234.txt",
    event_time_iso: str = DEFAULT_MOCK_EVENT_TIME_ISO,
    event_name: str = DEFAULT_MOCK_EVENT_NAME,
) -> dict[str, Any]:
    return {
        "Records": [{
            "Sns": {
                "Message": json.dumps({
                    "Records": [{
                        "eventName": event_name,
                        "eventTime": event_time_iso,
                        "s3": {"object": {"key": key}},
                    }]
                })
            }
        }]
    }


def get_mocked_put_metrics(mock_put_metric_data: mock.Mock) -> list[MetricData]:
    # We're only testing the behavior of the handler, so we don't care about what put_metric_data
    # _actually_ does (and especially not what boto3 does), so we just extract the list of metrics
    # passed to the mocked function by the handler to use in assertions
    return [
        metric for args in mock_put_metric_data.call_args_list for metric in args.kwargs["metrics"]
    ]


def get_mocked_put_rif_avail_events(
    mock_put_rif_available_event: mock.Mock,
) -> list[RifAvailableEvent]:
    # Similar to the above helper function, we don't care about the behavior of the
    # "put_rif_available_event" function, only what the Lambda handler calls it with. We extract
    # those calls out for assertions
    return [x.kwargs["event"] for x in mock_put_rif_available_event.call_args_list]


def get_mocked_put_load_avail_events(
    mock_put_load_available_event: mock.Mock,
) -> list[LoadAvailableEvent]:
    return [x.kwargs["event"] for x in mock_put_load_available_event.call_args_list]


def utc_timestamp(date_time: datetime) -> int:
    return calendar.timegm(date_time.utctimetuple())


@mock.patch(f"{MODULE_UNDER_TEST}.boto3.client", new=mock_boto3_client)
@mock.patch(f"{MODULE_UNDER_TEST}.boto3.resource", new=mock_boto3_resource)
@mock.patch(f"{MODULE_UNDER_TEST}.ETL_BUCKET_ID", DEFAULT_MOCK_BUCKET)
@mock.patch(f"{MODULE_UNDER_TEST}.METRICS_NAMESPACE", DEFAULT_MOCK_NAMESPACE)
@mock.patch(f"{MODULE_UNDER_TEST}.RIF_AVAILABLE_DDB_TBL", DEFAULT_MOCK_RIF_AVAIL_TBL)
@mock.patch(f"{MODULE_UNDER_TEST}.LOAD_AVAILABLE_DDB_TBL", DEFAULT_MOCK_LOAD_AVAIL_TBL)
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
                {
                    "Records": [{
                        "Sns": {
                            "Message": json.dumps(
                                {"Records": [{"eventName": DEFAULT_MOCK_EVENT_NAME}]}
                            )
                        }
                    }]
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
                    "Records": [{
                        "Sns": {
                            "Message": json.dumps({
                                "Records": [{
                                    "eventName": DEFAULT_MOCK_EVENT_NAME,
                                    "eventTime": DEFAULT_MOCK_EVENT_TIME_ISO,
                                    "s3": {"object": {}},
                                }]
                            })
                        }
                    }]
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

    @mock.patch(HANDLE_S3_EVENT_PATCH_PATH, autospec=True)
    def test_it_fails_if_s3_event_name_is_unknown(
        self, mock_handle_s3_event: mock.Mock, caplog: pytest.LogCaptureFixture
    ):
        # Arrange
        invalid_s3_event = "invalid"

        # Act
        with caplog.at_level(logging.WARNING):
            handler(
                event=generate_event(event_name=invalid_s3_event),
                context=mock_lambda_context,
            )

        # Assert
        assert f"Unsupported S3 event type: {invalid_s3_event}" in caplog.text
        assert mock_handle_s3_event.call_count == 0

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
        assert "ETL file or path does not match expected format" in caplog.text

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
        assert "ETL file or path does not match expected format" in caplog.text

    @mock.patch(HANDLE_S3_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_s3_event_if_valid_s3_event_type(self, mock_handle_s3_event: mock.Mock):
        # Arrange
        valid_s3_event = "ObjectCreated"

        # Act
        handler(event=generate_event(event_name=valid_s3_event), context=mock_lambda_context)

        # Assert
        assert mock_handle_s3_event.call_count == 1

    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(PUT_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(PUT_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_valid_first_incoming_event(
        self,
        mock_put_load_available_event: mock.Mock,
        mock_get_load_available_event: mock.Mock,
        mock_put_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
    ):
        # Arrange
        rif = RifFileType.BENEFICIARY
        folder = "Incoming"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mock_get_load_available_event.return_value = None

        # Act
        handler(event=generate_event(key=key), context=mock_lambda_context)

        # Assert
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.value.metric_name,
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                value=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                unit=PipelineMetric.TIME_DATA_FIRST_AVAILABLE_REPEATING.value.unit,
                dimensions={},
            ),
        ]
        # pytest doesn't have a built-in assertion helper for ensuring that two lists are the same
        # ignoring order. We could sort the lists, but the MetricData dataclass cannot be ordered
        # as-is due to its inner dict, so checking that both lists are of equal length and that all
        # elements of the expected list are in the actual list is the best workaround
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        actual_put_rif_events = get_mocked_put_rif_avail_events(
            mock_put_rif_available_event=mock_put_rif_available_event
        )
        expected_put_rif_events = [
            RifAvailableEvent(
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif=rif,
            ),
        ]
        assert len(actual_put_rif_events) == len(expected_put_rif_events)
        assert all((expected in actual_put_rif_events) for expected in expected_put_rif_events)

        actual_put_load_events = get_mocked_put_load_avail_events(
            mock_put_load_available_event=mock_put_load_available_event
        )
        expected_put_load_events = [
            LoadAvailableEvent(
                date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
            ),
        ]
        assert len(actual_put_load_events) == len(expected_put_load_events)
        assert all((expected in actual_put_load_events) for expected in expected_put_load_events)

    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(PUT_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(PUT_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_valid_subsequent_incoming_events(
        self,
        mock_put_load_available_event: mock.Mock,
        mock_get_load_available_event: mock.Mock,
        mock_put_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
    ):
        # Arrange
        rif = RifFileType.CARRIER
        event_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=1)
        folder = "Incoming"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mock_get_load_available_event.return_value = LoadAvailableEvent(
            date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
            group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
        )

        # Act
        handler(
            event=generate_event(key=key, event_time_iso=event_time.isoformat()),
            context=mock_lambda_context,
        )

        # Assert
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_AVAILABLE.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_AVAILABLE.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        actual_put_rif_events = get_mocked_put_rif_avail_events(
            mock_put_rif_available_event=mock_put_rif_available_event
        )
        expected_put_rif_events = [
            RifAvailableEvent(
                date_time=event_time,
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif=rif,
            ),
        ]
        assert len(actual_put_rif_events) == len(expected_put_rif_events)
        assert all((expected in actual_put_rif_events) for expected in expected_put_rif_events)

        assert mock_put_load_available_event.call_count == 0

    @mock.patch(IS_INCOMING_FOLDER_EMPTY_PATCH_PATH, autospec=True)
    @mock.patch(IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH, autospec=True)
    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(GET_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_nonfinal_done_event(
        self,
        mock_get_load_available_event: mock.Mock,
        mock_get_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.BENEFICIARY
        event_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=1)
        event_time_delta = round((event_time - DEFAULT_MOCK_EVENT_TIME_DATETIME).total_seconds())
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mock_get_rif_available_event.return_value = RifAvailableEvent(
            date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
            group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
            rif=rif,
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
            " is not complete. Stopping..." in caplog.text
        )

        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=event_time,
                value=utc_timestamp(event_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=event_time,
                value=event_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        assert mock_get_load_available_event.call_count == 0

    @mock.patch(IS_INCOMING_FOLDER_EMPTY_PATCH_PATH, autospec=True)
    @mock.patch(IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH, autospec=True)
    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(GET_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_nonfinal_done_event_with_table_missing_rif_available_event(
        self,
        mock_get_load_available_event: mock.Mock,
        mock_get_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        # Table is intentionally missing event indicating when the DME RIF was made available. This
        # is invalid state, as we should never run into this situation, but _if_ we do then the
        # Lambda should be able to still generate "data loaded" metrics
        mock_get_rif_available_event.return_value = None
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
            f"No corresponding event found for {rif.name} RIF in group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in table {DEFAULT_MOCK_RIF_AVAIL_TBL}; no time delta"
            " metrics can be computed for this RIF. Continuing..." in caplog.text
        )

        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

        assert mock_get_load_available_event.call_count == 0

    @mock.patch(IS_INCOMING_FOLDER_EMPTY_PATCH_PATH, autospec=True)
    @mock.patch(IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH, autospec=True)
    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(GET_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_final_done_event(
        self,
        mock_get_load_available_event: mock.Mock,
        mock_get_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
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
        mock_get_rif_available_event.return_value = RifAvailableEvent(
            date_time=rif_start_time, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR, rif=rif
        )
        mock_get_load_available_event.return_value = LoadAvailableEvent(
            date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR
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
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

    @mock.patch(IS_INCOMING_FOLDER_EMPTY_PATCH_PATH, autospec=True)
    @mock.patch(IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH, autospec=True)
    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(GET_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_final_done_event_with_table_missing_load_available_event(
        self,
        mock_get_load_available_event: mock.Mock,
        mock_get_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_start_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=11)
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        rif_done_time_delta = round((rif_done_time - rif_start_time).total_seconds())
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        mock_get_rif_available_event.return_value = RifAvailableEvent(
            date_time=rif_start_time, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR, rif=rif
        )
        mock_get_load_available_event.return_value = None
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
            f"No event found for group {DEFAULT_MOCK_GROUP_ISO_STR} in table"
            f" {DEFAULT_MOCK_LOAD_AVAIL_TBL}; no time delta metrics can be computed for this data"
            " load" in caplog.text
        )

        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=rif_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_DATA_LOAD_TIME.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.unit,
                dimensions={},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

    @mock.patch(IS_INCOMING_FOLDER_EMPTY_PATCH_PATH, autospec=True)
    @mock.patch(IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH, autospec=True)
    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(GET_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_final_done_event_with_table_missing_rif_available_event(
        self,
        mock_get_load_available_event: mock.Mock,
        mock_get_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
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
        # Table is intentionally missing event indicating when the DME RIF was made available. This
        # is invalid state, as we should never run into this situation, but _if_ we do then the
        # Lambda should be able to still generate "fully loaded" metrics
        mock_get_rif_available_event.return_value = None
        mock_get_load_available_event.return_value = LoadAvailableEvent(
            date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR
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
            f"No corresponding event found for {rif.name} RIF in group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in table {DEFAULT_MOCK_RIF_AVAIL_TBL}; no time delta"
            " metrics can be computed for this RIF. Continuing..." in caplog.text
        )

        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.metric_name,
                date_time=rif_done_time,
                value=load_done_time_delta,
                unit=PipelineMetric.TIME_DELTA_FULL_DATA_LOAD_TIME.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)

    @mock.patch(IS_INCOMING_FOLDER_EMPTY_PATCH_PATH, autospec=True)
    @mock.patch(IS_PIPEILNE_LOAD_COMPLETE_PATCH_PATH, autospec=True)
    @mock.patch(PUT_METRIC_DATA_PATCH_PATH, autospec=True)
    @mock.patch(GET_RIF_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    @mock.patch(GET_LOAD_AVAILABLE_EVENT_PATCH_PATH, autospec=True)
    def test_it_handles_final_done_event_with_table_missing_both_events(
        self,
        mock_get_load_available_event: mock.Mock,
        mock_get_rif_available_event: mock.Mock,
        mock_put_metric_data: mock.Mock,
        mock_load_complete: mock.Mock,
        mock_incoming_empty: mock.Mock,
        caplog: pytest.LogCaptureFixture,
    ):
        # Arrange
        rif = RifFileType.DME
        rif_done_time = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(hours=12)
        folder = "Done"
        key = f"{folder}/{DEFAULT_MOCK_GROUP_ISO_STR}/{rif.value}.txt"
        # Table is intentionally missing _both_ events. This is the worst-case scenario, and should
        # never happen, but we should still see "data loaded" and "fully loaded" metrics be
        # generated for CloudWatch
        mock_get_rif_available_event.return_value = None
        mock_get_load_available_event.return_value = None
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
            f"No event found for group {DEFAULT_MOCK_GROUP_ISO_STR} in table"
            f" {DEFAULT_MOCK_LOAD_AVAIL_TBL}; no time delta metrics can be computed for this data"
            " load" in caplog.text
        )
        assert (
            f"No corresponding event found for {rif.name} RIF in group"
            f" {DEFAULT_MOCK_GROUP_ISO_STR} in table {DEFAULT_MOCK_RIF_AVAIL_TBL}; no time delta"
            " metrics can be computed for this RIF. Continuing..." in caplog.text
        )
        actual_put_metrics = get_mocked_put_metrics(mock_put_metric_data=mock_put_metric_data)
        expected_put_metrics = [
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"data_type": rif.name.lower()},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_LOADED.value.unit,
                dimensions={
                    "data_type": rif.name.lower(),
                    "group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR,
                },
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED.value.unit,
                dimensions={"group_timestamp": DEFAULT_MOCK_GROUP_ISO_STR},
            ),
            MetricData(
                metric_name=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.metric_name,
                date_time=rif_done_time,
                value=utc_timestamp(rif_done_time),
                unit=PipelineMetric.TIME_DATA_FULLY_LOADED_REPEATING.value.unit,
                dimensions={},
            ),
        ]
        assert len(actual_put_metrics) == len(expected_put_metrics)
        assert all((expected in actual_put_metrics) for expected in expected_put_metrics)
