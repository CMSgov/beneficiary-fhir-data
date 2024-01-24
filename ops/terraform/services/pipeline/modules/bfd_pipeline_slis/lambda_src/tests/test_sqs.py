import json
from dataclasses import dataclass
from datetime import datetime
from unittest import mock

from common import RifFileType
from sqs import (
    PipelineLoadEvent,
    PipelineLoadEventMessage,
    PipelineLoadEventType,
    retrieve_load_event_msgs,
)


@dataclass
class MockSqsMessage:
    receipt_handle: str
    body: str


class TestSqsRetrieveLoadMsgs:
    def test_it_retrieves_messages_correctly(self):
        # Arrange
        mock_sqs_messages = [
            MockSqsMessage(
                receipt_handle="1",
                body=json.dumps(
                    {
                        "event_type": "RIF_AVAILABLE",
                        "date_time": 1706097600,
                        "group_iso_str": "2024-01-24T13:00:00Z",
                        "rif_type": "bene",
                    }
                ),
            ),
            MockSqsMessage(
                receipt_handle="2",
                body=json.dumps(
                    {
                        "event_type": "LOAD_AVAILABLE",
                        "date_time": 1706097600,
                        "group_iso_str": "2024-01-24T13:00:00Z",
                        "rif_type": "bene",
                    }
                ),
            ),
        ]
        mock_sqs_queue = mock.Mock()
        mock_sqs_queue.receive_messages.return_value = mock_sqs_messages

        # Act & Assert
        actual_events = retrieve_load_event_msgs(queue=mock_sqs_queue)
        expected_events = [
            PipelineLoadEventMessage(
                receipt_handle="1",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.RIF_AVAILABLE,
                    date_time=datetime.utcfromtimestamp(1706097600),
                    group_iso_str="2024-01-24T13:00:00Z",
                    rif_type=RifFileType.BENEFICIARY,
                ),
            ),
            PipelineLoadEventMessage(
                receipt_handle="2",
                event=PipelineLoadEvent(
                    event_type=PipelineLoadEventType.LOAD_AVAILABLE,
                    date_time=datetime.utcfromtimestamp(1706097600),
                    group_iso_str="2024-01-24T13:00:00Z",
                    rif_type=RifFileType.BENEFICIARY,
                ),
            ),
        ]

        assert len(actual_events) == len(expected_events)
        assert all((expected in actual_events) for expected in expected_events)
