import calendar
from dataclasses import asdict
from datetime import datetime, timedelta, timezone
from typing import Any, Protocol
from unittest import mock

import pytest

from common import RifFileType
from dynamo_db import (
    LoadAvailableEvent,
    LoadAvailableEventItem,
    RifAvailableEvent,
    RifAvailableEventItem,
    get_load_available_event,
    get_rif_available_event,
    put_load_available_event,
    put_rif_available_event,
)

DEFAULT_MOCK_GROUP_ISO_STR = "2024-01-12T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_ISO = "2024-01-19T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_DATETIME = datetime.fromisoformat(
    DEFAULT_MOCK_EVENT_TIME_ISO.removesuffix("Z")
).replace(tzinfo=timezone.utc)
DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP = calendar.timegm(
    DEFAULT_MOCK_EVENT_TIME_DATETIME.utctimetuple()
)
DEFAULT_DYNAMO_EXPIRATION_DATETIME = DEFAULT_MOCK_EVENT_TIME_DATETIME + timedelta(days=7)
DEFAULT_DYNAMO_EXPIRATION_UTC_TIMESTAMP = calendar.timegm(
    DEFAULT_DYNAMO_EXPIRATION_DATETIME.utctimetuple()
)
DEFAULT_RIF_TYPE = RifFileType.BENEFICIARY


class MockedTablePutItem(Protocol):
    def __call__(self, Item: dict[str, Any], *args: Any, **kwargs: dict[str, Any]): ...


def gen_mocked_put_item_side_effect(
    mocked_table_content: list[dict[str, Any]],
):
    def _generated_func(Item: dict[str, Any], *_: Any, **__: dict[str, Any]):
        mocked_table_content.append(Item)

    return _generated_func


@pytest.mark.parametrize(
    "response",
    [{}, {"OtherKey": ""}, {"Item": {}}, {"Item": {"group_iso_str": DEFAULT_MOCK_GROUP_ISO_STR}}],
)
def test_get_rif_available_event_returns_none_for_invalid_responses(response: dict[str, Any]):
    # Arrange
    mock_table = mock.Mock()
    mock_table.get_item.return_value = response

    # Act & Assert
    actual_rif_available_event = get_rif_available_event(
        table=mock_table, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR, rif=DEFAULT_RIF_TYPE
    )
    assert actual_rif_available_event is None


@pytest.mark.parametrize(
    "response",
    [{}, {"OtherKey": ""}, {"Item": {}}, {"Item": {"group_iso_str": DEFAULT_MOCK_GROUP_ISO_STR}}],
)
def test_get_load_available_event_returns_none_for_invalid_responses(response: dict[str, Any]):
    # Arrange
    mock_table = mock.Mock()
    mock_table.get_item.return_value = response

    # Act & Assert
    actual_load_available_event = get_load_available_event(
        table=mock_table, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR
    )
    assert actual_load_available_event is None


def test_get_rif_available_event_returns_valid_event():
    # Arrange
    mock_table = mock.Mock()
    mock_table.get_item.return_value = {
        "Item": asdict(
            RifAvailableEventItem(
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif=DEFAULT_RIF_TYPE.value,
                event_timestamp=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                dynamo_expiration=DEFAULT_DYNAMO_EXPIRATION_UTC_TIMESTAMP,
            )
        )
    }

    # Act & Assert
    actual_rif_available_event = get_rif_available_event(
        table=mock_table, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR, rif=DEFAULT_RIF_TYPE
    )
    expected_rif_available_event = RifAvailableEvent(
        date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
        group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
        rif=DEFAULT_RIF_TYPE,
    )
    assert actual_rif_available_event == expected_rif_available_event


def test_put_rif_available_event_puts_valid_event():
    # Arrange
    mock_table = mock.Mock()
    actual_table_content = []
    mock_table.put_item.side_effect = gen_mocked_put_item_side_effect(
        mocked_table_content=actual_table_content
    )

    # Act
    put_rif_available_event(
        mock_table,
        event=RifAvailableEvent(
            date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
            group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
            rif=DEFAULT_RIF_TYPE,
        ),
    )

    # Assert
    expected_table_content = [
        asdict(
            RifAvailableEventItem(
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                rif=DEFAULT_RIF_TYPE,
                event_timestamp=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                dynamo_expiration=DEFAULT_DYNAMO_EXPIRATION_UTC_TIMESTAMP,
            )
        )
    ]
    assert actual_table_content == expected_table_content


def test_get_load_available_event_returns_valid_event():
    # Arrange
    mock_table = mock.Mock()
    mock_table.get_item.return_value = {
        "Item": asdict(
            LoadAvailableEventItem(
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                event_timestamp=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                dynamo_expiration=DEFAULT_DYNAMO_EXPIRATION_UTC_TIMESTAMP,
            )
        )
    }

    # Act & Assert
    actual_load_available_event = get_load_available_event(
        table=mock_table, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR
    )
    expected_load_available_event = LoadAvailableEvent(
        date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME, group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR
    )
    assert actual_load_available_event == expected_load_available_event


def test_put_load_available_event_puts_valid_event():
    # Arrange
    mock_table = mock.Mock()
    actual_table_content = []
    mock_table.put_item.side_effect = gen_mocked_put_item_side_effect(
        mocked_table_content=actual_table_content
    )

    # Act
    put_load_available_event(
        mock_table,
        event=LoadAvailableEvent(
            date_time=DEFAULT_MOCK_EVENT_TIME_DATETIME,
            group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
        ),
    )

    # Assert
    expected_table_content = [
        asdict(
            LoadAvailableEventItem(
                group_iso_str=DEFAULT_MOCK_GROUP_ISO_STR,
                event_timestamp=DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP,
                dynamo_expiration=DEFAULT_DYNAMO_EXPIRATION_UTC_TIMESTAMP,
            )
        )
    ]
    assert actual_table_content == expected_table_content
